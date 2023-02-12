package com.xsb.xj;
import com.declarativa.interprolog.*;
import com.xsb.xj.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
/** A simple dispatcher of TreeCellEditor messages to its bunch of TreeNodeEditors, 
one for each tree template.
The same approach should work with lists, if polimorphism was desired for these */
class PolimorphicEditor implements TreeNodeEditorInterface{
	Hashtable<String,TreeNodeEditor> editors;
	TreeNodeEditor currentEditor;
       	protected int clickCountToStart = 3;

	PolimorphicEditor(PrologEngine engine,GUITerm[] templates){
		editors = new Hashtable<String,TreeNodeEditor>(templates.length);
		for(int t=0;t<templates.length;t++){
			String key = templates[t].findProperty(GUITerm.TYPENAME).toString();
			if (key==null) throw new XJException("typename property missing in "+templates[t]);
			editors.put(key,new TreeNodeEditor(engine,templates[t]));
		}
		currentEditor = null;
	}
	public Component getTreeCellEditorComponent(JTree tree, Object value, 
		boolean isSelected, boolean expanded, boolean leaf, int row){
		LazyTreeModel.LazyTreeNode treenode = (LazyTreeModel.LazyTreeNode)value;
		currentEditor = editors.get(treenode.getType());
		return currentEditor.getTreeCellEditorComponent(tree,value,isSelected,expanded,leaf,row);
	}
	public Object getCellEditorValue(){
		if (currentEditor==null) return null;
		else return currentEditor.getCellEditorValue();
	}
	public boolean isCellEditable(EventObject anEvent){
            if (anEvent instanceof MouseEvent) { 
                if(((MouseEvent)anEvent).getClickCount() >= clickCountToStart){
                    if (currentEditor==null) {
                        return true;
                    }
                    // without this JRE 1.4 crashes; but as a result now XJCellTupleEditor.setClickCountToStart is uneffective (in 1.4)
                    return currentEditor.isCellEditable(anEvent);
                } else {
                    return false;
                }
	    }
            return true;
	}
	public boolean shouldSelectCell(EventObject anEvent){
		return currentEditor.shouldSelectCell(anEvent);
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
		for (Enumeration<TreeNodeEditor> e = editors.elements() ; e.hasMoreElements() ;)
			((TreeNodeEditor)e.nextElement()).addCellEditorListener(l);
	}
	public void removeCellEditorListener(CellEditorListener l){
		for (Enumeration<TreeNodeEditor> e = editors.elements() ; e.hasMoreElements() ;)
			((TreeNodeEditor)e.nextElement()).removeCellEditorListener(l);
	}
	public boolean isDirty(){
		return currentEditor.isDirty();
	}
	public void setCurrentTuple(LazyTreeModel.LazyTreeNode node){
            //if(currentEditor == null){
		currentEditor = (TreeNodeEditor)editors.get(node.getType());
            //}
            currentEditor.setCurrentTuple(node);
	}
	public GUITerm getGT(){
            if(currentEditor != null){
		return currentEditor.getGT();
            } else return null;
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

}