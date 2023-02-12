package com.xsb.xj;
import java.awt.Component;

import javax.swing.JTable;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;

@SuppressWarnings("serial")
class ListCellEditor extends XJCellTupleEditor implements TableEditorInterface{
	int editingColumn; // -1 for none
	
	ListCellEditor(PrologEngine engine,GUITerm gt){
		super(engine,gt);
		editingColumn = -1;
	}

	// not using isSelected, hasFocus - is it really necessary to do something with them?
	public Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int column) {
        editingColumn  = table.convertColumnIndexToModel(column);
		Component component = (Component)cellComponents[editingColumn];
    	XJComboBox.setFireEvents(component,false);
		setCurrentTuple((TermModel)value);
    	XJComboBox.setFireEvents(component,true);
		return component;
	}

	public void cancelCellEditing(){
		//XJComboBox.setFireEvents(cellComponents[editingColumn],false);
		super.cancelCellEditing();
		editingColumn=-1;
	}

	/** This implementation must not mind being called twice, cf. XJTableView.editCellAt */
	public boolean stopCellEditing(){
		if (editingColumn==-1) return true; // already stopped
		//XJComboBox.setFireEvents(cellComponents[editingColumn],false);
		if (cellGTs[editingColumn].isReadOnly()) return true; // discard whatever edit was done
		boolean ok = ((XJComponent)cellComponents[editingColumn]).loadFromGUI();
		if (ok && super.stopCellEditing()) {
			editingColumn=-1;
			return true;
		} else return false;
	}

	/** Always returns the full row tuple */
	public Object getCellEditorValue(){
		Object value = gt.getTermModel();
		return value;
	}
		
        public GUITerm[] getCellGTs() {
            return cellGTs;
        }
        
        public XJComponent[] getCellComponents() {
            return cellComponents;
        }
        
}