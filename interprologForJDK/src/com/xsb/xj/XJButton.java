package com.xsb.xj;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.HelpManager;

/**
 *@version   $Id: XJButton.java,v 1.15 2004/07/08 19:43:22 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJButton extends JButton implements XJComponent {
	GUITerm gt;
	PrologEngine engine;

	// constants for Prolog developers
	final static String ICON   = "icon";

	//horizontal & vertical text positions.
	final static String H_POS  = "hpos";
	final static String V_POS  = "vpos";
	private static final String BORDERPAINTED = "borderpainted";

	private boolean firstTime;
	
	/** If not null, is entirely Java-specified, no direct Prolog interactions*/
	Action javaAction;
	
	/** Construct a JButton from a Java action, rather than by Prolog-side GT specification; 
	it will originate no direct Prolog interactions */
	public XJButton(Action action, PrologEngine engine){
		super(action);
		this.javaAction=action;
		this.gt = null; // GUITerm.makeGUI will patch this
		this.engine = engine;
	}
	
	public XJButton(PrologEngine engine, GUITerm gt) {
		super();
		javaAction=null;
		this.gt = gt;
		this.engine = engine;

		/*
		    if (!(gt.node instanceof String))
		    throw new XJException("Buttons require an atom node");
		  */
		TermModel zeroInset     = gt.findProperty("zeroInset");
		if(zeroInset != null) {
			this.setMargin(new Insets(0, 0, 0, 0));
		}

		//margin is distance between text and or icon and the edge (optional property)
		TermModel margin        = gt.findProperty("margin");
		if(margin != null) {
			if(margin.getChildCount() == 4) {
				int top     = ((TermModel) margin.getChild(0)).intValue();
				int left    = ((TermModel) margin.getChild(1)).intValue();
				int bottom  = ((TermModel) margin.getChild(2)).intValue();
				int right   = ((TermModel) margin.getChild(3)).intValue();
				this.setMargin(new Insets(top, left, bottom, right));
			} else {
				System.err.println("XJButton: margin need to be " +
					"specified as margin(top,left,bottom,right)");
			}
		}

		//set the horizontal text position
		//property is optional; trailing is the default c.f. jdk 1.4.1
		TermModel hPos          = gt.findProperty(H_POS);
		if(hPos != null) {
			String horizTextPosition  = hPos.node.toString();
			if(horizTextPosition.equals("right")) {
				this.setHorizontalTextPosition(SwingConstants.RIGHT);
			} else if(horizTextPosition.equals("left")) {
				this.setHorizontalTextPosition(SwingConstants.LEFT);
			} else if(horizTextPosition.equals("center")) {
				this.setHorizontalTextPosition(SwingConstants.CENTER);
			} else if(horizTextPosition.equals("leading")) {
				this.setHorizontalTextPosition(SwingConstants.LEADING);
			} else {
				this.setHorizontalTextPosition(SwingConstants.TRAILING);
			}
		}

		//set the vertical text position.
		//property is optional, center is the default c.f. jdk 1.4.1
		TermModel vPos          = gt.findProperty(V_POS);
		if(vPos != null) {
			String vertTextPosition  = vPos.node.toString();
			if(vertTextPosition.equals("top")) {
				this.setVerticalTextPosition(SwingConstants.TOP);
			} else if(vertTextPosition.equals("bottom")) {
				this.setVerticalTextPosition(SwingConstants.BOTTOM);
			} else {
				this.setVerticalTextPosition(SwingConstants.CENTER);
			}
		}
		
		TermModel borderPainted = gt.findProperty(BORDERPAINTED);
		if (borderPainted!=null) 
			setBorderPainted(borderPainted.node.toString().equals("true"));


		TermModel iconLocation  = gt.findProperty(ICON);
		if(iconLocation != null)
			setIcon(XJDesktop.fetchIcon(this,iconLocation));

		firstTime = true;
		// following is commented. See refreshGUI
		//if (gt.findProperty(GUITerm.DISABLED)!=null) {this.setEnabled(false);}

		if(!gt.tipDescription().equals("")) {
			setToolTipText(gt.tipDescription());
		}
                
        HelpManager.registerXJComponentForPopupHelp(this);
        final JPopupMenu popup = XJDesktop.makeOperationsPopupMenu(this);
        if (popup!=null){
        	// There are menu operations....
        	addMouseListener(new MouseAdapter(){
        		public void mousePressed(MouseEvent e){
        			popup.show((Component)XJButton.this,e.getX(),e.getY());
        		}
        	});
        }
		// refreshGUI();
	}

	public Dimension getPreferredSize() {
		if (javaAction!=null) return super.getPreferredSize();
		else return gt.getPreferredSize(super.getPreferredSize());
	}

	public PrologEngine getEngine() {
		return engine;
	}

	public GUITerm getGT() {
		return gt;
	}
	public void setGT(GUITerm gt){
		this.gt=gt;
	}

	public void refreshGUI() {
		if (javaAction!=null) return;
		if(firstTime) {
			// doing it here instead of constructor because if we setEnabled(false)
			// in constructor and if the button has any actions specified for it
			// those actions will be attached after constructor invocation and will make
			// the button enabled again (button will take all properties of the action).
			// refreshGUI is called after setting the actions, so it can reset it
			// back to disabled
			// done only during the first rendering, because in responce to some event
			// somebody might call button.setEnabled(true) later in the program
			// and we do not want it to return to disabled state  on refreshing
			// after that.
			if(gt.findProperty(GUITerm.DISABLED) != null) {
				this.setEnabled(false);
			}
                        TermModel mnemonic  = gt.findProperty("mnemonic");
                        if(mnemonic != null) {
                            String mnemonicString  = mnemonic.node.toString();
                            this.setMnemonic(mnemonicString.charAt(0));
                        }
			firstTime = false;
		}
		setText((String) gt.node);
	}

	public boolean loadFromGUI() {
		return true;
	}

	public boolean isDirty() {
		return false;
	}

	/**
	 * This implementation does nothing
	 *
	 *@param dv  The new defaultValue value
	 */
	public void setDefaultValue(TermModel dv) {
	}

	public void selectGUI(Object[] parts) {
		GUITerm.typicalAtomicSelect(this, parts);
	}

	/* Seems dead code:
	public void removeAllActionListeners() {
		ActionListener[] listeners  = this.getActionListeners();
		String text                 = getText();
		for(int i = 0; i < listeners.length; i++) {
			super.removeActionListener(listeners[i]);
		}
		setText(text);
	}*/

    public void destroy() {
    }
    
    /** Stronger and more effective: affects the action(s) listening to this button; just enabling a button does not necessarily 
    enable a (say) XJAction */
    public void setEnabledOfActions(final boolean enable){
    	Runnable doer = new Runnable(){
    		public void run(){
				@SuppressWarnings("unused")
				boolean found = false;
				for (ActionListener AL : getActionListeners()){
					if (AL instanceof Action){
						found = true;
						((Action)AL).setEnabled(enable);
					}
				}
				/*if (!found) ... BUT "interestingly" I discovered once that this was not called by the action found, so unconditionally...:*/
				setEnabled(enable);
    		}
    	};
    	
		if (SwingUtilities.isEventDispatchThread()) doer.run();
		else 
			SwingUtilities.invokeLater(doer); // does NOT block here
    }
    
}
