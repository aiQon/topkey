package de.cased.utilities;

import java.util.ArrayList;

public class ExtendedMessage extends Message {

	ArrayList<String> pairwisers = new ArrayList<String>();
	ArrayList<String> clusters = new ArrayList<String>();
	
	public ExtendedMessage(ArrayList<String> pairwiser, ArrayList<String> cluster) throws TooManyElementsException{
		if(pairwiser.size() + cluster.size() > 5){
			throw new TooManyElementsException();
		}
		
		pairwisers.addAll(pairwiser);
		clusters.addAll(cluster);
	}
	
	public ArrayList<String> getPairwisers(){
		return pairwisers;
	}
	
	public ArrayList<String> getClusters(){
		return clusters;
	}
}
