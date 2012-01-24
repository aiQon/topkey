package de.cased.utilities;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.mxUndoableEdit;
import com.mxgraph.util.mxUndoableEdit.mxUndoableChange;
import com.mxgraph.view.mxGraph;

public class Editor extends JPanel {


	private static final long serialVersionUID = 1L;
	private Set<String> availableNodes;
	protected String appTitle;
	protected mxGraphComponent graphComponent;
	public mxGraphComponent getGraphComponent() {
		return graphComponent;
	}

	protected mxUndoManager undoManager;
	public mxUndoManager getUndoManager() {
		return undoManager;
	}

	protected File currentFile;
	public File getCurrentFile() {
		return currentFile;
	}

	public void setCurrentFile(File currentFile) {
		this.currentFile = currentFile;
	}

	protected Logger logger;
	protected KeyboardHandler keyboardHandler;
	
	private boolean modified = false;
	private boolean verify_modified = false;
	private boolean key_modified = false;
	
	private KeyStore keyStore = new KeyStore();
	
	public KeyStore getKeyStore(){
		return keyStore;
	}
	
	public void resetKeyStore(){
		keyStore = new KeyStore();
		keyStore.initNodes(mxGraphModel.getChildren(graphComponent.getGraph().getModel(), graphComponent.getGraph().getDefaultParent()));
	}
	

	public Editor(String appTitle, mxGraphComponent component)
	{
		// Stores and updates the frame title
		this.appTitle = appTitle;

		// Stores a reference to the graph and creates the command history
		graphComponent = component;
		final mxGraph graph = graphComponent.getGraph();
		graph.setAllowDanglingEdges(false);
		undoManager = new mxUndoManager();

		// Do not change the scale and translation after files have been loaded
		graph.setResetViewOnRootChange(false);

		// Updates the modified flag if the graph model changes
		graph.getModel().addListener(mxEvent.CHANGE, changeTracker);

		// Adds the command history to the model and view
		graph.getModel().addListener(mxEvent.UNDO, undoHandler);
		graph.getView().addListener(mxEvent.UNDO, undoHandler);

		// Keeps the selection in sync with the command history
		mxIEventListener undoHandler = new mxIEventListener()
		{
			public void invoke(Object source, mxEventObject evt)
			{
				List<mxUndoableChange> changes = ((mxUndoableEdit) evt
						.getProperty("edit")).getChanges();
				graph.setSelectionCells(graph
						.getSelectionCellsForChanges(changes));
			}
		};

		undoManager.addListener(mxEvent.UNDO, undoHandler);
		undoManager.addListener(mxEvent.REDO, undoHandler);


		accessNodeList();
		
		// Installs rubberband selection and handling for some special
		// keystrokes such as F2, Control-C, -V, X, A etc.
		installHandlers(component);
		installListeners();
		updateTitle();
		SettingsGenerator.generateKeyAction(this).setEnabled(false);
		SettingsGenerator.generateDeployAction(this).setEnabled(false);
		
		logger = Logger.getLogger("KeyGenerator");
		
		setLayout(new BorderLayout());
		add(graphComponent, BorderLayout.CENTER);
		
	}
	
	private void accessNodeList() {
		try{
			FileReader fr = new FileReader("nodes.txt");
			BufferedReader br = new BufferedReader(fr);
			availableNodes = new HashSet<String>();
			String line;
			while((line = br.readLine()) != null){
				availableNodes.add(line);
			}
		}catch(IOException e){
			logger.log(Level.WARNING, e.toString());
		}
		
	}

	private void installHandlers(mxGraphComponent component) {
		installMouseHandler(component);
		installKeyHandler(component);
		
	}
	
