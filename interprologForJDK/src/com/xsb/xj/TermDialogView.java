package com.xsb.xj;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.HelpManager;
import com.xsb.xj.util.XJException;


/**
 * Description of the Class
 *
 *@version   $Id: TermDialogView.java,v 1.25 2005/03/12 20:23:21 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class TermDialogView extends JInternalFrame {
	JComponent content;
	PrologEngine engine;
	JFrame frame;
	JMenu windowsMenu;
	JMenuItem myMenuItem;
        
        static final String validateInput  = "validateInput";

	public TermDialogView(final XJComponent gui, JFrame frame) {
		super();

		this.setClosable(true);
		this.setResizable(true);
		this.setIconifiable(true);
		this.setMaximizable(true);
		this.addInternalFrameListener(
			new javax.swing.event.InternalFrameAdapter() {
                            public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
                                if((windowsMenu != null) && (myMenuItem != null)) {
                                    windowsMenu.remove(myMenuItem);
                                }
                                gui.getGT().destroyRenderers();
                            }
			});

		engine = gui.getEngine();
		this.frame = frame;

		this.setTitle(gui.getGT().getTitle());
		this.getAccessibleContext().setAccessibleName(gui.getGT().getTitle());

		content = (JComponent) gui;
		final OKChangeManager um  = new OKChangeManager(gui);
		JMenuBar mb               = null;

		if(um.okAction != null) {  // updatable
			mb = new JMenuBar();
			JMenu edit  = new JMenu("Edit");
			mb.add(edit);
			edit.setMnemonic((int) 'E');
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
		}

		JPanel panel              = new JPanel(new BorderLayout());
		panel.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(12, 11, 12, 11)));
		JPanel buttons            = new JPanel(new FlowLayout());

		JButton okButton          = null;
		if(um.okAction != null) {
			okButton = new JButton(um.okAction);
			buttons.add(okButton);
		}

		JButton cancelButton      = new JButton("Cancel");
		cancelButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					dispose();
				}
			});
			
		buttons.add(cancelButton);
		if(okButton != null) {
			okButton.setPreferredSize(cancelButton.getPreferredSize());
		}

		if(mb != null) {
			setJMenuBar(mb);
		}

		panel.add("Center", content);
		panel.add("South", buttons);

		getContentPane().add(panel);
		gui.getGT().refreshRenderers();

		/*
		    addWindowListener(new WindowAdapter(){
		    public void windowClosed(WindowEvent e){
		    um.closeAction.actionPerformed(null);
		    if (um.closeAction.actionConcluded()) dispose();
		    }
		    }
		    );
		 */
		// This segment may be better replaced by a simple message to pack():
		/*
		Dimension dimension = ((JComponent) gui).getPreferredSize();
		int height          = (int) (dimension.getHeight() + 
                    cancelButton.getPreferredSize().getHeight() + ((mb==null)?0:mb.getPreferredSize().getHeight()) + 73);
		int width           = (dimension.getWidth() > (2 * cancelButton.getPreferredSize().getWidth())) ? 
                (int) (dimension.getWidth() + 50) : (int) (2 * cancelButton.getPreferredSize().getWidth() + 50);
		
		//System.out.println("larger " + cancelButton.getPreferredSize().getWidth());
		setSize(width, height);
		*/
		pack();
		// ... down to here

		setCentered();
                addToWindowsMenu(gui, frame);
                setupHelp(gui, buttons);
		setIcon(gui.getGT());
		makeVisible();
		//now should send requestFocus() to someone
	}

        private void addToWindowsMenu(XJComponent gui, JFrame frame){
            JMenuBar menu             = frame.getJMenuBar();
            if(menu != null) {
                int menuCount  = menu.getMenuCount();
                int i          = 0;
                while((i < menuCount) && (windowsMenu == null)) {
                    if(menu.getMenu(i).getText().equals("Windows")) {
                        windowsMenu = menu.getMenu(i);
                    }
                    i++;
                }
                if(windowsMenu != null) {
                    myMenuItem = new JMenuItem(gui.getGT().getTitle());
                    myMenuItem.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            try {
                                setSelected(true);
                                TermDialogView.this.setIcon(false);
                            } catch(PropertyVetoException ex) {
                                System.out.println("weird exception trying to select internal window:" + ex);
                            }
                        }
                    });
                    windowsMenu.add(myMenuItem);
                }
            }
       }
        
        protected void setupHelp(XJComponent gui, JPanel buttons){
            JButton helpButton = HelpManager.createHelpButton(gui);
            if(helpButton != null){
                buttons.add(helpButton);
            }
        }
        
	private void setIcon(GUITerm gt) {
		Icon frameIcon;
		TermModel iconLocation  = gt.findProperty("icon");
		if(iconLocation != null) {
			URL iconURL  = getClass().getResource((String) iconLocation.node);//in classpath
			if(iconURL == null) {
				iconURL = getClass().getResource("/" + (String) iconLocation.node);
			}
			if(iconURL == null) {//file path, not in classpath
				File file  = new File((String) iconLocation.node);
				if(file.exists()) {
					try {
						iconURL = file.toURI().toURL();
					} catch(MalformedURLException e) {
						throw new XJException("bad file URL???");
					}
				}
			}
			if(iconURL != null) {
				frameIcon = new ImageIcon(iconURL);
			} else {
				frameIcon = XJDesktop.XSB_INTERNAL_ICON;
				System.err.println("Icon file not found " + (String) iconLocation.node);
			}
		} else {
			frameIcon = XJDesktop.XSB_INTERNAL_ICON;
		}
		setFrameIcon(frameIcon);
	}

	/** Waits for all drawing to occur */
	@Override
	public void setVisible(boolean aFlag) {
		super.setVisible(aFlag);
		XJDesktop.waitForSwing();
	}

	public void makeVisible() {
		frame.getContentPane().add(this);
		setVisible(true);
		try {
			setSelected(true);
		} catch(java.beans.PropertyVetoException e) {
			System.out.println("Exception in InternalTermFrame:" + e);
		}
	}

	public void dispose() {
		super.dispose();
		if((windowsMenu != null) && (myMenuItem != null)) {
			windowsMenu.remove(myMenuItem);
		}
	}

	public void setCentered() {
            int outerWidth = (frame == null)? (int)getToolkit().getScreenSize().getWidth()
            : (int)frame.getContentPane().getSize().getWidth();
            int outerHeight= (frame == null)? (int)getToolkit().getScreenSize().getHeight()
            : (int)frame.getContentPane().getSize().getHeight();
            this.setLocation((int) (outerWidth - this.getSize().getWidth()) / 2,
                    (int) (outerHeight - this.getSize().getHeight()) / 2);
	}
        
        public void setSize(int width, int height){
            int outerWidth = (frame == null)? (int)getToolkit().getScreenSize().getWidth()
                                            : (int)frame.getContentPane().getSize().getWidth();
            int outerHeight= (frame == null)? (int)getToolkit().getScreenSize().getHeight()
                                            : (int)frame.getContentPane().getSize().getHeight();
            // minus title bar size()
  //          outerHeight = outerHeight - this.getInsets().top - this.getInsets().bottom - 50;
            int newWidth = (width > outerWidth)?outerWidth:width;
            int newHeight = (height > outerHeight)?outerHeight:height;
            super.setSize(newWidth, newHeight);
        }
        
        public void setSize(Dimension d){
            int outerWidth = (frame == null)? (int)getToolkit().getScreenSize().getWidth()
            : (int)frame.getContentPane().getSize().getWidth();
            int outerHeight= (frame == null)? (int)getToolkit().getScreenSize().getHeight()
            : (int)frame.getContentPane().getSize().getHeight();
            // minus title bar size()
//            outerHeight = outerHeight - this.getInsets().top - this.getInsets().bottom - 50;
            double newWidth = (d.getWidth() > outerWidth)?outerWidth:d.getWidth();
            double newHeight = (d.getHeight() > outerHeight)?outerHeight:d.getHeight();
            super.setSize(new Dimension((int)newWidth, (int)newHeight));
        }

        public void setLocation(int x, int y){
            super.setLocation(((x > 0) ? x : 0), ((y > 0) ? y : 0));
        }
        
        public void setLocation(Point p){
            super.setLocation(new Point((int)((p.getX() > 0) ? p.getX() : 0),
                    (int)((p.getY() > 0) ? p.getY() : 0)));
        }
               
	public class OKChangeManager extends XJChangeManager {
		public PrologAction okAction;

		public OKChangeManager(XJComponent c) {
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
				if(gt.findProperty("enableok") == null) {
					okAction.setEnabled(isDirty());
				} else {
					okAction.setEnabled(true);
				}
			}
		}

		public class OKAction extends PrologAction {
			OKAction() {
				super(OKChangeManager.this.engine, OKChangeManager.this.component, "xjSaveTerm(Old,New,IsNew)", "OK");
				if(gt.findProperty("enableok") == null) {
					setEnabled(false);
				} else {
					setEnabled(true);
				}
				setThreaded(false);// we want to check actionConcluded() afterwards
			}

			public void actionPerformed(ActionEvent e) {
				if(!gt.loadAllFromGUI()) {
					return;
				} // first error was reported by its XJComponent
                                if(gt.findProperty(TermDialogView.validateInput) != null) {
                                    if(!doValidateInput(e)){
                                        return;
                                    }
                                }
				TermModel newTerm  = gt.getTermModel();
				// should abort here if there is one non optional node without a value
				setArguments(
					"[Old,New,IsNew]",
					new Object[]{(!isNewTerm ? getOldTerm().getTermModel() : null), newTerm, new Boolean(isNewTerm)}
					);
				super.actionPerformed(e);
				if(actionSucceeded()) {
					discardAllEdits();
					isNewTerm = false;
					updateActions();

					TermDialogView.this.setVisible(false);
					TermDialogView.this.dispose();
				}
			}
                        
                        protected boolean doValidateInput(ActionEvent e){
                            ValidateAction validate = new ValidateAction();
                            validate.actionPerformed(e);
                            return(validate.actionSucceeded());
                        }
		}
                
                public class ValidateAction extends PrologAction {
			ValidateAction() {
				super(TermDialogView.this.engine, TermDialogView.this.content, "xjValidateInput(New)", "Validate Form");
				setThreaded(false);// we want to check actionConcluded() afterwards
			}

			public void actionPerformed(ActionEvent e) {
				if(!gt.loadAllFromGUI()) {
					return;
				}// first error was reported by its XJComponent
				TermModel newTerm = gt.getTermModel();

				if(gt.findProperty(TermEditorDialog.validateInput) != null) {
					setArguments(
						"[New]",
						new Object[]{newTerm}
						);
					super.actionPerformed(e);
				}
			}
		}
                
	}

}
