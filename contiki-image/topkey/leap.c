#include "contiki.h"
#include "shell.h"
#include "serial-shell.h"

#include "dev/watchdog.h"

#include "net/rime.h"
#include "dev/cc2420.h"
#include "dev/leds.h"
#include "dev/light-sensor.h"
#include "dev/battery-sensor.h"
#include "dev/sht11-sensor.h"

#include "lib/checkpoint.h"

#include "net/rime/timesynch.h"

#include <stdio.h>
#include <string.h>
#include <srime.h>

#include <io.h>
#include <signal.h>
#include <ctype.h>
#include <stdlib.h>


#include "lib/list.h"
#include "lib/memb.h"

#include "net/rime.h"
#include "net/rime/mesh.h"

#include "cfs/cfs.h"
#include "cfs/cfs-coffee.h"

#include "lib/crc16.h"

#include "dev/button-sensor.h"

/*
Required for leap key distribution, so the node knows who sent him the keys and who he needs to ACK
*/
rimeaddr_t key_sender;


/*
Simple locking mechanism
*/
static int lock;
static int nack_running;

PROCESS(nack_process, "NACK_sending");
/*---------------------------------------------------------------------------*/
PROCESS(sky_shell_process, "Sky Contiki shell");
AUTOSTART_PROCESSES(&sky_shell_process);
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/

/*
Debug purpose.
*/
void dump_memory(char* data, size_t len)
{
    size_t i;
    printf("Data in [%p..%p): ",data,data+len);
    for (i=0;i<len;i++)
        printf("%02X ", ((unsigned char*)data)[i] );
    printf("\n");
}

/*
encodes a bitstring in a hexstring.
*/
static
char *encode_hex(char *message, int len){

    char *return_value = (char *)malloc(len*2); //double the space plus null byte
    char pseudo[16] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    int i = 0;
    uint8_t ch = 0x00;

    while(i < len){
        ch = (uint8_t) (message[i] & 0xF0);
        ch = ch >> 4;
        ch = ch & 0x0F;
        memcpy(return_value+i*2, &pseudo[ch], 1);
        ch = (uint8_t) (message[i] & 0X0F);
        memcpy(return_value+i*2+1, &pseudo[ch], 1);
        i++;
    }
    return_value[i*2] = 0x00;
    return return_value;
}

/*
decodes a hexstring into a bitstring.
*/
static
char *decode_hex(char *hex, int len)
{
    char *return_value = (char *)malloc(len>>1); //hier stand strlen(len)
	int i;

	for(i=0;i<len;i++){
		int dig =0;
		if(isdigit(hex[i]))
			dig = hex[i] - '0';
		else if(hex[i] >= 'A' && hex[i] <= 'F')
			dig = hex[i] - 'A' + 10;
		return_value[i>>1] = i%2==0 ? (dig*16) & 0xFF : (return_value[i>>1] + dig) & 0xFF;
	}

	return return_value;
}

/*
These are used for leap key exchange.
Keeping track of the sequence numbers received
and the total count of mesages to be received.
*/
static uint16_t sequence_mask = 0;
static uint16_t total_expected = 0;
static uint16_t remaining_counter = 7; //max


static struct unicast_conn uc;


/*
determine whether ACK or NACK should be send and do so.
*/
static void
process_nack(char *message, int len){
    char *encoded = encode_hex(message,len);
    printf("<NACK>%s</NACK>\n", encoded);
    free(encoded);
}

/*
For leap.
send final ack or nack.
*/
static void
send_ack_nack(){
    nack_running = 0;
    static char message[5];  //1+2+2
    memcpy(message, "\x80", 1); //set the header to '10 000 000'
    memcpy(message+1, &sequence_mask, 2);
    static unsigned short crc;
    crc = crc16_data(message, 3, 0xffff);
    memcpy(message+3, &crc, 2);

    if(key_sender.u8[0] != rimeaddr_node_addr.u8[0] && key_sender.u8[1] != rimeaddr_node_addr.u8[1]){
        packetbuf_copyfrom(message,5);
        unicast_send(&uc,&key_sender);
    }else{
        process_nack(message, 5);
    }

    total_expected = 0;
    sequence_mask = 0;
    remaining_counter = 0;
}

/*
processes headers of leap messages
*/
static void
process_header(char *message){

    static uint16_t seq;
    static uint16_t rem;
    static uint16_t start_nack_proc = 0;

    seq = (uint16_t)(((*message)>>3) & 0x7);//extract_seq(message);
    rem = (uint16_t)((*message) & 0x7);//extract_rem(message);

    if(total_expected == 0){  //first start
        start_nack_proc = 1;  //easy sync fix
    }

    total_expected = seq + rem;
    remaining_counter = rem;

    sequence_mask |= 1 << seq;

    if(start_nack_proc && remaining_counter > 0 && nack_running == 1){
    	nack_running = 1;
        process_start(&nack_process, "");
    }else if(remaining_counter == 0){
        send_ack_nack();
    }

}


