package de.cased.utilities;

import java.util.HashMap;
import java.util.Map;

import com.mxgraph.model.mxCell;

/**
 * This one stores LEAP Keys:
 * • [done] an individual key shared with the base station
 * • [done] a pairwise shared key with each of its direct neighbors
 * • [TODO] a cluster key for a node and all its neighbors securing mainly routing broadcasts
 * • [done] a group key shared among all sensor nodes and the sink used by the sink for encryption only.
 * 
 * Whereas the individual keys and the group key are stored in 'sink'.
 * 
 * @author stas
 *
 */
public class KeyStore {
	
//	/*
//	 * Stores the pairwise shared Keys. The key of the Set is the edge-object which combines both
//	 * nodes in question. To distinguish one can use:
//	 * 			mxICell edge_source = edge.getTerminal(true);
//	 *			mxICell edge_destination = edge.getTerminal(false);
//	 */
//	private Map<mxICell,Key> pairwiseSharedKeys = new HashMap<mxICell,Key>();
//	
//	public void addpairwiseSharedKey(mxICell edge, Key key){
//		pairwiseSharedKeys.put(edge, key);
//	}
	
	/*
	 * Stores individual keys for the connection of each node and the sink.
	 * And stores the common Group Key for the whole net.
	 */
	private Sink sink = new Sink();

	public Sink getSink_keys() {
		return sink;
	}

	public void setSink_keys(Sink sink_keys) {
		this.sink = sink_keys;
	}
	
	private Map<String,NodeKeyStore> nodes = new HashMap<String,NodeKeyStore>();

	public Map<String, NodeKeyStore> getNodes() {
		return nodes;
	}

	public void addNode(String name, NodeKeyStore keys) {
		this.nodes.put(name, keys);
	}

	public void initNodes(Object[] children) {
		for (Object object : children) {
			mxCell cell = (mxCell) object;
			if(cell.isVertex()){// && !cell.getId().equals("sink")){
				nodes.put(cell.getValue().toString(), new NodeKeyStore());
			}
		}		
	}
	
	
}