package com.xsb.xj.containers;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JComponent;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;

/**
 * XJ extension of javax.swing.Box. Allows for the creation of invisible gui
 * objects which can be used to help layout guis better. This is not a container
 * and probably should not have been added to this package.
 *
 *@author    Harpreet Singh
 *@version   $Id: XJBox.java,v 1.3 2004/07/08 19:43:22 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJBox extends JComponent implements XJComponent, XJComponentTree {
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;

	private final static String RIGID_AREA   = "rigidArea";
	private final static String HORIZ_STRUT  = "horizontalStrut";
	private final static String VERT_STRUT   = "verticalStrut";

	public XJBox(PrologEngine engine, GUITerm gt) {
		this.gt = gt;
		this.engine = engine;
		dirty = false;

		TermModel rigidArea   = gt.findProperty(RIGID_AREA);
		TermModel horizStrut  = gt.findProperty(HORIZ_STRUT);
		TermModel vertStrut   = gt.findProperty(VERT_STRUT);
		Component c = null;

		if(rigidArea != null) {
			c  = createRigidArea(gt);
		} else if(horizStrut != null) {
			c  = createHorizStrut(gt);
		} else if(vertStrut != null) {
			c  = createVertStrut(gt);
		} else {
			System.err.println("XJBox : specify either " + RIGID_AREA + " or "
				+ HORIZ_STRUT + " or " + VERT_STRUT);
		}


		if(c != null) {
			//this.setSize(c.getWidth(), c.getHeight());
			this.setMaximumSize(c.getMaximumSize());
			this.add(c);
		}

		return;
	}

	public TermModel[] getMyGUIs(){
		return gt.getMyGUIs(); 
	}
	public Dimension getBounds(GUITerm gt) {
		TermModel bounds  = gt.findProperty("bounds");
		if(bounds != null) {
			int childcount  = bounds.getChildCount();
			if(childcount == 2) {
				int w  = ((TermModel) bounds.getChild(0)).intValue();
				int h  = ((TermModel) bounds.getChild(1)).intValue();
				return new Dimension(w, h);
			} else {
				System.err.println("Bounds not specified in XJBox");
				return null;
			}
		} else {
			//System.err.println("Bounds not specified in XJBox");
			return null;
		}
	}

	public Component createRigidArea(GUITerm gt) {
		Dimension d  = getBounds(gt);
		if(d != null) {
			return Box.createRigidArea(d);
		} else {
			return null;
		}
	}

	public Component createHorizStrut(GUITerm gt) {
		Dimension d  = getBounds(gt);
		if(d != null) {
			return Box.createHorizontalStrut((int) d.getWidth());
		} else {
			return null;
		}
	}

	public Component createVertStrut(GUITerm gt) {
		Dimension d  = getBounds(gt);
		if(d != null) {
			return Box.createVerticalStrut((int) d.getHeight());
		} else {
			return null;
		}
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
	public void refreshGUI() { }

	public boolean loadFromGUI() {
		dirty = false;
		return true;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDefaultValue(TermModel dv) {
	}

	public void selectGUI(Object[] parts) {
		GUITerm.typicalContainerSelect(this, parts);
	}
        
        public void destroy() {
        }
        
}

