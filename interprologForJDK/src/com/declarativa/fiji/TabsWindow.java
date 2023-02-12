package com.declarativa.fiji;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * @author mc
 * A window with a JTabbedPane where "any" window can be embedded in a tab. A few are initialized to contain editors, justifications, etc,
 */
@SuppressWarnings("serial")
public class TabsWindow extends JFrame {
	
	static final String DETAB_TIP = "Right-click to move tab to window";
	public static final String EDITORS_TABWINDOW_PREF = "com.declarativa.fiji.TabsWindow.editors";
	public static final String JUSTIFIERS_TABWINDOW_PREF = "com.declarativa.fiji.TabsWindow.justifiers";
	public static final String FINDERS_TABWINDOW_PREF = "com.declarativa.fiji.TabsWindow.finders";
	public static TabsWindow editors;
	public static TabsWindow justifiers;
	public static TabsWindow finders;

	JTabbedPane tabsPane;
	ArrayList<JFrame> originalWindows;
	ArrayList<JMenuBar> originalMenuBars;
	ArrayList<Action> detabActions;
	
	public interface Editable {
		/**
		 * Destroy the UI object without bothering the user
		 */
		public void destroy();
		/**
		 * @return Whether this object has changes that may need saving
		 */
		public boolean isDirty();
		/** Attempt to close the object.
		 * @return Whether this UI object effectively closed
		 */
		public boolean doClose();
	}

	static void initialize(FijiSubprocessEngineWindow listener){
		editors = new TabsWindow("Editors",EDITORS_TABWINDOW_PREF, listener);
		justifiers = new TabsWindow("Justifications",JUSTIFIERS_TABWINDOW_PREF, listener);
		justifiers.setDefaultCloseOperation(HIDE_ON_CLOSE); // nothing to lose, no persistence
		finders = new TabsWindow("Term Search",FINDERS_TABWINDOW_PREF,listener);
		finders.setDefaultCloseOperation(HIDE_ON_CLOSE); // nothing to lose, no persistence
	}
	public static boolean inSomeTab(Component w){
		return (editors!=null && editors.inTab(w) || justifiers!=null && justifiers.inTab(w));
	}

