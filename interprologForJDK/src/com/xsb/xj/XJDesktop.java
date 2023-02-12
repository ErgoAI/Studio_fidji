package com.xsb.xj;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.plaf.FontUIResource;

import com.declarativa.interprolog.PrologEngine;
// for help button until moved from here
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.gui.ListenerWindow;
import com.xsb.xj.util.HelpManager;
import com.xsb.xj.util.OutputFrame;
import com.xsb.xj.util.SplashWindow;
import com.xsb.xj.util.XJException;

/**
 * Description of the Class
 *
 *@version   $Id: XJDesktop.java,v 1.43 2005/03/12 16:29:30 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJDesktop extends JFrame {

	public PrologEngine engine;
	JDesktopPane desktop;
	DesktopPlacer placer;
	InternalFrameListener focusTracker;
	JMenu windowsMenu;
	ListenerWindow console;

	public final static ImageIcon XSB_ICON           = new ImageIcon(
		XJDesktop.class.getResource("/com/xsb/xj/images/xsb_icon.gif"));
	public final static ImageIcon XSB_INTERNAL_ICON  = new ImageIcon(
		XJDesktop.class.getResource("/com/xsb/xj/images/xsb_internal_icon.gif"));

	public XJDesktop(String title, PrologEngine engine, XJTopLevel console){
		this(title,engine,(ListenerWindow)console);
	}
	public XJDesktop(String title, PrologEngine engine, ListenerWindow console) {
		//inset = 50
		this(title,
			new Rectangle(50, 50, Toolkit.getDefaultToolkit().getScreenSize().width - 50 * 2,
			Toolkit.getDefaultToolkit().getScreenSize().height - 50 * 2), engine, console);
	}

    public XJDesktop(String title, int width, int height, PrologEngine engine, ListenerWindow console) {
		this(title,
			new Rectangle((Toolkit.getDefaultToolkit().getScreenSize().width - width)/2,
                                      (Toolkit.getDefaultToolkit().getScreenSize().height - height)/2,
                                      width,
                                      height), engine, console);
	}

	public XJDesktop(String title, Rectangle r, PrologEngine engine, ListenerWindow console) {
		super(title);

		this.engine = engine;
		this.console = console;
		//final int inset = 50;
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//setBounds(inset, inset, screenSize.width - inset*2, screenSize.height-inset*2);
		setBounds(r);

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		desktop = new JDesktopPane();
		desktop.putClientProperty("JDesktopPane.dragMode", "outline");//Make dragging faster
		setContentPane(desktop);

		addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					mayExitApplication();
				}
			});
		setJMenuBar(desktopMenus());
		setVisible(true);
		placer = new DesktopPlacer(desktop);
		setXSBIcon();
                getAccessibleContext().setAccessibleName(title);
                getAccessibleContext().setAccessibleDescription(title);

		// sometimes there is an exception when some area is covered during paint
		// to avoid it do the following
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					RepaintManager.setCurrentManager(new XJRepaintManager());
				}
			});
	}

	// workaround the problem of getting exception in the beginning when some area is obstructed
	public class XJRepaintManager extends javax.swing.RepaintManager {
		public void paintDirtyRegions() {
			try {
				super.paintDirtyRegions();
			} catch(Exception ex) {
			}
		}
	}

	private SplashWindow splasher = null;

	public void showSplash(final String image) {
		setWaitCursor();
		splasher = new SplashWindow(this,new ImageIcon(image), true);
	}

	public void showSplash(final String image, boolean flag) {
		setWaitCursor();
		splasher = new SplashWindow(this,new ImageIcon(image), flag);
	}

	public void finishSplash() {
		if(splasher != null) {
			splasher.finishSplash();
			splasher = null;
			restoreCursor();
		}
	}

	public void setSplashProgress(int percentage, String status) {
		if(splasher == null) {
			throw new XJException("bad invocation of setSplashProgress");
		} else {
			splasher.setProgress(percentage, status);
		}
	}

	public void minimize() {
		setState(Frame.ICONIFIED);
	}

	/*
	    XJDesktop allows adding Window Listeners to it. These WindowListeners
	    handle window closing events. If window listeners are added by a programmer
	    (an XJ user) they are responsible for quiting the application if necessary.
	    See com.xsb.xj.util.WindowCloser for example.
	    If no WindowListener is added (there is only one window listener - this),
	    XJDesktop quits the application when window closing event is generated.
	    There is one hack here though - sometimes heavy weight popups add WindowListeners
	    in PopupFactory.recycleHeavyWeightPopup method and never remove it
	    (see Bug Id  4621508  in java bug database). This method checks for all
	    attached listeners except for PopupFactory listeners.
	  */
	public void mayExitApplication() {
		if(!checkEngineAvailable()) {
			System.out.println("Engine is not available to exit");
			return;
		}
		// if there are other window listeners that got attached (this one always gets attached first),
		// last attached is supposed to close the app
		if(getNumberOfXJListeners() > 1) {//There are listeners besides this one
			return;
		}
		System.exit(0);
		/*
		    if (windowsSavelyClosed()) {
		    if (prologIsDirty()){
		    int choice = JOptionPane.showConfirmDialog(this,
		    "Save changes to a file?",
		    "Save Before Exiting?",
		    JOptionPane.YES_NO_CANCEL_OPTION);
		    if (choice==JOptionPane.YES_OPTION && doSaveDialog() || choice==JOptionPane.NO_OPTION){
		    dispose();
		    engine.shutdown();
		    System.exit(0);
		    }
		    } else{
		    dispose();
		    engine.shutdown();
		    System.exit(0);
		    }
		    }
		  */
	}

	private int getNumberOfXJListeners() {
		int counter                              = 0;
		java.util.EventListener[] listenerArray  = getListeners(WindowListener.class);
		for(int i = 0; i < listenerArray.length; i++) {
			if(!(listenerArray[i].getClass().getName().startsWith("javax.swing.PopupFactory"))) {//internal anonymous class of PopupFactory
				counter++;
			}
		}
		return counter;
	}

	public boolean checkEngineAvailable() {
		if(!engine.isAvailable()) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(this, "First make sure your last top goal has ended.",
				"XSB Console is busy", JOptionPane.ERROR_MESSAGE);
			console.setVisible(true);
			return false;
		} else {
			return true;
		}
	}

	public void setXSBIcon() {
		setIconImage(XSB_ICON.getImage());
	}

	public void setIcon(String location) {
		setIconImage(new ImageIcon(getClass().getResource(location)).getImage());
	}

	public JMenu getWindowsMenu() {
		windowsMenu = new JMenu("Windows");
		windowsMenu.setMnemonic((int) 'W');
		if(console != null) {
			JMenuItem showConsole  = new JMenuItem(
				new AbstractAction("Show XSB Console") {
					public void actionPerformed(ActionEvent e) {
						console.setVisible(true);
					}
				});
			showConsole.setMnemonic((int) 'C');
			windowsMenu.add(showConsole);
		}
		JMenuItem minimize  = new JMenuItem(
			new AbstractAction("Minimize All") {
				public void actionPerformed(ActionEvent e) {
					JInternalFrame[] windows  = desktop.getAllFrames();
					System.out.println("Minimizing " + windows.length + " internal windows");
					try {
						for(int w = 0; w < windows.length; w++) {
							JInternalFrame frame  = windows[w];
							if(frame.isIconifiable()) {
								windows[w].setIcon(true);
							}
						}
					} catch(PropertyVetoException ex) {
						System.out.println("weird exception in desktopMenus():" + ex);
					}
				}
			});
		windowsMenu.add(minimize);
		minimize.setMnemonic((int) 'M');
		windowsMenu.addSeparator();
		return windowsMenu;
	}

	public JMenuBar desktopMenus() {
		JMenuBar mb  = new JMenuBar();
		mb.add(getFileMenu());
		mb.add(getWindowsMenu());
		mb.add(getHelpMenu());
		return mb;
	}

	public JMenu getFileMenu() {
		JMenu fileMenu  = new JMenu("File", true);
		fileMenu.setMnemonic((int) 'F');
		return fileMenu;
	}

	public JMenu getHelpMenu() {
		JMenu menu  = new JMenu("Help");
		menu.setMnemonic((int) 'H');
		return menu;
	}

	public void setCursor(Cursor cursor) {
		super.setCursor(cursor);
		JInternalFrame[] windows  = desktop.getAllFrames();
		for(int w = 0; w < windows.length; w++) {
			windows[w].setCursor(cursor);
		}
	}

	public void layDown() {
		placer.layDown();
	}

	public void layRight() {
		placer.layRight();
	}

	public Point nextLocation() {
		return placer.nextLocation();
	}

	public Point topLeft() {
		return placer.topLeft();
	}

	static class DesktopPlacer extends InternalFrameAdapter {
		JDesktopPane desktop;
		private int windowCount         = 0;
		final static int xOffset        = 25, yOffset        = 25;
		final static byte DOWNWARDS     = 1, TO_RIGHT     = 2;
		byte direction                  = 0;// current direction for occupying screen estate
		JInternalFrame departingWindow  = null;// a window from which to continue occupying screen estate

		public DesktopPlacer(JDesktopPane desktop) {
			this.desktop = desktop;
			direction = DOWNWARDS;
		}

		public void layDown() {
			direction = DOWNWARDS;
		}

		public void layRight() {
			direction = TO_RIGHT;
		}

		public void internalFrameActivated(InternalFrameEvent e) {
			Object source  = e.getSource();
			if(source instanceof JInternalFrame) {
				departingWindow = (JInternalFrame) source;
			} else {
				throw new RuntimeException("weird InternalFrameEvent source:" + source);
			}
		}

		public Point topLeft() {
			int x  = xOffset * windowCount;
			int y  = yOffset * windowCount;
			windowCount++;
			return boundPoint(new Point(x, y));
		}

		Point boundPoint(Point p) {
			int maxX  = desktop.getSize().width - 200;// we don't know the new window size...
			int maxY  = desktop.getSize().height - 70;
			if(p.x > maxX) {
				p.x = maxX;
				p.y = p.y + 10;
			}
			if(p.y > maxY) {
				p.y = maxY;
				p.x = p.x + 10;
				if(p.x > maxX) {
					p.x = maxX;
				}
			}
			return p;
		}

		public Point nextLocation() {
			return nextLocation(departingWindow);
		}

		Point nextLocation(JInternalFrame current) {
			int xc;
			int yc;
			int wc;
			int hc;
			if(current == null) {
				xc = 0;
				yc = 0;
				hc = 0;
				wc = 0;
			} else {
				xc = current.getLocation().x;
				yc = current.getLocation().y;
				hc = current.getHeight();
				wc = current.getWidth();
			}
			int x   = xc;
			int y   = yc;
			if(direction == DOWNWARDS) {
				y = y + hc + 2;
			} else if(direction == TO_RIGHT) {
				x = x + wc + 2;
			} else {
				throw new RuntimeException("bad direction");
			}
			return boundPoint(new Point(x, y));
		}

	}

	public class MyInternalFrame extends JInternalFrame {
		JMenuItem myMenuItem;

		public MyInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable) {
			super(title, resizable, closable, maximizable, iconifiable);
			setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
			setFrameIcon(XSB_INTERNAL_ICON);
			myMenuItem = windowsMenu.add(
				new AbstractAction(title) {
					public void actionPerformed(ActionEvent e) {
						try {
							setSelected(true);
							MyInternalFrame.this.setIcon(false);
						} catch(PropertyVetoException ex) {
							System.out.println("weird exception trying to select internal window:" + ex);
						}
					}
				});
                        this.getAccessibleContext().setAccessibleName(title);
                        this.getAccessibleContext().setAccessibleDescription(title + " frame");
			addInternalFrameListener(placer);
		}

		public void dispose() {
			super.dispose();
			windowsMenu.remove(myMenuItem);
		}

		public void makeVisible() {
			desktop.add(this);
			//setVisible(true);
			try {
				setSelected(true);
			} catch(java.beans.PropertyVetoException e) {
				System.out.println("Exception in InternalTermFrame:" + e);
			}
		}
	}

	public Object createTermForm(XJComponent c) {
		InternalTermFrame itf  = new InternalTermFrame(c);
		itf.setVisible(true);
		return itf;
	}

	public Object createTermForm(XJComponent c, int layer) {
		InternalTermFrame itf  = new InternalTermFrame(c);
		itf.setLayer(layer);
		itf.setVisible(true);
		return itf;
	}

	/*
	    public TermModel getTermFormResult(XJComponent gui){
	    TermEditorFrame form = new TermEditorFrame(gui,desktop.getRootPane());
	    create opaque glass pane
	    JPanel glass = new JPanel();
	    glass.setOpaque(false);
	    Attach modal behavior to frame
	    form.addInternalFrameListener(new ModalAdapter(glass));
	    Add modal internal frame to glass pane
	    glass.add(form);
	    Change glass pane to our panel
	    desktop.setGlassPane(glass);
	    this.getRootPane().setGlassPane(glass);
	    Show glass pane, then modal dialog
	    glass.setVisible(true);
	    form.setVisible(true);
	    return form.getResult();
	    }
	  */
	/*
	    static class ModalAdapter extends InternalFrameAdapter {
	    Component glass;
	    public ModalAdapter(Component glass) {
	    this.glass = glass;
	    Associate dummy mouse listeners
	    Otherwise mouse events pass through
	    MouseInputAdapter adapter = new MouseInputAdapter(){};
	    glass.addMouseListener(adapter);
	    glass.addMouseMotionListener(adapter);
	    }
	    public void internalFrameClosed(
	    InternalFrameEvent e) {
	    glass.setVisible(false);
	    }
	    }
	  */
	/**
	 * Example of a container allowing saving/deleting a term and top operations
	 * menu
	 *
	 *@version   $Id: XJDesktop.java,v 1.43 2005/03/12 16:29:30 tvidrevich Exp $
	 */
	public class InternalTermFrame extends MyInternalFrame {
		JComponent content;

		InternalTermFrame(final XJComponent gui) {
			super(gui.getGT().getTitle(), true, true, true, true);
			//setSize(gt.getPreferredSize(getPreferredSize()));

			gui.getGT().refreshRenderers();
			//setLocation(nextLocation());

			content = (JComponent) gui;

			final XJChangeManager um  = new XJChangeManager(gui);
			JMenuBar mb = null;

			if((um.deleteAction != null) || (um.saveAction != null)) { // updatable
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

                        JMenu helpMenu = HelpManager.createHelpMenu(gui);
                        if(helpMenu != null){
                            if(mb == null) {
                                mb = new JMenuBar();
                            }
                            mb.add(helpMenu);
                        }
                        
			if(mb != null) {
                            setJMenuBar(mb);
			}

			if((um.deleteAction != null) || (um.saveAction != null)) {
				JPanel panel    = new JPanel(new BorderLayout());
				JPanel buttons  = new JPanel(new FlowLayout());

				if(um.deleteAction != null) {
					buttons.add(new JButton(um.deleteAction));
				}

				if(um.saveAction != null) {
					buttons.add(new JButton(um.saveAction));
				}

                                // create a help button only of there is a button panel
                                // (so that it would not be the only button in the middle)
                                // there is always a help menu
                                JButton helpButton = HelpManager.createHelpButton(gui);
                                if(helpButton != null) {
					buttons.add(helpButton);
				}
                                
				panel.add("Center", content);
				panel.add("South", buttons);
				getContentPane().add(panel);
			} else {
				getContentPane().add(content);
			}

			// An example of "chaining" two actions together:
			addInternalFrameListener(
				new InternalFrameAdapter() {
					public void internalFrameClosing(InternalFrameEvent e) {
						um.closeAction.actionPerformed(null);
						if(um.closeAction.actionConcluded()) {
							dispose();
						}
					}
                                        public void internalFrameClosed(InternalFrameEvent e){
                                            gui.getGT().destroyRenderers();
                                        }
				});
                        setSize(this.getPreferredSize());
			this.setLocation((int) (desktop.getWidth() - this.getPreferredSize().getWidth()) / 2,
				(int) (desktop.getHeight() - this.getPreferredSize().getHeight()) / 2);

			pack();
			makeVisible();
			waitForSwing();

			//now should send requestFocus() to someone
		}
                
        public void setSize(int width, int height){
            int newWidth = (width > desktop.getWidth())?(desktop.getWidth()):width;
            int newHeight = (height > desktop.getHeight())?(desktop.getHeight()):height;
            super.setSize(newWidth, newHeight);
        }

        public void setSize(Dimension d){
            double newWidth = (d.getWidth() > desktop.getWidth())?(desktop.getWidth()):d.getWidth();
            double newHeight = (d.getHeight() > desktop.getHeight())?(desktop.getHeight()):d.getHeight();
            super.setSize(new Dimension((int)newWidth, (int)newHeight));
        }

        public void setLocation(int x, int y){
            super.setLocation(((x > 0) ? x : 0), ((y > 0) ? y : 0));
        }
        
        public void setLocation(Point p){
            super.setLocation(new Point((int)((p.getX() > 0) ? p.getX() : 0), 
                                        (int)((p.getY() > 0) ? p.getY() : 0)));
        }
 	}

	/**
	 * Minimal test to display an XJComponent
	 *
	 *@param c  Description of the Parameter
	 */
	public static JFrame testGUI(JComponent c){
		return testGUI(c,true);
	}
	public static JFrame testGUI(JComponent c, boolean makeVisible) {
		JFrame f  = new JFrame();
		XJComponent gui     = (XJComponent) c;
		GUITerm gt = gui.getGT();
		f.setTitle(gt.getUserTitle());
		f.getContentPane().add(c);
		gt.refreshRenderers();
		JMenu opsMenu = makeOperationsMenu(gui);
		if(opsMenu!=null) {
			JMenuBar mb = new JMenuBar();
			mb.add(opsMenu);
			f.setJMenuBar(mb);
		}
		f.pack();
		f.setVisible(true);
		waitForSwing();
		return f;
	}

	/**
	 * Example for displaying an XJComponent with undo/redo and top operations menu
	 *
	 *@param c  Description of the Parameter
	 */
	public static JFrameWithEdit testUndoGUI(JComponent c) {
		JFrameWithEdit f            = new JFrameWithEdit(/*"Test undo/save for some XJComponent"*/);
		f.getContentPane().add(c);
		JMenuBar mb         = new JMenuBar();
		JMenu edit          = new JMenu("Edit");
		f.editMenu = edit;

		XJChangeManager um  = new XJChangeManager((XJComponent) c);
		//edit.add(um.saveAction);
		//edit.add(um.deleteAction);
		edit.add(um.undoAction);
		edit.getItem(0).setAccelerator(XJChangeManager.undoKey);
		edit.add(um.redoAction);
		edit.getItem(1).setAccelerator(XJChangeManager.redoKey);

		mb.add(edit);

		XJComponent gui     = (XJComponent) c;
		GUITerm gt = gui.getGT();
		f.setTitle(gt.getUserTitle());
		gt.refreshRenderers();

		JMenu opsMenu = makeOperationsMenu(gui);
		if(opsMenu!=null)
			mb.add(opsMenu);

		f.setJMenuBar(mb);
		f.pack();
		f.setVisible(true);
		waitForSwing();
		return f;
	}
	
	static class JFrameWithEdit extends JFrame{
		JMenu editMenu;
		public JMenu getEditMenu(){
			return editMenu;
		}
	}
	
	/** Returns a menu with the operations specified for this component, or null if none; gui must also be a JComponent */
	public static JMenu makeOperationsMenu(XJComponent gui){
		return makeOperationsMenu(gui,"Operations");
	}
	
	public static JPopupMenu makeOperationsPopupMenu(XJComponent gui){
		XJAction[] ops      = gui.getGT().operations(gui.getEngine(), (JComponent)gui);
		if(ops.length > 0) {
			JPopupMenu opsMenu  = new JPopupMenu("Operations");
			XJAction.addMenuActions(opsMenu, ops);
			return opsMenu;
		} else return null;
	}
	
	public static JMenu makeOperationsMenu(XJComponent gui,String menuName){
		XJAction[] ops      = gui.getGT().operations(gui.getEngine(), (JComponent)gui);
		if(ops.length > 0) {
			JMenu opsMenu  = new JMenu(menuName);
			if (XJAction.addMenuActions(opsMenu, ops) >0)
				return opsMenu;
			else return null;
		} else return null;
	}
	
	public static JMenu addOperationsMenu(XJComponent gui,String menuName,JFrame window){
		JMenuBar mb = window.getJMenuBar();
		if (mb==null) {
			mb = new JMenuBar();
			window.setJMenuBar(mb);
		}
		JMenu menu = makeOperationsMenu(gui,menuName);
		mb.add(menu);
		return menu;
	}
	

	/**
	 * Encapsulate a goal as a Swing Action. Use this for global operations, by
	 * creating an Action and using it in JButton, JMenu, etc. By keeping its
	 * object od on the Prolog side you can disable/enable the GUI operations using
	 * Action.setEnabled()
	 *
	 *@param goal         Description of the Parameter
	 *@param description  Description of the Parameter
	 *@return             Description of the Return Value
	 */
	public PrologAction createPrologAction(Object goal, String description) {
		return new PrologAction(engine, this, goal, description);
	}

	public void minimizeFrameTitle(JInternalFrame frame) {
		frame.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
	}

        public void showOutputFrame() {
            OutputFrame outputFrame = new OutputFrame(this);
            outputFrame.setModal(true);
            outputFrame.setVisible(true);
        }
        
        public void showOutputFrame(String mode, String text) {
            showOutputFrame(mode, text, null);
        }
        
	public void showOutputFrame(String mode, String text, String details) {
		OutputFrame outputFrame = new OutputFrame(this);
		outputFrame.addWarning(mode, text, details);
                outputFrame.setModal(true);
		outputFrame.setVisible(true);
	}
        
	/**
	 * Call this method to close a heavy frame (like xjDesktop)
	 * from anything other then close button in the corner of the frame.
	 * For example, this method may be called from 'Exit' menu.
	 * It will invoke all the window listeners attached to the frame.
	 */
	public static void closeWindow( final java.awt.Window frame ) {
		SwingUtilities.invokeLater( new Runnable(){
			public void run() {
				frame.dispatchEvent( new WindowEvent( frame,
				WindowEvent.WINDOW_CLOSING ) );
			}});
	}
	
	public static class RadioMenu extends JMenu {
		private TermModel[] terms;
		private JRadioButtonMenuItem[] items;
		/** Construct a JMenu with Radio items; each TermModel will be ItemCaption(Tip,State) */
		public RadioMenu(String name, TermModel[] terms, String selected){
			super(name);
			this.terms = terms;
			items = new JRadioButtonMenuItem[terms.length];
			ButtonGroup bg = new ButtonGroup();
			for (int i = 0; i<terms.length; i++){
				TermModel t = terms[i];
				JRadioButtonMenuItem item = new JRadioButtonMenuItem(t.node.toString());
				items[i] = item;
				if (t.getChild(0)!=null) item.setToolTipText(t.getChild(0).toString());
				bg.add(item); add(item);
				if (selected!=null && t.node.toString().equals(selected))
					item.setSelected(true);
			}
		}
		public TermModel getTerm(){
			for (int i=0;i<items.length;i++){
				if (items[i].isSelected())
					return (TermModel)terms[i].getChild(1);
			}
			return null;
		}
	}
	
	public static RadioMenu makeRadioMenu(String name, TermModel[] terms, String selected){
		return new RadioMenu(name, terms, selected);
	}
	/** Wait for all pending AWT events to be processed; useful after lazy component creation, to avoid Prolog race conditions when
	 * the component becomes visible. If you implement a new XJ container make sure you call this */
	public static void waitForSwing(){
		if (!SwingUtilities.isEventDispatchThread())
		try {
			SwingUtilities.invokeAndWait(new Runnable(){
				@Override
				public void run() {
					// Do nothing...but only after Swing has handled previous events:-)
				}
			});
		} catch (Exception e) {
			throw new XJException("Failure waiting for Swing",e);
		}
	}
	public void setWaitCursor() {
		setWaitCursor(this);
	}

	public void restoreCursor() {
		restoreCursor(this);
	}
	public static void setWaitCursor(Component C) {
		ListenerWindow.setWaitCursor(C);
	}
	public static void restoreCursor(Component C) {
		ListenerWindow.restoreCursor(C);
	}
	
	public static Object findWindowOrSimilar(Object c){
		if (c instanceof JInternalFrame) return c;
		else if (c instanceof Window) return c;
		else if (c instanceof JComponent) return ((JComponent)c).getTopLevelAncestor();
		else throw new XJException("Weird component:"+c);
	}
	
	/** Finds the component's window or internal frame and brings it to the front */
	public static void bringToFront(Component c){
		Object top = findWindowOrSimilar(c);
		if (top instanceof Window) {
			((Window)top).setVisible(true);
			((Window)top).toFront();
		} else if (top instanceof JInternalFrame){
			((JInternalFrame)top).setVisible(true); 
			((JInternalFrame)top).toFront();
		} else throw new XJException("Bad top container");
	}
	
	public static ImageIcon fetchIcon(Object user, TermModel iconLocation){
		URL iconURL  = user.getClass().getResource((String) iconLocation.node);//in classpath
		if(iconURL == null) {
			iconURL = user.getClass().getResource("/" + (String) iconLocation.node);
		}
		if(iconURL == null) {//file path, not in classpath
			File file  = new File((String) iconLocation.node);
			if(file.exists()) {
				try {
					iconURL = file.toURI().toURL();
				} catch(MalformedURLException e) {
					throw new XJException("bad file URL??? "+iconURL);
				}
			} else {
				try {
					iconURL = new URL((String) iconLocation.node);
				} catch(MalformedURLException e) {
					throw new XJException("bad web URL??? :"+iconLocation.node);
				}
			}
		} 
		if(iconURL != null) {
			return new ImageIcon(iconURL);
		} else {
			System.err.println("Icon not found " + iconLocation.node);
			return null;
		}
	}
	
	public static void setMinimumHeight(Component C,int h){
		int w = C.getWidth();
		Dimension D = new Dimension(w,h);
		C.setMinimumSize(D);
	}
	public static void setPreferredHeight(Component C,int h){
		int w = C.getWidth();
		Dimension D = new Dimension(w,h);
		C.setPreferredSize(D);
	}
	
	public static void setClipboardText(String text){
		StringSelection selection = new StringSelection(text);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
	}
	
	public static String getClipboardText(){
		String text=null;
		try{
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			for (DataFlavor df : clipboard.getAvailableDataFlavors())
				System.out.println("Available:"+df);
			text = (String)clipboard.getData(DataFlavor.stringFlavor);
		} catch(Exception e){
			System.err.println("Bad clipboard:"+e);
		}
		if (text==null) text="";
		return text;
	}
	
	private static File clipBoardTextFile=null;
	public static String getClipboardTextFile(){
		try{
			if (clipBoardTextFile==null) clipBoardTextFile = File.createTempFile("xjClipboard",".txt");
			clipBoardTextFile.deleteOnExit();
			System.out.println(clipBoardTextFile);
			String text = getClipboardText();
			FileWriter fw = new FileWriter(clipBoardTextFile);
			fw.write(text,0,text.length());
			fw.close();
		} catch(Exception e){
			System.err.println("Error preparing clipboard file:"+e);
		}
		return clipBoardTextFile.getAbsolutePath();
	}
	/**
	 * @param fontSizePercentage relative size of all default fonts in app
	 */
	public static void initializeFontSize(int fontSizePercentage) {
        float multiplier = fontSizePercentage / 100.0f;
        UIDefaults defaults = UIManager.getDefaults();
        for (Enumeration<Object> e = defaults.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            Object value = defaults.get(key);
            if (value instanceof Font) {
                Font font = (Font) value;
                int newSize = Math.round(font.getSize() * multiplier);
                if (value instanceof FontUIResource) {
                    defaults.put(key, new FontUIResource(font.getName(), font.getStyle(), newSize));
                } else {
                    defaults.put(key, new Font(font.getName(), font.getStyle(), newSize));
                }
            }
        }
    }

}
