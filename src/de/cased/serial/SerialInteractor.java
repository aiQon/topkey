package de.cased.serial;

public abstract class SerialInteractor {

	public abstract void setPort(String port) throws Exception;
	public abstract void sendMsgAndKeywords(String[] commands, String[] keywords) throws Exception;
	public abstract void stop();
}
