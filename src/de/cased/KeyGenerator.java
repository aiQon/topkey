package de.cased;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JToolBar.Separator;
import javax.swing.UIManager;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxPerimeter;

import de.cased.utilities.Editor;
import de.cased.utilities.SettingsGenerator;

public class KeyGenerator extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private JTextArea status_bar;
	private Editor editor;
	
	public Editor getEditor() {
		return editor;
	}

	final mxGraph graph = new mxGraph();
	mxGraphComponent graphComponent;
	Logger logger;
	File currentFile;
	
	public File getCurrentFile() {
		return currentFile;
	}

	public void setCurrentFile(File currentFile) {
		this.currentFile = currentFile;
	}

	public KeyGenerator() {
		super("newDiagram");
		loadRXTX();
		setNativeLookAndFeel();
		setWindowAdapter();
		createLayout();
		buildLoggingInfrastructure();
		
//		URL url = ClassLoader.getSystemResource("icons/onebit_01.png");
//		System.out.println("url:" + url.toString());
		
		
//		ImageIcon ii = new ImageIcon(ClassLoader.getSystemResource("icons/onebit_01.png"));
//		new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
		
		
	}

	private void loadRXTX() {
		System.loadLibrary("rxtxSerial");
	}

	private void setWindowAdapter(){
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);				
				if(editor.isModified()){
					int result = JOptionPane.showConfirmDialog(null,
							"Lose Changes?", "Topology was modified", 
							JOptionPane.YES_NO_CANCEL_OPTION);
					if(result == JOptionPane.YES_OPTION){
						System.exit(0);
					}
					else{
						System.out.println("no");
					}
				}else
					System.exit(0);
			}
		});
	}

	private void buildLoggingInfrastructure() {
		 logger = Logger.getLogger("KeyGenerator");

	    try {
	      FileHandler fh = new FileHandler(){

	    	  public synchronized void publish(LogRecord record) {
	    		  status_bar.append(getFormatter().format(record));
	    	  }
	      
	      };
	      logger.addHandler(fh);
	      logger.setLevel(Level.ALL);
	      SimpleFormatter formatter = new SimpleFormatter();
	      fh.setFormatter(formatter);

	    } catch (SecurityException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
		
	}

	private void createLayout() {
	
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		JToolBar toolbar = new JToolBar();
		addButtons(toolbar);
		JMenuBar menu = new JMenuBar();
		setJMenuBar(menu);
		initiateEditor();
		buildMenu(menu);
        
		status_bar = new JTextArea(4,1);
		
		
		JScrollPane scrollPane = new JScrollPane(status_bar);
		
	    mainPanel.add(toolbar, BorderLayout.NORTH);
	    mainPanel.add(editor, BorderLayout.CENTER);
	    mainPanel.add(scrollPane, BorderLayout.SOUTH);
	    
	    setContentPane(mainPanel);
		
	}

	private void buildMenu(JMenuBar menu) {
		JMenu fileMenu = new JMenu("File");
		JMenu editMenu = new JMenu("Edit");
		JMenu actionsMenu = new JMenu("Actions");
		JMenu helpMenu = new JMenu("Help");
        menu.add(fileMenu);
        menu.add(editMenu);
        menu.add(actionsMenu);
        menu.add(helpMenu);
        
        fileMenu.add(SettingsGenerator.generateNewTopologyAction(editor));
        fileMenu.add(SettingsGenerator.generateOpenAction(editor));
        fileMenu.add(SettingsGenerator.generateSaveAction(editor));
        fileMenu.add(SettingsGenerator.generateSaveAsAction(editor));
        fileMenu.addSeparator();
        fileMenu.add(SettingsGenerator.generateExitAction(editor));
        
        editMenu.add(SettingsGenerator.generateUndoAction(editor));
        editMenu.add(SettingsGenerator.generateRedoAction(editor));
        editMenu.addSeparator();
        editMenu.add(SettingsGenerator.generateAddAction(editor));
        editMenu.add(SettingsGenerator.generateDeleteAction(editor));
        
        actionsMenu.add(SettingsGenerator.generateVerifyAction(editor));
        actionsMenu.add(SettingsGenerator.generateKeyAction(editor));
        actionsMenu.add(SettingsGenerator.generateDeployAction(editor));
        actionsMenu.addSeparator();
        actionsMenu.add(SettingsGenerator.generateZoomInAction(editor));
        actionsMenu.add(SettingsGenerator.generateZoomActualAction(editor));
        actionsMenu.add(SettingsGenerator.generateZoomOutAction(editor));
        
	}
	
	private void initiateEditor() {
		
		editor = new Editor("unsaved", createGraph());
	}

	

	private void addButtons(JToolBar toolbar) {	
		toolbar.add(SettingsGenerator.generateNewTopologyAction(editor));
		toolbar.add(SettingsGenerator.generateOpenAction(editor));
		toolbar.add(SettingsGenerator.generateSaveAction(editor));
		toolbar.add(SettingsGenerator.generateSaveAsAction(editor));

		toolbar.add(new Separator());
		
		toolbar.add(SettingsGenerator.generateReadIdFromSerialAction(editor));
		toolbar.add(SettingsGenerator.generateAddAction(editor));
		toolbar.add(SettingsGenerator.generateDeleteAction(editor));
		
		toolbar.add(new Separator());
		
		toolbar.add(SettingsGenerator.generateZoomInAction(editor));
		toolbar.add(SettingsGenerator.generateZoomActualAction(editor));
		toolbar.add(SettingsGenerator.generateZoomOutAction(editor));

		
		toolbar.add(new Separator());
		
		toolbar.add(SettingsGenerator.generateVerifyAction(editor));
		toolbar.add(SettingsGenerator.generateKeyAction(editor));
		toolbar.add(SettingsGenerator.generateDeployAction(editor));
	}
	

	private mxGraphComponent createGraph(){
		
		Object parent = graph.getDefaultParent();

		graph.getModel().beginUpdate();
		try
		{
		   initGraphStyle();
		   graph.insertVertex(parent, "sink", "Sink", 20, 20, 80, 30, 
				   "ROUNDED;strokeColor=#5d65df;fillColor=#ffc5ad;gradientColor=#df857d;fontSize=12;fontColor=#8f251d;fontFamily=Verdana");
		}
		finally
		{
		   graph.getModel().endUpdate();
		}
		
		return new mxGraphComponent(graph);
		
	}

	private void initGraphStyle() {
		initEdgeStyle();
		initVertexStyle();
	}

	private void initVertexStyle() {
		Map<String, Object> style = new Hashtable<String, Object>();
		
		
		style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
		style.put(mxConstants.STYLE_PERIMETER, mxPerimeter.RectanglePerimeter);
		style.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
		style.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
		style.put(mxConstants.STYLE_FILLCOLOR, "#C3D9FF");
		style.put(mxConstants.STYLE_STROKECOLOR, "#6482B9");
		style.put(mxConstants.STYLE_FONTCOLOR, "#ffffffff");
		
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#7d85df");
		style.put(mxConstants.STYLE_ROUNDED, true);
		graph.getStylesheet().putCellStyle("defaultVertex", style);
		
	}

	private void initEdgeStyle() {
		Map<String, Object> style = new Hashtable<String, Object>();
		
		style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
		style.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
		style.put(mxConstants.STYLE_STARTARROW, mxConstants.ARROW_CLASSIC);
		style.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
		style.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
		style.put(mxConstants.STYLE_STROKECOLOR, "#5d65df");
		style.put(mxConstants.STYLE_FONTCOLOR, "#446299");
		style.put(mxConstants.STYLE_NOLABEL, true);
		graph.getStylesheet().putCellStyle("defaultEdge", style);
	}

	private void setNativeLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
	}
	

	public static void main(String[] args) {
		
		mxConstants.SHADOW_COLOR = Color.LIGHT_GRAY;
		
		KeyGenerator frame = new KeyGenerator();
		frame.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setVisible(true);

	}

}
