package de.cased.utilities;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.util.mxGraphActions;


/**
 * @author Administrator
 * 
 */
public class KeyboardHandler extends mxKeyboardHandler
{

	public KeyboardHandler(mxGraphComponent graphComponent)
	{
		super(graphComponent);
	}


	public InputMap getInputMap(int condition)
	{
		InputMap map = super.getInputMap(condition);

		if (condition == JComponent.WHEN_FOCUSED && map != null)
		{
			map.put(KeyStroke.getKeyStroke("control S"), "save");
			map.put(KeyStroke.getKeyStroke("control shift S"), "saveAs");
			map.put(KeyStroke.getKeyStroke("control N"), "new");
			map.put(KeyStroke.getKeyStroke("control O"), "open");
			map.put(KeyStroke.getKeyStroke("control Z"), "undo");
			map.put(KeyStroke.getKeyStroke("control Y"), "redo");
			map.put(KeyStroke.getKeyStroke("control shift V"), "selectVertices");
			map.put(KeyStroke.getKeyStroke("control shift E"), "selectEdges");
			map.put(KeyStroke.getKeyStroke("control 0"), "zoomActual");
			map.put(KeyStroke.getKeyStroke("control Q"), "exit");
		}

		return map;
	}

	
	public ActionMap createActionMap()
	{
		ActionMap map = super.createActionMap();

		map.put("save", SettingsGenerator.generateSaveAction());
		map.put("saveAs", SettingsGenerator.generateSaveAsAction());
		map.put("new", SettingsGenerator.generateNewTopologyAction());
		map.put("open", SettingsGenerator.generateOpenAction());
		map.put("undo", SettingsGenerator.generateUndoAction());
		map.put("redo", SettingsGenerator.generateRedoAction());
		map.put("selectVertices", mxGraphActions.getSelectVerticesAction());
		map.put("selectEdges", mxGraphActions.getSelectEdgesAction());
		map.put("zoomActual", SettingsGenerator.generateZoomActualAction());
		map.put("exit", SettingsGenerator.generateExitAction());

		return map;
	}

}