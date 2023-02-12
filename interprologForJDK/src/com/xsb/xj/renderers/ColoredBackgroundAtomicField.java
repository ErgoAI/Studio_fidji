package com.xsb.xj.renderers;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJAction;
import com.xsb.xj.XJChangeManager;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;
import com.xsb.xj.util.GUIError;
import com.xsb.xj.util.XJException;

/** A JTextField capable of editing a single GUITerm node... with background color on the first GT child */
@SuppressWarnings("serial")
public class ColoredBackgroundAtomicField extends JTextField implements XJComponent, FocusListener, DocumentListener, XJComponentTree{
    GUITerm gt;
    PrologEngine engine;
    private boolean dirty;
    private boolean badInputInGUI; // if this is true, we know GUI data is incorrect
    
    public ColoredBackgroundAtomicField(PrologEngine engine, GUITerm gt){
        super(new FormattedDocument(),"",0);
        this.gt=gt;
        this.engine=engine;
        dirty = true;
        badInputInGUI=false;
        
        addFocusListener(this);
        //addActionListener(this);
        adjustToTerm(this,gt);
        // puts value into GUI:
        // no longer here: refreshGUI();
        getDocument().addDocumentListener(this);
    }
    
    protected Color getColor(){
        if(gt != null){
            if(gt.getChildCount()==2){
                return GUITerm.termToColor((TermModel)gt.getChild(0));
            }
        }
        return null;
    }
    
    protected TermModel getTextNode(){
        if(gt.getChildCount()==2){
            return (TermModel)gt.getChild(1);
        } else return null;
    }
    
    protected void setTextNode(Object v){
        TermModel oldValue = (TermModel)getTextNode().clone();
        getTextNode().setNodeValue(v);
        XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
        this,gt,XJChangeManager.ADDCHILDREN_EDIT,-1,new TermModel[]{oldValue},new TermModel[]{getTextNode()});
        gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
    }
    
    public void setChildren(TermModel[] values){
        gt.setChild(1,values[0]);
        refreshGUI();
        //            getTextNode().setNodeValue(values[0]);
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
	if(getTextNode() != null){
        if (!getTextNode().nodeIsVar()) {
            if (gt.isOpaque()) setText(getTextNode().toString());
            else setText(getTextNode().node.toString());
        } else {
            setText("");
        }} else{
	    setText("");
	}
        if(getColor() != null){
            this.setBackground(getColor());
        }

        if(gt.tipDescription().equals("")){
            setToolTipText(getText());
        }

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
        //System.out.println("Focus lost:"+gt);
        loadFromGUI();
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
            setTextNode(x);
            //if (object instanceof AtomicField) ((AtomicField)object).formatGUI();
            dirty = false;
            return true;
        }
    }
    
    Object validatedInput(){
        String s=getText();
        if (s.length()==0 && ! gt.isOptional()) return gt.message("Mandatory field");
        if (getTextNode().isVar() && s.length()==0) return getTextNode().node;
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
                /*if (dv!=null) {
                        if (!(dv.node instanceof String))
                                throw new XJException("Text fields require a String default");
                        gt.node = dv.node;
                } else gt.node=""; */
    }
    
    public boolean isDirty(){
        return dirty;
    }
    
    /** This loads data from the GUI into the GUITerm; beware this may get invoked AFTER operations, as ActionListeners
     * are messaged in arbitrary order, so do not rely on this to make operations work with the latest data in the GUI; for
     * that a closer monitoring of return/enter would be needed */
    // comment no longer applies due to a hack: XJAction  invokes loadFromGUI when its action event source is a XJComponent
        /*
        public void actionPerformed(ActionEvent e){
                if (e.getSource()!=this)
                throw new XJException("Inconsistent event source");
                loadFromGUI();
        }*/
    
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
        JOptionPane.showMessageDialog(c,x,
        "Data entry error in "+gt.getTitle(),JOptionPane.ERROR_MESSAGE);
        if (c!=null) {
            c.requestFocus();
            if (c instanceof JTextComponent) ((JTextComponent)c).selectAll();
        }
    }
    
    static void adjustToTerm(JTextComponent gui,GUITerm term){
        FormattedDocument doc = (FormattedDocument)gui.getDocument();
        doc.upperOnly = term.findProperty(GUITerm.ATOMUPPER)!=null;
        if (term.isOpaque()) gui.setEditable(false);
        if(!term.tipDescription().equals("")){
            gui.setToolTipText(term.tipDescription());
        }
        if (gui instanceof JTextField)
            ((JTextField)gui).setColumns(term.getCharWidth());
        if (term.findProperty(GUITerm.BORDERLESS)!=null) gui.setBorder(BorderFactory.createEmptyBorder());
        if (term.findProperty(GUITerm.READONLY)!=null) gui.setEditable(false);
    }
    // based on Sun's API spec example
    public static class FormattedDocument extends PlainDocument {
        public boolean upperOnly=false; // too public..
        
        public void insertString(int offs, String str, AttributeSet a)
        throws BadLocationException {
            if (str == null) return;
            if (upperOnly) {
                char[] upper = str.toCharArray();
                for (int i = 0; i < upper.length; i++) {
                    upper[i] = Character.toUpperCase(upper[i]);
                }
                super.insertString(offs, new String(upper), a);
            } else super.insertString(offs,str,a);
        }
    }
    
    /** Make the field visible and select its text */
    public void selectGUI(Object[] parts){
        GUITerm.typicalAtomicSelect(this,parts);
        selectAll();
    }
    
    public Color getBackground(){
        Color background = getColor();
        if(background == null){
            background = super.getBackground();
        }
        //System.out.println("background "+background);
        return background;
    }
    
    public void destroy() {
    }
    
}

