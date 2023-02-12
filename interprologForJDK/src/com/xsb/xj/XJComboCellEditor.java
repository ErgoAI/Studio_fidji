/*
 * XJComboCellEditor.java
 *
 * Created on January 24, 2002, 11:52 AM
 */

package com.xsb.xj;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;

/**
 *
 * @author  tanya
 * @version 
 */
@SuppressWarnings("serial")
public class XJComboCellEditor extends XJCellTupleEditor implements javax.swing.ComboBoxEditor{

    protected Component editor;
    
    /** Creates new XJComboCellEditor */
    public XJComboCellEditor(PrologEngine engine,GUITerm gt) {
        super(engine,gt);
        editor=(Component)cellComponents[0];
    }

    public java.lang.Object getCellEditorValue() { // probably not needed - was needed for lists
        Object value = gt.getTermModel();
	return value;
    }
    
    public void addActionListener(ActionListener l) {
        try{
            Method add = editor.getClass().getMethod("addActionListener",new Class[]{ActionListener.class});
            add.invoke(editor,new Object[]{l});
        } catch (Exception e){
            System.out.println("Warning: Editor class for XJComboBox elements does not have addActionListener method");
        }
    }

    public void removeActionListener(ActionListener l){
        try{
            Method remove = editor.getClass().getMethod("removeActionListener",new Class[]{ActionListener.class});
            remove.invoke(editor,new Object[]{l});
        } catch (Exception e){
            System.out.println("Warning: Editor class for XJComboBox elements does not have removeActionListener method");
        }
    }
    
    public java.lang.Object getItem() {
        if(gt!=null){
        return gt.getTermModel();
        } else {
            return null;
        }
    }
    
    public void setItem(java.lang.Object obj) {
        if(obj != null){
        setCurrentTuple((TermModel)obj);
        } else {
            // There will be a little problem with UNDO here.
            // This is the situation that is not being handled properly - 
            // when initial gt passed to XJComboBox is []
            // (i.e. no items in combo box and nothing is initially selected)
            // then if user types something in and does undo
            // to return to previous state of nothing-nothing
            // undo does not work properly.
        }
    }
    
    public void selectAll() {
    }
    
    public java.awt.Component getEditorComponent() {
        return editor;
    }
    
    public boolean stopCellEditing(){
        if(((XJComponent)cellComponents[0]).isDirty()){
            boolean ok = ((XJComponent)cellComponents[0]).loadFromGUI();
            if (ok) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
    
}
