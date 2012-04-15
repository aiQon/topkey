package de.cased.utilities;

public class Key {
	private char[] aes_128 = new char[16];
	private char[] aes_256 = new char[32];
	public char[] getAes_128() {
		return aes_128;
	}
	public void setAes_128(char[] aes_128) {
		this.aes_128 = aes_128;
	}
	public char[] getAes_256() {
		return aes_256;
	}
	public void setAes_256(char[] aes_256) {
		this.aes_256 = aes_256;
	}
	
	
}
