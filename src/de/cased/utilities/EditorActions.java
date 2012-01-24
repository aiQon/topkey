package de.cased.utilities;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.w3c.dom.Document;

import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.canvas.mxSvgCanvas;
import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxGdCodec;
import com.mxgraph.io.gd.mxGdDocument;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxCellRenderer.CanvasFactory;
import com.mxgraph.util.mxResources;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.png.mxPngEncodeParam;
import com.mxgraph.util.png.mxPngImageEncoder;
import com.mxgraph.util.png.mxPngTextDecoder;
import com.mxgraph.view.mxGraph;

import de.cased.KeyGenerator;
import de.cased.serial.SerialCommunicator;
import de.cased.serial.SerialPortSelector;
import de.cased.serial.SerialWorker;
import de.cased.serial.StealthNodeAdder;



public class EditorActions {
	
	public static final Editor getEditor(ActionEvent e)
	{	
		if (e.getSource() instanceof Component)
		{
			Component component = (Component) e.getSource();

			
			
			while (component != null
					&& !(component instanceof Editor)
					&& !(component instanceof KeyGenerator))
			{
				component = component.getParent();
			}
			if(component instanceof KeyGenerator){
				return ((KeyGenerator) component).getEditor();
			}
			else if(component instanceof Editor){
				return (Editor) component;
			}
			
		}

		return null;
	}
	
	@SuppressWarnings("serial")
	public static class ExitAction extends EditorAction
	{

		
		public ExitAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public ExitAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}

		public void actionPerformed(ActionEvent e)
		{
			editor = editor == null ? getEditor(e) : editor;

			if (editor != null)
			{
				if(editor.isModified() && 
						JOptionPane.showConfirmDialog(null,
								"Lose Changes?", "Topology was modified", 
								JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION){
					
					return;
				}else
					
				logger.log(Level.INFO, "Exiting gracefully");
				editor.exit();
			}
		}
	}
	
	
	
	@SuppressWarnings("serial")
	public static class SaveAction extends SaveActionGeneral
	{

