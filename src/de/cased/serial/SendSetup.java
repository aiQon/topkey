package de.cased.serial;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import de.cased.utilities.CRC16;


public class SendSetup extends SerialInteractor implements ReceiveStringFromSerial{

//	private String commands[];
	private SerialWorker sendingThread;
//	private String[] keywords;
	private SerialCommunicator serialHelper;
	
	private Logger logger = Logger.getLogger("KeyGenerator");
	
	public SendSetup(SerialWorker t) throws Exception{
		sendingThread = t;
//		serialHelper = new SerialCommunicator(port, this);
	}
	
	public void setPort(String port) throws Exception{
		serialHelper = new SerialCommunicator(port, this);
	}
	
	public void stop(){
		if(serialHelper != null)
			serialHelper.stop();
	}
	
	public synchronized void sendMsgAndKeywords(String[] commands, String[] keywords){
		try {
			if(serialHelper != null){
				serialHelper.send(commands, keywords);
				logger.log(Level.INFO, "sent messages");
			}
						
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public synchronized void receiveString(String input) {
		System.out.println("SendSetup got: " + input);
		
		
		/*
		 * entweder es kommt '<NACK>*' (filter welche commands neugeschickt werden müssen) oder 'Interrupted by Timer' (schick alles neu)
		 */
		
		if(input.indexOf("<NACK>") > -1){
			
			String msg = input.substring(input.indexOf("<NACK>")+6, input.indexOf("</NACK"));
			System.out.println("extracted NACK message is:" + msg);
			System.out.println("extracted NACK msg length is:" + msg.length());
			//control CRC
			String extractedCRC = msg.substring(msg.length()-4, msg.length());
			String toBeCRCed = msg.substring(0, msg.length()-4);
			
			System.out.println("extracted CRC:" + extractedCRC);
			System.out.println("toBeCRCed:" + toBeCRCed);
			String calculatedCRC = CRC16.calculate(toBeCRCed);
			
			if(calculatedCRC.equals(extractedCRC)){
				//continue here
				System.out.println("NACK message received correctly and passed on");
				sendingThread.receiveFailedTransmissions(toBeCRCed.substring(2)); //strip off the type byte
			} else {
				System.out.println("CRC fail");
			}
			
			
		}
		synchronized(sendingThread){
			sendingThread.notify();
			}
	}

}
