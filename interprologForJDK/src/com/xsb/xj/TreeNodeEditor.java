package com.xsb.xj;
import java.awt.Component;

import javax.swing.JTree;

import com.declarativa.interprolog.PrologEngine;
import com.xsb.xj.util.XJException;

@SuppressWarnings("serial")
class TreeNodeEditor extends XJCellTupleEditor implements TreeNodeEditorInterface{
	LazyTreeModel.LazyTreeNode currentNode;
	
	TreeNodeEditor(PrologEngine engine,GUITerm gt){
		super(engine,gt);
		currentNode=null;
	}
	public void setCurrentTuple(LazyTreeModel.LazyTreeNode node){
		currentNode = node;
		setCurrentTuple(node.getNodeTerm());
	}
	public Component getTreeCellEditorComponent(JTree tree, Object value, 
		boolean isSelected, boolean expanded, boolean leaf, int row){
		if (((LazyTreeModel.LazyTreeNode)value).isRepeated())
			throw new XJException("Should be unable to edit a repeated node!");
		setCurrentTuple((LazyTreeModel.LazyTreeNode)value);
		//System.out.println("entering getTreeCellEditorComponent for "+value);
		return (Component)topComponent;
	}
	public boolean stopCellEditing(){
		// System.out.println("entering stopCellEditing for "+currentNode);
		boolean ok = true;
		for (int c=0;c<cellComponents.length;c++){
			if (cellComponents[c].loadFromGUI()) continue;
			else{
				ok=false; break;
			}
		}
		if (ok && super.stopCellEditing()) {
			return true;
		} else return false;
	}
	/** Always returns the full row tuple */
	public Object getCellEditorValue(){
		// should we return a NEW LazyTreeNode instead ??? cache issues
		
		currentNode.setNodeTerm(gt.getTermModel());
		return currentNode;
	}		
}
 