		public SaveAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic, false);
		}
		
		public SaveAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor, false);
		}
		
		public void actionPerformed(ActionEvent e){
			super.actionPerformed(e);
			
		}
		
	}
	
	@SuppressWarnings("serial")
	public static class SaveAsAction extends SaveActionGeneral
	{

		public SaveAsAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic, true);
		}
		
		public SaveAsAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor, true);
		}
		
		public void actionPerformed(ActionEvent e){
			super.actionPerformed(e);
		}
		
	}
	
	
	@SuppressWarnings("serial")
	public abstract static class SaveActionGeneral extends EditorAction
	{
		public SaveActionGeneral(String text, ImageIcon icon, String desc,
				Integer mnemonic, boolean showDialog) {
			super(text, icon, desc, mnemonic);
			this.showDialog = showDialog;
		}
		
		public SaveActionGeneral(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor, boolean showDialog) {
			super(text, icon, desc, mnemonic, editor);
			this.showDialog = showDialog;
		}
		
		protected String lastDir = null;
		protected boolean showDialog;


		/**
		 * Saves XML+PNG format.
		 */
		protected void saveXmlPng(Editor editor, String filename,
				Color bg) throws IOException
		{
			mxGraphComponent graphComponent = editor.getGraphComponent();
			mxGraph graph = graphComponent.getGraph();

			// Creates the image for the PNG file
			BufferedImage image = mxCellRenderer.createBufferedImage(graph,
					null, 1, bg, graphComponent.isAntiAlias(), null,
					graphComponent.getCanvas());

			// Creates the URL-encoded XML data
			mxCodec codec = new mxCodec();
			String xml = URLEncoder.encode(
					mxUtils.getXml(codec.encode(graph.getModel())), "UTF-8");
			mxPngEncodeParam param = mxPngEncodeParam
					.getDefaultEncodeParam(image);
			param.setCompressedText(new String[] { "mxGraphModel", xml });

			// Saves as a PNG file
			FileOutputStream outputStream = new FileOutputStream(new File(
					filename));
			try
			{
				mxPngImageEncoder encoder = new mxPngImageEncoder(outputStream,
						param);

				if (image != null)
				{
					encoder.encode(image);

					editor.setModified(false);
					editor.setCurrentFile(new File(filename));
					editor.updateTitle();
				}
				else
				{
					JOptionPane.showMessageDialog(graphComponent,
							mxResources.get("noImageData"));
				}
			}
			finally
			{
				outputStream.close();
			}
		}

		public void save(Editor editor){
			if (editor != null)
			{
				mxGraphComponent graphComponent = editor.getGraphComponent();
				mxGraph graph = graphComponent.getGraph();
				FileFilter selectedFilter = null;
				KeyFileFilter xmlPngFilter = new KeyFileFilter(".png",
						"PNG+XML " + mxResources.get("file") + " (.png)");
				FileFilter vmlFileFilter = new KeyFileFilter(".html",
						"VML " + mxResources.get("file") + " (.html)");
				String filename = null;
				boolean dialogShown = false;

				if (showDialog || editor.getCurrentFile() == null)
				{
					String wd;

					if (lastDir != null)
					{
						wd = lastDir;
					}
					else if (editor.getCurrentFile() != null)
					{
						wd = editor.getCurrentFile().getParent();
					}
					else
					{
						wd = System.getProperty("user.dir");
					}

					JFileChooser fc = new JFileChooser(wd);

					// Adds the default file format
					FileFilter defaultFilter = xmlPngFilter;
					fc.addChoosableFileFilter(defaultFilter);

					// Adds special vector graphics formats and HTML
					fc.addChoosableFileFilter(new KeyFileFilter(".mxe",
							"mxGraph Editor (.mxe)"));
					fc.addChoosableFileFilter(new KeyFileFilter(".txt",
							"Graph Drawing (.txt)"));
					fc.addChoosableFileFilter(new KeyFileFilter(".svg",
							"SVG (.svg)"));
					fc.addChoosableFileFilter(vmlFileFilter);
					fc.addChoosableFileFilter(new KeyFileFilter(".html",
							"HTML (.html)"));

					// Adds a filter for each supported image format
					Object[] imageFormats = ImageIO.getReaderFormatNames();

					// Finds all distinct extensions
					HashSet<String> formats = new HashSet<String>();

					for (int i = 0; i < imageFormats.length; i++)
					{
						String ext = imageFormats[i].toString().toLowerCase();
						formats.add(ext);
					}

					imageFormats = formats.toArray();

					for (int i = 0; i < imageFormats.length; i++)
					{
						String ext = imageFormats[i].toString();
						fc.addChoosableFileFilter(new KeyFileFilter("."
								+ ext, ext.toUpperCase() + " (." + ext + ")"));
					}

					// Adds filter that accepts all supported image formats
					fc.addChoosableFileFilter(new KeyFileFilter.ImageFileFilter(
							mxResources.get("allImages")));
					fc.setFileFilter(defaultFilter);
					int rc = fc.showDialog(null, "save?");
					dialogShown = true;

					if (rc != JFileChooser.APPROVE_OPTION)
					{
						return;
					}
					else
					{
						lastDir = fc.getSelectedFile().getParent();
					}

					filename = fc.getSelectedFile().getAbsolutePath();
					editor.setCurrentFile(fc.getSelectedFile());
					selectedFilter = fc.getFileFilter();

					if (selectedFilter instanceof KeyFileFilter)
					{
						String ext = ((KeyFileFilter) selectedFilter)
								.getExtension();

						if (!filename.toLowerCase().endsWith(ext))
						{
							filename += ext;
						}
					}

					if (new File(filename).exists()
							&& JOptionPane.showConfirmDialog(graphComponent,"Override existing data?", "File already exists", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
					{
						return;
					}
				}
				else
				{
					filename = editor.getCurrentFile().getAbsolutePath();
				}

				try
				{
					String ext = filename
							.substring(filename.lastIndexOf('.') + 1);

					if (ext.equalsIgnoreCase("svg"))
					{
						mxSvgCanvas canvas = (mxSvgCanvas) mxCellRenderer
								.drawCells(graph, null, 1, null,
										new CanvasFactory()
										{
											public mxICanvas createCanvas(
													int width, int height)
											{
												mxSvgCanvas canvas = new mxSvgCanvas(
														mxUtils.createSvgDocument(
																width, height));
												canvas.setEmbedded(true);

												return canvas;
											}

										});

						mxUtils.writeFile(mxUtils.getXml(canvas.getDocument()),
								filename);
						editor.setModified(false);
						editor.updateTitle();
					}
					else if (selectedFilter == vmlFileFilter)
					{
						mxUtils.writeFile(mxUtils.getXml(mxCellRenderer
								.createVmlDocument(graph, null, 1, null, null)
								.getDocumentElement()), filename);
						editor.setModified(false);
						editor.updateTitle();
					}
					else if (ext.equalsIgnoreCase("html"))
					{
						mxUtils.writeFile(mxUtils.getXml(mxCellRenderer
								.createHtmlDocument(graph, null, 1, null, null)
								.getDocumentElement()), filename);
						editor.setModified(false);
						editor.updateTitle();
					}
					else if (ext.equalsIgnoreCase("mxe")
							|| ext.equalsIgnoreCase("xml"))
					{
						mxCodec codec = new mxCodec();
						String xml = mxUtils.getXml(codec.encode(graph
								.getModel()));

						mxUtils.writeFile(xml, filename);

						editor.setModified(false);
						editor.setCurrentFile(new File(filename));
						editor.updateTitle();
					}
					else if (ext.equalsIgnoreCase("txt"))
					{
						String content = mxGdCodec.encode(graph)
								.getDocumentString();

						mxUtils.writeFile(content, filename);
						editor.setModified(false);
						editor.updateTitle();
					}
					else
					{
						Color bg = null;

						if ((!ext.equalsIgnoreCase("gif") && !ext
								.equalsIgnoreCase("png"))
								|| JOptionPane.showConfirmDialog(
										graphComponent, "transparent Background?") != JOptionPane.YES_OPTION)
						{
							bg = graphComponent.getBackground();
						}

						if (selectedFilter == xmlPngFilter
								|| (editor.getCurrentFile() != null
										&& ext.equalsIgnoreCase("png") && !dialogShown))
						{
							saveXmlPng(editor, filename, bg);
						}
						else
						{
							BufferedImage image = mxCellRenderer
									.createBufferedImage(graph, null, 1, bg,
											graphComponent.isAntiAlias(), null,
											graphComponent.getCanvas());

							if (image != null)
							{
								ImageIO.write(image, ext, new File(filename));
								editor.setModified(false);
								editor.updateTitle();
							}
							else
							{
								JOptionPane.showMessageDialog(graphComponent,"no Image Data");
							}
						}
					}
					
					
				}
				catch (Throwable ex)
				{
					ex.printStackTrace();
					JOptionPane.showMessageDialog(graphComponent,
							ex.toString(), "ERROR",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		
		}
		
		
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			editor = editor == null ? getEditor(e) : editor;
			save(editor);

		}
	}
	
	@SuppressWarnings("serial")
	public static class UndoAction extends EditorAction
	{
		
		public UndoAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}
		
		public UndoAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}


		public void actionPerformed(ActionEvent e)
		{
			editor = editor == null ? getEditor(e) : editor;

			if (editor != null)
			{

				editor.getUndoManager().undo();
				editor.setModified(true);
				editor.updateTitle();
				
				
			}
		}
	}
	
	@SuppressWarnings("serial")
	public static class RedoAction extends EditorAction
	{
		
		public RedoAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}
		
		public RedoAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}


		public void actionPerformed(ActionEvent e)
		{
			editor = editor == null ? getEditor(e) : editor;

			if (editor != null)
			{

				editor.getUndoManager().redo();
				editor.setModified(true);
				editor.updateTitle();
				
			}
		}
	}
		
	
	public static class ZoomInAction extends EditorAction
	{


		public ZoomInAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public ZoomInAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}



		private static final long serialVersionUID = -7500195051313272384L;



		public void actionPerformed(ActionEvent e)
		{
			editor = editor == null ? getEditor(e) : editor;
			editor.getGraphComponent().zoomIn();
		}

	}

	public static class ZoomOutAction extends EditorAction
	{


		public ZoomOutAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public ZoomOutAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}



		private static final long serialVersionUID = -7500195051313272384L;



		public void actionPerformed(ActionEvent e)
		{

			editor = editor == null ? getEditor(e) : editor;
			editor.getGraphComponent().zoomOut();
		}
	
		
		
	}

	public static class ZoomActualAction extends EditorAction
	{


		public ZoomActualAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public ZoomActualAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}


		private static final long serialVersionUID = -7500195051313272384L;


		public void actionPerformed(ActionEvent e)
		{

			editor = editor == null ? getEditor(e) : editor;
			editor.getGraphComponent().zoomActual();
		}

	}
	
	public static class AddAction extends EditorAction
	{


		public AddAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public AddAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}



		private static final long serialVersionUID = -7500195051313272384L;



		public void actionPerformed(ActionEvent e)
		{
			editor = editor == null ? getEditor(e) : editor;

			if (editor != null)
			{
				editor.addVertex(editor.getGraphComponent().getWidth()/2, 
						editor.getGraphComponent().getHeight()/2, null);
				
			}
		}

	}

	
	@SuppressWarnings("serial")
	public static class OpenAction extends EditorAction
	{
		
		public OpenAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public OpenAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}
		
		protected String lastDir;

		/**
		 * 
		 */
		protected void resetEditor(Editor editor)
		{
			editor.setModified(false);
			editor.getUndoManager().clear();
			editor.getGraphComponent().zoomAndCenter();
		}

		/**
		 * Reads XML+PNG format.
		 */
		protected void openXmlPng(Editor editor, File file)
				throws IOException
		{
			Map<String, String> text = mxPngTextDecoder
					.decodeCompressedText(new FileInputStream(file));

			if (text != null)
			{
				String value = text.get("mxGraphModel");

				if (value != null)
				{
					Document document = mxUtils.parseXml(URLDecoder.decode(
							value, "UTF-8"));
					mxCodec codec = new mxCodec(document);
					codec.decode(document.getDocumentElement(), editor
							.getGraphComponent().getGraph().getModel());
					editor.setCurrentFile(file);
					resetEditor(editor);

					return;
				}
			}

			JOptionPane.showMessageDialog(editor,
					mxResources.get("imageContainsNoDiagramData"));
		}

		/**
		 * @throws IOException 
		 * 
		 */
		protected void openVdx(Editor editor, File file,
				Document document)
		{
			mxGraph graph = editor.getGraphComponent().getGraph();

			// Replaces file extension with .mxe
			String filename = file.getName();
			filename = filename.substring(0, filename.length() - 4) + ".mxe";

			if (new File(filename).exists()
					&& JOptionPane.showConfirmDialog(editor,
							mxResources.get("overwriteExistingFile")) != JOptionPane.YES_OPTION)
			{
				return;
			}

			((mxGraphModel) graph.getModel()).clear();
//			mxVdxCodec.decode(document, graph);
//			editor.getGraphComponent().zoomAndCenter();
//			editor.setCurrentFile(new File(lastDir + "/" + filename));
		}

		/**
		 * @throws IOException
		 *
		 */
		protected void openGD(Editor editor, File file,
				mxGdDocument document)
		{
			mxGraph graph = editor.getGraphComponent().getGraph();

			// Replaces file extension with .mxe
			String filename = file.getName();
			filename = filename.substring(0, filename.length() - 4) + ".mxe";

			if (new File(filename).exists()
					&& JOptionPane.showConfirmDialog(editor,
							mxResources.get("overwriteExistingFile")) != JOptionPane.YES_OPTION)
			{
				return;
			}

			((mxGraphModel) graph.getModel()).clear();
			mxGdCodec.decode(document, graph);
			editor.getGraphComponent().zoomAndCenter();
			editor.setCurrentFile(new File(lastDir + "/" + filename));
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			editor = editor == null ? getEditor(e) : editor;
			
			if (editor != null)
			{
				if (!editor.isModified()
						|| JOptionPane.showConfirmDialog(editor,"Lose Changes?") == JOptionPane.YES_OPTION)
				{
					mxGraph graph = editor.getGraphComponent().getGraph();

					if (graph != null)
					{
						String wd = (lastDir != null) ? lastDir : System.getProperty("user.dir");

						JFileChooser fc = new JFileChooser(wd);

						// Adds file filter for supported file format
						KeyFileFilter defaultFilter = new KeyFileFilter(
								".mxe", mxResources.get("allSupportedFormats")
										+ " (.mxe, .png, .vdx)")
						{

							public boolean accept(File file)
							{
								String lcase = file.getName().toLowerCase();

								return super.accept(file)
										|| lcase.endsWith(".png")
										|| lcase.endsWith(".vdx");
							}
						};
						fc.addChoosableFileFilter(defaultFilter);

						fc.addChoosableFileFilter(new KeyFileFilter(".mxe",
								"mxGraph Editor (.mxe)"));
						fc.addChoosableFileFilter(new KeyFileFilter(".png",
								"PNG+XML (.png)"));

						// Adds file filter for VDX import
						fc.addChoosableFileFilter(new KeyFileFilter(".vdx",
								"XML Drawing (.vdx)"));

						// Adds file filter for GD import
						fc.addChoosableFileFilter(new KeyFileFilter(".txt",
								"Graph Drawing (.txt)"));

						fc.setFileFilter(defaultFilter);

						int rc = fc.showDialog(null,
								mxResources.get("openFile"));

						if (rc == JFileChooser.APPROVE_OPTION)
						{
							lastDir = fc.getSelectedFile().getParent();

							try
							{
								if (fc.getSelectedFile().getAbsolutePath()
										.toLowerCase().endsWith(".png"))
								{
									openXmlPng(editor, fc.getSelectedFile());
								}
								else if (fc.getSelectedFile().getAbsolutePath()
										.toLowerCase().endsWith(".txt"))
								{
									mxGdDocument document = new mxGdDocument();
									document.parse(mxUtils.readFile(fc
											.getSelectedFile()
											.getAbsolutePath()));
									openGD(editor, fc.getSelectedFile(),
											document);
								}
								else
								{
									Document document = mxUtils
											.parseXml(mxUtils.readFile(fc
													.getSelectedFile()
													.getAbsolutePath()));

									if (fc.getSelectedFile().getAbsolutePath()
											.toLowerCase().endsWith(".vdx"))
									{
										openVdx(editor, fc.getSelectedFile(),
												document);
									}
									else
									{
										mxCodec codec = new mxCodec(document);
										codec.decode(
												document.getDocumentElement(),
												graph.getModel());
										editor.setCurrentFile(fc
												.getSelectedFile());
									}

									resetEditor(editor);
								}
							}
							catch (IOException ex)
							{
								ex.printStackTrace();
								JOptionPane.showMessageDialog(
										editor.getGraphComponent(),
										ex.toString(),
										mxResources.get("error"),
										JOptionPane.ERROR_MESSAGE);
							}
						}
					}
				}
			}
		}
	}
	
	@SuppressWarnings("serial")
	public static class DeleteAction extends EditorAction{
		public DeleteAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public DeleteAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}
		
		public void actionPerformed(ActionEvent e){
			editor = editor == null ? getEditor(e) : editor;
			editor.deleteSelectedCells();
		}
	
	}
	
	
	@SuppressWarnings("serial")
	public static class NewTopologyAction extends EditorAction{
		public NewTopologyAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public NewTopologyAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}
		
		public void actionPerformed(ActionEvent e){
			editor = editor == null ? getEditor(e) : editor;
			
			if (!editor.isModified()
					|| JOptionPane.showConfirmDialog(editor,"Lose Changes?") == JOptionPane.YES_OPTION)
			{
				mxGraph graph = editor.getGraphComponent().getGraph();
				graph.selectAll();
				editor.deleteSelectedCells();
				
				
			}
			
			
		}
	
	}
	
	@SuppressWarnings("serial")
	public static class VerifyAction extends EditorAction{
		public VerifyAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public VerifyAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}
		
		public void actionPerformed(ActionEvent e){
			editor = editor == null ? getEditor(e) : editor;
			boolean fail = false;
			
			logger.log(Level.INFO, "Verify");
			if(!checkForNameSemantic()){
				JOptionPane.showMessageDialog(null, "Please name Nodes after their micro IP addresses (e.g. 172.122)","Wrong Node Name(s)", JOptionPane.ERROR_MESSAGE);
				fail = true;
			}
			if(!checkForDoubleNames()){
				JOptionPane.showMessageDialog(null, "You have double identities, fix it please", "Double Node Name(s)", JOptionPane.ERROR_MESSAGE);
				fail = true;
			}
			if(!checkForConnectivity()){
				JOptionPane.showMessageDialog(null, "Atleast one node seems not to be reachable from the sink", "Connectivity not given", JOptionPane.ERROR_MESSAGE);
				fail = true;
			}
			
			if(!fail){
				editor.setVerifyModified(false);
				JOptionPane.showMessageDialog(null, "Verification succeeded. Naming convention met, no double identities found and all nodes are rechable.", "Successful", JOptionPane.INFORMATION_MESSAGE);
				editor.updateTitle();
			}
		
		}
	
		
		private boolean checkForConnectivity() {
			
			boolean ok = true;
			mxGraph graph = editor.getGraphComponent().getGraph();
			
			Object[] children = mxGraphModel.getChildren(graph.getModel(), graph.getDefaultParent());
			
			for (Object object : children) {
				mxCell cell = (mxCell)object;
				if(cell.isVertex() && !cell.getId().equals("sink")){
					if(!searchSink(cell, new HashSet<mxICell>())){
						ok = false;
						graph.getSelectionModel().addCell(cell);
					}
				}
			}		
			return ok;
		}
		
		private boolean searchSink(mxICell cell, Set<mxICell> visited_nodes){
			boolean ok1 = false;
			boolean ok2 = false;
			
			visited_nodes.add(cell);
			for (int i = 0; i < cell.getEdgeCount(); i++) {
				mxICell edge = cell.getEdgeAt(i);
				
				mxICell edge_source = edge.getTerminal(true);
				mxICell edge_destination = edge.getTerminal(false);
				
				if(edge_source.getId().equals(cell.getId()) && !visited_nodes.contains(edge_destination)){
					ok1 = isSink(edge_destination, visited_nodes);
				}else if(!visited_nodes.contains(edge_source)){
					ok2 = isSink(edge_source, visited_nodes);
				}else{
					continue;
				}
				if(ok1 | ok2)
					return true;
				
			}
			return false;
		}

		private boolean isSink(mxICell node, Set<mxICell> visited_nodes) {
			if(node != null){
				if(!node.getId().equals("sink")){
					return searchSink(node,visited_nodes);
				}else{
					return true;
				}
			}else{
				return false;
			}
		}
		
		
		private boolean checkForNameSemantic() {
			
			boolean ok = true;
			String regex = "^[0-9]+\\.[0-9]+$";
			Pattern p = Pattern.compile(regex);
			
			mxGraph graph = editor.getGraphComponent().getGraph();
			
			Object[] children = mxGraphModel.getChildren(graph.getModel(), graph.getDefaultParent());
			
			for (Object object : children) {
				mxCell cell = (mxCell)object;
				if(cell.isVertex() && !cell.getId().equals("sink")){
					
					Matcher m = p.matcher((String)cell.getValue());
					if(!m.matches()){
						logger.log(Level.INFO, "Cell not matching naming convention: " + cell.getValue());
						graph.getSelectionModel().addCell(cell);
						ok = false;
					}
					
				}
			}		
			return ok;
		}

		private boolean checkForDoubleNames() {
			boolean ok = true;
			mxGraph graph = editor.getGraphComponent().getGraph();
			
			Object[] children = mxGraphModel.getChildren(graph.getModel(), graph.getDefaultParent());
			
			Set<String> nameSet = new HashSet<String>();
			
			for (Object object : children) {
				mxCell cell = (mxCell)object;
				if(cell.isVertex()){
					if(!nameSet.add((String)cell.getValue())){
						logger.log(Level.INFO, "duplicate name found !");
						graph.getSelectionModel().addCell(cell);
						ok = false;
					}
					
				}
			}
			return ok;
		}
		
	}
	
	@SuppressWarnings("serial")
	public static class GenerateAction extends EditorAction{
		public GenerateAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public GenerateAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}
		
		public void actionPerformed(ActionEvent e){
			editor = editor == null ? getEditor(e) : editor;
			
			logger.log(Level.INFO, "GenerateAction");
			
			editor.resetKeyStore();
			
			generateSinkKeys();
			generatePairwiseSharedKeys();
			generateClusterKeys();
			JOptionPane.showMessageDialog(null, "LEAP Keys generated sucessfully", "Successful Keygeneration", JOptionPane.INFORMATION_MESSAGE);
			editor.setKeyModified(false);
			editor.updateTitle();
			
		}
		
		private void generatePairwiseSharedKeys() {
			mxGraph graph = editor.getGraphComponent().getGraph();
			Object[] children = mxGraphModel.getChildren(graph.getModel(), graph.getDefaultParent());
			
			for (Object object : children) {
				mxCell cell = (mxCell) object;
				mxICell source = cell.getTerminal(true);
				mxICell destination = cell.getTerminal(false);
				
				if(cell.isEdge()){
					if(!cell.getTerminal(true).getId().equals("sink") && !cell.getTerminal(false).getId().equals("sink")){
						Key pairwiseKey = generateKey(true, false, false);
						editor.getKeyStore().getNodes().get(source.getValue().toString()).addPairwiseKeys(destination.getValue().toString(), pairwiseKey);
						editor.getKeyStore().getNodes().get(destination.getValue().toString()).addPairwiseKeys(source.getValue().toString(), pairwiseKey);
					}
					
					editor.getKeyStore().getNodes().
						get(source.getValue().toString()).
							getNeighbors().
								add(destination.getValue().toString());
					
					
					editor.getKeyStore().
						getNodes().
							get(destination.getValue().toString()).
								getNeighbors().
									add(source.getValue().toString());
					
				}
			}
		}
		
		private void generateClusterKeys(){
			mxGraph graph = editor.getGraphComponent().getGraph();
			Object[] children = mxGraphModel.getChildren(graph.getModel(), graph.getDefaultParent());
			for (Object object : children) {
				mxCell cell = (mxCell) object;
				if(cell.isVertex() && !cell.getId().equals("sink")){
					int vertexEdgeCount = cell.getEdgeCount();
					
					Key clusterKey = generateKey(true, false, false);
					editor.getKeyStore().getNodes().get(cell.getValue().toString()).putClusterKey(cell.getValue().toString(), clusterKey);
					
					for(int i = 0; i < vertexEdgeCount; i++){
						mxICell edge = cell.getEdgeAt(i);
						mxICell partner = edge.getTerminal(true).getValue().toString().equals(cell.getValue().toString()) ? edge.getTerminal(false):edge.getTerminal(true);
						
						if(partner.getId().equals("sink"))
							continue;
						
//						if(partner.getValue().toString().equals(cell.getValue().toString())){
//							continue;
//						}
						
						editor.getKeyStore().getNodes().get(partner.getValue().toString()).putClusterKey(cell.getValue().toString(), clusterKey);
						
					}
				}
			}
		}

		private Key generateKey(boolean aes128, boolean aes256, boolean ecc){
			Key newKey = new Key();
			
			if(aes128){
				char aes_128[] = new char[16];
				for (int i = 0; i < aes_128.length; i++) {
					aes_128[i] = (char)Math.round(Math.random()*255);
				}
				newKey.setAes_128(aes_128);
			}
			
			return newKey;
		}
		
		private void generateSinkKeys(){
			mxGraph graph = editor.getGraphComponent().getGraph();
			editor.getKeyStore().getSink_keys().setGroup_key(generateKey(true,false,false));
			Object[] children = mxGraphModel.getChildren(graph.getModel(), graph.getDefaultParent());
			
			for (Object object : children) {
				mxCell cell = (mxCell) object;
				if(cell.isVertex() && !cell.getId().equals("sink")){
					Key individualKey = generateKey(true, false, false);
					editor.getKeyStore().getSink_keys().addIndividual_keys(cell.getValue().toString(), individualKey);
					editor.getKeyStore().getNodes().get(cell.getValue().toString()).setIndividualKey(individualKey);
				}
			}
		}
	}
	
	@SuppressWarnings("serial")
	public static class DeployAction extends EditorAction{
	
		
		
		public DeployAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public DeployAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}
		
		private String[] generatePackedKeys(Map<String,Key> keys, String nodeName){
			
			if(keys.size() == 0)
				return new String[0];
			
			String[][] joinedClustersArrays = KeyHelper.extractNodeKeyCombo(keys, KeyAlgorithms.AES_128, nodeName);
			String[] returnString = new String[joinedClustersArrays.length];
			
			for (int i = 0; i < joinedClustersArrays.length;i++) {
				if(!joinedClustersArrays[i][0].equals(nodeName)){
					
					String address = transformTo16BitHexString(joinedClustersArrays[i][0]);
					
					returnString[i]= address + joinedClustersArrays[i][1];
				}
			}
			return returnString;
		}
		
		static private String transformTo16BitHexString(String string){
			
			char value[] = new char[2];
			value[1] = (char) Integer.parseInt(string.substring(0, string.indexOf('.')));
			value[0] = (char) Integer.parseInt(string.substring(string.indexOf('.')+1));
			
			return KeyHelper.charArrayToHexString(value);
		}

		public void actionPerformed(ActionEvent e){
			editor = editor == null ? getEditor(e) : editor;
			
			logger.log(Level.INFO, "DeployAction");
			
			/*
			 * 
			 * Statistical stuff
			 */
			int individual_key_counter = 0;
			int group_key_counter = 0;
			int pairwise_key_counter = 0;
			int own_cluster_key_counter = 0;
			int joined_cluster_key_counter = 0;
			int finalize_counter = 0;
			
			int total_key_counter = 0;
			
			int node_counter = 0;
			int edge_counter = 0;
			
			
			mxGraph graph = editor.getGraphComponent().getGraph();
			Object[] children = mxGraphModel.getChildren(graph.getModel(), graph.getDefaultParent());
			edge_counter = countEdges(children);
			ArrayList<MessageWrapper> commandQueue = new ArrayList<MessageWrapper>();
			
			
			
			for (Object object : children) {
				mxCell cell = (mxCell) object;
				if(cell.isVertex() && !cell.getId().equals("sink")){
					node_counter++;
					NodeKeyStore keys = editor.getKeyStore().getNodes().get(cell.getValue());
					String individual_Key = KeyHelper.charArrayToHexString(keys.getIndividualKey().getAes_128());
					String own_cluster_key = KeyHelper.charArrayToHexString(keys.getClusterKeys().get(cell.getValue()).getAes_128());
					String[] cluster_keys = generatePackedKeys(keys.getClusterKeys(), (String) cell.getValue());
					
					
					String groupKey = KeyHelper.charArrayToHexString(editor.getKeyStore().getSink_keys().getGroup_key().getAes_128());
					String[] pairwiseKeys = generatePackedKeys(keys.getPairwiseKeys(), (String) cell.getValue());
					
					String destination = (String) cell.getValue();

//					String highbyte = KeyHelper.charArrayToHexString(destination.substring(0, destination.indexOf('.')).toCharArray());
//					String lowbyte = KeyHelper.charArrayToHexString(destination.substring(destination.indexOf('.')+1).toCharArray());
					
					
					try{
						System.out.println("building messages for " + destination);
						
//						System.out.println("pairwise_keys:");
//						for (String string : pairwiseKeys) {
//							System.out.println(string);
//						}
//						
//						System.out.println("cluster_keys:");
//						for (String string : cluster_keys) {
//							System.out.println(string);
//						}
						
						
						MessageWrapper mw = new MessageWrapper(destination,individual_Key,groupKey,own_cluster_key, pairwiseKeys, cluster_keys);
//						mw.buildBasicMessage(individual_Key, groupKey, own_cluster_key, (int) Math.ceil((pairwiseKeys.length + cluster_keys.length) / 5.0));
//						mw.buildExtendedMessages(pairwiseKeys, cluster_keys);
						commandQueue.add(mw);
					}catch(Exception ex){
						System.out.println("failed building message, bailing out");
						return;
					}
					
				}
			}
			
//			printStatistic(
//					individual_key_counter,
//					group_key_counter,
//					pairwise_key_counter,
//					own_cluster_key_counter,
//					joined_cluster_key_counter,
//					finalize_counter,
//					total_key_counter,
//					node_counter,
//					edge_counter);
			
			
//			printMessages(commandQueue);
			
//			SerialCommunicator.resetPersistency();
			JProgressBarDialog progress = new JProgressBarDialog(commandQueue.size());
			logger.log(Level.INFO, "generated progress bar with max=" + commandQueue.size());
			SerialWorker worker_thread = new SerialWorker(commandQueue,progress);
			logger.log(Level.INFO, "Thread who summoned worker: " + Thread.currentThread().getName());
			
			System.out.println("Degbugging commandQueue:");
			try{
				for (MessageWrapper messageWrapper : commandQueue) {
					for (String msg : messageWrapper.getMessages()) {
						System.out.println(msg);
					}
				}
			}catch(Exception e2){
				//nothing
			}
			worker_thread.start();
		
			
		}
		
		
		


		

