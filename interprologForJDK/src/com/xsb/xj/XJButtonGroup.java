package com.xsb.xj;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;

@SuppressWarnings("serial")
public class XJButtonGroup extends JPanel implements XJComponent, XJComponentTree{
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;
        private boolean horisontal = false;
        ButtonGroup group;

	public XJButtonGroup(PrologEngine engine, GUITerm gt){
		this.gt=gt;
		this.engine=engine;
		dirty = false;
        group = new ButtonGroup();
                
		GridBagLayout g = new GridBagLayout();
		setLayout(g);
                if(gt.findProperty("horizontal") != null){
                    this.horisontal = true;
                }
		GridBagConstraints leftColumn = new GridBagConstraints();  
		GridBagConstraints rightColumn = new GridBagConstraints();  
                if(!horisontal){
                    leftColumn.anchor=GridBagConstraints.EAST;
                    rightColumn.anchor=GridBagConstraints.WEST;
                    rightColumn.gridx=1; leftColumn.gridx=0;
                }
                
		for (int c=0;c<gt.getChildCount();c++){
			GUITerm child = (GUITerm)gt.children[c];
			if (child.isInvisible()) continue;
			
                        XJComponent line = child.makeGUI(engine);
                        
                        if(line instanceof AbstractButton){
                            group.add((AbstractButton)line);
                        } else {
                            if(gt.findProperty("nolabel") == null){
                                JLabel field = new JLabel(child.getTitle(),JLabel.RIGHT);
                                if(horisontal){
                                    leftColumn = new GridBagConstraints();
                                    leftColumn.gridx = 2*c;
                                    leftColumn.anchor = GridBagConstraints.CENTER;
                                }
                                g.setConstraints(field,leftColumn);
                                add(field);
                                field.setLabelFor((JComponent)line);
                            }
                        }
                        
                        if(horisontal){
                            rightColumn = new GridBagConstraints();
                            rightColumn.gridx = 2*c + 1;
                            rightColumn.anchor = GridBagConstraints.CENTER;
                        }
                        
                        TermModel insets                = child.findProperty("insets");
                        if(insets != null) {
				if(insets.getChildCount() == 4) {
					int top     = ((TermModel) insets.getChild(0)).intValue();
					int left    = ((TermModel) insets.getChild(1)).intValue();
					int bottom  = ((TermModel) insets.getChild(2)).intValue();
					int right   = ((TermModel) insets.getChild(3)).intValue();
					rightColumn.insets = new Insets(top, left, bottom, right);
				} else {
					System.err.println("XJButtonGroup: insets need to be " +
						"specified as insets(top,left,bottom,right)");
				}
			}
			g.setConstraints((JComponent)line,rightColumn); 
                        add((JComponent)line);
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

	public void refreshGUI(){}

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
        
        public void addButton(AbstractButton button){
            group.add(button);
        }
        
        public void destroy() {
        }
        
}
