package com.xsb.xj.containers;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;

import java.awt.Component;

import javax.swing.JSplitPane;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Displays values of a term in horizontal form
 *
 *@version   $Id: XJSplitPane.java,v 1.12 2004/07/08 19:43:22 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJSplitPane extends JSplitPane implements XJComponent, XJComponentTree {
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;

	public XJSplitPane(PrologEngine engine, GUITerm gt) {
		this.gt = gt;
		this.engine = engine;
		dirty = false;

		TermModel vertical         = gt.findProperty("vertical");
		TermModel dividerLocation  = gt.findProperty("divider");
		TermModel expandable       = gt.findProperty("expandable");
		TermModel dividerSize      = gt.findProperty("dividerSize");
		
		if(vertical != null) {
			setOrientation(JSplitPane.VERTICAL_SPLIT);
		}
	
		if(dividerLocation != null) {
			setDividerLocation((double) (((Integer) dividerLocation.node).intValue()) / 100);
                        setResizeWeight((double) (((Integer) dividerLocation.node).intValue()) / 100);
		}
		
		if(expandable != null) {
			setOneTouchExpandable(true);
		}
		
		if(dividerSize != null) {
			setDividerSize(((Integer) dividerSize.node).intValue());
		}
		
		if(gt.getChildCount() > 0) {
			GUITerm leftChild       = (GUITerm) gt.getChild(0);
			XJComponent component0  = leftChild.makeGUI(engine);
                        resizeLeftComponent((Component)component0, dividerLocation);
			setLeftComponent((Component) component0);

			if(gt.getChildCount() > 1) {
                            GUITerm rightChild      = (GUITerm) gt.getChild(1);
                            XJComponent component1  = rightChild.makeGUI(engine);
                            resizeRightComponent((Component)component1, dividerLocation);
                            setRightComponent((Component) component1);
			}
		}
	}
                
        /**
         * To make top component completelly closed if dividerLocation is 0.
         */
        private void resizeLeftComponent(Component component, TermModel dividerLocation){
            if((dividerLocation != null) && (component instanceof JComponent)) {
                if(((Integer) dividerLocation.node).intValue() == 0){
                    ((JComponent)component).setMinimumSize(new java.awt.Dimension(0,0));
                }
            }
        }
        
        /**
         * To make bottom component completelly closed if dividerLocation is 100.
         */
        private void resizeRightComponent(Component component, TermModel dividerLocation){
            if((dividerLocation != null) && (component instanceof JComponent)) {
                if(((Integer) dividerLocation.node).intValue() == 100){
                    ((JComponent)component).setMinimumSize(new java.awt.Dimension(0,0));
                }
            }
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

	public TermModel[] getMyGUIs(){
		return gt.getMyGUIs(); 
	}
	public void refreshGUI() {
	}

	public void setLeft(XJComponent left) {
		setInPlace(true,left);
	}
	public void setRight(XJComponent right) {
		setInPlace(false,right);
	}
	
	private void setInPlace(final boolean left,final XJComponent component){
		final int divider  = getDividerLocation();
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				component.getGT().refreshRenderers();
				if (left) setLeftComponent((Component) component);
				else setRightComponent((Component) component);
				setDividerLocation(divider);
				// revalidate(); ??
				if (left) gt.setChild(0, component.getGT());
				else gt.setChild(1, component.getGT());
			}
		});
	}

	public void setLeft(GUITerm newgt) {
		XJComponent comp  = newgt.makeGUI(engine);
		setInPlace(true,comp);
	}
	

	public void setRight(GUITerm newgt) {
		XJComponent comp  = newgt.makeGUI(engine);
		setInPlace(false,comp);
	}

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

/*	// Swing has a bug in setDividerLocation method. It does not work in constructor.
	// Code below is workaround for it.
	boolean isPainted          = false;
	boolean hasProportionalLocation = false;
	double proportionalLocation;

	public void setDividerLocation(double proportionalLocation) {
		if(!isPainted) {
			hasProportionalLocation = true;
			this.proportionalLocation = proportionalLocation;
		} else {
			super.setDividerLocation(proportionalLocation);
		}
	} 
        
	public void paint(Graphics g) {
		if(!isPainted) {
			if(hasProportionalLocation) {
				super.setDividerLocation(proportionalLocation);
			}
			isPainted = true;
		}
		super.paint(g);
	}*/
        
        public void destroy() {
        }
        
}
