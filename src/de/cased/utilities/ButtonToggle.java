package de.cased.utilities;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

public class ButtonToggle extends Thread{
	
	AbstractAction action;
	
	public ButtonToggle(AbstractAction ac){
		action=ac;
	}
	
	public void run(){
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			//dont care
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				action.setEnabled(true);
			}
		});
		
	}
}