/*
    gets binary message
     -------- --------------------- ----- -----
    | header | 'A' + Address + Key | ... | crc |
     -------- --------------------- ----- -----
    8 bits    19 Bytes              more    16 bits
              1 Byte prologue       entities
              2 Bytes dest. address of the
              16 Bytes key          prev.
                                    entry

    Prologue can either be a 'A' or 'B' indicating
    pairwise key or cluster key respectively.

    Address is encoded in the same way it is done in
    srime.
*/

static void
parse_extended_message(char *message, int len){

    //printf("[*] reached parse_extended_message\n");

    int pure_payload_len = len - 3; //1 Byte header, 2 Bytes CRC
    int entity_count = pure_payload_len / 19;
    char *first_entity = message+1;
    int i;
    for(i = 0; i < entity_count; i++){
        char prologue_byte = *(first_entity+19*i);
        uint16_t address;
        char *key = (char *) malloc(16);
        memcpy(&address, first_entity+1+19*i,    2);
        memcpy(key,      first_entity+1+19*i+2, 16);

        if(prologue_byte == 0x61){
            insert_into_pairwise(address,key,16);
        }else if(prologue_byte == 0x62){
            insert_into_cluster(address, key, 16);
        }
        else{
            //this should not happen
            //printf("[*] extended message has a prologue byte which was not expected: %c\n", prologue_byte);
        }
        //free(key); dont free it, its needed in srime.c //bad style, sorry
    }
}


/*

    gets the bin message and parses it for the
    3 common keys: ik, gk, ck (in this order)
     ------ -- -- -- ---
    |header|ik|gk|ck|crc|
    ------- -- -- -- ---
    8 bit   128 bits 16 bits
            each

*/
static void
parse_basic_message(char *message, int len){
  	set_individual_key(message+1);
    set_group_key(message+17);
    set_own_cluster_key(message+33);

}


/*
    gets the message in binary representation,
    and interprets the content.
    1111 0000
*/
static void
multiplex_message(char *message, int len){

    uint8_t type = (((uint8_t)*message)>>6) & 0xFF;
    switch(type){ //check first 2 bits of the header
        case 0:
            parse_basic_message(message, len);
            process_header(message);
            break;

        case 1:
            parse_extended_message(message, len);
            process_header(message);
            break;

        case 2:
            process_nack(message, len);
            break;

        case 3:
            //printf("received data1\n");
            break;
    }


}

/*

    gets the hex string.
    cuts off last 4 bytes => decodes their hex value(decodeHex)
    calculates crc of remaining message (in hex string representation!!!).
    should be crc !

*/

static int
verify_crc(char *message, int len){
    static unsigned short calculated;
    calculated = crc16_data(message, len-2, 0xffff);
    static unsigned short extracted;
    memcpy(&extracted, message+len-2, 2);
    return extracted==calculated ? 1 : 0;
}

/*
for leap.
processes a received message in its binary representation
by verifying its crc and multiplex it according to its type.
*/
static void
process_message(char *message, int len){
    if(verify_crc(message, len) == 0){
        return;
    }
    multiplex_message(message, len);
}


/*
for leap.
using non reliable connections so we can manage ack/nack messages on our own.
acks/nacks only sent after last message or after timeout.
*/
static void
recv_uc(struct unicast_conn *c, const rimeaddr_t *from)
{
    if(key_sender.u8[0] == 0 && key_sender.u8[1] ==0){
        key_sender.u8[0] = from->u8[0];
        key_sender.u8[1] = from->u8[1];
    }

    if(lock == 0){
    	lock = 1;
		char *local_buffer = (char *) malloc(packetbuf_datalen());
		memcpy(local_buffer,packetbuf_dataptr(),packetbuf_datalen());

		process_message(local_buffer, packetbuf_datalen());
		//printf("[*] message processed, freeing local_buffer\n");
		free(local_buffer);
		lock = 0;
    }else{
    	//need to check if contiki can work with mutexes/semaphores
    }
}

static const struct unicast_callbacks unicast_callbacks = {recv_uc};

/*---------------------------------------------------------------------------*/
/*------------------------------get rime address-----------------------------*/
PROCESS(shell_sky_getrimeaddress_process, "sky-rime_address");
SHELL_COMMAND(sky_getrimeaddress_command,
	      "sky-getrimeaddress",
	      "sky-getrimeaddress: gives the node's micro IP",
	      &shell_sky_getrimeaddress_process);
