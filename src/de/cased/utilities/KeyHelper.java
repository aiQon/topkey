package de.cased.utilities;

import java.util.Map;

public class KeyHelper {
	static public String charArrayToHexString(char in[]) {

	    byte ch = 0x00;
	    int i = 0; 

	    if (in == null || in.length <= 0)
	        return null;

	    String pseudo[] = {"0", "1", "2",
	    		"3", "4", "5", "6", "7", "8",
	    		"9", "A", "B", "C", "D", "E",
	    		"F"};
	    StringBuffer out = new StringBuffer(in.length * 2);
	    while (i < in.length) {
	        ch = (byte) (in[i] & 0xF0); 	// Strip off high nibble
	        ch = (byte) (ch >>> 4);			// shift the bits down
	        ch = (byte) (ch & 0x0F);       	// must do this is high order bit is on!
	        out.append(pseudo[ (int) ch]); 	// convert the nibble to a String Character
	        ch = (byte) (in[i] & 0x0F); 	// Strip off low nibble 
	        out.append(pseudo[ (int) ch]); 	// convert the nibble to a String Character
	        i++;
	    }
	    String rslt = new String(out);
	    return rslt;

	}
	
	static public char[] hexStringToCharArray(String in){
		char[] result = new char[in.length()/2];
		
		for (int i = 0; i < result.length; i++) {
			String s = in.substring(i*2, i*2+2);
			result[i] = (char) (Integer.parseInt(s, 16) & 0xff);
		}
		
		return result;
		
	}
	
	static public String[][] extractNodeKeyCombo(Map<String,Key> keyMap, KeyAlgorithms key_algo, String except) {
		
		String[][] data;
		
		if(except != null && keyMap.containsKey(except))
			data = new String[keyMap.size()-1][];
		else
			data = new String[keyMap.size()][];

		int i=0;
		for ( String key : keyMap.keySet() ){
			
			if(key.equals(except))
				continue;
			
			data[i] = new String[2];
			data[i][0] = key;
			switch(key_algo){
				case AES_128:
					data[i][1] = charArrayToHexString(keyMap.get(key).getAes_128());
					break;
				case AES_256:
					data[i][1] = charArrayToHexString(keyMap.get(key).getAes_256());
					break;
			}		
			
			i++;
		}
		return data;
	}
}
