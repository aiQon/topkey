package de.cased.utilities;

public class BasicMessage extends Message {

	String individual_key;
	String group_key;
	String own_cluster_key;
	
	public BasicMessage(String i, String g, String c){
		individual_key = i;
		group_key = g;
		own_cluster_key = c;
	}

	public String getIndividual_key() {
		return individual_key;
	}

	public String getGroup_key() {
		return group_key;
	}

	public String getOwn_cluster_key() {
		return own_cluster_key;
	}
	
	
}
