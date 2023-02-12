package com.xsb.xj;
import javax.swing.tree.TreeCellEditor;

/** A convenience interface due to make it easier to deal uniformly with
TreeNodeEditor and PolimorphicEditor */
interface TreeNodeEditorInterface extends TreeCellEditor{
	public boolean isDirty();
	void setCurrentTuple(LazyTreeModel.LazyTreeNode node);
	GUITerm getGT();
}