package com.xsb.xj;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;

@SuppressWarnings("serial")
public class XJLabel extends JLabel implements XJComponent{
	GUITerm gt;
	PrologEngine engine;
        
        final static String ICON   = "icon";
        
	public XJLabel(PrologEngine engine,GUITerm gt){
		super((gt.findProperty(GUITerm.RENDERALLNODES)!=null ? gt.toString() :gt.node.toString()),JLabel.RIGHT);
		this.gt=gt;
		this.engine=engine;
		if (gt.isNumber()) setHorizontalAlignment(JTextField.RIGHT);
        String tip = gt.tipDescription();
		if(tip != null){ if(!tip.equals("")){setToolTipText(tip);}}
		setAlignmentY(Component.TOP_ALIGNMENT);
		setOpaque(true);
		
		TermModel justify = gt.findProperty("justify");
		if(justify !=null) {
			String justification = (String)justify.node;
			if(justification.equals("left")){
				setHorizontalAlignment(SwingConstants.LEFT);
			}
			if(justification.equals("right")){
				setHorizontalAlignment(SwingConstants.RIGHT);
			}
			if(justification.equals("center")) {
				setHorizontalAlignment(SwingConstants.CENTER);
			}
		}
                setImage();
	}
        
	public Dimension getPreferredSize(){
		return gt.getPreferredSize(super.getPreferredSize());
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
		setText((gt.findProperty(GUITerm.RENDERALLNODES)!=null ? gt.toString() :gt.node.toString()));
		TermModel labelFor = gt.findProperty("labelfor");
		if(labelFor != null){
			if(labelFor.getChildCount() == 1){
				Integer reference = (Integer)((TermModel)(labelFor.getChild(0))).node;
				setLabelFor((Component)engine.getRealJavaObject(reference.intValue()));
			}
		}
		
		TermModel mnemonic = gt.findProperty("mnemonic");
		if(mnemonic != null){
			String mnemonicString  = mnemonic.node.toString();
			setDisplayedMnemonic(mnemonicString.charAt(0));
		}
	}
	/** This implementation does nothing */
	public void setDefaultValue(TermModel dv){}
	
	/** This implementation does nothing */
	public boolean loadFromGUI(){return true;}
	
	/** This implementation returns false*/
	public boolean isDirty(){
		return false;
	}

	public void selectGUI(Object[] parts){
		GUITerm.typicalAtomicSelect(this,parts);
	}
        
	protected void setImage(){
		TermModel iconLocation  = gt.findProperty(ICON);
		if(iconLocation != null)
			setIcon(XJDesktop.fetchIcon(this,iconLocation));
	}

	public void destroy() {
	}
        
}