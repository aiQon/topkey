package de.cased.serial;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;



public class SerialPortSelector extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	//private static final long serialVersionUID = 1L;
	private JComboBox dropDown;
	private JButton ok;
	private String[] keywords;
	private String[] commands;
//	private Editor editor;
	

	SerialInteractor interactor;
	
	public SerialPortSelector(SerialInteractor inter){
		super(null,"Select Serial Port for Interaction", Dialog.ModalityType.APPLICATION_MODAL);

//		this.editor = editor;
		interactor = inter;
		
		String[] portList = SerialCommunicator.retrieveHostSerialPortList();
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel nortPanel = new JPanel();
		JPanel southPanel = new JPanel();
		
		JLabel deviceLabel = new JLabel("Select the device port");
		dropDown = new JComboBox(portList);
		ok = new JButton("Ok");
		JButton cancel = new JButton("Cancel");
		
		ok.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ok.setEnabled(false);
//				SerialCommunicator.setPersistentIdentifier(dropDown.getSelectedItem().toString());
				try {
					interactor.setPort(dropDown.getSelectedItem().toString());
					if(commands != null && keywords != null)
						interactor.sendMsgAndKeywords(commands, keywords);
					
					
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}	
//				interactor.performOperation(dropDown.getSelectedItem().toString());
				setVisible(false);
			}
		});
		
		cancel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		
		nortPanel.add(deviceLabel);
		nortPanel.add(dropDown);
		
		southPanel.add(ok);
		if(portList.length == 0)
			ok.setEnabled(false);
		southPanel.add(cancel);
		
		mainPanel.add(nortPanel,BorderLayout.CENTER);
		
		mainPanel.add(southPanel, BorderLayout.SOUTH);
		
		setContentPane(mainPanel);
		pack();
		centerOnScreen();
		setVisible(true);
		
	}

	public SerialPortSelector(StealthNodeAdder stealth, String[] keywords2,
			String[] commands) {
		this(stealth);
		this.commands = commands;
		this.keywords = keywords2;
		// TODO Auto-generated constructor stub
	}

	private void centerOnScreen() {
		Dimension dim = getToolkit().getScreenSize();
		  Rectangle abounds = getBounds();
		  setLocation((dim.width - abounds.width) / 2,
		      (dim.height - abounds.height) / 2);
	}
	
}
