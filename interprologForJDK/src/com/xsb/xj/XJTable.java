package com.xsb.xj;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.Autoscroll;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.renderers.SortButtonRenderer;
import com.xsb.xj.util.ExcelAdapter;
import com.xsb.xj.util.HeaderListener;
import com.xsb.xj.util.LazyRefreshListener;
import com.xsb.xj.util.TransferableXJSelection;
import com.xsb.xj.util.XJException;
import com.xsb.xj.util.XJTableHeader;

/**
 * XJ version of javax.swing.JTable
 *
 *@version   $Id: XJTable.java,v 1.35 2004/08/26 01:14:39 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJTable extends JScrollPane implements DnDCapable, LazyRefreshListener, XJTemplateComponent {
    public final String NOVERTICALLINES  = "noverticallines";
    public final String EMPTYCOLOR = "emptyColor";
    public final String ROWCOUNTERFUNCTOR = "rowCounterFunctor";
    public final String PAGEFETCHER = "pageFetcher";

    GUITerm gt;
    PrologEngine engine;
    XJTableView theJTable;
    XJAbstractListModel model;
    final GUITerm[] cellGTs;
    TermModel rendererTemplate, editorTemplate;

    public XJTable(PrologEngine engine, GUITerm gt) {
        TableCellRenderer renderer;
        TableEditorInterface editor;
        this.gt = gt;
        this.engine = engine;

        TermModel templateObject     = getTemplate();

        if(templateObject.isList()) {
            // we have possibly more than one template / node type
            GUITerm[] renderingTemplates  = new GUITerm[templateObject.getChildCount()];
            GUITerm[] editingTemplates    = new GUITerm[renderingTemplates.length];
            for(int t = 0; t < renderingTemplates.length; t++) {
                GUITerm oneTemplate  = (GUITerm) templateObject.getChild(t);
                renderingTemplates[t] = (GUITerm) oneTemplate.clone();
                editingTemplates[t] = (GUITerm) oneTemplate.clone();
            }
            rendererTemplate = new TermModel(".", renderingTemplates, true);
            editorTemplate = new TermModel(".", editingTemplates, true);
            renderer = new PolimorphicRenderer(engine, renderingTemplates, false);
            editor = new PolimorphicListEditor(engine, editingTemplates);
        } else {
            GUITerm template  = (GUITerm) templateObject;

            // working on template copies; this prevents us from getting myGUI bindings defined outside the template,
            // so we might want to change something here or elsewhere later
            rendererTemplate = (GUITerm) template.clone();
            editorTemplate = (GUITerm) template.clone();
            // transient variables are gone with cloning, but XJCellTupleRenderer and XJCellTupleEditor will rebuild them:
            renderer = new XJCellTupleRenderer(engine, (GUITerm) rendererTemplate, false);
            editor = new ListCellEditor(engine, (GUITerm) editorTemplate);
        }

        cellGTs = editor.getCellGTs();

        if(gt.isList()) {
            model = new EagerListModel(gt, cellGTs);
        } else if(gt.isLazyList()) {
            if(gt.findProperty(GUITerm.PROLOGCACHED) != null) {
                model = new PrologCachedListModel(engine, this, gt, cellGTs);
            } else {
                model = new LazyListModel(engine, this, gt, cellGTs);
                TermModel rowCounter = gt.findProperty(ROWCOUNTERFUNCTOR);
                if (rowCounter!=null)
                	((LazyListModel)model).setCountSolutionsPred(rowCounter.toString());
                TermModel pageFetcher = gt.findProperty(PAGEFETCHER);
                if (pageFetcher!=null)
                	((LazyListModel)model).setUnorderedPageFetcherPred(pageFetcher.toString());
            }
            ((LazyListModel) model).addLazyRefreshListener(this);
        }
        //ColumnHeaderToolTips tips    = new ColumnHeaderToolTips();
        theJTable = new XJTableView(this, model, renderer, editor);
        setViewportView(theJTable);

        new ExcelAdapter(theJTable);
        // not working... setColumnHeaderView(theJTable.getTableHeader());

        // NOT NECESSARY ANYMORE, IN 1.3.1_01????: theJTable.addNotify();

        if(gt.findProperty(this.NOVERTICALLINES) != null) {
            theJTable.setShowVerticalLines(false);
        }
        TermModel emptyBackground = gt.findProperty(EMPTYCOLOR);
        if (emptyBackground!=null){
        	theJTable.emptyBackground = GUITerm.termToColor(emptyBackground);
        	theJTable.setBackground(theJTable.emptyBackground);
        	XJTable.this.setBackground(theJTable.emptyBackground);
			model.addTableModelListener(new TableModelListener(){
				public void tableChanged(TableModelEvent e){
					if (model.getRowCount()==0){
						theJTable.setBackground(theJTable.emptyBackground);
						XJTable.this.setBackground(theJTable.emptyBackground);
					}
					else {
						setBackground(theJTable.normalBackground);
						XJTable.this.setBackground(theJTable.normalBackground);
					}
				}
			});
        }
    }

    public TermModel getTemplate() {
        if(gt.isList()) {
            return gt.listTemplate();
        } else if(gt.isLazyList()) {
            return gt.lazyListTemplate();
        } else {
            return null;
        }
    }

    /**
     * applies changes made to the global template to the local copies
     */
    public void constructionEnded() {
        rendererTemplate.assignTermChanges(getTemplate());
        editorTemplate.assignTermChanges(getTemplate());
    }

    public JTable getJTable() {
        return theJTable;
    }

    public XJAbstractListModel getModel() {
        return model;
    }

    public Dimension getPreferredSize() {
        return gt.getPreferredSize(super.getPreferredSize());
    }

    public void destroy() {
        if(model instanceof LazyListModel) {// combo box with lazy list
            ((LazyListModel) model).destroy();
        }
    }
    
    public void setEnabled(boolean yes){
    	theJTable.setEnabled(yes);
    }
    
    public int getRowCount(){
    	return model.getRowCount();
    }

	static class XJTableView extends JTable implements MouseListener, Autoscroll {
        XJTable theXJTable;
        TableEditorInterface editor;
        private int scrollMargin = 12;
        Color emptyBackground = null;
        Color normalBackground;

        XJTableView(XJTable theXJTable, TableModel model, TableCellRenderer renderer, TableEditorInterface editor) {
            //results in missing header: super(model,columns);
            super(model, new XJTableColumnModel((XJAbstractListModel) model));
            //results in missing header: setColumnModel(columns);
            this.theXJTable = theXJTable;

            //this.renderer=renderer;
            this.editor = editor;
            XJTableColumnModel columnModel         = (XJTableColumnModel) getColumnModel();

            GUITerm gt                             = theXJTable.getGT();
            boolean sortable                       = false;
            SortButtonRenderer sbRenderer          = null;

            setTableHeader(new XJTableHeader(this, (XJTableColumnModel) columnModel));

            if(gt.findProperty("sortable") != null) {
                sortable = true;
            	if (gt.isLazyList())
            		sbRenderer = new SortButtonRenderer();
            	else 
            		setAutoCreateRowSorter(true);
            }
            
            int maxCellHeight                      = 0;// will use the tallest cell to determine row height
            int c                                  = 0;
            for(Enumeration<TableColumn> e = columnModel.getColumns(false); e.hasMoreElements(); ) {
                TableColumn column  = e.nextElement();

                Component rc        = (Component) editor.getCellComponents()[c++];//??? will have to change
                Dimension rcd       = rc.getPreferredSize();
                int width           = rcd.width;
                column.setCellEditor(editor);
                column.setCellRenderer(renderer);
                column.setPreferredWidth(width);
                if(sortable && gt.isLazyList()) {
                    column.setHeaderRenderer(sbRenderer);
                }

                // could be further tuned for headers, cf.
                // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html#custom
                if(rcd.height > maxCellHeight) {
                    maxCellHeight = rcd.height;
                }
            }

            setRowHeight(maxCellHeight + getRowMargin());
            addMouseListener(this);
            if(gt.findProperty(GUITerm.SINGLESELECTIONS) != null) {
                setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            } else {
                setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            }
			
			if(gt.findProperty(GUITerm.ADJUSTTOFIRSTROW) != null) {
            	setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
            	new SizeAdjusterToFirstRow(XJTableView.this);
			} else setAutoResizeMode(AUTO_RESIZE_OFF);

            if(sortable && gt.isLazyList()) {
                getTableHeader().addMouseListener(new HeaderListener(getTableHeader(), sbRenderer, gt.findProperty("sortable"), theXJTable.rendererTemplate));
            }

            /*
             *  Handle selectionChanged operations (not functions)
             */
            XJAction[] topOps                      = gt.operations(theXJTable.getEngine(), theXJTable);
            final XJAction selectionChangedAction  = XJAction.findSelectionChanged(topOps);
            if(selectionChangedAction != null) {
                // let's keep it light, and assume no modal interactions occur:
                selectionChangedAction.setInAWTThread(true);
                selectionChangedAction.setCursorChanges(false);
                getSelectionModel().addListSelectionListener(
                            new ListSelectionListener() {
                                public void valueChanged(ListSelectionEvent e) {
                                    //System.out.println("valueChanged:"+Thread.currentThread());
                                    if(!e.getValueIsAdjusting() && loadFromGUI()) {
                                        selectionChangedAction.doit();
                                    }
                                }
                            });
            }
            final XJAction rowCountChangedAction  = XJAction.findRowCountChanged(topOps);
            if(rowCountChangedAction != null) {
                //rowCountChangedAction.setInAWTThread(true);
                //rowCountChangedAction.setThreaded(true);
                rowCountChangedAction.setCursorChanges(false);
                getModel().addTableModelListener(
					new TableModelListener() {
						public void tableChanged(TableModelEvent e) {
							//  event info on Mac does not vary with type... so even updates will trigger this
							//if(e.getType()==TableModelEvent.INSERT || e.getType()==TableModelEvent.DELETE) {
								rowCountChangedAction.doit();
							//}
						}
					});
            }
          
            normalBackground = getBackground();
        }

        /*
         *  This method is being called when the row is finished to be edited
         *  (since JTable is a listener to the editor)
         *  and focus is changed or another row is selected.
         *  Had to rewrite this method of JTable (though not recommended, see original method)
         *  to check whether the edited component was really edited (not isDirty).
         *  Without check, it was calling model.setValueAt() which
         *  resulted in trying to update a lazy model (if the model is lazy but not updatable).
         *  If this workaround is not satisfactory, it is possible to remove
         *  this method and
         *  to change model.setValueAt() to check whether the value got changed.
         */
        public void editingStopped(javax.swing.event.ChangeEvent e) {
            if(editor != null) {
                if(editor.isDirty()) {
                    super.editingStopped(e);
                } else {
                    removeEditor();
                }
            }
        }
        
        public void setFont(Font F){
        	JTableHeader h = getTableHeader();
        	if (h!=null) h.setFont(F);
        	super.setFont(F);
        }

        // MouseListener methods
        public void mouseReleased(MouseEvent e) { }

        public void mouseExited(MouseEvent e) { }

        public void mousePressed(MouseEvent e) { }

        /**
         * Handle 'doubleclick' operations and functions
         *
         *@param e  Description of the Parameter
         */
        public void mouseClicked(MouseEvent e) {
            final int row_        = rowAtPoint(e.getPoint());
            int viewColumnIndex  = columnAtPoint(e.getPoint());
            
            if(e.getClickCount() == 2 && row_ >= 0 && viewColumnIndex != -1 && row_ < getRowCount()) {
            	final int row = convertRowIndexToModel(row_);
                // cf. comments in processMouseEvent
                int modelColumnIndex              = convertColumnIndexToModel(viewColumnIndex);
                Runnable rememberFunctionResults  =
                    new Runnable() {
                        public void run() {
                            setValueAt(editor.getCellEditorValue(), row, -1);
                        }
                    };
                
                
				editor.setCurrentTuple((TermModel) XJTableView.this.getModel().getValueAt(row, modelColumnIndex));
                GUITerm GT = editor.getCellGTs()[modelColumnIndex];
				//System.out.println("GT properties:"+java.util.Arrays.toString(GT.properties));
                XJAction[] cellops                = GT.operations(theXJTable.getEngine(), theXJTable, rememberFunctionResults);
                XJAction todo                     = XJAction.findDoubleClick(cellops);
                //System.out.println("todo:"+todo);
                if(todo != null && theXJTable.loadFromGUI()) {
                    selectRow(row);
                    getColumnModel().getSelectionModel().setSelectionInterval(viewColumnIndex, viewColumnIndex);
                    
                    e.consume();
                    todo.doit();
                }
            }
            ;
        }

        public void mouseEntered(MouseEvent e) {
            if((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
                int viewColumnIndex   = columnAtPoint(e.getPoint());
                int modelColumnIndex  = convertColumnIndexToModel(viewColumnIndex);
                if(viewColumnIndex != -1) {
                    System.out.println(editor.getCellGTs()[modelColumnIndex].propsDescription());
                }
            }
        }

        /**
         * Handle popup menu operations and functions on cells
         *
         *@param e  Description of the Parameter
         */
        protected void processMouseEvent(MouseEvent e) {
            if(e.isPopupTrigger()) {
                final int row_         = rowAtPoint(e.getPoint());
                int viewColumnIndex   = columnAtPoint(e.getPoint());
                
                if(row_ >= 0 && viewColumnIndex != -1 && theXJTable.loadFromGUI() && row_ < getRowCount()) {
                	final int row = convertRowIndexToModel(row_);
                	int modelColumnIndex  = convertColumnIndexToModel(viewColumnIndex);
                    e.consume();
                    getColumnModel().getSelectionModel().setSelectionInterval(viewColumnIndex, viewColumnIndex);
                    getSelectionModel().setSelectionInterval(row, row);

                    // We'll use our editor to support functions, which may change data
                    // Notice that the editor has been freed above with loadFromGUI
                    editor.setCurrentTuple((TermModel) XJTableView.this.getModel().getValueAt(row, modelColumnIndex));
                    // Our popup will appear assynchronously, and its actions will
                    // execute later, so we need the following; be sure not to originate
                    // further user interactions before this runs
                    Runnable rememberFunctionResults  =
                        new Runnable() {
                            public void run() {
                                // col does not matter, we're passing the full tuple
                                setValueAt(editor.getCellEditorValue(), row, -1);
                            }
                        };
                    JPopupMenu pm                     = XJAtomicField.operationsPopup(
                        editor.getCellGTs()[modelColumnIndex], theXJTable.getEngine(), theXJTable, rememberFunctionResults
                        );
                    if(pm.getComponentCount() > 0){
                        pm.show(this, e.getX(), e.getY());
                    }
                } else {
                    super.processMouseEvent(e);
                }
            } else {
                super.processMouseEvent(e);
            }
        }

        private void selectRow(int row) {
            getSelectionModel().setSelectionInterval(row, row);
        }

        public void setClickCountToStart(int count) {
            editor.setClickCountToStart(count);
        }

        public int getClickCountToStart() {
            return editor.getClickCountToStart();
        }

        /*
         *  This COULD BE overriden from JTable so we can know when a list row is closed due
         *  to navigation to another row:
         *  public boolean editCellAt(int row, int column, java.util.EventObject e){
         *  if (editingRow!=-1 && editingRow!=row){
         *  int wasEditing = editingRow;
         *  if (!editor.stopCellEditing()) return false;
         *  if (!editor.closeRow(wasEditing)) return false;
         *  }
         *  return super.editCellAt(row,column,e);
         *  }
         */
        /**
         * Simply ask the editor to stop editing and get its value. This may get
         * called several times, so the implementation should be aware of that
         *
         *@return   Description of the Return Value
         */
        public boolean loadFromGUI() {
            int oldRow     = editingRow;
            int oldColumn  = editingColumn;
            if(oldRow != -1
            /*
             *  actually editing something
             */
                && isDirty()) {
                if(editor.stopCellEditing()) {
                    setValueAt(editor.getCellEditorValue(), convertRowIndexToModel(oldRow), convertColumnIndexToModel(oldColumn));
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        /**
         * Delegate to our ListCellEditor
         *
         *@return   The dirty value
         */
        public boolean isDirty() {
            return editor.isDirty();
        }

        /**
         * Indexes are assumed to exist in the list, and container selection to
         * have been performed already
         *
         *@param indexes  Description of the Parameter
         */
        public void selectGUI(int[] indexes) {
            ListSelectionModel lsm  = getSelectionModel();
            lsm.setValueIsAdjusting(true);
            if(!lsm.isSelectionEmpty()) {
                lsm.clearSelection();
            }
            int lower               = Integer.MAX_VALUE;
            for(int p = 0; p < indexes.length; p++) {
                int index  = indexes[p];
                if(index < lower) {
                    lower = index;
                }
                lsm.addSelectionInterval(index, index);
            }
            lsm.setValueIsAdjusting(false);
            final int flower        = lower;
            SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                scrollRectToVisible(getCellRect(flower, 0, true));
                            }
                        });

        }

        DragGestureListener createDragGestureListener() {
            DragGestureListener dgl  =
                new DragGestureListener() {
                    public void dragGestureRecognized(DragGestureEvent event) {
                        if(event.getDragAction() != DnDConstants.ACTION_COPY) {
                            return;
                        }
                        // not doing any difference, apparently DnD gets this concurrently with others:
                        // event.getTriggerEvent().consume();
                        Point where  = event.getDragOrigin();
                        int row_      = rowAtPoint(where);
                        int col      = columnAtPoint(where);

                        if(row_ >= 0 && row_ < getRowCount() && col != -1) {
                        	int row = convertRowIndexToModel(row_);
                            if(!getSelectionModel().isSelectedIndex(row)) {
                                System.err.println("Data must be selected just before being dragged");
                                return;
                            }
                            DragSourceListener dsl        =
                                new com.xsb.xj.util.DragSourceAdapter() {
                                    public void dragDropEnd(DragSourceDropEvent dsde) {
                                        //System.out.println("Drop successful ? : " + dsde.getDropSuccess());
                                    }
                                };
                            TransferableXJSelection txjs  = new TransferableXJSelection(theXJTable, theXJTable.getSelectedTerms());
                            event.getDragSource().startDrag(event, DragSource.DefaultCopyDrop, txjs, dsl);
                            // System.out.println("Started drag");
                        }
                    }
                };
            return dgl;
        }

        TermModel point2Term(Point where) {
            int row  = rowAtPoint(where);
            int col  = columnAtPoint(where);
            if(row >= 0 && row < getRowCount() && col != -1) {
                return (TermModel) getValueAt(convertRowIndexToModel(row), -1);
            } else {
                return null;
            }
        }
        
        // autoscroll as per
        // http://www.oreilly.com/catalog/jswing/chapter/dnd.beta.pdf
        // We've been told to scroll because the mouse cursor is in our
        // scroll zone.
        public void autoscroll(Point cursorLocn) {
            // Figure out which row we're on.
            int realrow = rowAtPoint(cursorLocn);
            int realcolumn = columnAtPoint(cursorLocn);
            Rectangle tableBounds = getBounds(); 
            Rectangle scrollPaneBounds = getParent().getBounds();
            // tableBounds usually greater than scrollPaneBounds
            // and after scroll to bottom/right 
            // tableBounds.y/tableBounds.x may assume
            // negative values (when the beginning rows/columns are invisible)
            if(cursorLocn.y + tableBounds.y <= scrollMargin ){
                realrow = realrow < 1 ? 0 : realrow - 1 ;
            } else if (cursorLocn.y >= -tableBounds.y+scrollPaneBounds.height-scrollMargin){
                realrow = realrow < getRowCount() - 1 ? realrow + 1 : realrow;
            }
            if(cursorLocn.x + tableBounds.x <= scrollMargin ){
                realcolumn = realcolumn < 1 ? 0 : realcolumn - 1 ;
            } else if (cursorLocn.x >= -tableBounds.x + scrollPaneBounds.width - scrollMargin){
                realcolumn = realcolumn < getRowCount() - 1 ? realcolumn + 1 : realcolumn;
            }
            scrollRectToVisible(getCellRect(realrow, realcolumn, false));
        }
        
        public java.awt.Insets getAutoscrollInsets() {
            Rectangle tableBounds = getBounds();
            Rectangle scrollPaneBounds = getParent().getBounds();
            return new java.awt.Insets(
            - tableBounds.y + scrollMargin, // top
            - tableBounds.x + scrollMargin, //left
            tableBounds.height +  tableBounds.y - scrollPaneBounds.height + scrollMargin, // bottom
            tableBounds.width +  tableBounds.x  - scrollPaneBounds.width + scrollMargin // right
            );
        }
    }

    /**
     * Returns the terms currently selected in the list
     *
     *@return   The selectedTerms value
     */
    public TermModel[] getSelectedTerms() {
        int[] selectedIndexes       = theJTable.getSelectedRows();
        // there is a bug in getSelectedRows() (Sun Bug Id 4730055)
        // when it sometimes returns non-existing rows (> row count)
        // following is a workaround until the bug is fixed
        Vector<Integer> tempSelectedIndices  = new Vector<Integer>();
        int rowCount                = model.getRowCount();
        for(int i = 0; i < selectedIndexes.length; i++) {
            if(selectedIndexes[i] < rowCount) {
                tempSelectedIndices.add(new Integer(theJTable.convertRowIndexToModel(selectedIndexes[i])));
            }
        }
        selectedIndexes = new int[tempSelectedIndices.size()];
        for(int i = 0; i < selectedIndexes.length; i++) {
            selectedIndexes[i] = tempSelectedIndices.elementAt(i).intValue();
        }
        // end of workaround
        TermModel[] selectedTerms   = new TermModel[selectedIndexes.length];
        for(int i = 0; i < selectedIndexes.length; i++) {
            selectedTerms[i] = (TermModel) model.getValueAt(selectedIndexes[i], -1);
        }
        return selectedTerms;
    }
    
    /** For more efficiency in confirmation dialogs etc. */
    public int getSelectionSize(){
    	return theJTable.getSelectedRows().length;
    }
    
    public boolean updateSingleSelection(TermModel newTerm){
    	int[] selectedIndexes = theJTable.getSelectedRows();
    	if (selectedIndexes.length!=1)
    		throw new XJException("updateSingleSelection demands that a single list item is selected");
    	return model.setTerm(selectedIndexes[0],newTerm);
    }

    /**
     * Add a new unedited default term to the end of the list. Returns false if
     * it can not added. Current implementation is heavy, make sure you use this
     * just in reaction to single user events, not in bulk
     *
     *@return   Description of the Return Value
     */

    public boolean addNew() {
        if(!loadFromGUI()) {
            return false;
        }
        GUITerm template       = (GUITerm) getTemplate();
        if(template.containsOpaque() || template.containsReadonly() && template.containsInvisible()) {
            return false;
        }
        // Follows some convoluted stuff, although conceptually clean...
        GUITerm newTemplate    = (GUITerm) template.clone();
        newTemplate.makeGUI(getEngine());// set dummy renderers
        newTemplate.setAllDefaultValues();// gets default values set by the renderers custom constructors etc.
        model.addNewTerm(newTemplate.getTermModel());
        // Now let's try to find a new cell to edit
        int firstEditableCell  = -1;
        int newRow             = theJTable.getRowCount() - 1;
        for(int c = 0; c < theJTable.getColumnCount(); c++) {
            if(theJTable.isCellEditable(newRow, c)) {
                firstEditableCell = c;
                break;
            }
        }
        if(firstEditableCell != -1) {
            theJTable.editCellAt(newRow, firstEditableCell);
        }
        return true;
    }

    /**
     * Add terms to end of list, delegating the task to the model. The terms
     * must be acceptable to the list template
     *
     *@param terms  The feature to be added to the Terms attribute
     */
    public boolean addTerms(TermModel[] terms) {
        return model.addTerms(terms);
    }

    public boolean addTerms(TermModel list) {
        return model.addTerms(list.flatList());
    }

    /**
     * Remove terms from anywhere in the list, delegating the task to the model.
     * Terms are identifying by their list model ordering
     *
     *@param indexes  Description of the Parameter
     */
    public boolean deleteTerms(int[] indexes) {
        return model.deleteTerms(indexes);
    }

    public boolean deleteTerms(Integer[] indexes) {
        int[] int_indexes  = new int[indexes.length];
        for(int i = 0; i < indexes.length; i++) {
            int_indexes[i] = indexes[i].intValue();
        }
        return model.deleteTerms(int_indexes);
    }

    /**
     * Terms are identified by similarity, a notion whose precise implementation
     * depends on the list model
     *
     *@param terms  Description of the Parameter
     */
    public boolean deleteTerms(TermModel[] terms) {
        return model.deleteTerms(terms);
    }
    
    /** Delete all list elements */
    public boolean clear(){
    	int N = model.getRowCount();
    	int[] indexes = new int[1];
    	boolean RC = true;
    	for (int i=N-1; (RC && i>=0); i--){
    		indexes[0] = i;
    		RC=model.deleteTerms(indexes);
    	}
    	return RC;
    }
    
    /** Delete all items and replace them by these */
    public boolean setList(TermModel[] terms){
    	return clear() && addTerms(terms) ;
    }

	/** Convenience method acceptable only for lazy lists */
	public void invalidateAndRefresh() {
		model.invalidateAndRefresh();
	}

    public void setContext(TermModel c) {
        if(!(model instanceof LazyListModel)) {
            throw new XJException("Context can only be changed for lazy components");
        }
        XJDesktop.setWaitCursor(this);
        ((LazyListModel) model).setContext(c);
        XJDesktop.restoreCursor(this);
    }

    public TermModel getContext() {
        return ((LazyListModel) model).getContext();
    }

    // XJComponent interface methods
    public PrologEngine getEngine() {
        return engine;
    }

    public GUITerm getGT() {
        return gt;
    }
	public void setGT(GUITerm gt){
		this.gt=gt;
	}

    /**
     * No op: GUI is refreshed automatically using the standard JTable machinery
     */
    public void refreshGUI() {
        if(!(model instanceof LazyListModel)) {
            theJTable.revalidate();
            theJTable.repaint();
        }
    }

    public boolean loadFromGUI() {
        return theJTable.loadFromGUI();
    }

    /**
     * Delegates into our XJTableView
     *
     *@return   The dirty value
     */
    public boolean isDirty() {
        return theJTable.isDirty();
    }

    /**
     * This implementation does nothing. An eager list default value is always
     * [], and the concept is meaningless for lazy lists, as XJ is not in the
     * business of creating new relations
     *
     *@param dv  The new defaultValue value
     */
    public void setDefaultValue(TermModel dv) { }

    /*
     *  Implementation accepting indexes to specify a selection, bad idea because it's less general and too fragile:
     *  public void selectGUI(Object[] parts){
     *  GUITerm.typicalCommonSelect(theJTable);
     *  if (parts==null || parts.length==0) return;
     *  int[] indexes = new int[parts.length];
     *  for (int p=0; p<parts.length; p++){
     *  TermModel term = (TermModel)parts[p];
     *  possibly in the future: specification by term:
     *  int index = theXJTable.getModel().indexOfTerm(term); ...
     *  int index = term.intValue();
     *  if (index<0) throw new XJException("Can not select negative list item index");
     *  indexes[p] = index;
     *  }
     *  theJTable.selectGUI(indexes);
     *  }
     */
    /**
     * Make the list visible, and may select the items specified by terms
     *
     *@param terms  Description of the Parameter
     */
    public void selectGUI(Object[] terms) {
        GUITerm.typicalCommonSelect(theJTable);
        selectItems(terms);
    }

    protected void selectItems(Object[] terms) {
        if(terms == null) {
            return;
        }
        if(terms.length == 0) {
            theJTable.clearSelection();
            return;
        }
        Vector<Integer> bag     = new Vector<Integer>();
        for(int p = 0; p < terms.length; p++) {
            TermModel term  = (TermModel) terms[p];
            int index       = model.indexOfTerm(term);
            if(index != -1) {
                bag.addElement(new Integer(index));
            }
        }
        int[] indexes  = new int[bag.size()];
        for(int i = 0; i < indexes.length; i++) {
            indexes[i] = bag.elementAt(i).intValue();
        }
        theJTable.selectGUI(indexes);
    }

    /**
     * Index is assumed to exist in the list, index starts from 1, and container
     * selection to have been performed already
     *
     *@param index  The new selectedRow value
     */
    public void setSelectedRow(int index) {
        int adjustedIndex       = index - 1;// because Java starts count from 0
        ListSelectionModel lsm  = theJTable.getSelectionModel();
        lsm.setValueIsAdjusting(true);
        if(!lsm.isSelectionEmpty()) {
            lsm.clearSelection();
        }

        lsm.addSelectionInterval(adjustedIndex, adjustedIndex);

        lsm.setValueIsAdjusting(false);

        final int finalIndex    = adjustedIndex;
        SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            theJTable.scrollRectToVisible(theJTable.getCellRect(finalIndex, 0, true));
                        }
                    });

    }

    // do we need synchronization on oldSelection? probably not
    public void willRefresh() {
        // oldSelection = getSelectedTerms(); oldSelection was not used for anything... and this breaks occasionally after setContext
        //System.out.println("will refresh, "+oldSelection.length+" items selected");
        //if(oldSelection.length > 0) {
        //    System.out.println("oldSelection first:" + oldSelection[0]);
        //}
    }

    public void didRefresh() {
        // System.out.println("did refresh, current selection now has "+theJTable.getSelectedRows().length+" items");
        // System.out.println("attempting to select "+oldSelection.length+" old items");
        //selectItems(oldSelection);
    }

    /**
     * Specifies the number of clicks needed by our editor to start editing a
     * row.
     *
     *@param count  an int specifying the number of clicks needed to start
     *      editing
     *@see          #getClickCountToStart
     */
    public void setClickCountToStart(int count) {
        theJTable.setClickCountToStart(count);
    }

    public int getClickCountToStart() {
        return theJTable.getClickCountToStart();
    }

    public void setSelectedTerm(TermModel term) {
        int[] selectedIndexes  = theJTable.getSelectedRows();
        if(selectedIndexes.length > 0) {
            int selectedRow  = selectedIndexes[0];
            model.setValueAt(term, selectedRow, -1);
        }
    }

    /**
     * Returns the term item at the location in (contained) JTable coordinates,
     * or null if none is there
     *
     *@param where  Description of the Parameter
     *@return       Description of the Return Value
     */
    public TermModel point2Term(Point where) {
        return theJTable.point2Term(where);
    }

    // DnDCapable methods
    public DragGestureListener createDragGestureListener() {
        return theJTable.createDragGestureListener();
    }

    public JComponent getRealJComponent() {
        return theJTable;
    }
    
    public static class SizeAdjusterToFirstRow implements TableModelListener{
    	boolean adjustedSizes = false;
    	JTable table;
    	
    	public SizeAdjusterToFirstRow(JTable table){
    		this.table=table;
			table.getModel().addTableModelListener(this);
    	}
		// from http://docs.oracle.com/javase/tutorial/uiswing/examples/components/TableRenderDemoProject/src/components/TableRenderDemo.java
		// modified to use sizes from first row
		public void initColumnSizes(JTable table) {
			TableModel model = table.getModel();
			if (model.getRowCount()==0) return;
			TableColumn column = null;
			Component comp = null;
			int headerWidth = 0;
			int cellWidth = 0;
			TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();

			for (int i = 0; i < model.getColumnCount(); i++) {
				column = table.getColumnModel().getColumn(i);
				comp = headerRenderer.getTableCellRendererComponent(null, column.getHeaderValue(),
					false, false, 0, 0);
				headerWidth = comp.getPreferredSize().width;

				comp = table.getDefaultRenderer(model.getColumnClass(i)).
					getTableCellRendererComponent(table, model.getValueAt(0,i),false, false, 0, i);
				cellWidth = comp.getPreferredSize().width;
				column.setPreferredWidth(Math.max(headerWidth, cellWidth));
			}
		}
		// TableModelListener method:
		public void tableChanged(TableModelEvent e){
			if (!adjustedSizes && table.getModel().getRowCount()>0){
				adjustedSizes = true;
				initColumnSizes(table);
			}
		}
	
    }
    
}
