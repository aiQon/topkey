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

#define DEBUG_SNIFFERS 0
#define MAX_RETRANSMISSIONS 4
#define NUM_HISTORY_ENTRIES 4

rimeaddr_t key_sender;

PROCESS(nack_process, "NACK_sending");
/*---------------------------------------------------------------------------*/
PROCESS(sky_shell_process, "Sky Contiki shell");
AUTOSTART_PROCESSES(&sky_shell_process);
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* OPTIONAL: Sender history.
 * Detects duplicate callbacks at receiving nodes.
 * Duplicates appear when ack messages are lost. */
/*
struct history_entry {
  struct history_entry *next;
  rimeaddr_t addr;
  uint8_t seq;
};

LIST(history_table);
MEMB(history_mem, struct history_entry, NUM_HISTORY_ENTRIES);
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


static uint16_t sequence_mask = 0;
static uint16_t total_expected = 0;
static uint16_t remaining_counter = 7; //max


static struct unicast_conn uc;
/*
    determine whether ACK or NACK should be send and do so.
*/

static void
send_ack_nack(){
    
    process_exit(&nack_process);
    static char message[5];  //1+2+2
    memcpy(message, "\x80", 1); //set the header to '10 000 000'
    //static unsigned short swapped_seq;
    //swapped_seq = ((sequence_mask >>8) & 0xFF) + ((sequence_mask << 8) & 0xFF00);
    memcpy(message+1, &sequence_mask, 2);
    static unsigned short crc;
    crc = crc16_data(message, 3, 0xffff);
    //static unsigned short swapped_crc;
    //swapped_crc = ((crc >> 8) & 0xFF) + ((crc << 8) & 0xFF00);
    memcpy(message+3, &crc, 2);

    packetbuf_copyfrom(message,5);
    unicast_send(&uc,&key_sender);
    free(message);
    
}




/*
static
int extract_address(char *input){
	char *address_key_separator = strchr(input,':');
	int length_of_address = address_key_separator-input;
	char *address_work = (char*) malloc(length_of_address+1);
	memcpy(address_work,input,length_of_address);
	char *address_sep = strchr(address_work,'.');
	address_work[address_sep-address_work] = 0x00;
	address_work[length_of_address] = 0x00;
	int return_value = (atoi(address_work)<<8)+atoi(address_sep+1);
	free(address_work);
	return return_value;
}
*/

/*
static
char *extract_key(char *input){
	char *address_key_separator = strchr(input,':');
	//printf("extracted key pointer=%s\n",address_key_separator+1);
	return address_key_separator+1;
}
*/

/*
static void
recv_ack(struct runicast_conn *c, const rimeaddr_t *from, uint8_t seqno)
{
	printf("runicast: got ACK from %d.%d, seqno %d\n",
		     from->u8[0], from->u8[1],
		     packetbuf_attr(PACKETBUF_ATTR_PACKET_ID));
}
*/
/*
static int
extract_seq(char *message){
    return (int)(((*message)>>3) & 0xFF);
}

static int
extract_rem(char *message){
    return (int)((*message) & 0xFF);
}
*/

/*

    gets the message in binary representation and
    parses the header...
    proceses sequence number, triggers timer,
    schedules NACK transmissions etc...

*/

