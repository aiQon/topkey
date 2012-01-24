package de.cased.utilities;


import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

public abstract class EditorAction extends AbstractAction {

	private static final long serialVersionUID = 1L;
	protected Logger logger;
	protected Editor editor;
	
	public EditorAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
		super(text, icon);
		logger = Logger.getLogger("KeyGenerator");
		putValue(SHORT_DESCRIPTION, desc);
		putValue(MNEMONIC_KEY, mnemonic);
	}
	
	public EditorAction(String text, ImageIcon icon, String desc, Integer mnemonic, Editor editor){
		this(text, icon, desc, mnemonic);
		this.editor = editor;
	}

}