//		private void printStatistic(int individual_key_counter,
//				int group_key_counter, int pairwise_key_counter,
//				int own_cluster_key_counter, int joined_cluster_key_counter,
//				int finalize_counter, int total_key_counter, int node_counter,
//				int edge_counter) {
//			
//			System.out.println("Statistic:");
//			System.out.println("individual key transmissions: " + individual_key_counter);
//			System.out.println("group key submissions: " + group_key_counter);
//			System.out.println("pairwise key transmissions: " + pairwise_key_counter);
//			System.out.println("own cluster key transmissions: " + own_cluster_key_counter);
//			System.out.println("joined cluster key transmissions: " + joined_cluster_key_counter);
//			System.out.println("finalize transmissions: " + finalize_counter);
//			
//			System.out.println("");
//			System.out.println("Total key messages transmitted: " + total_key_counter);
//			System.out.println("node count: " + node_counter);
//			System.out.println("edge count: " + edge_counter);
//			System.out.println("-----------------------");
//			
//		}

		private void printMessages(ArrayList<MessageWrapper> commandQueue) {
			for (MessageWrapper messageWrapper : commandQueue) {
				try {
					for (String msg : messageWrapper.getMessages()) {
						System.out.println(msg);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}

		public int countEdges(Object[] children) {
			Set<Object> edge_set = new HashSet<Object>();
			for (Object cell: children){
				
				Object[] edges = mxGraphModel.getEdges(editor.getGraphComponent().getGraph().getModel(), cell);
				
				for(Object edge: edges){
					mxICell edge_cell = (mxICell) edge;
					if(edge_cell.getTerminal(true).getId().toString().equals("sink") || edge_cell.getTerminal(false).getId().toString().equals("sink"))
						continue;
					else
						edge_set.add(edge);
				}
			}
			
			return edge_set.size();
		}
		
	
	}
	
	@SuppressWarnings("serial")
	public static class ReadIdFromSerialAction extends EditorAction{
		public ReadIdFromSerialAction(String text, ImageIcon icon, String desc,
				Integer mnemonic) {
			super(text, icon, desc, mnemonic);
		}
		
		public ReadIdFromSerialAction(String text, ImageIcon icon, String desc,
				Integer mnemonic, Editor editor) {
			super(text, icon, desc, mnemonic, editor);
		}
		
		public void actionPerformed(ActionEvent e){
			editor = editor == null ? getEditor(e) : editor;
			this.setEnabled(false);
			new ButtonToggle(this).start();
			logger.log(Level.INFO, "ReadIdFromSerialAction");
//			SerialCommunicator.resetPersistency();

			try{
				String[] list = SerialCommunicator.retrieveHostSerialPortList();
				StealthNodeAdder stealth = new StealthNodeAdder(editor);
				String[] keywords2 = {"[0-9]{1,3}[.][0-9]{1,3}\n"};
				String[] commands = new String[]{"sky-getrimeaddress"};
				if(list.length == 1){
					stealth.setPort(list[0]);
				} else {
					SerialPortSelector nodeAdder = new SerialPortSelector(stealth, keywords2, commands);
				}
			}catch(Exception e2){
				//USB stuff went wrong
			}
//			SerialCommunicator.resetPersistency();
			
		}
	
	}

	
	
	
}
