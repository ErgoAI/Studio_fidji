package com.xsb.xj;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JRadioButton;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.XJException;

@SuppressWarnings("serial")
public class XJRadioButton extends JRadioButton implements XJComponent{
	GUITerm gt;
	PrologEngine engine;
        private boolean firstTime;
	private boolean dirty;

        static final String DISABLED = "disabled";
	
	public XJRadioButton(PrologEngine engine, GUITerm gt){
		super();
		this.engine=engine;
		dirty = false;

		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
		this.gt=gt;

		/* if (!(gt.node instanceof Integer) && !(gt.node instanceof String))
			throw new XJException("Incorrect value for checkbox");*/
		setText(gt.getTitle());

                if (gt.findProperty(DISABLED)!=null) this.setEnabled(false);
                
		if(!gt.tipDescription().equals("")){
                    setToolTipText(gt.tipDescription());
                }
		addItemListener(new ItemListener (){
			public void itemStateChanged(ItemEvent e){
			    dirty=true;
			    loadFromGUI();
			}
		});
                firstTime = true;
	}

	public Dimension getPreferredSize(){
		return gt.getPreferredSize(super.getPreferredSize());
	}
	
	public void setDefaultValue(TermModel dv){
		if (dv!=null) {
			if (!(dv.node instanceof Integer) && !(gt.node instanceof String)) 
				throw new XJException("Incorrect value for radiobutton");
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
			} else throw new XJException("radiobuttons require 1 or 0:"+value);
		} else if (gt.node instanceof String){
			String value = (String)gt.node;
			if(value.equalsIgnoreCase("true")){
				setSelected(true);
			} else if(value.equalsIgnoreCase("false")){
				setSelected(false);
			} else throw new XJException("radiobuttons require true or false:"+value);
		}
                
		dirty = false;

                TermModel buttonGroupTerm = gt.findProperty("buttonGroup");
                if(buttonGroupTerm != null){
                    if(buttonGroupTerm.getChildCount() == 1){
                        Integer reference = (Integer)((TermModel)(buttonGroupTerm.getChild(0))).node;
                        Object buttonGroup = engine.getRealJavaObject(reference.intValue());
                        synchronized(this){
                            if(firstTime && (buttonGroup instanceof XJButtonGroup)){
                                ((XJButtonGroup)buttonGroup).addButton(this);
                                firstTime = false;
                            }
                       }
                    }
                }

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