/*-----------------------------end of get rime address-----------------------*/


/*------------------------------recceive setup information-----------------------------*/
PROCESS(shell_sky_printkeys_process, "sky-printkeys");
SHELL_COMMAND(sky_printkeys_command,
	      "printkeys",
	      "printkeys: prints the local LEAP keys to console [DEBUG]",
	      &shell_sky_printkeys_process);
/*-----------------------------end of get rime address-----------------------*/



/*------------------------------recceive setup information-----------------------------*/
PROCESS(shell_sky_setup_process, "sky-setup");
SHELL_COMMAND(sky_setup_command,
	      "setup",
	      "setup: sends LEAP configuration information to nodes",
	      &shell_sky_setup_process);
/*-----------------------------end of get rime address-----------------------*/

/*
debug purpose.
printing keys to console.
*/
static void
print_key(char *headline, char *key){
	printf("%s\n", headline);
	int i;
	for(i=0;i<16;i++){
		printf("0x%X ", key[i] & 0xFF);
	}
	printf("\n");
}


/*
    NACK needs to be send if remaining_counter does not change for a certain
    threashold of time.
    Or if remaining_counter reached 0 and the sequence_mask indicates that
    there are packts missing.
    Otherwise send ACK.
*/

PROCESS_THREAD(nack_process, ev, data) {
    static struct etimer et;
    PROCESS_BEGIN();

    etimer_set(&et, 100*remaining_counter);//*2! //TODO look this value upand hardcode, should be 15ms
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));

    if(nack_running > 0)
    	send_ack_nack();

    PROCESS_END();
}


/*
debugging purpose.
printing all leap keys.
*/
PROCESS_THREAD(shell_sky_printkeys_process, ev, data) {
	PROCESS_BEGIN();

		print_key("[*] individual key:", get_individual_key());
		print_key("[*] own cluster key:", get_own_cluster_key());
		print_key("[*] group key:", get_group_key());

		printf("[*] cluster keys:\n");
		print_cluster_list();
		printf("\n");

		printf("[*] pairwise keys:\n");
		print_pairwise_list();
		printf("\n");

	PROCESS_END();
}

/*
for leap.
prints own µIP address. Used by thejava tool.
*/
PROCESS_THREAD(shell_sky_getrimeaddress_process, ev, data)
{
  int i;
  PROCESS_BEGIN();

  for(i=0; i<RIMEADDR_SIZE; i++){
	  printf("%d",rimeaddr_node_addr.u8[i]);
	  if(i < RIMEADDR_SIZE-1) printf(".");
  }
  printf("\n");

  PROCESS_END();
}

/*
for leap.
used by the base station node to receive the messages from
UART/USB and send them to the other nodes.
*/
PROCESS_THREAD(shell_sky_setup_process, ev, data)
{

  PROCESS_BEGIN();

  char *args = (char *) data;

  if(key_sender.u8[0] == 0 && key_sender.u8[1] ==0){
    key_sender.u8[0] = rimeaddr_node_addr.u8[0]; //so we dont send to ourselves
    key_sender.u8[1] = rimeaddr_node_addr.u8[1];
  }
  rimeaddr_t addr;

  char *recv1 = strtok(args,". ");
  addr.u8[0] = atoi(recv1);
  char *recv2 = strtok(NULL, ". ");
  addr.u8[1] = atoi(recv2);
  char *msg = strtok(NULL, ". ");
  char *bin_msg = decode_hex(msg, strlen(msg));
  int bin_len = strlen(msg)/2;

  //dump_memory(bin_msg, bin_len);

  if(!rimeaddr_cmp(&addr, &rimeaddr_node_addr)) {
    packetbuf_copyfrom(bin_msg, bin_len);
  	unicast_send(&uc, &addr);
  } else {
    //printf("not sending messages to myself, processing it\n");
    process_message(bin_msg, bin_len);
  }
  free(bin_msg);


  PROCESS_END();
}
/*---------------------------------------------------------------------------*/

/*
this is the main() method of the contiki task.
it starts here.
*/
PROCESS_THREAD(sky_shell_process, ev, data)
{
  PROCESS_EXITHANDLER(unicast_close(&uc);)
  PROCESS_BEGIN();

  unicast_open(&uc, 144, &unicast_callbacks);
  cfs_coffee_format();
  printf("[*] formatting nvram during startup [DEBUG]\n");

  //this needs so much to be refactored
  key_sender.u8[0] = 0;
  key_sender.u8[1] = 0;
  lock = 0;

  serial_shell_init();
  init_srime();
  restore_keys();  //RIME

  shell_register_command(&sky_getrimeaddress_command);
  shell_register_command(&sky_setup_command);
  shell_register_command(&sky_printkeys_command);

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