static void
process_header(char *message){

    static uint16_t seq;
    static uint16_t rem;
    static uint16_t start_nack_proc = 0;
    
    printf("[*] reached process_header()\n");
    
    seq = (int)(((*message)>>3) & 0x7);//extract_seq(message);
    
    rem = (int)((*message) & 0x7);//extract_rem(message);

    printf("[*] extracted header infos= seq:%d,rem=%d\n", seq, rem);

    if(total_expected == 0){  //first start
        start_nack_proc = 1;  //easy sync fix
    }

    total_expected = seq + rem;
    remaining_counter = rem;

    sequence_mask |= 1 << seq;

    if(start_nack_proc && remaining_counter > 0){
 //       process_start(&nack_process, "");
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
    
    printf("[*] reached parse_extended_message\n");

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
        
        if(prologue_byte == 'a'){
            printf("[*] inserting pairwise key\n");
            insert_into_pairwise(address,key,16);
        }else if(prologue_byte == 'b'){
            printf("[*] inserting cluster key\n");
            insert_into_cluster(address, key, 16);
        }
        else{
            //this should not happen
            printf("[*] extended message has a prologue byte which was not expected: %c\n", prologue_byte);
        }
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
process_nack(char *message, int len){
    
    printf("<NACK>%s</NACK>\n", encode_hex(message,len));
}

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

    printf("[*] reached multiplex_message()\n");
    dump_memory(message, len);
    uint8_t type = (((uint8_t)*message)>>6) & 0xFF;
    printf("[*] message type is:%d\n", type);
    switch(type){ //check first 2 bits of the header
        case 0:
            printf("received basic message\n");
            parse_basic_message(message, len);
            process_header(message);
            break;

        case 1:
            printf("received extended message\n");
            parse_extended_message(message, len);
            process_header(message);
            break;

        case 2:
            printf("received NACK\n");
            process_nack(message, len);
            break;

        case 3:
            printf("received data1\n");
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

    printf("[*] reached verify_crc(), message length is:%d\n", len);

    static unsigned short calculated;
    calculated = crc16_data(message, len-2, 0xffff);
    printf("[*] calculated crc=%d\n", calculated);
    dump_memory(&calculated, 2);

/*
    static char *crc_expected_tmp;
    crc_expected_tmp = message+len-2;
    static unsigned short ext;
    ext = ((*crc_expected_tmp)<<8) + (*(crc_expected_tmp+1));
    printf("[*] expected crc=%d\n", ext);
    dump_memory(&ext, 2);
    printf("[*] CRC raw extract:\n");
    dump_memory(crc_expected_tmp, 2);
*/
    
    static unsigned short extracted;
    memcpy(&extracted, message+len-2, 2);
    printf("[*] extracted CRC in uint16t:%d\n", extracted);
    printf("[*] extracted CRC in raw:\n");
    dump_memory(message+len-2, 2);

    return extracted==calculated ? 1 : 0;
}

static void
process_message(char *message, int len){
    printf("[*] reached process_message()\n");

    dump_memory(message, len);

    if(verify_crc(message, len) == 0){
        printf("[*] packet rejected due to crc error\n");
        return;
    }
  
    printf("[*] message in binary:\n");
    dump_memory(message, len);

    multiplex_message(message, len);
}

static void
recv_uc(struct unicast_conn *c, const rimeaddr_t *from)
{
  printf("runicast message received from %d.%d\n",
	 from->u8[0], from->u8[1]);
    
    key_sender.u8[0] = from->u8[0];
    key_sender.u8[1] = from->u8[1];

    char *local_buffer = (char *) malloc(packetbuf_datalen());
    memcpy(local_buffer,packetbuf_dataptr(),packetbuf_datalen());

    process_message(local_buffer, packetbuf_datalen());
    printf("[*] message processed, freeing local_buffer\n");    
    free(local_buffer);
}

static const struct unicast_callbacks unicast_callbacks = {recv_uc};


/*
PROCESS(shell_sky_alldata_process, "sky-alldata");
SHELL_COMMAND(sky_alldata_command,
	      "sky-alldata",
	      "sky-alldata: sensor data, power consumption, network stats",
	      &shell_sky_alldata_process);
*/
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
#define MAX(a, b) ((a) > (b)? (a): (b))
#define MIN(a, b) ((a) < (b)? (a): (b))
struct spectrum {
  int channel[16];
};
#define NUM_SAMPLES 4
static struct spectrum rssi_samples[NUM_SAMPLES];
*/

/*
static int
do_rssi(void)
{
  static int sample;
  int channel;
  
  rime_mac->off(0);

  cc2420_on();
  for(channel = 11; channel <= 26; ++channel) {
    cc2420_set_channel(channel);
    rssi_samples[sample].channel[channel - 11] = cc2420_rssi() + 53;
  }
  
  rime_mac->on();
  
  sample = (sample + 1) % NUM_SAMPLES;

  {
    int channel, tot;
    tot = 0;
    for(channel = 0; channel < 16; ++channel) {
      int max = -256;
      int i;
      for(i = 0; i < NUM_SAMPLES; ++i) {
	max = MAX(max, rssi_samples[i].channel[channel]);
      }
      tot += max / 20;
    }
    return tot;
  }
}
*/
/*---------------------------------------------------------------------------*/
/*
struct sky_alldata_msg {
  uint16_t len;
  uint16_t clock;
  uint16_t timesynch_time;
  uint16_t light1;
  uint16_t light2;
  uint16_t temp;
  uint16_t humidity;
  uint16_t rssi;
  uint16_t cpu;
  uint16_t lpm;
  uint16_t transmit;
  uint16_t listen;
  rimeaddr_t best_neighbor;
  uint16_t best_neighbor_etx;
  uint16_t best_neighbor_rtmetric;
  uint16_t battery_voltage;
  uint16_t battery_indicator;
};
*/


void print_key(char *headline, char *key){
	printf("%s\n", headline);
	int i;
	for(i=0;i<16;i++){
		printf("0x%X ", key[i] & 0xFF);
	}
	printf("\n");
}


/*

static uint16_t sequence_mask = 0;
static uint16_t total_expected = 0;
static uint16_t remaining_counter = 0;
*/

/*
    NACK needs to be send if remaining_counter does not change for a certain
    threashold of time.
    Or if remaining_counter reached 0 and the sequence_mask indicates that
    there are packts missing.
    Otherwise send ACK.
*/

PROCESS_THREAD(nack_process, ev, data) {
	static struct etimer et;
    uint16_t remaining_save;
    PROCESS_BEGIN();
    
    do{
        if(remaining_counter == 0)
            break;

        remaining_save = remaining_counter;
        
        //maybe increase this value...needs testing
        etimer_set(&et, CLOCK_SECOND/128*2); //TODO look this value upand hardcode, should be 15ms
        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));
        //etimer_reset(&et);

        //if no update came in, send ACK/NACK (break out of the loop)
        if(remaining_save == remaining_counter){
            break;
        }

    }while(remaining_save != remaining_counter);
    
    send_ack_nack();

    PROCESS_END();
}

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

		printf("DEBUGGING :D ab hier baustelle\n");
		printf("[*] trying to get key for 97.14\n");
		rimeaddr_t node;
		node.u8[0] = 97;
		node.u8[1] = 14;
		char *key = get_pairwise_key(&node);
		if(key != NULL)
			print_key("[*] key for 97.14:", key);
		else
			printf("key not found.\n");

	PROCESS_END();
}



