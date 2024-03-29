

//#include "NN.h"
//#include "ECC.h"
#include "net/rime/srime.h"
#include "net/rime/mesh.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "cfs/cfs.h"
#include "cfs/cfs-coffee.h"


char individual_key[16];// = "\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0";
char own_cluster_key[16];// = "\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0";
char group_key[16];// = "\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0\x0";

char *get_individual_key(){
	return individual_key;
}

char *get_own_cluster_key(){
	return own_cluster_key;
}

char *get_group_key(){
	return group_key;
}









char *individual_key_file = "ind_key_file";
char *own_cluster_key_file = "own_c_file";
char *group_key_file = "group_key_file";

char *cluster_file = "cluster_file";
char *pairwise_file = "pair_file";


//0 = allow
//everything else = dont allow new keys
int block_key_insertion=0;

void set_individual_key(char *in){
	if(!block_key_insertion)
		memcpy(individual_key, in, 16);
}

void set_own_cluster_key(char *in){
	if(!block_key_insertion)
		memcpy(own_cluster_key, in, 16);
}

void set_group_key(char *in){
	if(!block_key_insertion)
		memcpy(group_key, in, 16);
}




//node *cluster_list;
//node *pairwise_list;

LIST(cluster_list);
LIST(pairwise_list);

list_t get_pairwise_list(){
    return pairwise_list;
}

void init_srime(){

}

void srime_close(struct mesh_conn *c){
	mesh_close(c);
}

node *create_new_node(int address, char *key, int key_len){
	//printf("[*] entered create_new_node\n");
	node *new_node = (node*) malloc(sizeof(node));
	if(new_node == NULL){
		//printf("[*] failed creating new node: no RAM\n");
		return NULL;		//kein RAM mehr
	}
	//printf("[*] new node created successfully\n");
	new_node->address = address;
	new_node->key=key;
	new_node->key_len=key_len;
	//printf("[*] values assigned\n");
	new_node->next= NULL;
	return new_node;
}

node *search_address_in_list(list_t list, int address){
    node *l = *list;
    while(l != NULL){
        if(l->address == address){
            return l;
        }
        l = l->next;
    }

    return NULL;
}


node *insert_into_list(list_t list, int address, char *key, int key_len){
    node *searchable = search_address_in_list(list, address);
    if(searchable != NULL){
        return NULL;
        //list_remove(list, searchable);
    }
    node *new_node = create_new_node(address, key, key_len);
    list_add(list, new_node);
    return new_node;
}


node* insert_into_pairwise(int address, char *key, int key_len){
	if(!block_key_insertion){
        return insert_into_list(pairwise_list, address, key, key_len);
	}else
		return NULL;
}

node* insert_into_cluster(int address, char *key, int key_len){
	if(!block_key_insertion){
        return insert_into_list(cluster_list, address, key, key_len);
	}else
		return NULL;
}




char *decode_uip(node *element){
	char *ip_address = (char*) malloc(8); //3 letters for first octet, 1 letter for the perios, 3 letters for second octet and 1 letter for the nullbyte=> 3+1+3+1=8
	sprintf(ip_address, "%d.%d", ((element->address)>>8) & 0xff, (element->address) & 0xff);
	return ip_address;
}

int rimeaddr2int(rimeaddr_t *address){
	if(address != NULL){
		int result = address->u8[0] << 8;
		result += address->u8[1];
		return result;
	}else
		return NULL;
}

void print_list(list_t list){
	node *current = *list;
	while(current != NULL){
        //if(current->key_len != 16) //TODO dirty workaround to test for initial semanticless list element
        //    continue;
		char *uip = decode_uip(current);
		printf("{uip:%s,key:",uip);

		int i;
		for(i=0;i<16;i++){
			printf("0x%X ", current->key[i] & 0xFF);
		}

		printf("}\n");


		free(uip);
		current = current->next;
	}
}

char *get_pairwise_key(rimeaddr_t *neighbor){
	node *searching = *pairwise_list; //unsicher mit dem * bei rvalue

	while(searching != NULL){
		if(searching->address == rimeaddr2int(neighbor)){
			printf("found pairwise key for address: %d.%d\n", neighbor->u8[0], neighbor->u8[1]);
			return searching->key;
		}
		searching = searching->next;
	}

	return NULL;
}

void print_cluster_list(){
	print_list(cluster_list);
}

void print_pairwise_list(){
	print_list(pairwise_list);
}