	private void installKeyHandler(final mxGraphComponent component) {
		component.addKeyListener(new KeyAdapter()
		{
			
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_DELETE)
				{
					deleteSelectedCells();
				}
			}

			
		});
		
		keyboardHandler = new KeyboardHandler(graphComponent);
	}
	
	protected void deleteSelectedCells() {
		Object[] selected = graphComponent.getGraph().getSelectionCells();
		for(int i = 0; i<selected.length; i++){
			mxCell cellInQuestion = ((mxCell)selected[i]);
//			System.out.println(cellInQuestion.getId());
			if(cellInQuestion.getId().equals("sink"))
				graphComponent.getGraph().getSelectionModel().removeCell(cellInQuestion);
		}
		logger.log(Level.INFO, "Erase selection: " + graphComponent.getGraph().getSelectionCount());
		graphComponent.getGraph().removeCells(graphComponent.getGraph().getSelectionCells());
	}

	private void installMouseHandler(final mxGraphComponent component) {
		component.getGraphControl().addMouseListener(new MouseAdapter()
		{
			public void mouseReleased(MouseEvent e)
			{
				if(e.getButton() == MouseEvent.BUTTON1 && 
				   e.getClickCount() == 2 && 
				   component.getCellAt(e.getX(), e.getY(), false) == null){
					
					logger.log(Level.INFO, "Create new Node");
					addVertex(e.getX(), e.getY(), null);
				}
			}

		});
	}

	protected mxIEventListener changeTracker = new mxIEventListener()
	{
		public void invoke(Object source, mxEventObject evt)
		{
			setModified(true);
		}
	};
	
	protected mxIEventListener undoHandler = new mxIEventListener()
	{
		public void invoke(Object source, mxEventObject evt)
		{
			undoManager.undoableEditHappened((mxUndoableEdit) evt
					.getProperty("edit"));
		}
	};
	
	public void updateTitle()
	{
		JFrame frame = (JFrame) SwingUtilities.windowForComponent(this);
		
		if (frame != null)
		{
			String title = (currentFile != null) ? currentFile
					.getAbsolutePath() : "newDiagram";

			if (modified)
			{
				title += "*";
			}
			
			if(!verify_modified){
				SettingsGenerator.generateKeyAction(this).setEnabled(true);
				if(key_modified)
					SettingsGenerator.generateDeployAction(this).setEnabled(false);
				else
					SettingsGenerator.generateDeployAction(this).setEnabled(true);
			}else{
				SettingsGenerator.generateKeyAction(this).setEnabled(false);
				SettingsGenerator.generateDeployAction(this).setEnabled(false);
			}
			

			frame.setTitle(title);
		}
	}
	
	
	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
		this.verify_modified = modified;
		this.key_modified = modified;
	}
	
	public void setVerifyModified(boolean value){
		this.verify_modified = value;
	}
	
	public void setKeyModified(boolean value){
		this.key_modified = value;
	}
	
	
	
	
	protected void installListeners()
	{
		MouseWheelListener wheelTracker = new MouseWheelListener()
		{
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if(e.isControlDown()){
					if (e.getWheelRotation() < 0)
					{
						graphComponent.zoomIn();
						
					}
					else
					{
						graphComponent.zoomOut();
					}
				}
			}

		};


		graphComponent.addMouseWheelListener(wheelTracker);
		graphComponent.getGraphControl().addMouseListener(new MouseAdapter()
		{

			public void mousePressed(MouseEvent e)
			{
				// Handles context menu on the Mac where the trigger is on mousepressed
				mouseReleased(e);
			}

			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showGraphPopupMenu(e);
				}
			}

		});

	}
	
	protected void showGraphPopupMenu(MouseEvent e)
	{
		final Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(),
				graphComponent);
		JCheckBoxMenuItem item = new JCheckBoxMenuItem("Add Node");
		JCheckBoxMenuItem fancyMenu = new JCheckBoxMenuItem("Show Details");
		
		if(graphComponent.getCellAt((int)pt.getX(), (int)pt.getY()) == null){
			fancyMenu.setEnabled(false);
		}

		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				logger.log(Level.INFO, "Create new Node");
				addVertex((int)pt.getX(), (int)pt.getY(), null);
			}
		});
		
		fancyMenu.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				logger.log(Level.INFO, "Accessing a Node's Details");
				showNodeDetails((int)pt.getX(), (int)pt.getY());
			}
		});



		JPopupMenu menu = new JPopupMenu();
		menu.add(item);
		menu.add(fancyMenu);
		menu.show(graphComponent, pt.x, pt.y);

		e.consume();
	}
	
	protected void showNodeDetails(int x, int y) {
		Object cell = graphComponent.getCellAt(x, y); // die Zelle im KeyManagement wiederfinden und den KeyStorage extrahieren
		String cellName = (String)((mxCell) cell).getValue();
		NodeKeyStore keys = keyStore.getNodes().get(cellName);
		if(keys != null){
			NodeDetails details = new NodeDetails(cellName, keys, keyStore.getSink_keys().getGroup_key());
			details.setVisible(true);
		}else{	//keine keys gefunden
			JOptionPane.showConfirmDialog(null, "No Node with ID " + cellName + " found.", "No Node found", JOptionPane.WARNING_MESSAGE);
		}
		
		
	}

	public void exit(){
		System.exit(0);
	}
	
	public void addVertex(int x, int y, String in_name){
		String name;
		if(in_name != null)
			name = in_name;
		else if(availableNodes != null && availableNodes.size() > 0){
			name=availableNodes.iterator().next();
			availableNodes.remove(name);
		}else
			name = "new node";
		
		graphComponent.getGraph().insertVertex(graphComponent.getGraph().getDefaultParent(), null, name, x, y, 80, 30);
		setModified(true);
		updateTitle();
	
	}

	
	
	
}
