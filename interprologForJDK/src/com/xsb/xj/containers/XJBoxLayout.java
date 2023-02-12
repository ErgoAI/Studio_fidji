package com.xsb.xj.containers;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import java.awt.Dimension;

/**
 * XJ extension of javax.swing.JPanel with BoxLayout. Ignores the root node.
 *
 *@author    Harpreet Singh
 *@version   $Id: XJBoxLayout.java,v 1.2 2004/07/08 19:43:22 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJBoxLayout extends JPanel implements XJComponent, XJComponentTree {
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;

	public XJBoxLayout(PrologEngine engine, GUITerm gt) {
		this.gt = gt;
		this.engine = engine;
		dirty = false;

		BoxLayout b;
		TermModel align  = gt.findProperty("align");

		//alignment
		if(align != null && ((String) align.node).equals("y_axis")) {
			b = new BoxLayout(this, BoxLayout.Y_AXIS);
		} else {
			b = new BoxLayout(this, BoxLayout.X_AXIS);
		}

		setLayout(b);

		//add components
		if (!gt.isInvisible() && gt.findProperty(GUITerm.RENDERTOPNODE)!=null) {
			// avoid recursive loop...
			add((JComponent)gt.makeGUIforNode(engine,XJBoxLayout.class));
		}

		for(int c = 0; c < gt.getChildCount(); c++) {
			GUITerm child  = (GUITerm) gt.children[c];

			if(!child.isInvisible()) {
				XJComponent component  = child.makeGUI(engine);
				add((JComponent) component);
			}
		}
	}

	public PrologEngine getEngine() {
		return engine;
	}

	public TermModel[] getMyGUIs(){
		return gt.getMyGUIs(); 
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
        
    public Dimension getPreferredSize(){
    	Dimension D = super.getPreferredSize();
    	return gt.getPreferredSize(D);
    }
}

