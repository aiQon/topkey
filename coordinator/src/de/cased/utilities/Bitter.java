package de.cased.utilities;
/*
 * Sets an intern char variable as state.
 * Manipulates the state with set/clear operations to adjust flags.
 * Orientation from right: set(1) on a fresh instance means set state to "0x02"
 */

public class Bitter {

	private int header;
	
	
	public void set(int index){
		header |= 1 << index;
	}

	public void clear(int index){
		header &= ~(1 << index);
	}
	
	
	
	public void setSeq(int input){
		if(input > 7)
			return;
		
		header |= input << 3;
	}
	
	public void setRem(int input){
		if(input > 7)
			return;
		header |= input;
	}
	
	
	public static void main(String[] args) {
	
		Bitter bit = new Bitter();
		bit.set(6);
		bit.set(0);
		System.out.println(bit.getChar());

	}

	public void setType(PacketType type) {
		
		switch(type){
		case BasicPacket:
			clear(7);
			clear(6);
			break;
		case ExtendedPacket:
			clear(7);
			set(6);
			break;
		case Data0:
			set(7);
			clear(6);
			break;
		case Data1:
			set(7);
			set(6);
			break;
		}
		
	}
	
	@Override
	public String toString() {
		return ((char) header & 0xFF) + "";
	}
	
	public char getChar(){
		return (char)(header & 0xFF);
	}

}
