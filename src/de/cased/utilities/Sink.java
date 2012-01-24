package de.cased.utilities;

import java.util.HashMap;

public class Sink {
	private HashMap<String,Key> individual_keys = new HashMap<String,Key>();
	private Key group_key;
	
	
	public HashMap<String, Key> getIndividual_keys() {
		return individual_keys;
	}
	public void setIndividual_keys(HashMap<String, Key> individual_keys) {
		this.individual_keys = individual_keys;
	}
	public void addIndividual_keys(String node_name, Key key){
		individual_keys.put(node_name, key);
	}
	
	
	public Key getGroup_key() {
		return group_key;
	}
	public void setGroup_key(Key group_key) {
		this.group_key = group_key;
	}
}
