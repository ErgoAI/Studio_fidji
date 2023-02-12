package com.xsb.xj.containers;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;

import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import java.awt.Dimension;

/**
 * XJ extension of javax.swing.JPanel with FlowLayout.
 *
 *@author    HSingh
 *@version   $Id: XJFlowLayout.java,v 1.2 2004/07/08 19:43:22 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJFlowLayout extends JPanel implements XJComponent, XJComponentTree {
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;

	public XJFlowLayout(PrologEngine engine, GUITerm gt) {
		this.gt = gt;
		this.engine = engine;
		dirty = false;

		FlowLayout f     = new FlowLayout();
		TermModel hgap   = gt.findProperty("hgap");
		TermModel vgap   = gt.findProperty("vgap");
		TermModel align  = gt.findProperty("align");
		
		//horizontal gap
		if(hgap != null) {
			int h  = ((Integer) hgap.node).intValue();
			f.setHgap(h);
		}

		//vertical gap
		if(vgap != null) {
			int v  = ((Integer) vgap.node).intValue();
			f.setVgap(v);
		}

		//alignment
		if(align != null) {
			String a  = (String) align.node;
			if(a.equals("center")) {
				f.setAlignment(FlowLayout.CENTER);
			} else if(a.equals("leading")) {
				f.setAlignment(FlowLayout.LEADING);
			} else if(a.equals("left")) {
				f.setAlignment(FlowLayout.LEFT);
			} else if(a.equals("right")) {
				f.setAlignment(FlowLayout.RIGHT);
			} else if(a.equals("trailing")) {
				f.setAlignment(FlowLayout.TRAILING);
			} else {
				;
			}
		}
		
		setLayout(f);

		//add components
		for(int c = 0; c < gt.getChildCount(); c++) {
			GUITerm child  = (GUITerm) gt.children[c];

			if(!child.isInvisible()) {
				XJComponent component  = child.makeGUI(engine);
				add((JComponent) component);
			}
		}
	}

	public TermModel[] getMyGUIs(){
		return gt.getMyGUIs(); 
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
    public Dimension getPreferredSize(){
    	Dimension D = super.getPreferredSize();
    	return gt.getPreferredSize(D);
    }
        
}

