package de.cased.utilities;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class NodeDetails extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NodeDetails(String address, NodeKeyStore keyStore, Key groupKey){
//		super(null,"Node Details",Dialog.DEFAULT_MODALITY_TYPE);
		super(null,"Node Details",Dialog.ModalityType.MODELESS);
		
		
		
		
		//will hold everything
		JPanel mainPanel = new JPanel(new BorderLayout());
		
		//general Purpose section
		JPanel generalPurpose = new JPanel();
		JLabel address_label = new JLabel("Address");
		JTextField address_field = new JTextField(20);
		address_field.setText(address);
		address_field.setEditable(false);
		
		generalPurpose.add(address_label);
		generalPurpose.add(address_field);
		
		TitledBorder general_border = new TitledBorder("General");
		generalPurpose.setBorder(general_border);
		
		
		//LEAP section
		JPanel leapPanel = new JPanel();
		leapPanel.setLayout(new BoxLayout(leapPanel, BoxLayout.Y_AXIS));
		JLabel individual_key_label = new JLabel("Individual Key");
		JTextField individual_key_field = new JTextField(32);
		individual_key_field.setText(KeyHelper.charArrayToHexString(keyStore.getIndividualKey().getAes_128()));
		individual_key_field.setEditable(false);
		
		JLabel cluster_key_label = new JLabel("Own Cluster Key");
		JTextField cluster_key_field = new JTextField(32);
		cluster_key_field.setText(KeyHelper.charArrayToHexString(keyStore.getClusterKeys().get(address).getAes_128()));
		cluster_key_field.setEditable(false);
		
		JLabel cluster_neighbors_label = new JLabel("Joined Clusters");
		String[] columnNames = {"Neighbor","Key"};
		String[][] clusterData = KeyHelper.extractNodeKeyCombo(keyStore.getClusterKeys(),KeyAlgorithms.AES_128, address);
		JTable clusterKeyTable = new JTable(clusterData,columnNames);
		clusterKeyTable.getColumnModel().getColumn(0).setMinWidth(70);
		clusterKeyTable.getColumnModel().getColumn(0).setMaxWidth(70);
		
		JLabel group_key_label = new JLabel("Group Key");
		JTextField group_key_field = new JTextField(32);
		group_key_field.setText(KeyHelper.charArrayToHexString(groupKey.getAes_128()));
		group_key_field.setEditable(false);
		
		//JTable stuff
		
		String[][] data = KeyHelper.extractNodeKeyCombo(keyStore.getPairwiseKeys(),KeyAlgorithms.AES_128, null);
		
		
		JLabel pairwise_key = new JLabel("Pairwise Key");		
		JTable pairwiseTable = new JTable(data,columnNames);
		pairwiseTable.setEnabled(false);
		pairwiseTable.getColumnModel().getColumn(0).setMinWidth(70);
		pairwiseTable.getColumnModel().getColumn(0).setMaxWidth(70);
		
		
		leapPanel.add(individual_key_label);
		leapPanel.add(individual_key_field);
		
		leapPanel.add(Box.createRigidArea(new Dimension(0,20)));
		
		leapPanel.add(cluster_key_label);
		leapPanel.add(cluster_key_field);
		
		leapPanel.add(Box.createRigidArea(new Dimension(0,20)));
		
		leapPanel.add(cluster_neighbors_label);
		leapPanel.add(clusterKeyTable);
		
		leapPanel.add(Box.createRigidArea(new Dimension(0,20)));
		
		leapPanel.add(group_key_label);
		leapPanel.add(group_key_field);
		
		leapPanel.add(Box.createRigidArea(new Dimension(0,20)));
		
		leapPanel.add(pairwise_key);
		leapPanel.add(pairwiseTable);
		
		TitledBorder leap_border = new TitledBorder("LEAP");
		leapPanel.setBorder(leap_border);
		
		TitledBorder neighbors_border = new TitledBorder("Neighbor");
		JPanel neighborsPanel = new JPanel();
		neighborsPanel.setBorder(neighbors_border);
		JList neighborList = new JList(keyStore.getNeighbors().toArray());
		neighborList.setFixedCellWidth(300);
		neighborsPanel.add(neighborList);
		
		
		mainPanel.add(generalPurpose, BorderLayout.NORTH);
		mainPanel.add(leapPanel,BorderLayout.CENTER);
		mainPanel.add(neighborsPanel, BorderLayout.SOUTH);
		setContentPane(mainPanel);
		pack();
		centerOnScreen();
	}

	

	private void centerOnScreen() {
		Dimension dim = getToolkit().getScreenSize();
		  Rectangle abounds = getBounds();
		  setLocation((dim.width - abounds.width) / 2,
		      (dim.height - abounds.height) / 2);
	}
	
}
