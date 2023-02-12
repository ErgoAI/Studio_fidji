package com.xsb.xj.containers;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.ValueRow;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;
import com.xsb.xj.XJChangeManager;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.UndoableEditEvent;

/**
 * Renders a GUITerm tree into a simple panel with a column of labels and a
 * column of corresponding values. Children captions are used for the labels;
 * the children themselves are rendered using ValueRow objects. Interprets first
 * level 'invisible' properties harshly: makes the whole child subterm invisible
 * Options: nolabel - do not display label by a value
 *
 *@version   $Id: LabelValueColumn.java,v 1.19 2004/07/13 14:33:58 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class LabelValueColumn extends JPanel implements XJComponent, XJComponentTree {
	GUITerm gt;
	PrologEngine engine;

	public LabelValueColumn(PrologEngine engine, GUITerm gt) {
		this.gt = gt;
		this.engine = engine;

		GridBagLayout g  = new GridBagLayout();
		setLayout(g);

		for(int c = 0; c < gt.getChildCount(); c++) {
			GUITerm child  = (GUITerm) gt.children[c];
			addRow(g, child);
		}
	}

	public TermModel[] getMyGUIs(){
		return gt.getMyGUIs(); 
	}
	public void addRow(GridBagLayout g, GUITerm child) {
		if(!child.isInvisible()) {

			JComponent line                 = new ValueRow(engine, child);

			GridBagConstraints leftColumn   = new GridBagConstraints();
			leftColumn.gridx = 0;

			TermModel labelAnchor           = gt.findProperty("label_anchor");
			if(labelAnchor != null) {
				String labelAnchor_str  = labelAnchor.node.toString();
				setAnchor(leftColumn, labelAnchor_str);
			} else {
				leftColumn.anchor = GridBagConstraints.EAST;
			}

			GridBagConstraints rightColumn  = new GridBagConstraints();
			rightColumn.gridx = 1;

			TermModel anchor                = child.findProperty("anchor");
			if(anchor != null) {
				String anchor_str  = anchor.node.toString();
				setAnchor(rightColumn, anchor_str);
			} else {
				rightColumn.anchor = GridBagConstraints.WEST;
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
					System.err.println("LabelValueColumn: insets need to be " +
						"specified as insets(top,left,bottom,right)");
				}
			}

			if(gt.findProperty("nolabel") == null) {
				JLabel label    = new JLabel(child.getTitle(), JLabel.RIGHT);
				g.setConstraints(label, leftColumn);
				add(label);
				label.setLabelFor(line);
                                line.getAccessibleContext().setAccessibleName(label.getText());
                                TermModel mnemonic = child.findProperty("mnemonic");
                                if(mnemonic != null){
                                    String mnemonicString  = mnemonic.node.toString();
                                    label.setDisplayedMnemonic(mnemonicString.charAt(0));
                                }

				TermModel font  = child.findProperty("label_font");
				if(font != null) {
					GUITerm.setGtFont(font, label);
				}
			}

			if(child.findProperty("fillarea") != null) {
				rightColumn.fill = java.awt.GridBagConstraints.BOTH;
				rightColumn.weightx = 1.0;
				rightColumn.weighty = 1.0;
			}

			g.setConstraints(line, rightColumn);
			add(line);
		}
	}

	public void addRowGt(GUITerm child) {
		GridBagLayout g  = (GridBagLayout) getLayout();
                TermModel[] oldChildren = gt.getChildren();
		gt.addChildren(new TermModel[]{child});
		addRow(g, child);
		child.refreshRenderers();
                XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
				this,gt,XJChangeManager.ADDCHILDREN_EDIT,-1,oldChildren,gt.getChildren());
		gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
	}

	public void removeRowGt(XJComponent child) {
		GUITerm childGt  = child.getGT();
		// remove from visual layout
		int i;
		for(i = 0; i < getComponentCount(); i++) {
			ValueRow row  = (ValueRow) getComponent(i);
			if(row.getComponent(0).equals((JComponent) child)) {
				break;
			}
		}

		if(i < getComponentCount()) {
			// component found
			remove(i);

			if(gt.findProperty("nolabel") == null) {
				// there is a label to remove too
				remove(i - 1);
			}

			//repaint();
			updateUI();

			// remove from Gt
			TermModel[] newChildren  = new TermModel[gt.getChildCount() - 1];
			int oldIndex             = 0;
			while((oldIndex < gt.getChildCount() - 1) && (gt.children[oldIndex] != childGt)) {
				newChildren[oldIndex] = gt.children[oldIndex];
				oldIndex++;
			}

			if(gt.children[oldIndex] == childGt) {
				oldIndex++;
				while(oldIndex < gt.getChildCount()) {
					newChildren[oldIndex - 1] = gt.children[oldIndex];
					oldIndex++;
				}
                                TermModel[] oldChildren = gt.getChildren();
				gt.setChildren(newChildren);
                                
                                XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
				this,gt,XJChangeManager.DELETECHILDREN_EDIT,-1,oldChildren,gt.getChildren());
                                gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
			}
                        
		}
	}
        
        /** Used to implement undo/redo */
	public void setChildren(TermModel[] terms){
            if((terms != null) && (gt.getChildCount() < terms.length)){
                // need to implement
                // undo remove operation
            } else {
                // need to implement
                // undo add operation
            }
            gt.setChildren(terms);
	}

	/**
	 * Anchor value should not be null.
	 *
	 *@param gb      Gridbag Constriants object
	 *@param anchor  Non null anchor value as string.
	 */
	public static void setAnchor(GridBagConstraints gb, String anchor) {

		if(anchor.equalsIgnoreCase("center")) {
			gb.anchor = GridBagConstraints.CENTER;
		} else if(anchor.equalsIgnoreCase("north")) {
			gb.anchor = GridBagConstraints.NORTH;
		} else if(anchor.equalsIgnoreCase("northeast")) {
			gb.anchor = GridBagConstraints.NORTHEAST;
		} else if(anchor.equalsIgnoreCase("east")) {
			gb.anchor = GridBagConstraints.EAST;
		} else if(anchor.equalsIgnoreCase("southeast")) {
			gb.anchor = GridBagConstraints.SOUTHEAST;
		} else if(anchor.equalsIgnoreCase("south")) {
			gb.anchor = GridBagConstraints.SOUTH;
		} else if(anchor.equalsIgnoreCase("southwest")) {
			gb.anchor = GridBagConstraints.SOUTHWEST;
		} else if(anchor.equalsIgnoreCase("west")) {
			gb.anchor = GridBagConstraints.WEST;
		} else if(anchor.equalsIgnoreCase("northwest")) {
			gb.anchor = GridBagConstraints.NORTHWEST;
		}
	}

	/**
	 * This implementation does nothing; the LabelValueColumn sub components will
	 * receive their own refreshGUI() messages
	 */
	public void refreshGUI() { }

	/**
	 * This implementation is a no op
	 *
	 *@return   Description of the Return Value
	 */
	public boolean loadFromGUI() {
		return true;
	}

	public GUITerm getGT() {
		return gt;
	}

	public void setGT(GUITerm gt){
		this.gt=gt;
	}
	public PrologEngine getEngine() {
		return engine;
	}

	/**
	 * This implementation returns false
	 *
	 *@return   The dirty value
	 */
	public boolean isDirty() {
		return false;
	}

	/**
	 * This implementation does nothing
	 *
	 *@param dv  The new defaultValue value
	 */
	public void setDefaultValue(TermModel dv) { }

	/**
	 * Make the panel visible and select a subcomponent
	 *
	 *@param parts  Description of the Parameter
	 */
	public void selectGUI(Object[] parts) {
		GUITerm.typicalContainerSelect(this, parts);
	}

        public void destroy() {
        }
    public Dimension getPreferredSize(){
    	Dimension D = super.getPreferredSize();
    	//System.out.println(this+" prefers dimension "+D);
    	return gt.getPreferredSize(D);
    }
}
