package de.cased.utilities;

public class CRC16 {

	   public static String calculate(String args) { 
		   	char[] toBeCalculated = KeyHelper.hexStringToCharArray(args);
	        return crc16(toBeCalculated, toBeCalculated.length, 0xffff);

	    }
	   
	   public static void main(String[] args) { 
	        int crc = 0xFFFF;          // initial value

//	        char[] bytes = new char[] {0, 12, 16, 24, 23, 126, 74, 126};
	        String value = "01C4FD39666EC2933B141FC0E57974335339E5E20D5FD2FB04CE0B20EF15C61E30FCB0A5034DDF79C56ABEC68A84EB2728";
//	        String value = "016FDCED924E20C5D2BD2057E5DF1CDB93C583C3822040916A7CB12848402A7EEFED1253C45E5C97360355E4994DFBA71B";
//	        char[] toBeCRCed = KeyHelper.hexStringToCharArray(value);
//setup 180.54 001B9CA09ACAD096EA49DD62A6F8B0EC466E1240EF5EB126BDF078A32FEBCBA4B5CA7461623816A2D94123A14FCA7A839a3b
	        
	        
//	        System.out.println("CRC16-CCITT = " + CRC16.crc16(bytes, crc));
		    
	        System.out.println("CRC16-CCITT = " + CRC16.crc16(KeyHelper.hexStringToCharArray(value), KeyHelper.hexStringToCharArray(value).length, crc));
//	        System.out.println(calculate("013727A80C2047C263144FA8F52EC16E92E43724BD38601DE1D876AF98F381384928E0C218A76E404BE9174AFAC83FBCDF"));
		}
		
		public static String crc16(char[] bytes, int len, int crcInitial) {
			
	        
			for (int i = 0; i < len; i++) {
	        	crcInitial ^= (((int) bytes[i]) & 0xFF);
	            crcInitial = ((crcInitial >> 8) & 0xFFFF) | ((crcInitial << 8) & 0xFFFF);
	            crcInitial ^= ((crcInitial & 0xff00) << 4)  & 0xFFFF;
	            crcInitial ^= (((crcInitial >> 8) & 0xFFFF) >> 4) & 0xFFFF;
	            crcInitial ^= ((crcInitial & 0xff00) >> 5) & 0xFFFF;
	        }
	        System.out.println(crcInitial);
	        int crc = crcInitial & 0xffff;
	        String hexstring = Integer.toHexString(crc).toUpperCase();
	        return reverseEndianness(addPadding(hexstring));
		}
		
		private static String reverseEndianness(String addPadding) {
			
			return addPadding.substring(2) + addPadding.substring(0, 2);
		}

		public static String addPadding(String in){
			if(in.length() == 0)
				return "0000";
			else if(in.length() == 1)
				return "000" + in;
			else if(in.length() == 2)
				return "00" + in;
			else if(in.length() == 3)
				return "0" + in;
			else
				return in;
		}

	
	
}
