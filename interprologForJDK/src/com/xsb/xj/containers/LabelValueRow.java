package com.xsb.xj.containers;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.ValueRow;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;

/**
 * Displays values of a term in horizontal form.
 *
 *@version   $Id: LabelValueRow.java,v 1.16 2004/07/08 19:43:22 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class LabelValueRow extends JPanel implements XJComponent, XJComponentTree {
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;
	private boolean drawImage  = false;
	private ImageIcon img;

	public LabelValueRow(PrologEngine engine, GUITerm gt) {
		this.gt = gt;
		this.engine = engine;
		dirty = false;

		GridBagLayout g       = new GridBagLayout();
		setLayout(g);

		for(int c = 0; c < gt.getChildCount(); c++) {
			GUITerm child                     = (GUITerm) gt.children[c];

			if(child.isInvisible()) {
				continue;
			}

			JComponent line                   = new ValueRow(engine, child);

			if(gt.findProperty("nolabel") == null) {
				GridBagConstraints labelCell  = new GridBagConstraints();
				JLabel label                  = new JLabel(child.getTitle(), JLabel.RIGHT);
				label.setLabelFor(line);
                                TermModel mnemonic = child.findProperty("mnemonic");
                                if(mnemonic != null){
                                    String mnemonicString  = mnemonic.node.toString();
                                    label.setDisplayedMnemonic(mnemonicString.charAt(0));
                                }

				labelCell.gridy = 0;
				labelCell.gridx = c;
				//labelCell.anchor=GridBagConstraints.NORTH;

				g.setConstraints(label, labelCell);
				add(label);
				
				TermModel font = child.findProperty("label_font");
				if(font != null) {
					//System.out.println("setting font for label");
					GUITerm.setGtFont(font, label);
					System.out.println(label.getFont());
				}
				
			}

			GridBagConstraints componentCell  = new GridBagConstraints();
			componentCell.gridy = 1;
			componentCell.gridx = c;
			//componentCell.anchor=GridBagConstraints.SOUTH;

			TermModel insets                  = child.findProperty("insets");
			if(insets != null) {
				if(insets.getChildCount() == 4) {
					int top     = ((TermModel) insets.getChild(0)).intValue();
					int left    = ((TermModel) insets.getChild(1)).intValue();
					int bottom  = ((TermModel) insets.getChild(2)).intValue();
					int right   = ((TermModel) insets.getChild(3)).intValue();
					componentCell.insets = new Insets(top, left, bottom, right);
				} else {
					System.err.println("LabelValueRow: insets need to be specified as insets(top,left,bottom,right)");
				}
			}

			if(child.findProperty("fillarea") != null) {
				componentCell.fill = java.awt.GridBagConstraints.BOTH;
				componentCell.weightx = 1.0;
				componentCell.weighty = 1.0;
			}

			g.setConstraints(line, componentCell);
			add(line);
		}
		setOpaque(false);

		TermModel background  = gt.findProperty("background");
		if(background != null) {
			img = new ImageIcon((String) background.node);
			setPreferredSize(new Dimension(img.getIconWidth(), img.getIconHeight()));
			drawImage = true;
		}
	}

	public TermModel[] getMyGUIs(){
		return gt.getMyGUIs(); 
	}
	public void paintComponent(Graphics g) {
		if(drawImage) {
			g.drawImage(img.getImage(), 0, 0, null);
		}

		super.paintComponent(g);
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
	public void refreshGUI() { }

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
        
        public void destroy() {
        }
        
    public Dimension getPreferredSize(){
    	Dimension D = super.getPreferredSize();
    	return gt.getPreferredSize(D);
    }
}
