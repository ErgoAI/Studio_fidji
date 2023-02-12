package com.xsb.xj;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.TermModelListener;

@SuppressWarnings("serial")
abstract class XJCellTupleEditor extends AbstractCellEditor implements TermModelListener{
	GUITerm[] cellGTs;
	XJComponent topComponent;
	XJComponent[] cellComponents;
	TermModel currentTuple;
	/** template for the cell */
	GUITerm gt;
	protected int clickCountToStart = 3;
	boolean dirtyTerm;
	public static String CLICKCOUNTTOEDIT="clickCountToEdit";
	
	XJCellTupleEditor(PrologEngine engine,GUITerm gt){
		this.gt = gt;
		topComponent = gt.makeGUI(engine);
		cellComponents = gt.collectSignificantRenderers();
		cellGTs = new GUITerm[cellComponents.length];
		for(int c=0;c<cellComponents.length;c++)
			cellGTs[c] = ((XJComponent)(cellComponents[c])).getGT();
		ignoreConstantNodes();
		currentTuple = null;
		dirtyTerm=false;
		gt.addTermModelListener(this);
	}
	
	// not very pretty
	void ignoreConstantNodes(){
		int newIndex = 0; int i;
		for (i=0; i<cellGTs.length; i++){
			if (cellGTs[i].isConstant()) continue;
			cellGTs[newIndex] = cellGTs[i];
			cellComponents[newIndex] = cellComponents[i];
			newIndex++;
		}
		if (newIndex!=i) {
			// constant nodes found
			XJComponent[] newCellComponents = new XJComponent[newIndex];
			GUITerm[] newCellGTs = new GUITerm[newIndex];
			for (int c=0;c<newIndex;c++){
				newCellComponents[c] = cellComponents[c];
				newCellGTs[c] = cellGTs[c];
			}
			cellGTs = newCellGTs; cellComponents = newCellComponents;
		}
	}
	
	/** Returns true iff the current tuple has changed or some cell component in the current row is dirty */
	public boolean isDirty(){
		if (dirtyTerm) return true;
		return aComponentIsDirty();
	}
	
	boolean aComponentIsDirty(){
		for(int c=0;c<cellComponents.length;c++)
			if (cellComponents[c].isDirty()) return true;
		return false;
	}
	
	public void termChanged(TermModel source){
		dirtyTerm=true;
		//System.out.println("Term changed:"+source);
	}
	public void setCurrentTuple(TermModel tuple){
		//System.out.println("Editor-setCurrentTuple:"+tuple);
                if(tuple != null && tuple != currentTuple) {
		currentTuple = tuple;
		gt.assign(currentTuple);
		dirtyTerm=false;
		gt.refreshRenderers();
                }
	}
	
    /**
     * Specifies the number of clicks needed to start editing.
     *
     * @param count  an int specifying the number of clicks needed to start editing
     * @see #getClickCountToStart
     */
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
    
    public int getClickCountToStart(int column) {
        GUITerm g = cellGTs[column];
        TermModel clickCountProp = g.findProperty(CLICKCOUNTTOEDIT);
        if(clickCountProp != null){
            return clickCountProp.intValue();
        } else return clickCountToStart;
    }
    
	// CellEditor methods:
	public boolean isCellEditable(EventObject anEvent){
	    if (anEvent instanceof MouseEvent) { 
                Object source = anEvent.getSource();
                if (source instanceof JTable){
                    int viewColumnIndex = 
                    ((JTable)source).columnAtPoint(((MouseEvent)anEvent).getPoint());
                    int col = ((JTable)source).convertColumnIndexToModel(viewColumnIndex);
                    GUITerm g = cellGTs[col];
                    // preciously was checking for opaque and readonly
                    // removed so that editable component itself handles those properties
                    // editable cell (even readonly) allows user to select text in it
                    if (/* g.isOpaque()  || */ g.isConstant() /* || g.isReadOnly() */){
                        return false;
                    }
                    else return ((MouseEvent)anEvent).getClickCount() >= getClickCountToStart(col);
                } else {
                    return ((MouseEvent)anEvent).getClickCount() >= getClickCountToStart();
                }
	    }
	    return true;
	}
	public boolean shouldSelectCell(EventObject anEvent){
		return true;
	}

	public GUITerm getGT(){
		return gt;
	}
	
}