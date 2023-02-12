package com.xsb.xj;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JSeparator;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;

@SuppressWarnings("serial")
public class XJSeparator extends JSeparator implements XJComponent{
	GUITerm gt;
	PrologEngine engine;

	public XJSeparator(PrologEngine engine, GUITerm gt){
		super();
		this.engine=engine;

		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
		this.gt=gt;

	}

	public Dimension getPreferredSize(){
		return gt.getPreferredSize(super.getPreferredSize());
	}
	
	public void setDefaultValue(TermModel dv){
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
	
	/** Assumes that the GUI value is always acceptable */
	public boolean loadFromGUI(){
		return true;
	}
	
	public boolean isDirty(){
		return false;
	}

	public void selectGUI(Object[] parts){
		GUITerm.typicalAtomicSelect(this,parts);
	}

        public void destroy() {
        }
        
}
