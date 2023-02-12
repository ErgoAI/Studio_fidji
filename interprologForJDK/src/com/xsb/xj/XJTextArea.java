package com.xsb.xj;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.gui.ListenerWindow;
import com.xsb.xj.util.GUIError;
import com.xsb.xj.util.XJException;

/** Actually it's a subclass of JScrollPane */
@SuppressWarnings("serial")
public class XJTextArea extends JScrollPane implements XJComponent, DocumentListener, FocusListener, PropertyChangeListener{
    GUITerm gt;
    private JTextArea t;
    PrologEngine engine;
    private boolean dirty;
    private JPopupMenu popupMenu;
    private boolean badInputInGUI; // if this is true, we know GUI data is incorrect
    /** If some action is wired to this textarea, the enter key will function as if this was a textfield, 
    firing the actions. If operations are defined (for a popup menu) then popup event handlers wired outside will fail to be triggered */
    private ArrayList<ActionListener> actionListeners;


    public XJTextArea(PrologEngine engine,GUITerm gt){
        this.engine=engine;
        setGT(gt);
        dirty = true;
        badInputInGUI=false;
        if (gt.findProperty(GUITerm.AUTOSIZE)!=null){
        	setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        	t = new XJTextArea2(0,0);
        } else{
			setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        	t = new XJTextArea2(7,35);
        }
        XJAtomicField.adjustToTerm(t,gt);
        // no longer here: refreshGUI();
        t.setLineWrap(true); t.setWrapStyleWord(true);
        setViewportView(t);
        setAlignmentY(Component.TOP_ALIGNMENT);
        t.getDocument().addDocumentListener(this);
        /* Somehow not working:
		Keymap myK = JTextComponent.addKeymap("XJ",t.getKeymap());
		myK.addActionForKeyStroke(KeyStroke.getKeyStroke('a'),new AbstractAction(){
			public void actionPerformed(ActionEvent e){
				System.out.println("hello!");
			}
		});*/
		actionListeners = new ArrayList<ActionListener>();
		t.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				if (e.getKeyCode()==KeyEvent.VK_ENTER){
					e.consume();
					handleEnterKey();
				}
			}
		});
    }
    
    public void propertyChange(PropertyChangeEvent evt) {
    	Action source = (Action)evt.getSource();
    	t.setEditable(source.isEnabled());
    }
    
    protected void handleEnterKey(){
    	if (actionListeners.size()>0 && isEnabled()){
    		if (!loadFromGUI()) return;
    		SwingUtilities.invokeLater(new Runnable(){
    			public void run(){
					ActionEvent e = new ActionEvent(t,ActionEvent.ACTION_PERFORMED ,"Pressed enter");
					for (ActionListener AL : actionListeners){
						AL.actionPerformed(e);
					}
    			}
    		});
    	}
    }
    
    public void setText(String text){
		t.setText(text);
    	t.setCaretPosition(0);
    	badInputInGUI = false;
    	dirty=true;
    	loadFromGUI();
	}
    
    public String getText(){
    	return t.getText();
    }

    public JTextArea getTextArea(){
		return this.t;
    }

    class XJTextArea2 extends JTextArea{
        XJTextArea2(int rows, int cols){
            super(new XJAtomicField.FormattedDocument(),"",rows,cols);
            addFocusListener(XJTextArea.this);
        }
		protected void processMouseEvent(MouseEvent e){
			if (e.isPopupTrigger() && popupMenu.getComponentCount() > 0){
				e.consume();
				if (loadFromGUI()) {
					requestFocus();
					popupMenu.show(this,e.getX(),e.getY());
				}
			} else super.processMouseEvent(e);
		}
    }
    
    public void addEditMenuItems(JMenu menu){
    	menu.addSeparator();
		Action cut = t.getActionMap().get(DefaultEditorKit.cutAction);
		if (cut!=null)
			menu.add(cut);
		Action copy = t.getActionMap().get(DefaultEditorKit.copyAction);
		if (copy!=null){
			copy.putValue(Action.NAME, "Copy");
			menu.add(copy);
		}
		Action paste = t.getActionMap().get(DefaultEditorKit.pasteAction);
		if (paste!=null)
			menu.add(paste);
    }
    
    public void activatePopupEditMenu(){
    	ListenerWindow.popupEditMenuFor(getTextArea());
    }

    public PrologEngine getEngine(){
        return engine;
    }

    public GUITerm getGT(){
        return gt;
    }
	public void setGT(GUITerm gt){
		this.gt=gt;
		popupMenu = operationsPopup();
	}

    public void refreshGUI(){
        if (!gt.nodeIsVar()) {
            if (gt.isOpaque()) t.setText(gt.toString());
            else t.setText(gt.node.toString());
        } else t.setText("");
        t.setCaretPosition(0);
        dirty = false;
    }

    Object validatedInput(){
        return gt.coerceNodeText(t.getText());
    }

    // The following methods are identical to XJAtomicField, some might miggrate to a helper class...

    public Dimension getPreferredSize(){
        return gt.getPreferredSize(super.getPreferredSize());
    }

    public Dimension getMinimumSize(){
        return gt.getPreferredSize(super.getMinimumSize());
    }

    // DocumentListener methods
    public void insertUpdate(DocumentEvent e){dirty=true;badInputInGUI=false;}
    public void changedUpdate(DocumentEvent e){dirty=true;badInputInGUI=false;}
    public void removeUpdate(DocumentEvent e){dirty=true;badInputInGUI=false;}

    public void focusGained(FocusEvent e){
        // might change aspect
        // System.out.println("Focus gained:"+gt);
    }
    public void focusLost(FocusEvent e){
        // System.out.println("Focus lost:"+gt);
        loadFromGUI();
    }

    public boolean loadFromGUI(){
        if (badInputInGUI) return false; // already reported error
        if (!dirty) return true;
        Object x = validatedInput();
        if (x instanceof GUIError) {
            badInputInGUI=true;
            XJAtomicField.showError(x,this,gt);
            return false;
        }else {
            gt.setNodeValue(x);
            dirty = false;
            return true;
        }
    }

    public void setDefaultValue(TermModel dv){
        // use the direct variable assignment to avoid the undo treatment
        if (dv!=null) {
            if (!(dv.node instanceof String))
                throw new XJException("Text areas require a String default");
            gt.node = dv.node;
        } else gt.node="";
    }

    public void selectGUI(Object[] parts){
        GUITerm.typicalAtomicSelect(this,parts);
        t.selectAll();
        t.requestFocus();
    }

    public boolean isDirty(){
        return dirty;
    }

    JPopupMenu operationsPopup(){
        return XJAtomicField.operationsPopup(gt,engine,this);
    }


    /**
     * Adds the specified action listener to receive
     * action events from this textfield.
     *
     * @param l the action listener to be added
     */
    public synchronized void addActionListener(ActionListener l) {
        actionListeners.add(l);
        if (l instanceof Action) 
        	((Action)l).addPropertyChangeListener(this);
    }

    /**
     * Removes the specified action listener so that it no longer
     * receives action events from this textfield.
     *
     * @param l the action listener to be removed
     */
    public synchronized void removeActionListener(ActionListener l) {
        /*if ((l != null) && (getAction() == l)) {
            setAction(null);
        } else {
            listenerList.remove(ActionListener.class, l);
        }*/
        actionListeners.remove(l);
    }

    public void destroy() {
    }
    
    // end of methods similar to XJAtomicField
    
    public void setForeground(Color C){
    	super.setForeground(C);
    	if (t!=null) t.setForeground(C); 
    }
    
    public void setEnabled(boolean enabled){
    	super.setEnabled(enabled);
    	t.setEnabled(enabled);
    }
}