	private TabsWindow(String title, String prefName, FijiSubprocessEngineWindow listener){
		super(title);
		originalWindows = new ArrayList<JFrame>();
		originalMenuBars = new ArrayList<JMenuBar>();
		detabActions = new ArrayList<Action>();
		tabsPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		getContentPane().add(tabsPane, BorderLayout.CENTER);
		tabsPane.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				int index = tabsPane.getSelectedIndex();
				if (index!=-1)
					setJMenuBar(originalMenuBars.get(index));
			}
		});
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				boolean dirty = false;
				int firstDirty = -1;
				for (int i = 0; i<originalWindows.size(); i++){
					JFrame w = originalWindows.get(i);
					if (w instanceof Editable)
						if (((Editable)w).isDirty()){
							dirty = true;
							firstDirty = i;
							break;
						}
				}
				if (dirty && JOptionPane.showConfirmDialog(TabsWindow.this,"There is at least one unsaved editor window. Throw away the changes?",
					"Close without saving?",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION){
					tabsPane.setSelectedIndex(firstDirty);
					return;
				}
				ArrayList<JFrame> copy = new ArrayList<JFrame>(originalWindows); 
				for (JFrame w:copy)
					if (w instanceof Editable)
						((Editable)w).destroy();
				setVisible(false);
			}
		});
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		String P = listener.getPreference(prefName);
		if (P!=null){
			Rectangle R = FijiPreferences.pref2Rectangle(P);
			if (R!=null) setBounds(R);
		} else setSize(FijiSubprocessEngineWindow.NEW_WINDOW_SIZE);
		listener.updateBoundsPreference(prefName,this);
	}
	
	private JFrame findSimilar(JFrame window){
		String title = window.getTitle();
		for (JFrame w:originalWindows)
			if (w.getTitle().equals(title) && w!=window)
				return w;
		return null;
	}
	
	public int addWindow(JFrame window, Action tabItemAction){
		return addWindow(window, tabItemAction, false, null);
	}
	
	public int addWindow(JFrame window, Action tabItemAction, boolean replaceSimilar){
		return addWindow(window, tabItemAction, replaceSimilar, null);
	}
	/**
	 * @param window whose content will be moved into a tab pane; the window will be made invisible
	 * @param tabItemAction action which will be disabled while the window is tabbed
	 * @param replaceSimilar replace the first existing tabbed window with same title
	 * @param closer  to run when the tab close button is clicked; can be null
	 * @return new tab pane index
	 */
	public int addWindow(JFrame window, Action tabItemAction, boolean replaceSimilar, Runnable closer){
		if (!(window.getContentPane().getLayout() instanceof BorderLayout))
			throw new RuntimeException("Tab windows must have BorderLayout:"+window);
		if (replaceSimilar){
			JFrame similar = findSimilar(window);
			if (similar != null)
				destroy(similar);
		}
		originalWindows.add(window);
		detabActions.add(tabItemAction); tabItemAction.setEnabled(false);
		originalMenuBars.add(window.getJMenuBar());
		
		int index = tabsPane.getTabCount();
		Container content = window.getContentPane();
		window.setVisible(false);
		window.setContentPane(new JLabel("DUMMY"));
		ImageIcon imageIcon = (window.getIconImage()!=null ? new ImageIcon(window.getIconImage()) : null);
		tabsPane.insertTab(window.getTitle(),null,content,DETAB_TIP,index);
		ButtonTabComponent button = new ButtonTabComponent(closer,imageIcon);
		tabsPane.setTabComponentAt(tabsPane.indexOfComponent(content), button);
                /*
                  // MK: made setVisible(true) unconditional. Not optimal,
                  //     but previously it was a BAD bug: if the entire
                  //     FinderWindow `window' is closed by clicking X, then
                  //     subsequent Tools->Term Finder actions won't make
                  //     the search window visible.
                  //     Ideally, closing FinderWindow should close all
                  //     the tabs in the TabsWindow.
		if (index == 0){ // first pane
			pack();
			setVisible(true);
		}
                */
                pack();
                setVisible(true);

		tabsPane.setSelectedComponent(content);
		//setJMenuBar(window.getJMenuBar());
		toFront();
		return index;
	}
	
	JFrame tabToWindow(int index){
		Container content = (Container)tabsPane.getComponentAt(index);
		JFrame window = originalWindows.remove(index);
		tabsPane.remove(index);
		window.setJMenuBar(originalMenuBars.remove(index));
		window.setContentPane(content);
		window.validate();
		window.setVisible(true);
		if (tabsPane.getSelectedIndex() != -1)
			setJMenuBar(originalMenuBars.get(tabsPane.getSelectedIndex()));
		else
			setJMenuBar(null);
		detabActions.remove(index).setEnabled(true);
		return window;
	}

	public Frame bringToFront(Frame w) {
		int index = originalWindows.indexOf(w);
		if (index != -1){
			tabsPane.setSelectedIndex(index);
			toFront();
			setVisible(true);
			return this;
		} else { // not in tab pane
			w.setVisible(true); w.toFront();
			return w;
		}	
	}

	public void updateTitle(JFrame w) {
		updateTitle(w,w.getTitle());
	}
	public void updateTitle(JFrame w, String newTitle) {
		int index = originalWindows.indexOf(w);
		if (index != -1){
			tabsPane.setTitleAt(index, newTitle);
			tabsPane.getTabComponentAt(index).revalidate();
		} 
	}

	public void updateIcon(JFrame w) {
		int index = originalWindows.indexOf(w);
		ImageIcon ii = (w.getIconImage()!=null?new ImageIcon(w.getIconImage()):null);
		if (index != -1)
			((ButtonTabComponent)(tabsPane.getTabComponentAt(index))).setStatusIcon(ii);
	}
	
	public JFrame currentWindow(JFrame w){
		int index = originalWindows.indexOf(w);
		if (index != -1){
			return this;
		} else { // not in tab pane
			return w;
		}	
	}
	
	public boolean inTab(Component w){
		return originalWindows.contains(w);
	}
	
	/** Disposes the window, and removes its tabbed content if here
	 * @param w
	 * Grabbed from http://docs.oracle.com/javase/tutorial/displayCode.html?code=http://docs.oracle.com/javase/tutorial/uiswing/examples/components/TabComponentsDemoProject/src/components/ButtonTabComponent.java
	 */
	public void destroy(Window w) {
		int index = originalWindows.indexOf(w);
		if (index != -1){
			tabsPane.remove(index);
			originalWindows.remove(index);
			originalMenuBars.remove(index);
			detabActions.remove(index);
			setJMenuBar(null);
		}
		if (w instanceof Editable)
			((Editable)w).destroy();
		else
			w.dispose();
	}
	
	public TabAction makeTabActionFor(JFrame window){
		return new TabAction(window);
	}
	
	public class TabAction extends AbstractAction{
		JFrame window;
		public TabAction(JFrame window){
			super("Embed In Tab");
			putValue(Action.SHORT_DESCRIPTION,"Embed window in "+getTitle()+" tab");
			this.window = window;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (window instanceof Editable)
				addWindow(window,this,false,new Runnable(){
					@Override
					public void run() {
						((Editable)window).doClose();
					}
				});
			else
				addWindow(window,this);
		}
	}
	
	/**
	 * Component to be used as tabComponent;
	 * Contains a JLabel to show the text and 
	 * a JButton to close the tab it belongs to 
	 */
	class ButtonTabComponent extends JPanel {	
		JLabel label;
	    public ButtonTabComponent(Runnable closer,ImageIcon statusIcon) {
	        //unset default FlowLayout' gaps
	        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
	        if (tabsPane == null) {
	            throw new NullPointerException("TabbedPane is null");
	        }
	        setOpaque(false);
	         
	        //make JLabel read titles from JTabbedPane
	        label = new JLabel(statusIcon,SwingConstants.TRAILING) {
	            public String getText() {
	                int i = tabsPane.indexOfTabComponent(ButtonTabComponent.this);
	                if (i != -1) {
	                    return tabsPane.getTitleAt(i);
	                }
	                return null;
	            }

				@Override
				protected void processMouseEvent(MouseEvent e) {
					if (!e.isPopupTrigger()){
					//if (!((e.getModifiers()&MouseEvent.BUTTON1_MASK)!=0 && ((e.getModifiers()&InputEvent.CTRL_MASK)!=0 || e.isMetaDown()))){
						super.processMouseEvent(e);
						return;
					}
					e.consume();
					final int i = tabsPane.indexOfTabComponent(ButtonTabComponent.this);
					tabsPane.setSelectedIndex(i);
					JPopupMenu popup = new JPopupMenu();
					JMenuItem detab = new JMenuItem("Move to stand alone window");
					popup.add(detab);
					detab.addActionListener(new ActionListener(){
						@Override
						public void actionPerformed(ActionEvent e) {
							tabToWindow(i);
						}
					});
					popup.show(this, e.getX(), e.getY());
					
				}
	            
	        };
	        label.setToolTipText(DETAB_TIP);
	        add(label);
            label.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(MouseEvent e) {
					tabsPane.setSelectedIndex(tabsPane.indexOfTabComponent(ButtonTabComponent.this));
				}          	
            });
	        //add more space between the label and the button
	        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
	        //tab button
	        JButton button = new TabButton(closer);
	        add(button);
	        //add more space to the top of the component
	        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
	    }
	 
	    void setStatusIcon(Icon icon){
	    	label.setIcon(icon);
	    }
	    
	    private class TabButton extends JButton implements ActionListener {
	    	Runnable closer;
	        public TabButton(Runnable closer) {
	        	this.closer = closer;
	            int size = 17;
	            setPreferredSize(new Dimension(size, size));
	            if (closer==null)
	            	setToolTipText("close this tab");
	            //Make the button looks the same for all Laf's
	            setUI(new BasicButtonUI());
	            //Make it transparent
	            setContentAreaFilled(false);
	            //No need to be focusable
	            setFocusable(false);
	            setBorder(BorderFactory.createEtchedBorder());
	            setBorderPainted(false);
	            //Making nice rollover effect
	            //we use the same listener for all buttons
	            addMouseListener(buttonMouseListener);
	            setRolloverEnabled(true);
	            //Close the proper tab by clicking the button
	            addActionListener(this);
	        }
	 
	        public void actionPerformed(ActionEvent e) {
	            int i = tabsPane.indexOfTabComponent(ButtonTabComponent.this);
	            if (i != -1) {
	            	if (closer!=null) closer.run();
	            	else destroy(originalWindows.get(i)); // general case
	            	int newIndex = tabsPane.getSelectedIndex();
	            	if (newIndex!=-1)
	            		setJMenuBar(originalMenuBars.get(newIndex));
	            }
	        }
	 
	        //we don't want to update UI for this button
	        public void updateUI() {
	        }
	 
	        //paint the cross
	        protected void paintComponent(Graphics g) {
	            super.paintComponent(g);
	            Graphics2D g2 = (Graphics2D) g.create();
	            //shift the image for pressed buttons
	            if (getModel().isPressed()) {
	                g2.translate(1, 1);
	            }
	            g2.setStroke(new BasicStroke(2));
	            g2.setColor(Color.BLACK);
	            if (getModel().isRollover()) {
	                g2.setColor(Color.MAGENTA);
	            }
	            int delta = 6;
	            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
	            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
	            g2.dispose();
	        }
	    }
	 
	    private final MouseListener buttonMouseListener = new MouseAdapter() {
	        public void mouseEntered(MouseEvent e) {
	            Component component = e.getComponent();
	            if (component instanceof AbstractButton) {
	                AbstractButton button = (AbstractButton) component;
	                button.setBorderPainted(true);
	            }
	        }
	 
	        public void mouseExited(MouseEvent e) {
	            Component component = e.getComponent();
	            if (component instanceof AbstractButton) {
	                AbstractButton button = (AbstractButton) component;
	                button.setBorderPainted(false);
	            }
	        }
	    };
	}

}
