package com.xsb.xj.util;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;

import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJAction;
import com.xsb.xj.XJAtomicField;
import com.xsb.xj.XJComponent;

/** A JTextField capable of editing a single GUITerm node */
@SuppressWarnings("serial")
public class XJPasswordField extends JPasswordField implements XJComponent, FocusListener, DocumentListener{
    GUITerm gt;
    PrologEngine engine;
    private boolean dirty;
    private boolean badInputInGUI; // if this is true, we know GUI data is incorrect
    
    private static final String FOCUS_LISTENER = "focuslistener";
    private boolean focusChangeListener;
    
    public XJPasswordField(PrologEngine engine, GUITerm gt){
        super();
        this.gt=gt;
        this.engine=engine;
        dirty = true;
        badInputInGUI=false;
        
        addFocusListener(this);
        //addActionListener(this);
        XJAtomicField.adjustToTerm(this,gt);
        // puts value into GUI:
        // no longer here: refreshGUI();
        getDocument().addDocumentListener(this);
        if (gt.findProperty(GUITerm.DISABLED)!=null) this.setEnabled(false);
        if (gt.findProperty(FOCUS_LISTENER)!=null) {this.focusChangeListener=true;} else {this.focusChangeListener=false;}
    }
    
    public PrologEngine getEngine(){
        return engine;
    }
    
    public GUITerm getGT(){
        return gt;
    }
	public void setGT(GUITerm gt){
		this.gt=gt;
	}
    
    public void refreshGUI(){
        if (!gt.nodeIsVar()) {
            if (gt.isOpaque()) setText(gt.toString());
            else setText(gt.node.toString());
        } else setText("");
        dirty = false;
    }
    
    public Dimension getPreferredSize(){
        return gt.getPreferredSize(super.getPreferredSize());
    }
    
    // DocumentListener methods
    public void insertUpdate(DocumentEvent e){dirty=true; badInputInGUI=false;}
    public void changedUpdate(DocumentEvent e){dirty=true; badInputInGUI=false;}
    public void removeUpdate(DocumentEvent e){dirty=true; badInputInGUI=false;}
    
    // FocusListener methods
    public void focusGained(FocusEvent e){
        // might change aspect
        //System.out.println("Focus gained:"+gt);
    }
    public void focusLost(FocusEvent e){
        Object oldValue = gt.node;
        loadFromGUI();
        if(gt.node != null){
            if(this.focusChangeListener && !gt.node.equals(oldValue)){
                fireActionPerformed();
            }
        }
    }
    
    public boolean loadFromGUI(){
        if (badInputInGUI) return false; // already reported error
        if (!dirty) return true;
        Object x = validatedInput();
        //System.out.println("validatedInput=="+x);
        if (x instanceof GUIError) {
            badInputInGUI=true;
            showError(x,this,gt);
            return false;
        }else {
            gt.setNodeValue(x);
            //if (object instanceof AtomicField) ((AtomicField)object).formatGUI();
            dirty = false;
            return true;
        }
    }
    
    Object validatedInput(){
        //return gt.coerceNodeText(getPassword());
        String s = new String(getPassword());
        if (gt.isVar() && s.length()==0) {return gt.node;}
        // vars with empty input remain vars; perhaps maxSize==0...
        else {
            TermModel minSize = gt.findProperty(GUITerm.MINSIZE);
            if (minSize!=null && s.length()<minSize.intValue())
                return gt.message("A minimum of "+minSize.intValue()+" characteres is needed");
            TermModel maxSize = gt.findProperty(GUITerm.MAXSIZE);
            if (maxSize!=null && s.length()>maxSize.intValue())
                return gt.message("No more than "+maxSize.intValue()+" characteres are admissible");
            return s;
        } 
    }
    
    public void setDefaultValue(TermModel dv){
        // use the hacked assignment to avoid the undo treatment
        if (dv!=null) {
            if (!(dv.node instanceof String))
                throw new XJException("Text fields require a String default");
            gt.node = dv.node;
        } else gt.node="";
    }
    
    public boolean isDirty(){
        return dirty;
    }
    
    /** Make the field visible and select its text */
    public void selectGUI(Object[] parts){
        GUITerm.typicalAtomicSelect(this,parts);
        selectAll();
    }
    
    protected void processMouseEvent(MouseEvent e){
        if (e.isPopupTrigger()){
            e.consume();
            if (loadFromGUI()) {
                requestFocus();
                operationsPopup().show(this,e.getX(),e.getY());
            }
        } else super.processMouseEvent(e);
    }
    
    JPopupMenu operationsPopup(){
        return operationsPopup(gt,engine,this);
    }
    
    static JPopupMenu operationsPopup(GUITerm term,PrologEngine engine,Component parent){
        return operationsPopup(term,engine,parent,null);
    }
    
    static JPopupMenu operationsPopup(GUITerm term,PrologEngine engine,Component parent,Runnable rememberFunctionResults){
        JPopupMenu popup = new JPopupMenu("Operations");
        XJAction[] actions=term.operations(engine,parent,rememberFunctionResults);
        for (int a=0;a<actions.length;a++)
            if (actions[a].isMenuOperation()) popup.add(actions[a].buildMenu());
        return popup;
    }
    
    static void showError(Object x,Component c,GUITerm gt){
        if (!(x instanceof GUIError))
            throw new XJException("bad use of showError:"+x);
        Toolkit.getDefaultToolkit().beep();
        System.out.println("Data entry error in "+gt.getTitle());
        //JOptionPane.showMessageDialog(c,x,
        //"Data entry error in "+gt.getTitle(),JOptionPane.ERROR_MESSAGE);
        if (c!=null) {
            c.requestFocus();
            if (c instanceof JTextComponent) ((JTextComponent)c).selectAll();
        }
    }
    
    public void destroy() {
    }
    
}
