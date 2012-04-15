package de.cased.utilities;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class JProgressBarDialog extends JDialog {

	private int max;
	private Logger logger = Logger.getLogger("KeyGenerator");
	private JProgressBar progressBar;
	private boolean abort = false;
	JButton abort_button;
	
	public void setProgressValue(final int value){
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				progressBar.setValue(value);
				
			}
		});
		
		if(progressBar.getValue() == max){
			abort_button.setText("done");
		}
	}
	
	public boolean is_aborting(){
		return abort;
	}
	
	public JProgressBarDialog(int max) {
		super(null,"Key Transmission in Progress", Dialog.ModalityType.MODELESS);
		this.max = max;
		
		progressBar = new JProgressBar(0,max);
		progressBar.setStringPainted(true);
		abort_button = new JButton("Cancel");
		abort_button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				kill();
				
			}
		});
		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());
		main.add(progressBar, BorderLayout.CENTER);
		main.add(abort_button, BorderLayout.SOUTH);
		setContentPane(main);
		pack();
		centerOnScreen();
		setVisible(true);
		
	}
	
	public void kill(){
		abort = true;
		setVisible(false);
	}
	
	private void centerOnScreen() {
		Dimension dim = getToolkit().getScreenSize();
		  Rectangle abounds = getBounds();
		  setLocation((dim.width - abounds.width) / 2,
		      (dim.height - abounds.height) / 2);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	

}
