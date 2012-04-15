package de.cased.utilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NodeKeyStore {
	private Key individualKey;
	private Map<String,Key> pairwiseKeys = new HashMap<String, Key>();
	private Map<String,Key> clusterKey = new HashMap<String,Key>();
	private Set<String> neighbors = new HashSet<String>();
	
	
	public Set<String> getNeighbors() {
		return neighbors;
	}
	public Key getIndividualKey() {
		return individualKey;
	}
	public void setIndividualKey(Key individualKey) {
		this.individualKey = individualKey;
	}
	
	public Map<String, Key> getPairwiseKeys() {
		return pairwiseKeys;
	}
	public void addPairwiseKeys(String name, Key key) {
		this.pairwiseKeys.put(name, key);
	}
	
	public Map<String, Key> getClusterKeys() {
		return clusterKey;
	}
	
	public void putClusterKey(String name, Key key) {
		this.clusterKey.put(name, key);
	}
}
