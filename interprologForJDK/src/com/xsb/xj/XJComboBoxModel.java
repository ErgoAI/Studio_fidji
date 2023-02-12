/*
 * XJComboBoxModel.java
 *
 * Created on January 23, 2002, 12:05 PM
 */

package com.xsb.xj;

import java.io.Serializable;

import javax.swing.MutableComboBoxModel;
import javax.swing.event.*;

import javax.swing.*;

import java.util.Vector;

import com.declarativa.interprolog.*;
/**
 *
 * @author  tanya
 * @version
 */
@SuppressWarnings("serial")
public class XJComboBoxModel extends XJAbstractListModel implements ComboBoxModel<Object>, MutableComboBoxModel<Object>, Serializable {

    protected EventListenerList listenerList = new EventListenerList();

    /** Creates new XJComboBoxModel */
    public XJComboBoxModel(GUITerm gt,GUITerm[] cellGTs) {
        super(gt,cellGTs);
    }


    private TermModel[] getTermList(){
        if(gt.getChildCount()==2){
            return TermModel.flatList((TermModel)gt.getChild(1));
        } else {
            return (new TermModel[0]);
        }
    }

    private TermModel getSelectedTerm(){
        if(gt.getChildCount()!=0){
            return (TermModel)gt.getChild(0);
        } else {
            return null;
        }
    }


