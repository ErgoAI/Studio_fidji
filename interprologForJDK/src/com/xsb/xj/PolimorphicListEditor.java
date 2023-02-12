package com.xsb.xj;
import com.declarativa.interprolog.*;
import com.xsb.xj.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
/** A simple dispatcher of TableCellEditor messages to its bunch of ListCellEditors, 
one for each list template.
The same approach should work with lists, if polimorphism was desired for these */
class PolimorphicListEditor implements TableEditorInterface{
        Hashtable<String,ListCellEditor> editors;
	ListCellEditor currentEditor;
       	protected int clickCountToStart = 3;
        GUITerm[]  cellGTs;
        XJComponent[] cellComponents;

	PolimorphicListEditor(PrologEngine engine, GUITerm[] templates){
		editors = new Hashtable<String,ListCellEditor>(templates.length);
                if(templates.length>0){
                    String key = templates[0].findProperty(GUITerm.TYPENAME).toString();
                    if (key==null) throw new XJException("typename property missing in "+templates[0]);
                    ListCellEditor firstEditor = new ListCellEditor(engine,templates[0]);
                    editors.put(key,firstEditor);
                    cellGTs=firstEditor.cellGTs;
                    cellComponents=firstEditor.cellComponents;
                        
                    for(int t=1;t<templates.length;t++){
                        key = templates[t].findProperty(GUITerm.TYPENAME).toString();
                        if (key==null) throw new XJException("typename property missing in "+templates[t]);
                        editors.put(key,new ListCellEditor(engine,templates[t]));
                    }
                } else {
                    throw new XJException("required at least one list template");
                }
		currentEditor = null;
	}
	public Component getTableCellEditorComponent(JTable table,Object value,
		boolean isSelected,int row,int column){
		currentEditor = (ListCellEditor)editors.get(((TermModel)value).getChild(0).toString());
		return currentEditor.getTableCellEditorComponent(table,value,isSelected,row,column);
	}
        
	public Object getCellEditorValue(){
		if (currentEditor==null) return null;
		else return currentEditor.getCellEditorValue();
	}
        
	public boolean isCellEditable(EventObject anEvent){
            if (anEvent instanceof MouseEvent) { 
                    if (currentEditor==null) {
                        if(((MouseEvent)anEvent).getClickCount() >= clickCountToStart){
                            return true;
                        } else {
                            return false;
                        }
                    }
                    else return currentEditor.isCellEditable(anEvent);
  	    }
            return true;
	}
	public boolean shouldSelectCell(EventObject anEvent){
            if(currentEditor == null){
                return true;
            } else {
		return currentEditor.shouldSelectCell(anEvent);
            }
	}
	public boolean stopCellEditing(){
		// Following attemps to prevent a NullPointerException being throw upon massive triple-clicking
		boolean stopped = currentEditor==null || currentEditor.stopCellEditing();
		if (stopped) currentEditor = null; // consistency enforcing..
		return stopped;
	}
	public void cancelCellEditing(){
		if (currentEditor!=null) currentEditor.cancelCellEditing();
		currentEditor=null;
	}
	public void addCellEditorListener(CellEditorListener l){
		for (Enumeration<ListCellEditor> e = editors.elements() ; e.hasMoreElements() ;)
			((ListCellEditor)e.nextElement()).addCellEditorListener(l);
	}
	public void removeCellEditorListener(CellEditorListener l){
		for (Enumeration<ListCellEditor> e = editors.elements() ; e.hasMoreElements() ;)
			((ListCellEditor)e.nextElement()).removeCellEditorListener(l);
	}
	public boolean isDirty(){
            if(currentEditor == null){
                // editing was stopped before
                return false;
            } else {
		return currentEditor.isDirty();
            }
	}
        
	public void setCurrentTuple(TermModel tuple){
            //if(currentEditor == null){
		currentEditor = (ListCellEditor)editors.get(tuple.getChild(0).toString());
            //}
            currentEditor.setCurrentTuple(tuple);
	}
	public GUITerm getGT(){
            if(currentEditor != null){
		return currentEditor.getGT();
            } else return null;
	}
        
    public GUITerm[] getCellGTs(){
    	if(currentEditor != null){
			return currentEditor.getCellGTs();
        } else return cellGTs; // I would return null as with trees but I need some GTs in XJTable constructor
    }
        
    public void setClickCountToStart(int count) {
		clickCountToStart = count;
    }

    /**
     *  ClickCountToStart controls the number of clicks required to start
     *  editing.
     */
    public int getClickCountToStart() {
		return clickCountToStart;
    }

    public XJComponent[] getCellComponents() {
        if(currentEditor != null){
            return currentEditor.getCellComponents();
        } else return cellComponents;
    }
    
}