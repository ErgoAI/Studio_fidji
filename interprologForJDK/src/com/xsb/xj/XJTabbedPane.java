package com.xsb.xj;

import java.awt.Image;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;

/**
 * Description of the Class
 *
 *@author    Harpreet Singh
 *@version   $Id: XJTabbedPane.java,v 1.6 2004/07/08 19:43:22 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJTabbedPane extends JTabbedPane implements XJComponent, XJComponentTree {
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;
	final static String ICON       = "icon";
	final static String PLACEMENT  = "placement";
	final static String SELECTED   = "selected";

	public XJTabbedPane(PrologEngine engine, GUITerm gt) {
		this.gt = gt;
		this.engine = engine;
		dirty = false;

		TermModel tabPlacement                 = gt.findProperty(PLACEMENT);
		if(tabPlacement != null) {
			String placement  = (String) tabPlacement.node;
			setPlacement(placement);
		} else {
			this.setTabPlacement(SwingConstants.BOTTOM);
		}

		addChildren();
		int activeTab                          = 0;
		TermModel selectedTab                  = gt.findProperty(SELECTED);
		if(selectedTab != null) {
			int childcount  = selectedTab.getChildCount();
			if(childcount == 1) {
				TermModel activeTab_tm  = (TermModel) selectedTab.getChild(0);
				activeTab = activeTab_tm.intValue() - 1;
			}
			if(activeTab < this.getTabCount() && activeTab >= 0) {
				this.setSelectedIndex(activeTab);
			}
		}
		XJAction[] topOps                      = gt.operations(this.engine, this);
		final XJAction selectionChangedAction  = XJAction.findSelectionChanged(topOps);
		if(selectionChangedAction != null) {
			selectionChangedAction.setInAWTThread(true);
			selectionChangedAction.setCursorChanges(false);
			this.addChangeListener(
				new ChangeListener() {
					public void stateChanged(ChangeEvent evt) {
						if(loadFromGUI()) {
							selectionChangedAction.doit();
						}
					}
				});
		}
	}

	public TermModel[] getMyGUIs(){
		return gt.getMyGUIs(); 
	}
	
	private void setPlacement(String placement) {
		if(placement.equals("left")) {
			this.setTabPlacement(SwingConstants.LEFT);
		} else if(placement.equals("right")) {
			this.setTabPlacement(SwingConstants.RIGHT);
		} else if(placement.equals("top")) {
			this.setTabPlacement(SwingConstants.TOP);
		} else {
			this.setTabPlacement(SwingConstants.BOTTOM);
		}
	}

	private void addChildren() {
		String title  = null;
		for(int c = 0; c < this.gt.getChildCount(); c++) {
			GUITerm child  = (GUITerm) gt.children[c];
			if(!child.isInvisible()) {
				XJComponent line        = child.makeGUI(engine);
				title = "";

				TermModel caption       = child.findProperty(GUITerm.CAPTION);
				if(caption != null) {
					title = (String) caption.node;
				} else {
					;
				}

				this.addTab(title, (JComponent) line);
				int tabIndex            = this.getTabCount() - 1;

				String tip = child.tipDescription();
				if(!tip.equals("")) 
					this.setToolTipTextAt(tabIndex, tip);			
				if(child.findProperty(GUITerm.DISABLED) != null) {
					this.setEnabledAt(tabIndex, false);
				}

				TermModel iconLocation  = child.findProperty(ICON);
				if(iconLocation != null) {
					ImageIcon icon = XJDesktop.fetchIcon(this,iconLocation);
					this.setIconAt(tabIndex, icon);
					Image grayIconImage  = GrayFilter.createDisabledImage(icon.getImage());
					this.setDisabledIconAt(tabIndex, new ImageIcon(grayIconImage));
				}
			}
		}
	}
	// Methods from XJComponent Interface.
	public PrologEngine getEngine() {
		return engine;
	}

	public GUITerm getGT() {
		return gt;
	}
	public void setGT(GUITerm gt){
		this.gt=gt;
	}

	public void refreshGUI() {
	}

	public boolean loadFromGUI() {
		dirty = false;
		return true;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void selectGUI(Object[] parts) {
		GUITerm.typicalContainerSelect(this, parts);
	}

	public void setDefaultValue(TermModel dv) {
	}
        
        public void destroy() {
        }
        
}
