/*
    XJComboBox.java
    Created on January 23, 2002, 10:32 AM
  */
package com.xsb.xj;

import com.declarativa.interprolog.*;
import com.xsb.xj.util.*;

import java.awt.Dimension;
import java.awt.event.*;
import java.awt.Rectangle;

import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.*;
import javax.swing.*;

/**
 *@author    tanya
 *@created   December 11, 2002
 *@version
 */
@SuppressWarnings("serial")
public class XJComboBox extends JComboBox<Object> implements XJTemplateComponent, XJComponentTree {

	// If there are some problems in editable combo box first see comments to setItem method in XJComboCellEditor

	GUITerm gt;
	PrologEngine engine;
	XJAbstractListModel model;
	final GUITerm[] cellGTs;
	XJComboCellEditor editor;
	boolean dirty;
	GUITerm rendererTemplate, editorTemplate;
	public int popupWidth = -1;
    private boolean firstRefresh = true;
    /** hack to allow use in list and tree renderers/editors*/
    private boolean fireEvents = true;
	
	@SuppressWarnings("unchecked") 
	public XJComboBox(PrologEngine engine, GUITerm gt) {
		super();
		if (gt.findProperty(GUITerm.PLAINCOMBO)==null)
			setUI(new SteppedComboBoxUI());
		this.gt = gt;
		this.engine = engine;
		this.dirty = false;

		GUITerm template = (GUITerm) getTemplate();
		if(!gt.tipDescription().equals("")){
			setToolTipText(gt.tipDescription());
		}
		// working on template copies; this prevents us from getting myGUI bindings outside the template,
		// so we might want to change something here or elsewhere later
		rendererTemplate = (GUITerm) template.clone();
		editorTemplate = (GUITerm) template.clone();

		// transient variables are gone with cloning, but XJCellTupleRenderer and XJCellTupleEditor will rebuild them:
		XJCellTupleRenderer renderer = new XJCellTupleRenderer(engine, rendererTemplate, false);
		cellGTs = renderer.cellGTs;

		if(gt.findProperty("lazylist") != null) {// combo box with lazy list
			model = new LazyComboListModel(engine, gt, cellGTs);
		} else {// eager combo box
			if((gt.getChildCount() == 2) || (gt.getChildCount() == 0)) {
				model = new XJComboBoxModel(gt, cellGTs);
			} else {
				throw new XJException("XJComboBox: list is not correct - child count " + gt.getChildCount());
			}
		}

		if(gt.findProperty("popupWidth") != null ) {
			TermModel pWidth = gt.findProperty("popupWidth");
			popupWidth=pWidth.intValue();
		}

		Object selItem = ((ComboBoxModel<Object>) model).getSelectedItem();
		setModel((ComboBoxModel<Object>) model);
		if(selItem != null) {
			setSelectedItem(selItem);
		}

		TermModel editable = gt.findProperty(GUITerm.EDITABLE);
		if(editable != null) {
			setEditable(true);
			editor = new XJComboCellEditor(engine, editorTemplate);
			setEditor(editor);
		}
		setRenderer(renderer);

		/*
		    Handle selectionChanged operations (not functions); this may be used (also) in table cells, not tree nodes
		    as the event firing control in setfireEvents is not fully implemented yet
		 */
		XJAction[] topOps = gt.operations(engine, this);
		final XJAction selectionChangedAction = XJAction.findSelectionChanged(topOps);
		if(selectionChangedAction != null) {
			// let's keep it light, and assume no modal interactions occur:
			selectionChangedAction.setInAWTThread(true);
			selectionChangedAction.setCursorChanges(false);
			addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dirty = true;
						if(loadFromGUI() && fireEvents) {
							selectionChangedAction.doit();
						}
					}
				});
			if(editor != null) {
				editor.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							dirty = true;
							if(loadFromGUI() && fireEvents) {
								selectionChangedAction.doit();
							}
						}
					});
			}
		} else {
			addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dirty = true;
						loadFromGUI();
					}
				});
			if(editor != null) {
				editor.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							dirty = true;
							loadFromGUI();
						}
					});
			}
		}
	}
	
	/** Hack to let tables control whether one of these (in an entire cell) fires events, to avoid spurious (non user) events */
	public static void setFireEvents(Object component, boolean fire){
		if (component instanceof XJComboBox)
			((XJComboBox)component).fireEvents=fire;
	}
	
	public Dimension getPreferredSize() {
		return gt.getPreferredSize(super.getPreferredSize());
	}

	public TermModel getTemplate() {
		if(gt.findProperty("list") != null) {
			return gt.listTemplate();
		} else if(gt.findProperty("lazylist") != null) {
			return gt.lazyListTemplate();
		} else {
			return null;
		}
	}

	/**
	 * applies changes made to the global template to the local copies
	 */
	public void constructionEnded() {
		rendererTemplate.assignTermChanges(getTemplate());
		editorTemplate.assignTermChanges(getTemplate());
	}

	/**
	 * Returns the terms currently selected in the ComboBox
	 */
	public TermModel[] getSelectedTerms() {
		TermModel[] selectedTerms = new TermModel[1];
		selectedTerms[0] = (TermModel) getSelectedItem();
		return selectedTerms;
	}

	/**
	 * Simply ask the editor to stop editing and get its value. This may get called
	 * several times, so the implementation should be aware of that
	 */
	public boolean loadFromGUI() {
		if(!isDirty()) {
			return true;
		}
		if(isDirty()) {
			if(editor != null) {
				if(editor.stopCellEditing()) {
					return true;
				} else {
					return false;
				}
			} else {
				this.dirty = false;
				return true;
			}
		} else {
			return true;
		}
	}

	public boolean isDirty() {
		/*
		    if(editor!=null){  // ComboBox is not editable
		    return editor.isDirty();
		    } else {
		 */
		return this.dirty;
		//}
	}

	/**
	 * Refresh the GUI with the data in my GUITerm; this method has the
	 * responsability to invoke repaint(), and to clear the dirty flag
	 */
	public void refreshGUI() {
            synchronized(this){
                if(this.firstRefresh){
                    registerForCloseNotify();
                    
                }
            }
	}

	/**
	 * The GUITerm which, or whose root, this XJComponent renders
	 */
	public GUITerm getGT() {
		return gt;
	}
	public void setGT(GUITerm gt){
		this.gt=gt;
	}

	/**
	 * The PrologEngine used for operations and persistence etc.
	 */
	public PrologEngine getEngine() {
		return engine;
	}

	/**
	 * Sets the GUITerm node to a default value. This is invoked when a "new term"
	 * is being created. Implementations should interpret d as they see fit, which
	 * may be null if no 'default=d' property was defined. Value setting should
	 * occur quietly, namely without firing undo events as we don't want this
	 * "editing" to be deemed relevant
	 */
	public void setDefaultValue(TermModel d) {
	}

	public void selectGUI(Object[] parts) {
		GUITerm.typicalAtomicSelect(this, parts);
	}

	public Dimension getPopupSize() {
		Dimension size = getSize();
		if (popupWidth < 1) popupWidth = size.width;
		return new Dimension(popupWidth, size.height);
	}	
	
	class SteppedComboBoxUI extends MetalComboBoxUI{
		protected ComboPopup createPopup() {
			BasicComboPopup popup =
				new BasicComboPopup(comboBox) {

					public void show() {
						Dimension popupSize = ((XJComboBox) comboBox).getPopupSize();
						popupSize.setSize(popupSize.width,
							getPopupHeightForRowCount(comboBox.getMaximumRowCount()));
						Rectangle popupBounds = computePopupBounds(0,
							comboBox.getBounds().height, popupSize.width, popupSize.height);
						scroller.setMaximumSize(popupBounds.getSize());
						scroller.setPreferredSize(popupBounds.getSize());
						scroller.setMinimumSize(popupBounds.getSize());
						list.invalidate();
						int selectedIndex = comboBox.getSelectedIndex();
						if(selectedIndex == -1) {
							list.clearSelection();
						} else {
							list.setSelectedIndex(selectedIndex);
						}
						list.ensureIndexIsVisible(list.getSelectedIndex());
						setLightWeightPopupEnabled(comboBox.isLightWeightPopupEnabled());

						show(comboBox, popupBounds.x, popupBounds.y);
					}
				};
			popup.getAccessibleContext().setAccessibleParent(comboBox);
			return popup;
		}
	}

        public void setContext(TermModel c){
            if (!(model instanceof LazyComboListModel))
                throw new XJException("Context can only be changed for lazy components");
            ((LazyComboListModel)model).setContext(c);
        }
        
        public void destroy(){
            if(gt.findProperty("lazylist") != null) {// combo box with lazy list
                ((LazyComboListModel)model).destroy();
            }
        }
        
        protected void registerForCloseNotify(){
 /*           JInternalFrame frame = null;
            try{
                frame = (JInternalFrame)SwingUtilities.getAncestorOfClass(javax.swing.JInternalFrame.class,this);
            } catch(Exception e){
                System.out.println(e);
            }
            if(frame != null){
//                System.out.println("Found frame");
                frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter(){
                    public void internalFrameClosed(javax.swing.event.InternalFrameEvent e1){
                        XJComboBox.this.destroy();
                    }
                });
                this.firstRefresh = false;
            } else {
                java.awt.Window window = SwingUtilities.getWindowAncestor(this);
                if(window != null){
//                    System.out.println("Found window");
                    window.addWindowListener(new java.awt.event.WindowAdapter(){
                        public void windowClosed(java.awt.event.WindowEvent e2){
                            XJComboBox.this.destroy();
                        }
                    });
                    this.firstRefresh = false;
                } else {
//                    System.out.println("Not Found frame or window");
                }
            } */
        }

}

