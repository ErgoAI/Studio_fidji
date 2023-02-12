package com.xsb.xj;
import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;

/** Renders a GUITerm tree into a single row panel, one node after another, left to right depth first. 
Asks each child to produce an XJComponent with makeGUI, if it is not invisible.
Does not render captions for nodes. Useful for example for tree node templates */
@SuppressWarnings("serial")
public class ValueRow extends JPanel implements XJComponent,XJComponentTree{
	GUITerm gt;
	PrologEngine engine;
	public ValueRow(PrologEngine engine,GUITerm gt){
        setOpaque(false);
		this.gt=gt;
		this.engine=engine;
		setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
		layGT(engine,gt);
	}
	void layGT(PrologEngine engine,GUITerm gt){
		XJComponent component = null;
		if (!gt.isInvisible()) {
			component = gt.makeGUIforNode(engine,ValueRow.class); // avoid recursive loop
			add((JComponent)component);
		}
		// if (!gt.isLeaf() && (component!=null && !(component instanceof XJComponentTree)))
		// The above condition was preventing making the root node invisible
		if (!gt.isLeaf() && !(component!=null && (component instanceof XJComponentTree))){
			for (int c=0;c<gt.getChildCount();c++)
				layGT(engine,(GUITerm)(gt.getChild(c)));
		}
	}
	public GUITerm getGT(){
		return gt;
	}
	public void setGT(GUITerm gt){
		this.gt=gt;
	}
	public PrologEngine getEngine(){
		return engine;
	}
	/** This implementation does nothing; the ValueRow sub components will receive their own refreshGUI() messages */
	public void refreshGUI(){}
	/** This implementation does nothing */
	public boolean loadFromGUI(){return true;}
	/** This implementation returns false*/
	public boolean isDirty(){
		return false;
	}
	/** This implementation does nothing */
	public void setDefaultValue(TermModel dv){}

	public void selectGUI(Object[] parts){
		GUITerm.typicalContainerSelect(this,parts);
	}
        
        public void destroy() {
        }
        
}