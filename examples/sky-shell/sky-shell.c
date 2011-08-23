/*
 * Copyright (c) 2008, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of the Contiki operating system.
 *
 * $Id: sky-shell.c,v 1.17 2010/02/15 17:47:01 adamdunkels Exp $
 */

/**
 * \file
 *         Tmote Sky-specific Contiki shell
 * \author
 *         Adam Dunkels <adam@sics.se>
 */

#include "contiki.h"
#include "shell.h"
#include "serial-shell.h"

//#include "net/rime/neighbor.h"
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
#include<ctype.h>
#include <stdlib.h>


#include "lib/list.h"
#include "lib/memb.h"

#include "net/rime.h"

#include "cfs/cfs.h"
#include "cfs/cfs-coffee.h"

#include "dev/button-sensor.h"

#define DEBUG_SNIFFERS 0
#define MAX_RETRANSMISSIONS 4
#define NUM_HISTORY_ENTRIES 4

/*---------------------------------------------------------------------------*/
PROCESS(sky_shell_process, "Sky Contiki shell");
AUTOSTART_PROCESSES(&sky_shell_process);
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* OPTIONAL: Sender history.
 * Detects duplicate callbacks at receiving nodes.
 * Duplicates appear when ack messages are lost. */
struct history_entry {
  struct history_entry *next;
  rimeaddr_t addr;
  uint8_t seq;
};

LIST(history_table);
MEMB(history_mem, struct history_entry, NUM_HISTORY_ENTRIES);


static
char *decodeHex(char *hexkey){
	char *return_value = (char *)malloc(strlen(hexkey)>>1);
	int i;

	for(i=0;i<strlen(hexkey);i++){
		int dig =0;
		if(isdigit(hexkey[i]))
			dig = hexkey[i] - '0';
		else if(hexkey[i] >= 'A' && hexkey[i] <= 'F')
			dig = hexkey[i] - 'A' + 10;
		return_value[i>>1] = i%2==0 ? (dig*16) & 0xFF : (return_value[i>>1] + dig) & 0xFF;
	}

	return return_value;
}

static
int extract_address(char *input){
	char *address_key_separator = strchr(input,':');
	int length_of_address = address_key_separator-input;
	char *address_work = (char*) malloc(length_of_address+1);
	memcpy(address_work,input,length_of_address);
	char *address_sep = strchr(address_work,'.');
	address_work[address_sep-address_work] = 0x00;
	int return_value = (atoi(address_work)<<8)+atoi(address_sep+1);  //TODO hier ansetzen und atoi selber schreiben :( oder gleich binary übertragen
	free(address_work);
	return return_value;
}

static
char *extract_key(char *input){
	char *address_key_separator = strchr(input,':');
	printf("extracted key pointer=%s\n",address_key_separator+1);
	return address_key_separator+1;
}


static void
recv_ack(struct runicast_conn *c, const rimeaddr_t *from, uint8_t seqno)
{
	printf("runicast: got ACK from %d.%d, seqno %d\n",
		     from->u8[0], from->u8[1],
		     packetbuf_attr(PACKETBUF_ATTR_PACKET_ID));
}


static void
recv_runicast(struct runicast_conn *c, const rimeaddr_t *from, uint8_t seqno)
{
  /* OPTIONAL: Sender history */
  struct history_entry *e = NULL;
  for(e = list_head(history_table); e != NULL; e = e->next) {
    if(rimeaddr_cmp(&e->addr, from)) {
      break;
    }
  }
  if(e == NULL) {
    /* Create new history entry */
    e = memb_alloc(&history_mem);
    if(e == NULL) {
      e = list_chop(history_table); /* Remove oldest at full history */
    }
    rimeaddr_copy(&e->addr, from);
    e->seq = seqno;
    list_push(history_table, e);
  } else {
    /* Detect duplicate callback */
    if(e->seq == seqno) {
      printf("runicast message received from %d.%d, seqno %d (DUPLICATE)\n",
	     from->u8[0], from->u8[1], seqno);
      return;
    }
    /* Update existing history entry */
    e->seq = seqno;
  }

  printf("runicast message received from %d.%d, seqno %d\n",
	 from->u8[0], from->u8[1], seqno);


  //eventuell die daten in den lokalen puffer hier kopieren,
    //bin mir unsicher, ob zu schnelles empfangen
    //den packetbuffer überschreiben kann, bevor er richtig verarbeitet wurde

    char *local_buffer = (char *) malloc(packetbuf_datalen()+1);//+1 wegen 0x00 für printf
    memcpy(local_buffer,packetbuf_dataptr(),packetbuf_datalen());
    local_buffer[packetbuf_datalen()] = '\x00';

    printf("[*] message received:%s\n",local_buffer);
    printf("[*] start interpretation:\n");

    //key pointer to be used
    char *key_pointer;



    /*
     * -i individual key
     * -o own cluster key
     * -g group key
     * -p pairwise key
     * -c foreign cluster key
     * -r done, command to save the so far received keys and prohibit new key insertion
     */
    if((key_pointer = strstr(local_buffer,"-i")) != NULL){
  	  char *key = key_pointer+3; //to skip to the actual key
  	  char *decoded = decodeHex(key);
  	  set_individual_key(decoded);
  	  printf("decoded individual:");
  	  int j;
  	  for(j=0;j<16;j++){
  	  	printf("0x%.2X ", decoded[j] & 0xFF);
  	  }
  	  printf("\n");
    }else if((key_pointer = strstr(local_buffer,"-o")) != NULL){
  	  char *key = key_pointer+3; //to skip to the actual key
  	  char *decoded = decodeHex(key);
  	  set_own_cluster_key(decoded);
  	  printf("decoded own cluster key:");
  	  int j;
  	  for(j=0;j<16;j++){
  	  	printf("0x%.2X ", decoded[j] & 0xFF);
  	  }
    }else if((key_pointer = strstr(local_buffer,"-g")) != NULL){
    	  char *key = key_pointer+3; //to skip to the actual key
    	  char *decoded = decodeHex(key);
    	  set_group_key(decoded);
    	  printf("decoded group key:");
    	  int j;
    	  for(j=0;j<16;j++){
    	  	printf("0x%.2X ", decoded[j] & 0xFF);
    	  }

    }else if((key_pointer = strstr(local_buffer,"-p")) != NULL){
  	  char *interest = key_pointer+3; //to skip to the actual message

  	  int address = extract_address(interest);
  	  printf("return_address=%d\n", address);
  	  char *key = decodeHex(extract_key(interest));
  	  insert_into_pairwise(address,key,16);
  	  printf("decoded pairwise key:");
  	  int j;
  	  for(j=0;j<16;j++){
  	  	printf("0x%.2X ", key[j] & 0xFF);
  	  }
    }else if((key_pointer = strstr(local_buffer,"-c")) != NULL){
  	  char *interest = key_pointer+3; //to skip to the actual message
  	  int address = extract_address(interest);
  	  printf("return_address=%d\n", address);
  	  char *key = decodeHex(extract_key(interest)); //interest looks like "123.123:87688afb86ab"
  	  insert_into_cluster(address,key,16);
  	  printf("decoded cluster key:");
  	  int j;
  	  for(j=0;j<16;j++){
  	  	printf("0x%.2X ", key[j] & 0xFF);
  	  }
    }else if((key_pointer = strstr(local_buffer,"-r")) != NULL){
  	  persist_keys();
  	  lock_key_insertion();
  	  //we are done inserting keys into this node
    }

    free(local_buffer);



}
static void
sent_runicast(struct runicast_conn *c, const rimeaddr_t *to, uint8_t retransmissions)
{
  //printf("runicast message sent to %d.%d, retransmissions %d\n",
  //	 to->u8[0], to->u8[1], retransmissions);
}
static void
timedout_runicast(struct runicast_conn *c, const rimeaddr_t *to, uint8_t retransmissions)
{
  printf("runicast message timed out when sending to %d.%d, retransmissions %d\n",
	 to->u8[0], to->u8[1], retransmissions);
}
static const struct runicast_callbacks runicast_callbacks = {recv_runicast,
								 recv_ack,
							     sent_runicast,
							     timedout_runicast};

