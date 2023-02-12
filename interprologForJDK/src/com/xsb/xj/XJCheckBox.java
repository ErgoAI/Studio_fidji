package com.xsb.xj;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.XJException;

@SuppressWarnings("serial")
public class XJCheckBox extends JCheckBox implements XJComponent{
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;

        static final String DISABLED = "disabled";
	
	public XJCheckBox(PrologEngine engine,GUITerm gt){
		super();
		this.engine=engine;
		dirty = false;

		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
		this.gt=gt;

		/* if (!(gt.node instanceof Integer) && !(gt.node instanceof String))
			throw new XJException("Incorrect value for checkbox");*/
		if(gt.findProperty("nocaption")==null){
		    setText(gt.getTitle());
		}

                if (gt.findProperty(DISABLED)!=null) this.setEnabled(false);
                
		TermModel margin        = gt.findProperty("margin");
		if(margin != null) {
			if(margin.getChildCount() == 4) {
				int top     = ((TermModel) margin.getChild(0)).intValue();
				int left    = ((TermModel) margin.getChild(1)).intValue();
				int bottom  = ((TermModel) margin.getChild(2)).intValue();
				int right   = ((TermModel) margin.getChild(3)).intValue();
				this.setMargin(new Insets(top, left, bottom, right));
			} else {
				System.err.println("XJCheckBox: margin need to be " +
					"specified as margin(top,left,bottom,right)");
			}
		}
                
                if(!gt.tipDescription().equals("")){
                    setToolTipText(gt.tipDescription());
                }
		
		addItemListener(new ItemListener (){
			public void itemStateChanged(ItemEvent e){
			    dirty=true;
			    loadFromGUI();
			}
		});
	}

	public Dimension getPreferredSize(){
		return gt.getPreferredSize(super.getPreferredSize());
	}
	
	public void setDefaultValue(TermModel dv){
		if (dv!=null) {
			if (!(dv.node instanceof Integer) && !(gt.node instanceof String)) 
				throw new XJException("Incorrect value for checkbox");
			gt.node = dv.node;
		} else {
			gt.node=new Integer(0); 
			// setSelected(false);
		}
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
		if (gt.node instanceof Integer){
			int value = ((Integer)gt.node).intValue();
			if(value == 1){
				setSelected(true);
			} else if(value == 0){
				setSelected(false);
			} else throw new XJException("checkboxes require 1 or 0:"+value);
		} else if (gt.node instanceof String){
			String value = (String)gt.node;
			if(value.equalsIgnoreCase("true")){
				setSelected(true);
			} else if(value.equalsIgnoreCase("false")){
				setSelected(false);
			} else throw new XJException("checkboxes require true or false:"+value);
		}

		dirty = false;
	}
	
	/** Assumes that the GUI value is always acceptable */
	public boolean loadFromGUI(){
		if (!dirty) return true;
		gt.setNodeValue(isSelected()?new Integer(1):new Integer(0));
		dirty = false;
		return true;
	}
	
	public boolean isDirty(){
		return dirty;
	}

	public void selectGUI(Object[] parts){
		GUITerm.typicalAtomicSelect(this,parts);
	}
        
        public void destroy() {
        }
        
}
