package de.cased.serial;

import de.cased.utilities.Editor;

public class StealthNodeAdder extends SerialInteractor implements ReceiveStringFromSerial{
	
	private Editor editor;
	private SerialCommunicator serialHelper;
	
	public StealthNodeAdder(Editor editor){
		this.editor = editor;
	}
	
//	public void performOperation(String port){
//		
//		try {
////			serialHelper = new SerialCommunicator(port, this);
//			String[] keywords = {"[0-9]{1,3}[.][0-9]{1,3}\n"};
//			serialHelper.send(new String[]{"sky-getrimeaddress"}, keywords);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	@Override
	public void receiveString(String input) {
		editor.addVertex(editor.getGraphComponent().getWidth()/2, 
				editor.getGraphComponent().getHeight()/2, input);
	}

	@Override
	public void setPort(String port) throws Exception {
		serialHelper = new SerialCommunicator(port, this);
		
	}

	@Override
	public void sendMsgAndKeywords(String[] commands, String[] keywords) throws Exception {
//		// TODO Auto-generated method stub
//		String[] keywords2 = {"[0-9]{1,3}[.][0-9]{1,3}\n"};
//		serialHelper.send(new String[]{"sky-getrimeaddress"}, keywords2);
		serialHelper.send(commands, keywords);
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
}
