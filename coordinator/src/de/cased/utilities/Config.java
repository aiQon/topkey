package de.cased.utilities;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Config {
	
	private static String config_file = "config.ini";
	private Properties prop = new Properties();
	
	private static Config instance = null;
	private Config() {
		try{
			InputStream inStream = new FileInputStream(config_file);
			prop.load(inStream);
		}catch(Exception e){
			System.out.println("Could not load config: config.ini");
			System.exit(12);
		}
	}
	
	public static Config getInstance(){
		if(instance == null)
			instance = new Config();
		return instance;
	}
	
	public String getProperty(String key){
		return prop.getProperty(key);
	}
}