/*---------------------------------------------------------------------------*/
/*
PROCESS_THREAD(shell_sky_alldata_process, ev, data)
{
  static unsigned long last_cpu, last_lpm, last_transmit, last_listen;
  unsigned long cpu, lpm, transmit, listen;
  struct sky_alldata_msg msg;
  struct neighbor *n;
  PROCESS_BEGIN();


  SENSORS_ACTIVATE(light_sensor);
  SENSORS_ACTIVATE(battery_sensor);
  SENSORS_ACTIVATE(sht11_sensor);
  
  msg.len = sizeof(struct sky_alldata_msg) / sizeof(uint16_t);
  msg.clock = clock_time();
//  msg.timesynch_time = timesynch_time();
  msg.light1 = light_sensor.value(LIGHT_SENSOR_PHOTOSYNTHETIC);
  msg.light2 = light_sensor.value(LIGHT_SENSOR_TOTAL_SOLAR);
  msg.temp = sht11_sensor.value(SHT11_SENSOR_TEMP);
  msg.humidity = sht11_sensor.value(SHT11_SENSOR_HUMIDITY);
  msg.rssi = do_rssi();
  
  energest_flush();
  
  cpu = energest_type_time(ENERGEST_TYPE_CPU) - last_cpu;
  lpm = energest_type_time(ENERGEST_TYPE_LPM) - last_lpm;
  transmit = energest_type_time(ENERGEST_TYPE_TRANSMIT) - last_transmit;
  listen = energest_type_time(ENERGEST_TYPE_LISTEN) - last_listen;

  while(cpu >= 65536ul || lpm >= 65536ul ||
	transmit >= 65536ul || listen >= 65536ul) {
    cpu /= 2;
    lpm /= 2;
    transmit /= 2;
    listen /= 2;
  }
  
  msg.cpu = cpu;
  msg.lpm = lpm;
  msg.transmit = transmit;
  msg.listen = listen;

  last_cpu = energest_type_time(ENERGEST_TYPE_CPU);
  last_lpm = energest_type_time(ENERGEST_TYPE_LPM);
  last_transmit = energest_type_time(ENERGEST_TYPE_TRANSMIT);
  last_listen = energest_type_time(ENERGEST_TYPE_LISTEN);

  rimeaddr_copy(&msg.best_neighbor, &rimeaddr_null);
  msg.best_neighbor_etx =
    msg.best_neighbor_rtmetric = 0;
//  n = neighbor_best();
  if(n != NULL) {
//    rimeaddr_copy(&msg.best_neighbor, &n->addr);
//    msg.best_neighbor_etx = neighbor_etx(n);
//    msg.best_neighbor_rtmetric = n->rtmetric;
  }
  msg.battery_voltage = battery_sensor.value(0);
  msg.battery_indicator = sht11_sensor.value(SHT11_SENSOR_BATTERY_INDICATOR);
  shell_output(&sky_alldata_command, &msg, sizeof(msg), "", 0);


  SENSORS_DEACTIVATE(light_sensor);
  SENSORS_DEACTIVATE(battery_sensor);
  SENSORS_DEACTIVATE(sht11_sensor);

  PROCESS_END();
}
*/
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
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
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/



