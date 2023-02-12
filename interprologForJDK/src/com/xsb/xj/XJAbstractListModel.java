package com.xsb.xj;
import com.xsb.xj.util.*;
import com.declarativa.interprolog.*;

import javax.swing.table.*;
import javax.swing.event.*;

/** An abstract class for XJ list models. Regular Swing TableModel offer methods for cell updating.
This class maps those methods into term updating, and adds methods for term inserting and deleting. */
@SuppressWarnings("serial")
public abstract class XJAbstractListModel extends AbstractTableModel{
	GUITerm gt;
	final GUITerm[] cellGTs;
	final boolean[] columnIsEditable;
        protected boolean sorted = false;
        protected TermModel sortTerm = null;

	XJAbstractListModel(GUITerm gt,GUITerm[] cellGTs){
		this.cellGTs=cellGTs;
		this.gt = gt;
		columnIsEditable = new boolean[cellGTs.length];
		for (int i=0; i< columnIsEditable.length; i++){
			GUITerm g = cellGTs[i];
			// The following condition is incorrect in the situation when a cell gets  
			// two XJComponents, say because of some user class constructor doing work
			// without using makeGUI (and hence not setting GUITerm.renderer)
			// We don't care as this should be rare and the user will get some consistent
			// behavior later, such as a component cell refusing to be edited
			if (/*g.isOpaque() || */ g.isConstant() /* || g.isReadOnly() */)
				columnIsEditable[i] = false;
			else columnIsEditable[i] = true; //commented for now, but should be taken care by individual component, shouldn't it?
		}
	}
	
	public boolean isSorted(){
		return sorted;
	}
	
	public TermModel getSortTerm(){
		return this.sortTerm;
	}
	
	public abstract void setSortTerm(TermModel newSortTerm);
	
	public void checkSortProperty(){
		TermModel sortedProperty = gt.findProperty("sorted");
		if(sortedProperty != null){
			if(sortedProperty.getChildCount() == 1){
				sorted = true;
				sortTerm = ((TermModel)sortedProperty.getChild(0));
				if(!sortTerm.isList()){
					throw new XJException("Incorrect sort specification: " +
					"expecting list of form [asc(Index1), desc(Index2), ...]" +
					sortTerm);
				}
			} else if(sortedProperty.getChildCount() == 2){
				sorted = true;
				String sortDirection = (String)((TermModel)sortedProperty.getChild(0)).node;
				Integer sortIndex = (Integer)((TermModel)sortedProperty.getChild(1)).node;
				sortTerm = buildSortTerm(sortDirection, sortIndex);
			} else {
				throw new XJException("Sorted property for lists "+
				"requires at least one argument");
			}
		}
	}
	
	public TermModel buildSortTerm(String sortDirection, Integer sortIndex){
		TermModel indexModel = new TermModel(sortIndex);
		TermModel sortTermModel = new TermModel(sortDirection, new TermModel[]{indexModel});
		return new TermModel(".", new TermModel[]{sortTermModel, new TermModel("[]", true)}, true);
	}

	public int getColumnCount(){
		return cellGTs.length;
	}
	public Class<?> getColumnClass(int columnIndex){
		return TermModel.class;
	}
	public String getColumnName(int index){
		return cellGTs[index].getTitle();
	}
	/** Ignores the first argument: columns are considered editable or not irrespective
	of the tuple */
	public boolean isCellEditable(int rowIndex, int columnIndex){
            return true;
		//return columnIsEditable[columnIndex]; 
	}
	/** Returns the whole TermModel in a row; the cell editor will digest it*/
	public Object getValueAt(int row,int col){
		return getTerm(row);
	}
	/** Ignores the last argument. First argument is always the whole list row term */
	public void setValueAt(Object value,int row, int column){
		Object oldValue = getTerm(row);
                // Could check here whether the value got changed
                setTerm(row,(TermModel)value);
		XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
			this,gt,XJChangeManager.SETTERM_EDIT,row,oldValue,value);
		gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
	}

	public abstract TermModel getTerm(int row);
	
	/** This method has the responsibility to fire model changes */
	public abstract boolean setTerm(int row,TermModel value);
	
	/** Returns index of row term in the model, or -1 if not found. */
	public int indexOfTerm(TermModel term){
		for(int i=0;i<getRowCount();i++)
			if (term.variant(getTerm(i))) return i;
		return -1;
	}
	
	/** Add a new term to the end of the model */
	public void addNewTerm(TermModel newTerm){
		addTerms(new TermModel[]{newTerm});
	}
	
	/** Add terms to some place in the model by arbitrary order. 
	The terms are assumed to be compatible with the list template */
	public abstract boolean addTerms(TermModel[] terms);
	
	/** Remove terms from anywhere in the model. Remaining terms will get their indexes adjusted
	to remain contiguous. */
	public abstract boolean deleteTerms(int[] indexes);
	public abstract boolean deleteTerms(TermModel[] less);
	
	public void invalidateAndRefresh() {
		throw new XJException("This is invoke able only for some subclasses");
	}
}
