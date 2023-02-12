package com.xsb.xj.util;

import com.xsb.xj.LazyListModel;
import com.xsb.xj.XJTableColumnModel;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

/**
 * Manages tooltips for table header and listens for double clicks on column
 * divides for resizing of column width. Also adds a popup menu on the columns
 * which allows the user to hide and show columns in the table.
 *
 *@author    Harpreet Singh
 *@version   $Id: XJTableHeader.java,v 1.4 2004/03/22 14:28:05 hsingh Exp $
 */
@SuppressWarnings("serial")
public class XJTableHeader extends JTableHeader implements ActionListener, MouseListener, MouseMotionListener {
    private static final int DEFAULT_COLUMN_PADDING  = 5;

    private JTable _table;
    private XJTableColumnModel _tcm;
    private int _lastColumn;
    private JPopupMenu _columnPopup;

    public XJTableHeader(JTable table, XJTableColumnModel tcm) {
        super(tcm);

        _table = table;
        _tcm = tcm;
        createColumnPopup();

        setReorderingAllowed(true);
        addMouseMotionListener(this);
        addMouseListener(this);
    }

    public void mouseClicked(MouseEvent me) {
        int cursorType  = getCursor().getType();

        if((cursorType == Cursor.E_RESIZE_CURSOR ||
            cursorType == Cursor.W_RESIZE_CURSOR) &&
            me.getClickCount() == 2) {
            resizeColumn(columnAtPoint(me.getPoint()));
        }
    }

    public void mouseEntered(MouseEvent me) { }

    public void mouseExited(MouseEvent me) { }

    public void mousePressed(MouseEvent me) {
        showPopupMenu(me);
    }

    public void mouseReleased(MouseEvent me) {
        showPopupMenu(me);
    }

    public void mouseMoved(MouseEvent me) {
        int columnNumber  = _tcm.getColumnIndexAtX(me.getX());

        // Save repeated calls to setToolTipText by remember last column number
        if(columnNumber >= 0 && columnNumber != _lastColumn) {
            setToolTipText(_tcm.getColumn(columnNumber).getHeaderValue().toString());
            _lastColumn = columnNumber;
        }
    }

    public void mouseDragged(MouseEvent me) { }


    private void resizeColumn(int columnNumber) {
        TableColumn column  = _tcm.getColumn(columnNumber);
        column.setPreferredWidth(getMaxColumnWidth(columnNumber, column));
    }

    private int getMaxColumnWidth(int columnNum, TableColumn column) {
        TableCellRenderer tableCellRenderer;
        TableCellRenderer headerRenderer     = column.getHeaderRenderer();
        Component comp                       = null;
        int maxWidth                         = 0;
        int cellWidth                        = 0;
        int rowCount                         = 0;

        Font font;
        FontMetrics fontMetrics;

        if(headerRenderer != null) {
            comp = headerRenderer.getTableCellRendererComponent(_table, column.getHeaderValue(), false, false, 0, columnNum);
        } else {
            comp = this.getDefaultRenderer().getTableCellRendererComponent(_table, column.getHeaderValue(), false, false, 0, columnNum);
        }

        maxWidth = getHeaderWidth(comp, column.getHeaderValue());

        if(_table.getModel() instanceof LazyListModel) {
            Map<?, ?> rowTermsCache  = ((LazyListModel) _table.getModel()).getRowCache();
            if(rowTermsCache == null || rowTermsCache.isEmpty()) {
                rowCount = 0;
                //System.out.println("row cache is empty");
            } else {
                Set<?> keySet  = rowTermsCache.keySet();
                for(Iterator<?> it = keySet.iterator(); it.hasNext(); ) {
                    Integer mapKey      = (Integer) it.next();
                    Vector<?> cacheVector  = (Vector<?>) rowTermsCache.get(mapKey);
                    rowCount += cacheVector.size();
                }
            }
            //System.out.println("lazy list cache size:" + rowCount);
        } else {
            //eager list
            rowCount = _table.getRowCount();
            //System.out.println("eager list cache size:" + rowCount);
        }

        for(int i = 0; i < rowCount; i++) {
            tableCellRenderer = _table.getCellRenderer(i, columnNum);

            comp = tableCellRenderer.getTableCellRendererComponent(_table, _table.getValueAt(i, columnNum), false, false, i, columnNum);

            font = comp.getFont();
            fontMetrics = comp.getFontMetrics(font);

            if(comp instanceof JTextComponent) {
                cellWidth = SwingUtilities.computeStringWidth(fontMetrics,
                    ((JTextComponent) comp).getText());
            } else {
                cellWidth = comp.getPreferredSize().width;
            }

            maxWidth = Math.max(maxWidth, cellWidth);
        }

        return (maxWidth + DEFAULT_COLUMN_PADDING);
    }

    private int getHeaderWidth(Component comp, Object headerValue) {
        Font font                = comp.getFont();
        FontMetrics fontMetrics  = comp.getFontMetrics(font);
        int width                = 0;

        if(comp instanceof JTextComponent) {
            width = SwingUtilities.computeStringWidth(fontMetrics,
                ((JTextComponent) comp).getText());
        } else {
            if(headerValue instanceof String) {
                width = SwingUtilities.computeStringWidth(fontMetrics, (String) headerValue);
            }
        }

        return width;
    }

    public void createColumnPopup() {
        _columnPopup = new JPopupMenu("Table Columns");

        for(Enumeration<TableColumn> e = _tcm.getColumns(false); e.hasMoreElements(); ) {
            TableColumn column = e.nextElement();

            JMenuItem jmi = _columnPopup.add(new MyJCheckBoxMenuItem(column));
            jmi.setSelected(_tcm.isColumnVisible(column));
            jmi.addActionListener(this);
        }
    }

    public void showPopupMenu(MouseEvent me) {
        if(me.isPopupTrigger()) {
            _columnPopup.show(me.getComponent(), me.getX(), me.getY());
        }
    }

    public void actionPerformed(ActionEvent e) {
        MyJCheckBoxMenuItem jmi  = (MyJCheckBoxMenuItem) e.getSource();
        TableColumn column       = jmi.getColumn();

        boolean success          = _tcm.setColumnVisible(column, jmi.getState());
        if(!success) {
            jmi.setState(!jmi.getState());
        }
    }
}

/**
 * Description of the Class
 *
 *@author    Harpreet Singh
 *@version   $Id: XJTableHeader.java,v 1.4 2004/03/22 14:28:05 hsingh Exp $
 */
@SuppressWarnings("serial")
class MyJCheckBoxMenuItem extends JCheckBoxMenuItem {

    private TableColumn _tc;

    public MyJCheckBoxMenuItem(TableColumn tc) {
        super(tc.getHeaderValue().toString());
        _tc = tc;
    }

    public TableColumn getColumn() {
        return _tc;
    }
}
