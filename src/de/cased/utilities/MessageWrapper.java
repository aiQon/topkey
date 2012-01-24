package de.cased.utilities;

import java.util.ArrayList;
import java.util.BitSet;


public class MessageWrapper {
	
	String destination;	
	ArrayList<Message> messages = new ArrayList<Message>();
//	ArrayList<Integer> misses = new ArrayList<Integer>();
	
	
	
	public MessageWrapper(String in, String individual_Key, String groupKey, String own_cluster_key, 
			String[] pairwiseKeys, String[] cluster_keys) throws UnbalancedPairwiseClusterKeysException{
		
		if(pairwiseKeys.length != cluster_keys.length)
			throw new UnbalancedPairwiseClusterKeysException();
		
		System.out.println("generating a messageWrapper");
		destination = in;
		
		messages.add(new BasicMessage(individual_Key, groupKey, own_cluster_key));
		System.out.println("build a BasicMessage");
		
		int pair_counter = 0;
		int cluster_counter = 0;
		
		
		while(pair_counter < pairwiseKeys.length | cluster_counter < cluster_keys.length){
			
			System.out.println("building an ExtendedMessage");
			
			int freeSlots = 5;
			ArrayList<String> pairwise = new ArrayList<String>();
			ArrayList<String> cluster = new ArrayList<String>();
			
			for(;freeSlots > 0 && pair_counter < pairwiseKeys.length; pair_counter++, freeSlots--){
				System.out.println("added pairwise key to extended message");
				pairwise.add(pairwiseKeys[pair_counter]);
			}
			for(;freeSlots > 0 && cluster_counter < cluster_keys.length; cluster_counter++, freeSlots--){
				System.out.println("added cluster key to extended message");
				cluster.add(cluster_keys[cluster_counter]);
			}
			
			
			try{
				messages.add(new ExtendedMessage(pairwise, cluster));
				System.out.println("added an extended Message");
			} catch(TooManyElementsException e){
				System.out.println("[*] tried to insert too many elements into " +
						"message object, this should not happen");
			}
		}
		
		
	}
	
	public void adjustMisses(ArrayList<Integer> misses){
		System.out.println("hit adjust misses");
		ArrayList<Message> newMessageList = new ArrayList<Message>();
		for (Integer i : misses) {
			System.out.println("adding message Number:" + i);
			newMessageList.add(messages.get(i));
		}
		messages = newMessageList;
	}
	
	public int getMsgCount(){
		return messages.size();
	}
	
	public String generateHeader(PacketType type, int sequenceNumber,
			int remainingCounter) throws Exception {
		
		Bitter header = new Bitter();
		header.setType(type);
		header.setSeq(sequenceNumber);
		header.setRem(remainingCounter);
		
		String returnString = KeyHelper.charArrayToHexString(new char[]{header.getChar()});
		
		return returnString;
	}
	
	public String buildBasicMessage(String individual_Key,
			String groupKey, String own_cluster_key, int seq, int remainingPackets) throws Exception{
		
		String result = "";
		result += generateHeader(PacketType.BasicPacket, seq, remainingPackets);
		result += individual_Key;
		result += groupKey;
		result += own_cluster_key;
		
		result += CRC16.calculate(result); //CRC is calculated over binary, CRC16 class handles conversion
		
		return result;
	}

	
	public String[] getMessages() throws Exception{
		String result[] = new String[messages.size()];
		
		for(int i = 0; i < messages.size(); i++){
			System.out.println("getMessage(): message.size()=" + messages.size());
			String msg = generateMessage(messages.get(i), i);
			result[i] = "setup " + destination + " " + msg;
		}
		
		return result;
	}

	private String generateMessage(Message message, int i) throws Exception {
		String returns = "";
		if(message instanceof BasicMessage){
			BasicMessage bmsg = (BasicMessage) message;
			
			returns = buildBasicMessage(bmsg.getIndividual_key(), bmsg.getGroup_key(), bmsg.getOwn_cluster_key(), i, messages.size()-i-1);
			

		}else{
			ExtendedMessage emsg = (ExtendedMessage) message;
			returns = buildExtendedMessage(emsg.getClusters(), emsg.getPairwisers(), i, messages.size()-i-1);
		}
		return returns;
	}

	private String buildExtendedMessage(ArrayList<String> clusters,
			ArrayList<String> pairwisers, int sequenceNumber, int remainingCounter) throws Exception {

		if(clusters.size() + pairwisers.size() > 5){
			throw new TooManyElementsException();
		}
		
		String result = "";
		
		result += generateHeader(PacketType.ExtendedPacket, sequenceNumber, remainingCounter);
		for (String string : clusters) {
			result += "62" + string;
		}
		
		for (String string : pairwisers) {
			result += "61" + string;
		}
		
		result += CRC16.calculate(result);
		return result;
	}
}