PROCESS_THREAD(shell_sky_setup_process, ev, data)
{

  PROCESS_BEGIN();

  char *args = (char *) data;

  printf("[*] setup %s\n", args);

  rimeaddr_t addr;

  char *recv1 = strtok(args,". ");
  addr.u8[0] = atoi(recv1);
  char *recv2 = strtok(NULL, ". ");
  addr.u8[1] = atoi(recv2);
  char *msg = strtok(NULL, ". ");
  char *bin_msg = decode_hex(msg, strlen(msg));
  int bin_len = strlen(msg)/2;

  dump_memory(bin_msg, bin_len);

  if(!rimeaddr_cmp(&addr, &rimeaddr_node_addr)) {
    packetbuf_copyfrom(bin_msg, bin_len);
  	unicast_send(&uc, &addr);
  } else {
    printf("not sending messages to myself, processing it\n");
    process_message(bin_msg, bin_len);
  }
  free(bin_msg);


  PROCESS_END();
}
/*---------------------------------------------------------------------------*/




PROCESS_THREAD(sky_shell_process, ev, data)
{
  PROCESS_EXITHANDLER(unicast_close(&uc);)
  PROCESS_BEGIN();

  unicast_open(&uc, 144, &unicast_callbacks);

  cfs_coffee_format();
  printf("[*] formatting nvram during startup [DEBUG]\n");

  serial_shell_init();
  init_srime();
  restore_keys();  //RIME

  shell_register_command(&sky_getrimeaddress_command);
  shell_register_command(&sky_setup_command);
  shell_register_command(&sky_printkeys_command);

  
  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
