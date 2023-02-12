package com.xsb.xj.renderers;

import com.xsb.xj.util.BevelArrowIcon;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Renderer for header button. Taken from
 * http://www2.gol.com/users/tame/swing/examples/JTableExamples5.html Only
 * allows for sorting by one column. Keeps a pointer to the column in
 * _selectedColumn and its current state in _selectedColumnState.
 *
 *@author    Tatyana Vidrevich
 *@author    Harpreet Singh
 *@version   $Id: SortButtonRenderer.java,v 1.5 2004/03/22 14:30:00 hsingh Exp $
 */
@SuppressWarnings("serial")
public class SortButtonRenderer extends JLabel implements TableCellRenderer {
    /**
     * Unkown column state
     */
    public final static int NONE  = 0;

    /**
     * Descending
     */
    public final static int DOWN  = 1;

    /**
     * Ascending
     */
    public final static int UP    = 2;

    JLabel _upLabel;
    JLabel _downLabel;

    TableColumn _selectedColumn;
    int _selectedColumnState      = NONE;

    public SortButtonRenderer() {
        super();
        setHorizontalTextPosition(LEFT);
        setHorizontalAlignment(JLabel.CENTER);

        _downLabel = new JLabel();
        _downLabel.setHorizontalTextPosition(LEFT);
        _downLabel.setHorizontalAlignment(JLabel.CENTER);
        _downLabel.setIcon(new BevelArrowIcon(BevelArrowIcon.DOWN, false, false));

        _upLabel = new JLabel();
        _upLabel.setHorizontalTextPosition(LEFT);
        _upLabel.setHorizontalAlignment(JLabel.CENTER);
        _upLabel.setIcon(new BevelArrowIcon(BevelArrowIcon.UP, false, false));
    }

    /**
     * Reflects the state of the _selectedColumn.
     *
     *@param table       parent table
     *@param value       table column ?
     *@param isSelected  true if selected
     *@param hasFocus    true if has focus
     *@param row         row number
     *@param column      column number in the view
     *@return            The tableCellRendererComponent value
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label             = this;
        TableColumn tableColumn  = table.getColumnModel().getColumn(column);
        int currentState         = getState(tableColumn);

        if(currentState == DOWN) {
            label = _downLabel;
        } else if(currentState == UP) {
            label = _upLabel;
        }

        if(table != null) {
            javax.swing.table.JTableHeader header  = table.getTableHeader();
            if(header != null) {
                label.setForeground(header.getForeground());
                label.setBackground(header.getBackground());
                label.setFont(header.getFont());
            }
        }

        label.setText(value == null ? "" : value.toString());
        label.setBorder(javax.swing.UIManager.getBorder("TableHeader.cellBorder"));

        return label;
    }

    /**
     *@param column  The column on which the table is being sorted.
     */
    public void setSelectedColumn(TableColumn column) {
        int nextState  = getNextState(column);

        _selectedColumn = column;
        _selectedColumnState = nextState;
    }

    /**
     * If column and _selectedColumn are equal returns _selectedColumnState,
     * otherwise returns NONE.
     *
     *@param column  column whose state is requested
     *@return        the current state of the column.
     */
    public int getState(TableColumn column) {
        int columnState  = NONE;
        if(column == _selectedColumn) {
            columnState = _selectedColumnState;
        }

        return columnState;
    }

    /**
     * If the state of the column is NONE or DOWN returns UP, and if the state
     * is UP returns DOWN.
     *
     *@param column  column whose next state is requested
     *@return        The nextState value
     */
    public int getNextState(TableColumn column) {
        int currentState  = getState(column);
        int nextState     = NONE;
        if(currentState == NONE || currentState == DOWN) {
            nextState = UP;
        } else if(currentState == UP) {
            nextState = DOWN;
        }

        return nextState;
    }
}