    /** Add terms to some place in the model by arbitrary order.
     * The terms are assumed to be compatible with the list template  */
    public boolean addTerms(TermModel[] terms) {
//        System.out.println("Adding terms "+terms);
//        for(int i=0; i<terms.length; i++){
//            System.out.println(terms[i]);
//        }
        int oldCount = getRowCount();
        TermModel[] oldChildren = gt.getChildren();
        if(gt.getChildCount()==2){
            TermModel [] oldElements = getTermList();
            int newSize = terms.length+oldElements.length;
            TermModel [] newElements = new TermModel [newSize];
            for (int i=0; i<oldElements.length; i++){
                newElements[i] = oldElements[i];
            }
            for(int i=oldElements.length; i<newSize; i++){
                newElements[i] = terms[i-oldElements.length];
            }
            gt. setChild(1,TermModel.makeList(newElements));
            int newCount = getRowCount();
            if (newCount>oldCount) {
                XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
                this,gt,XJChangeManager.ADDCHILDREN_EDIT,-1,oldChildren,gt.getChildren());
                gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
                fireContentsChanged(this, 0, getSize());
            }
        }
        return true;
    }

    public boolean deleteTerms(int[] indexes) {
        TermModel[] oldChildren = gt.getChildren();
        if(oldChildren.length == 2){
            int oldSize = getTermList().length;

            //instead of ((TermModel)gt.getChild(1)).deleteChildren(indexes);
            if (indexes.length > 0) {
				TermModel [] oldElements = getTermList();
				TermModel[] newElements = new TermModel[oldElements.length-indexes.length];
				int oldIndex=0;
				for (int newIndex=0;newIndex<newElements.length;newIndex++){
					while (inArray(oldIndex,indexes)) oldIndex++;
					newElements[newIndex] = oldElements[oldIndex];
					oldIndex++;
				}
				gt. setChild(1,TermModel.makeList(newElements));
            }
            //

            int newSize = getTermList().length;
            if (oldSize != newSize){
                XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
                this,gt,XJChangeManager.DELETECHILDREN_EDIT,-1,oldChildren,gt.getChildren());
                gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
                fireContentsChanged(this, 0, newSize); // or oldsize?
            }
        }
        return true;
    }

    public boolean deleteTerms(TermModel[] less) {
        TermModel[] oldChildren = gt.getChildren();

        if(oldChildren.length == 2){
            int oldSize = getTermList().length;

            // instead of ((TermModel)gt.getChild(1)).deleteChildren(less);
            if (less.length > 0) {
				Vector<TermModel> newTemp = new Vector<TermModel>();
				TermModel [] oldElements = getTermList();

				for (int oldIndex=0;oldIndex<oldElements.length;oldIndex++){
					if(inArray(oldElements[oldIndex],less)) continue;
					newTemp.addElement(oldElements[oldIndex]);
				}

				TermModel[] newElements = new TermModel[newTemp.size()];
				for(int i=0;i<newElements.length;i++){
					newElements[i]=newTemp.elementAt(i);
				}
				gt. setChild(1,TermModel.makeList(newElements));
            }
            //
            int newSize = getTermList().length;
            if (oldSize != newSize){
                XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
                this,gt,XJChangeManager.DELETECHILDREN_EDIT,-1,oldChildren,gt.getChildren());
                gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
                fireContentsChanged(this, 0, newSize);
            }
        }
        return true;
    }

    public TermModel getTerm(int row) {
        return getTermList()[row];
    }

    public int getRowCount() {
       return getTermList().length;
    }

    public boolean setTerm(int row, TermModel value) {
        setSelectedItem(value);
        return true;
    }

    public java.lang.Object getSelectedItem() {
        return getSelectedTerm();
    }

    public void addListDataListener(javax.swing.event.ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }

    public void setSelectedItem(java.lang.Object anObject) {
        //System.out.println("Set selected item "+anObject);
        TermModel selectedT=getSelectedTerm();
        if ( (selectedT != null) ||
             (selectedT == null && anObject != null) ) {
                 if(!(gt.getChildCount()==0)){
                     TermModel prevValue = (TermModel)gt.getChild(0);
                     gt.setChild(0,(TermModel)anObject);
                     fireContentsChanged(this, 0, this.getSize());
                     XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
                this,gt,XJChangeManager.SETTERM_EDIT,0,prevValue,anObject);
                     gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
                 } else {
                     gt.setChildren(new TermModel[]{(TermModel)anObject});
                     fireContentsChanged(this, 0, this.getSize());
                     XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
                this,gt,XJChangeManager.ADDCHILDREN_EDIT,0,null,new TermModel[]{(TermModel)anObject});
                     gt.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
                 }

        }
    }

    public void setChildren(TermModel[] terms){
    	gt.setChildren(terms);
        fireContentsChanged(this, 0, this.getSize());
    }


    public void removeListDataListener(javax.swing.event.ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }

    protected void fireContentsChanged(Object source, int index0, int index1)
    {
    Object[] listeners = listenerList.getListenerList();
    ListDataEvent e = null;

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
        if (listeners[i] == ListDataListener.class) {
        if (e == null) {
            e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, index0, index1);
        }
        ((ListDataListener)listeners[i+1]).contentsChanged(e);
        }
    }
    }

    protected void fireIntervalAdded(Object source, int index0, int index1)
    {
    Object[] listeners = listenerList.getListenerList();
    ListDataEvent e = null;

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
        if (listeners[i] == ListDataListener.class) {
        if (e == null) {
            e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, index0, index1);
        }
        ((ListDataListener)listeners[i+1]).intervalAdded(e);
        }
    }
    }
    protected void fireIntervalRemoved(Object source, int index0, int index1)
    {
    Object[] listeners = listenerList.getListenerList();
    ListDataEvent e = null;

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
        if (listeners[i] == ListDataListener.class) {
        if (e == null) {
            e = new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED, index0, index1);
        }
        ((ListDataListener)listeners[i+1]).intervalRemoved(e);
        }
    }
    }
/*
    public EventListener[] getListeners(Class listenerType) {
    return listenerList.getListeners(listenerType);
    }
*/
    public int getSize() {
        return this.getRowCount();
    }

    public java.lang.Object getElementAt(int param) {
        return getTerm(param);
    }

    public void removeElement(java.lang.Object obj) {
    }

    public void insertElementAt(java.lang.Object obj, int param) {
    }

    public void addElement(java.lang.Object obj) {
    }

    public void removeElementAt(int param) {
    }

    static boolean inArray(int x,int[] a){
        for(int i=0;i<a.length;i++)
            if (x==a[i]) return true;
        return false;
    }

    static boolean inArray(Object x,Object[] a){
        for(int i=0;i<a.length;i++)
            if (x.equals(a[i])) return true;
        return false;
    }

    public void setSortTerm(TermModel newSortTerm) {
    }
    
}
