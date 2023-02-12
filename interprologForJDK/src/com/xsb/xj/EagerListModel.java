package com.xsb.xj;
import java.util.Arrays;

import javax.swing.event.UndoableEditEvent;

import com.declarativa.interprolog.TermModel;

/** Model for XJ eager lists, whose elements are contained in a Prolog list term */
@SuppressWarnings("serial")
public class EagerListModel extends XJAbstractListModel{
	EagerListModel(GUITerm gt,GUITerm[] cellGTs){
		super(gt,cellGTs);
	}
	
	public int getRowCount(){
		return gt.getChildCount();
	}
	public TermModel getTerm(int row){
		return gt.children[row];
	}
	
	public boolean setTerm(int row,TermModel value){
		gt.setChild(row,value);
		fireTableRowsUpdated(row,row);
		return true;
	}
	
	/** Terms are added in order to the end of the list model */
	public boolean addTerms(TermModel[] terms){
		insertTerms(terms, getRowCount());
		return true;
	}
      
        /** Terms are added in order at position */
	public void insertTerms(TermModel[] terms, int position){
            int oldCount = getRowCount();
            if(position > oldCount){ 
                throw new java.lang.IndexOutOfBoundsException(); 
            }
            if(terms != null){
                int newCount = oldCount + terms.length;
                if (newCount > oldCount) {
                    TermModel[] oldChildren = gt.getChildren();
                    if(oldChildren == null){
                        gt.addChildren(terms);
                    } else {
                    TermModel[] newChildren = new TermModel[newCount];
                    System.arraycopy(oldChildren, 0, newChildren, 0, position);
                    System.arraycopy(terms, 0, newChildren, position, terms.length);
                    System.arraycopy(oldChildren, position, 
                                 newChildren, position + terms.length, oldCount - position);
                    gt.setChildren(newChildren);
                    }
                
                    XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
                    this,gt,XJChangeManager.ADDCHILDREN_EDIT,-1,oldChildren,gt.getChildren());
                    gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
                    fireTableRowsInserted(position,position + terms.length - 1);
                }
            }
	}
	
	public boolean deleteTerms(int[] indexes){
		TermModel[] oldChildren = gt.getChildren();
		gt.deleteChildren(indexes);
		TermModel[] newChildren = gt.getChildren();
		if (oldChildren != newChildren){
			XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
				this,gt,XJChangeManager.DELETECHILDREN_EDIT,-1,oldChildren,newChildren);
			gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
			Arrays.sort(indexes);
			fireTableRowsDeleted(indexes[0],indexes[indexes.length-1]);
		}
		return true;
	}
	
	public boolean deleteTerms(TermModel[] terms){
		TermModel[] oldChildren = gt.getChildren();
		gt.deleteChildren(terms);
		TermModel[] newChildren = gt.getChildren();
		if (oldChildren != newChildren){
			XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
				this,gt,XJChangeManager.DELETECHILDREN_EDIT,-1,oldChildren,newChildren);
			gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
			fireTableDataChanged();
		}
		return true;
	}
	
	/** Used to implement undo/redo */
	public void setChildren(TermModel[] terms){
		gt.setChildren(terms);
		// a bit abrupt, this may get refined later as we diversify each model's edition methods:
		fireTableDataChanged();
	}
	
	public void setSortTerm(TermModel newSortTerm) {
	}
	
	/** 
	 * A shortcut to get all elements in the list
	 * from Prolog without doing component.getGT()
	 * and then gt.getTermModel(). Since gt is a 
	 * subclass of TermModel it would get serialized
	 * during component.getGT() call instead of being
	 * passed as a reference and passing it back and forth 
	 * would take a long time.
	 */
	public TermModel[] getAllTerms(){
		return gt.getTermModel().flatList();
	}
}