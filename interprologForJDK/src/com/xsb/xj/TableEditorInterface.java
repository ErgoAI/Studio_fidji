package com.xsb.xj;
import javax.swing.table.TableCellEditor;

import com.declarativa.interprolog.TermModel;

/** A convenience interface due to make it easier to deal uniformly with
TreeNodeEditor and PolimorphicEditor */
interface TableEditorInterface extends TableCellEditor{
	void setCurrentTuple(TermModel tuple);
        GUITerm[] getCellGTs();
        XJComponent[] getCellComponents();
        boolean isDirty();
        void setClickCountToStart(int count);
        int getClickCountToStart();
}