static struct runicast_conn runicast;


PROCESS(shell_sky_alldata_process, "sky-alldata");
SHELL_COMMAND(sky_alldata_command,
	      "sky-alldata",
	      "sky-alldata: sensor data, power consumption, network stats",
	      &shell_sky_alldata_process);
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



#define MAX(a, b) ((a) > (b)? (a): (b))
#define MIN(a, b) ((a) < (b)? (a): (b))
struct spectrum {
  int channel[16];
};
#define NUM_SAMPLES 4
static struct spectrum rssi_samples[NUM_SAMPLES];
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
/*---------------------------------------------------------------------------*/
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

void print_key(char *headline, char *key){
	printf("%s\n", headline);
	int i;
	for(i=0;i<16;i++){
		printf("0x%X ", key[i] & 0xFF);
	}
	printf("\n");
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
		print_key("[*] key for 97.14:", key);

	PROCESS_END();
}



/*---------------------------------------------------------------------------*/
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

  /* Make sure that the values are within 16 bits. */
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

  PROCESS_EXITHANDLER(runicast_close(&runicast);)
  PROCESS_BEGIN();

  //unicast_open(&uc, 146, &unicast_callbacks);

  char *args = (char *) data;

  //printf("received setup call with params: %s\n", args);

  //-d = destination
  //-o = ownCLusterKey
  //-c = foreignClusterKey(several)
  //-g = groupKey
  //-i = individualKey
  //-p = pairwiseKey(several)
  // -r done



  if(!runicast_is_transmitting(&runicast)) {
    rimeaddr_t addr;

    char *message = (char *) malloc(6+strlen(args));
    memcpy(message,"setup \x00", 7);
    strcat(message,args);

    packetbuf_copyfrom(message, strlen(message));
    free(message);

    char *recv1 = strtok(args+3,". ");
    addr.u8[0] = atoi(recv1);
    char *recv2 = strtok(NULL, ". ");
    addr.u8[1] = atoi(recv2);
    if(!rimeaddr_cmp(&addr, &rimeaddr_node_addr)) {
    	runicast_send(&runicast, &addr, MAX_RETRANSMISSIONS);
    } else
     printf("not sending messages to myself\n");
  }




  PROCESS_END();
}
/*---------------------------------------------------------------------------*/




PROCESS_THREAD(sky_shell_process, ev, data)
{
  PROCESS_EXITHANDLER(runicast_close(&runicast);)
  PROCESS_BEGIN();

  runicast_open(&runicast, 144, &runicast_callbacks);

  cfs_coffee_format();
  printf("[*] formatting nvram during startup [DEBUG]\n");

  serial_shell_init();
  shell_blink_init();
  shell_rime_netcmd_init();
  /*  shell_rime_ping_init();
  shell_rime_debug_init();
  shell_rime_debug_runicast_init();*/
  /*  shell_rime_sniff_init();*/
  shell_sky_init();
  shell_power_init();
  shell_powertrace_init();
  /*  shell_base64_init();*/
  shell_text_init();
  shell_time_init();
  restore_keys();  //RIME

  shell_register_command(&sky_getrimeaddress_command);
  shell_register_command(&sky_setup_command);
  shell_register_command(&sky_printkeys_command);

  
  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
