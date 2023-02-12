package com.xsb.xj;

import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.XJException;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.WindowConstants;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * Description of the Class
 *
 *@version   $Id: TermChooserDialog.java,v 1.7 2005/08/01 18:01:16 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class TermChooserDialog extends JDialog {
    public static final String PROP_ADD_NEW  = "add_new";

    XJComponent termsHolder;
    TermModel[] chosen;
    
	public TermChooserDialog(JComponent owner, JComponent termsComponent, int maxChoices) {
		this((Frame)(owner.getTopLevelAncestor()),termsComponent,maxChoices);
	}
	
    public TermChooserDialog(Frame owner, JComponent termsComponent, int maxChoices) {
        super(owner, true);

        termsHolder = (XJComponent) termsComponent;
        setTitle(termsHolder.getGT().getTitle());

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			public void windowClosed(WindowEvent e){
			    termsHolder.getGT().destroyRenderers();
			}
	    });


        // the following suggests using the XJChooser interface to push it out of here...
        if(termsComponent instanceof XJTree) {
            JTree tree  = ((XJTree) termsComponent).getJTree();
            if(maxChoices > 1) {
                tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
            } else {
                tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            }
        } else if(termsComponent instanceof XJTable) {
            JTable table  = ((XJTable) termsComponent).getJTable();
            if(maxChoices > 1) {
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            } else {
                table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            }
        } else {
            throw new XJException("Chooser dialog requires (just) a list or tree");
        }

        getContentPane().add((JComponent) termsHolder, BorderLayout.CENTER);

        JPanel bottom   = new JPanel(new FlowLayout());
        JButton addNew  = new JButton("Add");
        JButton pick    = new JButton("Choose");
        JButton cancel  = new JButton("Cancel");

        if(termsHolder.getGT().findProperty(PROP_ADD_NEW) != null) {
            bottom.add(addNew);
            addNew.addActionListener(
                        new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                chosen = new TermModel[1];
                                chosen[0] = new TermModel(PROP_ADD_NEW);
                                dispose();
                            }
                        });
        }

        bottom.add(pick);
        bottom.add(cancel);

        getContentPane().add(bottom, BorderLayout.SOUTH);

        cancel.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            chosen = new TermModel[0];
                            dispose();
                        }
                    });

        pick.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if(termsHolder instanceof XJTable) {
                                chosen = ((XJTable) termsHolder).getSelectedTerms();
                            } else {
                                chosen = ((XJTree) termsHolder).getSelectedTerms();
                            }
                            dispose();
                        }
                    });

        // setSize(300, 400);
        pack();
        setLocationRelativeTo(owner);
    }

    /*
     *  this might support a more Prolog-sided implementation, with buttons defined there:
     *  public void setCurrentChoice(TermModel[] c){
     *  chosen=c;
     *  }
     */
    public TermModel[] choose() {
        setVisible(true);
        return chosen;
    }
}

