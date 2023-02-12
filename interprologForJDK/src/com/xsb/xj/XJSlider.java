package com.xsb.xj;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.XJException;

import java.util.*;

/** Can act as operation/function event source. It keeps the value in the node as a number. 
Interprets some additional properties e.g. horizontal/vertical, etc*/
@SuppressWarnings("serial")
public class XJSlider extends JSlider implements XJComponent,ChangeListener, FocusListener{
	GUITerm gt;
	PrologEngine engine;
	private Vector<ActionListener> listeners=null;
	private boolean dirty;
	
	/** constants for Prolog developers */
	static final String MINIMUM = "minimum";
	static final String MAXIMUM = "maximum";
	static final String PAINTLABELS = "paintlabels";
	static final String MINTICK = "mintick";
	static final String MAXTICK = "maxtick";
	static final String SNAP = "snap";
	/** hack to prevent spurious actions being invoked while sliders in lists */
	private boolean selfRefreshing=false;
	
	public XJSlider(PrologEngine engine,GUITerm gt){
		super();
		this.engine=engine;
		dirty = false;
		
		//setMajorTickSpacing(10);
		//setMinorTickSpacing(1);
		setPaintTicks(true);
		
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
		
		this.gt=gt;
		
		if (!gt.isVar() && !(gt.node instanceof Integer)) 
			throw new XJException("Sliders require an integer node");
		
		// the slider default value will be set later
		if(gt.findProperty(SNAP) != null)
			setSnapToTicks(true);
		
		if (gt.findProperty(PAINTLABELS)!=null) 
			setPaintLabels(true);
		else 
			setPaintLabels(false);
		
		TermModel minTick = gt.findProperty(MINTICK);
		if(minTick == null)
			setMinorTickSpacing(1);
		else
			setMinorTickSpacing(((Integer)minTick.node).intValue());
		
		TermModel maxTick = gt.findProperty(MAXTICK);
		if(maxTick == null)
			setMajorTickSpacing(10);
		else
			setMajorTickSpacing(((Integer)maxTick.node).intValue());
		
		TermModel minimum = gt.findProperty(MINIMUM);
		if (minimum!=null) 
			setMinimum(((Integer)minimum.node).intValue());
		
		TermModel maximum = gt.findProperty(MAXIMUM);
		if (maximum!=null) 
			setMaximum(((Integer)maximum.node).intValue());
		
		TermModel vertical = gt.findProperty("vertical");
		if (vertical!=null) 
			setOrientation(VERTICAL); // cf. SwingConstants
		
		setToolTipText(gt.tipDescription());
		
		//  no longer here: refreshGUI();
		addChangeListener(this);
		addFocusListener(this);
	}

	public Dimension getPreferredSize(){
		return gt.getPreferredSize(super.getPreferredSize());
	}
	
	public void setDefaultValue(TermModel dv){
		// use the hacked assignment to avoid the undo treatment
		if (dv!=null) {
			if (!(dv.node instanceof Integer)) 
				throw new XJException("Sliders do require an integer node");
			gt.node = dv.node;
		} else gt.node=new Integer(0); 
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
		selfRefreshing = true;
		// a slider should never hold a var, except temporarily when a list is setting it up:
		//if (gt.isVar()) setDefaultValue(gt.findProperty(DEFAULT)); 
		setValue(((Integer)gt.node).intValue());
		selfRefreshing = false;
		dirty = false;
	}
	
	/** Assumes that the GUI value is always acceptable */
	public boolean loadFromGUI(){
		if (!dirty) return true;
		gt.setNodeValue(new Integer(getValue()));
		dirty = false;
		return true;
	}
	
 	public void selectGUI(Object[] parts){
		GUITerm.typicalAtomicSelect(this,parts);
	}
   	
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider)e.getSource();
        if (source!=this) 
        	throw new XJException("bad vibes in XJSlider");
        if (!getValueIsAdjusting()) {
        	loadFromGUI();
        	if (!selfRefreshing) fireActionEvent();
        } else dirty=true;
    }
	
	/** This implementation returns false*/
	public boolean isDirty(){
		return dirty;
	}

	// Action method name/signature pattern
	public void addActionListener(ActionListener l){
		if (listeners==null) listeners=new Vector<ActionListener>();
		listeners.addElement(l);
	}
	public void removeActionListener(ActionListener l){
		listeners.removeElement(l);
	}
	
	protected void fireActionEvent(){
		if (listeners!=null){
			ActionEvent ae = new ActionEvent(this,0,"slider changed");
			for (int i=0;i<listeners.size();i++)
				listeners.elementAt(i).actionPerformed(ae);
		}
	}

	//public void getAction(Object o) {
	//	System.out.println(o.getClass().getName());
	//}
	// FocusListener methods
	public void focusGained(FocusEvent e){
		// might change aspect
	}
	public void focusLost(FocusEvent e){
		loadFromGUI();
	}
	
        public void destroy() {
        }
        
}