package com.xsb.xj.renderers;
import java.awt.Color;
import java.awt.event.FocusListener;

import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJAtomicField;
import com.xsb.xj.XJChangeManager;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;

/** A JTextField capable of editing a single GUITerm node...with color specified on the first child */
@SuppressWarnings("serial")
public class ColoredAtomicField extends XJAtomicField implements XJComponent, FocusListener, DocumentListener, XJComponentTree{
    
    public ColoredAtomicField(PrologEngine engine,GUITerm gt){
        super(engine,gt);
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
    
    public void refreshGUI(){
        super.refreshGUI();
        if(getColor() != null){
            this.setForeground(getColor());
        }    
    }
    
    public Color getForeground(){
        Color foreground = getColor();
        if(foreground == null){
            foreground = super.getForeground();
        }
        //System.out.println("Foreground "+foreground);
        return foreground;
    }    
}

