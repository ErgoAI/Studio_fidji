package com.xsb.xj.containers;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JScrollPane;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;

/* Displays values of a term in scrollabe container
 *
 */
@SuppressWarnings("serial")
public class XJScrollPane extends JScrollPane implements XJComponent, XJComponentTree{
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;

	public XJScrollPane(PrologEngine engine, GUITerm gt){
		this.gt=gt;
		this.engine=engine;
		dirty = false;

		if(gt.getChildCount()>0){
			GUITerm viewportGT = (GUITerm)gt.getChild(0);
			//XJComponent component0 = new ValueRow(engine,leftChild);
			XJComponent component0 = viewportGT.makeGUI(engine);
 			setViewportView((Component)component0);
		}
        }
        
        public Dimension getPreferredSize(){
		return gt.getPreferredSize(super.getPreferredSize());
	}

	public TermModel[] getMyGUIs(){
		return gt.getMyGUIs(); 
	}
	public PrologEngine getEngine(){
		return engine;
	}

	public GUITerm getGT(){
		return gt;
	}
	public void setGT(GUITerm gt){
		this.gt=gt;
	}

	public void refreshGUI(){
	}

	public boolean loadFromGUI(){
	    dirty=false;
	    return true;
	}

	public boolean isDirty(){
		return dirty;
	}

	public void setDefaultValue(TermModel dv){
	}

	public void selectGUI(Object[] parts){
		GUITerm.typicalContainerSelect(this,parts);
	}

        public void destroy() {
        }
        
}
