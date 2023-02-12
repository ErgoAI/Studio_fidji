package com.xsb.xj.containers;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;

/**
 *  XJToolbar extends JToolBar
 *
 *@author     HSingh
 *@created    November 7, 2002
 */
@SuppressWarnings("serial")
public class XJToolBar extends JToolBar implements XJComponent, XJComponentTree {
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;

	public XJToolBar(PrologEngine engine, GUITerm gt) {
		this.gt = gt;
		this.engine = engine;
		dirty = false;

		for(int c = 0; c < gt.getChildCount(); c++) {
			GUITerm child = (GUITerm) gt.children[c];
			XJComponent line = child.makeGUI(engine);
			this.add((JComponent) line);
			TermModel separator = child.findProperty("separator");
			if(separator != null) {
				JSeparator js = new JSeparator(SwingConstants.VERTICAL);
				js.setMaximumSize(new Dimension(2, 20));
				this.add(js);
			}
		}

		TermModel rollover = gt.findProperty("rollover");
		if(rollover != null) {
			this.putClientProperty(new String("JToolBar.isRollover"), Boolean.TRUE);
		}

		TermModel notfloatable = gt.findProperty("notfloatable");
		if(notfloatable != null) {
			this.setFloatable(false);
		}

		TermModel vertical = gt.findProperty("vertical");
		if(vertical != null) {
			this.setOrientation(SwingConstants.VERTICAL);
		}

		setToolTipText(gt.tipDescription());
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
        
}

