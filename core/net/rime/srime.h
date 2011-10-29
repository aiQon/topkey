
#ifndef S_RIME_
#define S_RIME_

//#include "ECC.h"
//#include "aes_impl.h"

#include "contiki.h"
#include "net/rime.h"
#include "net/rime/mesh.h"


/*
 * einfach verkettete liste. das letzte element hat einen "next" pointer auf NULL.
 */
struct key_list_node {
	int address; //16 bit int-> first 8 bit = first octet, last 8 bit = last octet
	char *key;
	int key_len;
	struct key_list_node *next;
};

typedef struct key_list_node node;


static const int AES_mask = 0x0001;
static const int ECC_mask = 0x0002;

void init_srime();
void srime_close(struct mesh_conn *c);
void init_fixed_values();
void srime_send(struct mesh_conn *mesh, char *msg, char *rcv);
void set_individual_key(char *in);
void set_own_cluster_key(char *in);
void set_group_key(char *in);

node* insert_into_pairwise(int address, char *key, int key_len);
node* insert_into_cluster(int address, char *key, int key_len);
void persist_keys();
void lock_key_insertion();

void restore_keys();

char *get_individual_key();
char *get_own_cluster_key();
char *get_group_key();
void print_cluster_list();
void print_pairwise_list();
char *get_pairwise_key(rimeaddr_t *neighbor);



#endif
