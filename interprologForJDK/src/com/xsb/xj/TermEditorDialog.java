package com.xsb.xj;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.HelpManager;

/**
 * Creates a dialog with ok and cancel buttons to display gui term. Ok button is
 * added only when the term is updatable. The text for the ok button can be
 * specified by the user.
 *
 *@version   $Id: TermEditorDialog.java,v 1.28 2004/08/09 21:35:05 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class TermEditorDialog extends JDialog {
	JComponent content;
	PrologEngine engine;
	TermModel newTerm;

	static final String validateInput  = "validateInput";
	String okButtonText         = "OK";

	public TermEditorDialog(XJComponent gui) {
		this((Frame)null, gui, "OK");
	}

	public TermEditorDialog(JComponent owner, XJComponent gui) {
		this((Window)((owner!=null?owner.getTopLevelAncestor():null)), gui, "OK");
	}
	public TermEditorDialog(Frame owner, XJComponent gui) {
		this(owner, gui, "OK");
	}

        /*public TermEditorDialog(Dialog owner, XJComponent gui) {
            this(owner, gui, "OK");
        } */

	public TermEditorDialog(XJComponent gui, String text) {
		this((Frame)null, gui, text);
	}

        /*
        public TermEditorDialog(Dialog owner, XJComponent gui, String text) {
		super(owner);
                initComponents(owner, gui, text);
        } */
        
	public TermEditorDialog(Window owner, XJComponent gui, String text) {
		super(owner);
        initComponents(owner, gui, text);
	}
               
    protected void initComponents(Window owner, final XJComponent gui, String text){
		// setSize(gui.getGT().getPreferredSize(getPreferredSize()));  commented because of some code below....
		// System.out.println("owner:"+owner+", gui:"+gui+", text:"+text);
		setModal(true);
		// System.out.println("modality:"+getModalityType()); APPLICATION_MODAL
		okButtonText = text;
		engine = gui.getEngine();
		//setLocation(nextLocation());

		content = (JComponent) gui;
		final TermEditManager um  = new TermEditManager(gui);
		JMenuBar mb               = null;

		if(um.okAction != null) { // updatable
			mb = new JMenuBar();
			JMenu edit  = new JMenu("Edit");
			edit.setMnemonic((int) 'E');
			mb.add(edit);
			edit.add(um.undoAction);
			edit.add(um.redoAction);
			edit.getItem(0).setAccelerator(XJChangeManager.undoKey);
			edit.getItem(1).setAccelerator(XJChangeManager.redoKey);
		}

		XJAction[] ops            = gui.getGT().operations(engine, (Component) gui);
		if(XJAction.hasMenuActions(ops)) {
			if(mb == null) {
				mb = new JMenuBar();
			}
			JMenu opsMenu  = new JMenu("Operations");
			opsMenu.setMnemonic((int) 'O');
			XJAction.addMenuActions(opsMenu, ops);
			mb.add(opsMenu);
			setJMenuBar(mb);
		}

		JPanel panel              = new JPanel(new BorderLayout());
		JPanel buttons            = new JPanel(new FlowLayout());

		panel.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(12, 11, 12, 11)));


		JButton cancelButton      = new JButton("Cancel");
		buttons.add(cancelButton);
		cancelButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					newTerm = null;
					javax.swing.SwingUtilities.invokeLater(
					new Runnable() {
						public void run() {
							TermEditorDialog.this.dispatchEvent(new java.awt.event.WindowEvent(TermEditorDialog.this,
							java.awt.event.WindowEvent.WINDOW_CLOSING));
						}
					});
													/*
														dispatching window closing event (as above) will call dispose()
														since DefaultCloseOperation is JDialog.DISPOSE_ON_CLOSE
														setVisible(false);
														dispose();
													 */
				}
			});

		JButton okButton          = null;
		if(um.okAction != null) {
			okButton = new JButton(um.okAction);
			buttons.add(okButton);
			getRootPane().setDefaultButton(okButton);
		}

		if(okButton != null) {//size of buttons is equal to larger button.
			if(okButton.getPreferredSize().getWidth() >
			cancelButton.getPreferredSize().getWidth()) {
				
				cancelButton.setPreferredSize(okButton.getPreferredSize());
			} else {
				okButton.setPreferredSize(cancelButton.getPreferredSize());
			}
		}

		panel.add("Center", content);
		panel.add("South", buttons);
       	// useless: gui.getGT().refreshRenderers(); // ??? crash?
		getContentPane().add(panel);
       	gui.getGT().refreshRenderers();

		// This segment may be better replaced by a simple message to pack():
		/*
		Dimension dimension       = ((JComponent) gui).getPreferredSize();
		int height                = (int) (dimension.getHeight() + cancelButton.getPreferredSize().getHeight()
			+ ((mb == null) ? 0 : mb.getPreferredSize().getHeight()) + 73);
		int width                 = (dimension.getWidth() > (2 * cancelButton.getPreferredSize().getWidth())) ?
			(int) (dimension.getWidth() + 33) :
			(int) (2 * cancelButton.getPreferredSize().getWidth() + 33);
		setSize(width, height);
		*/
		pack();
		// gui.getGT().refreshRenderers(); // setFocusCycleRoot(boolean focusCycleRoot) ????
		// useless: content.setFocusCycleRoot(true);
		/* WORSE?:
		XJComponent[] renderers = gui.getGT().collectSignificantRenderers();
		if (renderers.length>0)
			((Component)renderers[0]).requestFocusInWindow();*/
		// ... down to here
		
		//setCentered();
		setLocationRelativeTo(owner);
		this.setTitle(gui.getGT().getTitle());
		this.getAccessibleContext().setAccessibleName(gui.getGT().getTitle());
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setupHelp(gui, buttons);
                
		addWindowListener(new WindowAdapter(){
			public void windowClosed(WindowEvent e){
				gui.getGT().destroyRenderers();
			}
		});
	}

	protected void setupHelp(XJComponent gui, JPanel buttons) {
            JButton helpButton = HelpManager.createHelpButton(gui);
            if(helpButton != null){
                buttons.add(helpButton);
            }
	}

	public class TermEditManager extends XJChangeManager {
		public AbstractAction okAction;

		public TermEditManager(XJComponent c) {
			super(c);
			if(insertable || updatable) {
				okAction = new OKAction();
			} else {
				okAction = null;
			}
		}

		protected void updateActions() {
			super.updateActions();
			if(okAction != null) {
				okAction.setEnabled(isDirty());
			}
		}

		public class OKAction extends PrologAction {
			OKAction() {
				super(TermEditorDialog.this.engine, TermEditorDialog.this.content, "xjValidateInput(New)", okButtonText);
				// setEnabled(false); // enabled from the beginning, for now
				setThreaded(false);// we want to check actionConcluded() afterwards
			}

			public void actionPerformed(ActionEvent e) {
				if(!gt.loadAllFromGUI()) {
					return;
				}// first error was reported by its XJComponent
				newTerm = gt.getTermModel();

				if(gt.findProperty(TermEditorDialog.validateInput) != null) {
					setArguments(
						"[New]",
						new Object[]{newTerm}
						);
					super.actionPerformed(e);
					if(actionSucceeded()) {
						discardAllEdits();
						updateActions();
					} else {
						newTerm = null;
						return;
					}
				}

				// should abort here if there is one non optional node without a value
				javax.swing.SwingUtilities.invokeLater(
							new Runnable() {
								public void run() {
									TermEditorDialog.this.dispatchEvent(new java.awt.event.WindowEvent(TermEditorDialog.this,
										java.awt.event.WindowEvent.WINDOW_CLOSING));
								}
							});

				/*
				    setVisible(false);
				    dispose();
				 */
			}
		}
	}

	/** Waits for all drawing to occur */
	@Override
	public void setVisible(boolean aFlag) {
		super.setVisible(aFlag);
		XJDesktop.waitForSwing();
	}

	public TermModel edit() {
		//System.err.println("Current thread:"+Thread.currentThread());
		setVisible(true);
		return newTerm;
	}

	/*
	    might try to release cache here
	    public void dispose(){
	    System.out.println("dispose is called");
	    super.dispose();
	    }
	  */
}
