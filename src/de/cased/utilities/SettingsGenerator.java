package de.cased.utilities;

import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.KeyStroke;


public class SettingsGenerator {
	public static EditorAction generateExitAction(){
		return generateExitAction(null);
	}
	
	public static EditorAction generateExitAction(Editor editor){
		
		return new EditorActions.ExitAction(
				"Exit", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_35.png")),
				"Exit the KeyGenerator", 
				new Integer(KeyEvent.VK_X), editor);
	}
	
	
	public static EditorAction generateOpenAction(){
		return generateOpenAction(null);
	}
	
	public static EditorAction generateOpenAction(Editor editor){

		return new EditorActions.OpenAction(
				"Open", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_13.png")),
				"Opens a saved Topology", 
				new Integer(KeyEvent.VK_O), editor);
	}

	public static EditorAction generateSaveAction(){
		return generateSaveAction(null);
	}
	
	public static EditorAction generateSaveAction(Editor editor){

		return new EditorActions.SaveAction(
				"Save", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_11.png")),
				"Saves current Topology", 
				new Integer(KeyEvent.VK_S), editor);
	}
	
	public static EditorAction generateSaveAsAction(){
		return generateSaveAsAction(null);
	}
	
	public static EditorAction generateSaveAsAction(Editor editor){

		return new EditorActions.SaveAction(
				"Save As", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_12.png")),
				"Saves current Topology under different path", 
				new Integer(KeyEvent.VK_A), editor);
	}
	
	public static EditorAction generateUndoAction(){
		return generateUndoAction(null);
	}
	
	public static EditorAction generateUndoAction(Editor editor){

		return new EditorActions.UndoAction(
				"Undo", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_29.png")),
				"Takes back the last action", 
				new Integer(KeyStroke.getKeyStroke("control Z").getKeyCode()), editor);
	}
	
	public static EditorAction generateRedoAction(){
		return generateRedoAction(null);
	}
	
	public static EditorAction generateRedoAction(Editor editor){

		return new EditorActions.RedoAction(
				"Redo", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_27.png")),
				"Takes back the last undo", 
				new Integer(KeyStroke.getKeyStroke("control Y").getKeyCode()), editor);
	}
	
	public static EditorAction generateZoomActualAction(){
		return generateZoomActualAction(null);
	}
	
	public static EditorAction generateZoomActualAction(Editor editor){

		return new EditorActions.ZoomActualAction(
				"Normalize View", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_02.png")),
				"Reverts all scaling", 
				new Integer(KeyStroke.getKeyStroke("control 0").getKeyCode()), editor);
	}
	
	public static EditorAction generateAddAction(){
		return generateAddAction(null);
	}
	
	public static EditorAction generateAddAction(Editor editor){

		return new EditorActions.AddAction(
				"Add Vertex", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_31.png")),
				"Adds another vertex to the topology", 
				new Integer(KeyStroke.getKeyStroke("N").getKeyCode()), editor);
	}
	
	public static EditorAction generateDeleteAction(){
		return generateDeleteAction(null);
	}
	
	public static EditorAction generateDeleteAction(Editor editor){

		return new EditorActions.DeleteAction(
				"Delete Selection", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_32.png")),
				"Deletes selected Items", 
				new Integer(KeyStroke.getKeyStroke((char)KeyEvent.VK_DELETE).getKeyCode()), editor);
	}
	
	public static EditorAction generateNewTopologyAction(){
		return generateNewTopologyAction(null);
	}
	
	public static EditorAction generateNewTopologyAction(Editor editor){
		System.out.println(ClassLoader.getSystemResource(".").toString());
		return new EditorActions.NewTopologyAction(
				"New Topology", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_39.png")),
				"Creates an empty Topology", 
				new Integer(KeyStroke.getKeyStroke("control N").getKeyCode()), editor);
	}
	
	public static EditorAction generateVerifyAction(){
		return generateVerifyAction(null);
	}
	
	private static EditorAction verifyAction = null;
	public static EditorAction generateVerifyAction(Editor editor){
		if(verifyAction == null)
			verifyAction =  new EditorActions.VerifyAction(
					"Verify Topology", 
					new ImageIcon(ClassLoader.getSystemResource("icons/onebit_34.png")),
					"Verifies the built Topology", 
					new Integer(KeyStroke.getKeyStroke("control J").getKeyCode()), editor);
		
		return verifyAction;
	}
	
	public static EditorAction generateKeyAction(){
		return generateKeyAction(null);
	}
	
	private static EditorAction keyAction = null;
	public static EditorAction generateKeyAction(Editor editor){
		if(keyAction == null)
			keyAction =  new EditorActions.GenerateAction(
				"Generate Keys", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_04.png")),
				"Generates Keys for LEAP+", 
				new Integer(KeyStroke.getKeyStroke("control K").getKeyCode()), editor);
		return keyAction;
	}
	
	public static EditorAction generateDeployAction(){
		return generateDeployAction(null);
	}
	
	private static EditorAction deployAction = null;
	public static EditorAction generateDeployAction(Editor editor){
		if(deployAction == null)
			deployAction = new EditorActions.DeployAction(
				"Deploys Keys", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_15.png")),
				"Deploys generated Keys to Nodes", 
				new Integer(KeyStroke.getKeyStroke("control L").getKeyCode()), editor);
		return deployAction;
	}
	
	public static EditorAction generateZoomInAction(){
		return generateZoomInAction(null);
	}
	
	public static EditorAction generateZoomInAction(Editor editor){

		return new EditorActions.ZoomInAction(
				"Zooms In", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_51.png")),
				"Zooms into the editor", 
				null,editor);
	}
	
	public static EditorAction generateZoomOutAction(){
		return generateZoomOutAction(null);
	}
	
	public static EditorAction generateZoomOutAction(Editor editor){

		return new EditorActions.ZoomOutAction(
				"Zooms Out", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_52.png")),
				"Zooms out of the editor", 
				null,editor);
	}
	
	public static EditorAction generateReadIdFromSerialAction(){
		return generateReadIdFromSerialAction(null);
	}
	
	public static EditorAction generateReadIdFromSerialAction(Editor editor){

		return new EditorActions.ReadIdFromSerialAction(
				"Reads node ID", 
				new ImageIcon(ClassLoader.getSystemResource("icons/onebit_14.png")),
				"Read a node's ID from serial port", 
				null,editor);
	}
}