/*
int save_key(char *key, int key_size, char *file_name){
	int fd_write;
	int n=0;


	cfs_remove(file_name);

	fd_write = cfs_open(file_name, CFS_WRITE);
	if(fd_write != -1) {
	  n = cfs_write(fd_write, key, key_size);
	  cfs_close(fd_write);
	  printf("Successfully saved to %s. Wrote %i bytes\n", file_name,n);

	} else {
	  printf("ERROR: could not write to memory, saving to %s failed.\n", file_name);
	}
	return n;
}
*/

/*
void save_address_key_pairs(char *file, node *list){
	int fd_write = cfs_open(file, CFS_WRITE);
	if(fd_write != -1){
		node *current_node = list;
		while(current_node != NULL){
			cfs_write(fd_write,&(current_node->address), sizeof(int)); //2 Byte = sizeof(int) auf telosB
			cfs_write(fd_write,current_node->key, 16);
			current_node = current_node->next;
		}
		cfs_close(fd_write);
	}
}
*/

/*
void persist_keys(){
	if(!block_key_insertion){
		cfs_coffee_format();
		save_key(individual_key, 16, individual_key_file);
		save_key(own_cluster_key,16, own_cluster_key_file);
		save_key(group_key, 16, group_key_file);
		//save_cluster_keys();
		save_address_key_pairs(cluster_file, cluster_list);
		save_address_key_pairs(pairwise_file, pairwise_list);
	}

}
*/

/*
int is_key_available(char *key_file){
	int handle = cfs_open(key_file,CFS_READ);
	if(handle == -1)
		return 0;
	cfs_close(handle);

	return 1;
}
*/

/*
int check_key_existance(){
	return  is_key_available(individual_key_file) &&
			is_key_available(own_cluster_key_file) &&
			is_key_available(group_key_file);
}
*/

/*
int restore_key(char *key, int key_size, char *file){
	key = (char *)malloc(key_size);
	int handle = cfs_open(file,CFS_READ);
	if(handle != -1){
		int read_bytes = cfs_read(handle,key, key_size);
		cfs_close(handle);
		printf("[*] restored key from %s\n", file);
		return read_bytes;
	}
	return 0;
}
*/

/*
void restore_cluster(){
	int fd_read = cfs_open(cluster_file, CFS_READ);
	if(fd_read != -1){
		//printf("[*] restoring cluster keys...\n");
		int read_bytes=0;
		char buff[18];
		int address;
		char key[16];
		while((read_bytes=cfs_read(fd_read,buff,18))>0){
			memcpy(&address,buff,2);
			memcpy(key,buff,16);
			node *element = insert_into_cluster(address,key,16);
			char *uip = decode_uip(element);
			//printf("[*] read cluster key for %s\n", uip);
			free(uip);
			//printf("[*] read key: ");
		}
	}else{
		printf("[*] no keys available to be restored\n");
	}
}
*/

void restore_keys(){
/*
    if(check_key_existance()){
		restore_key(individual_key, 16, individual_key_file);
		restore_key(own_cluster_key, 16, own_cluster_key_file);
		restore_key(group_key, 16, group_key_file);

		//checke es hier, weil es nicht immer da sein muss
		//=> wenn nur ein node im netzwerk ist, gibt es keine
		//foreign cluster keys
		//individual, own_cluster ud group_key gibts aber immer
		if(is_key_available(cluster_file)){
			restore_cluster();
		}else{
			//printf("[*] no cluster keys available\n");
		}


		block_key_insertion=1;
		printf("SRIME: keys restored.\n");
	}else{
		block_key_insertion=0;
		printf("SRIME: no keys found.\n");
	}
    */
}


void lock_key_insertion(){
	block_key_insertion=1;
}









/*
int find_rcv_index(char *rcv){
	printf("find_rcv_index()");
	int i;
	for(i = 0; i < MAX_CONTACTS; i++){
		if(contacts[i].id == 0)
			break;
		if(strcmp(contacts[i].id, rcv) == 0){
			printf("found ");
			return i;
		}
	}
	return -1;
}
*/

void srime_send(struct mesh_conn *mesh, char *msg, char *rcv){

//	int i;
//	for(i = 0; i < MAX_CONTACTS; i++){
//		if(contacts[i].id == 0)
//			break;
//		if(strcmp(contacts[i].id, rcv) == 0){
//			printf("found\n");
//			packetbuf_copyfrom(msg, strlen(msg));
//			printf("copied msg:'%s' with length %d to buffer\n", msg, strlen(msg));
//			mesh_send(mesh, &contacts[i].addr); //TODO eventuel & entfernen OO
//			printf("sent to %d.%d\n", contacts[i].addr.u8[0],contacts[i].addr.u8[1]);
//		}
//	}

}
