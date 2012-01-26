package de.cased.serial;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.cased.utilities.Bitter;
import de.cased.utilities.JProgressBarDialog;
import de.cased.utilities.KeyHelper;
import de.cased.utilities.MessageWrapper;

public class SerialWorker extends Thread {
	
	private enum ResponseTypes {NO_RESPONSE, NACK};
	private ArrayList<MessageWrapper> wrappers;
//	private ArrayList<String> failedTransmissions = new ArrayList<String>();
	String latestResponse = "";
	boolean responseChecked = false;
	private JProgressBarDialog progress;
	
	private Logger logger = Logger.getLogger("KeyGenerator");
	
	private SerialInteractor interactor;
	
	public SerialWorker(ArrayList<MessageWrapper> commandQueue, JProgressBarDialog progress) {
		this.wrappers = commandQueue;
		this.progress = progress;

	}
	
	@Override
	public void run(){
		synchronized (this) {
			try {
				initUSB();
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
				//failed opening the USB device, need to bail out
				
			}
			long start_time = System.currentTimeMillis();
			String[] keywords = {".*<NACK>[0-9A-F]{10}</NACK>.*"};
			int i = 0;
			
			while(i < wrappers.size()){
				progress.setProgressValue(i+1);
				if(progress.is_aborting() == true)
					return;
				else{
					String commandQueue[];
					try {
						commandQueue = wrappers.get(i).getMessages();
						sendCommand(commandQueue, keywords);
					} catch (Exception e1) {
						System.out.println("Failed creating sending message:" + e1.getMessage());
						e1.printStackTrace();
					}
					
					try {
						logger.log(Level.INFO, "Current Thread falling asleep: " + Thread.currentThread().getName());
						this.wait(100); //can be lowered to 8, I think...
					} catch (Exception e) {
						logger.log(Level.SEVERE, "SerialWorker failed falling asleep.");
						System.out.println("Message:" + e.getMessage());
						e.printStackTrace();
					}
					
					
					switch(checkResponsePacket()){
					case NO_RESPONSE:
						continue;
					case NACK:
						if(analyseNACK(i)){
							i++;
						}
						break;
				}
				}
			}
			
			
//			SerialCommunicator.resetPersistency();
			long end_time = System.currentTimeMillis();
			long run = end_time-start_time;
			System.out.println("deployment took: " + run + " ms");
		}
		cleanUSB();
		
	}

	
	private void cleanUSB() {
		interactor.stop();
	}


	private void initUSB() throws Exception {
		String[] list = SerialCommunicator.retrieveHostSerialPortList();
		interactor = new SendSetup(this);
		if(list.length == 1){
			interactor.setPort(list[0]);
		} else {
			new SerialPortSelector(interactor);
		}
		
	}

	/*
	 * returns true if all messages arrived, otherwise alters MessageWrapper 
	 * 		if necessary and returns false
	 */
	private boolean analyseNACK(int i) {
		
		if(latestResponse.length() == 0)
			return false;
		
		char[] responseAsCharArray = KeyHelper.hexStringToCharArray(latestResponse);
		
		if(responseAsCharArray.length == 0)
			return false;

		int seq = new Integer(responseAsCharArray[0]);
		
		System.out.println("extracted sequence mask:"+ seq);
		MessageWrapper wrap = wrappers.get(i);
		int expectedCount = wrap.getMsgCount();
		
		
		int diff_mask = seq ^ (int)(Math.pow(2, expectedCount)-1);
		
		ArrayList<Integer> misses = new ArrayList<Integer>();
		
		while(expectedCount >= 0){
//			System.out.println("loop with expectedCount=" + expectedCount);
			
			int bitInQ = (diff_mask >> (expectedCount-1)) & 1;
			if(bitInQ == 1){
				System.out.println("found miss at " + (expectedCount-1));
				misses.add(expectedCount-1);
			}
			
			expectedCount--;
		}

		latestResponse = "";
		
		if(misses.size() == 0){
			System.out.println("nothing was missing");
			return true;
		}
		else{
			System.out.println("something was missing");
			wrap.adjustMisses(misses);
			return false;
		}
	}

	private ResponseTypes checkResponsePacket() {
		synchronized (latestResponse) {
			if(latestResponse.length() == 0)
				return ResponseTypes.NO_RESPONSE;
			
			else
				return ResponseTypes.NACK;
			
		}
		
	}

	public void receiveFailedTransmissions(String input){
		synchronized (latestResponse) {
			latestResponse = input;	
		}
		logger.log(Level.INFO, "SerialWorker received NACK transmission: " + input);
	}
	
	private void sendCommand(String message[], String[] keywords) throws Exception{
		
		interactor.sendMsgAndKeywords(message, keywords);
//		String[] list = SerialCommunicator.retrieveHostSerialPortList();
//		if(list.length == 1){
//			SerialInteractor interactor = new SendSetup(message, this, keywords);
//			interactor.performOperation(list[0]);
//		} else {
//			SerialPortSelector commandSender = new SerialPortSelector(new SendSetup(message, this, keywords));
//		}
	}
}
