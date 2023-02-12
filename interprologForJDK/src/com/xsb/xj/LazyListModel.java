package com.xsb.xj;

import java.awt.Component;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.SwingUtilities;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.LazyRefreshListener;
import com.xsb.xj.util.XJException;

/**
 * Description of the Class
 *
 *@version   $Id: LazyListModel.java,v 1.16 2004/02/23 22:51:36 hsingh Exp $
 */
@SuppressWarnings("serial")
public class LazyListModel extends XJAbstractListModel {
    PrologEngine engine;
    Component someComponent;
    int rowCountCache;// -1 means invalid row count / empty cache
    /** Goal functor for row counting predicate, which must have the form Functor(+Unique,+GoalWithContextAlreadyApplied,-RowCount). 
    Unique must be true or fail */
    protected String countSolutionsPred;
    /** Goal functor for fetching a list page, which must have the form Functor(+GoalWithContextAlreadyApplied,+T,FirstIndex,Last,Array) */
    protected String unorderedPageFetcherPred;
    /**HashMap of Vectors of rows. Keys for hashmap are
    0 for rows 0 to REFRESH_PAGE-1
    1 for REFRESH_PAGE to 2*REFRESH_PAGE -1
    and so on */
    Map<Integer,Vector<TermModel>> rowTermsCache;
    TermModel T, G, originalContext, context;
    // Will do our updates
    UpdateRowAction updateAction;
    InsertRowsAction insertAction;
    DeleteRowsAction deleteAction;
    protected Vector<LazyRefreshListener> refreshListeners;

    public LazyListModel(PrologEngine engine, Component c, GUITerm gt, GUITerm[] cellGTs) {
        super(gt, cellGTs);
        rowTermsCache = Collections.synchronizedMap(new HashMap<Integer,Vector<TermModel>>());
        countSolutionsPred = "countSolutions";
        unorderedPageFetcherPred = "get_solution_models";
        someComponent = c;
        this.engine = engine;
        TermModel lazy      = (TermModel) gt.getChild(0);
        if(!(lazy.node.equals("lazy")) || lazy.getChildCount() != 3) {
            throw new XJException("bad lazy term:" + lazy);
        }
        T = (TermModel) lazy.getChild(0);
        //TODO: All vars in T should have an element in the VarList in flora(GoalAtom,VarList)
        G = GUITerm.floraPreprocessWithVarList((TermModel) lazy.getChild(1));
        originalContext = (TermModel) lazy.getChild(2);
        context = null;
        TermModel template  = gt.lazyListTemplate();
        if(!template.isList()) {
            if(((GUITerm) template).findProperty(GUITerm.UPDATABLE) != null) {
                updateAction = new UpdateRowAction();
            }
            if(((GUITerm) template).findProperty(GUITerm.INSERTABLE) != null) {
                insertAction = new InsertRowsAction();
            }
            if(((GUITerm) template).findProperty(GUITerm.DELETABLE) != null) {
                deleteAction = new DeleteRowsAction();
            }
        }
        if((updateAction == null) && gt.findProperty(GUITerm.UPDATABLE) != null) {
            updateAction = new UpdateRowAction();
        }
        if((insertAction == null) && gt.findProperty(GUITerm.INSERTABLE) != null) {
            insertAction = new InsertRowsAction();
        }
        if((deleteAction == null) && gt.findProperty(GUITerm.DELETABLE) != null) {
            deleteAction = new DeleteRowsAction();
        }

        invalidateCache();
        refreshListeners = new Vector<LazyRefreshListener>();
        checkSortProperty();
    }
    
    protected void setCountSolutionsPred(String S){
    	countSolutionsPred = S;
    }
    
    protected void setUnorderedPageFetcherPred(String S){
    	unorderedPageFetcherPred = S;
  	}
  	 
    public void addLazyRefreshListener(LazyRefreshListener l) {
        refreshListeners.addElement(l);
    }

    public void removeLazyRefreshListener(LazyRefreshListener l) {
        refreshListeners.removeElement(l);
    }

    protected void fireWillRefresh() {
        for(int i = 0; i < refreshListeners.size(); i++) {
            (refreshListeners.elementAt(i)).willRefresh();
        }
    }

    protected void fireDidRefresh() {
        for(int i = 0; i < refreshListeners.size(); i++) {
            refreshListeners.elementAt(i).didRefresh();
        }
    }

    public synchronized void setContext(TermModel c) {
        context = c;
        invalidateAndRefresh();
    }

    public synchronized void setSortTerm(TermModel newSortTerm) {
        if(newSortTerm == null) {
            sorted = false;
        } else {
            sorted = true;
        }
        if(((sortTerm == null) && (newSortTerm != null)) || !sortTerm.equals(newSortTerm)) {
            sortTerm = newSortTerm;
            invalidateAndRefresh();
        }
    }

    public TermModel getContext() {
        return context;
    }
    
    /** Returns the "rubberstamping" term for the list, as specified in lazy(T,Goal,Context) */
    public TermModel getT(){
    	return T;
    }

    /** Returns the Goal template for the list, as specified in lazy(T,Goal,Context) */
    public TermModel getG(){
    	return G;
    }

    // cache management
    synchronized void invalidateCache() {
        rowCountCache = -1;
        rowTermsCache.clear();
    }

    /*
     *  causes hanging: synchronized
     */
    public void invalidateAndRefresh() {
        // System.out.println("Let's invalidate and refresh a list");
        Runnable doer = new Runnable(){
        	public void run(){
				fireWillRefresh();// here and not after, to make sure we get old selected values before a change
				invalidateCache();
				fireTableDataChanged();
				fireDidRefresh();
        	}
        };
		if (SwingUtilities.isEventDispatchThread()) doer.run();
		else 
			SwingUtilities.invokeLater(doer); // does NOT block here
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return
        /*
         *  updateAction!=null &&
         */
            super.isCellEditable(rowIndex, columnIndex);
    }

    // The next two could also be implemented with a PrologAction, but let's assume no user-friendly errors
    // occur and be as light as possible (well, could be even ligher by passing G as a String)
    public synchronized int getRowCount() {
        if(rowCountCache == -1) {
            //System.out.println("Computing rowCount...");
            Integer N  = (Integer) engine.deterministicGoal(
                "recoverTermModels([Gmodel,OCmodel],[G,OC]), " +
                (context == null ? "(var(OC)->OC=null;true), " : "recoverTermModel(Cmodel,C), C=OC, ") +
                countSolutionsPred + (isSorted() ? "(true" : "(fail") +  ",G,N)," +
                // (isSorted() ? "countUniqueSolutions(G,N), " : "countSolutions(G,N), ") +
                "ipObjectSpec('java.lang.Integer',Int,[N],_)",
                "[Gmodel,OCmodel,Cmodel,SortIndexListModel]",
                new Object[]{G, originalContext, context, getSortTerm()},
                "[Int]"
                )[0];
            rowCountCache = N.intValue();
            //System.out.println("...computed:"+rowCountCache);
        }
        return rowCountCache;
    }

    static final int REFRESH_PAGE = 50;

    public synchronized TermModel getTerm(int r) {
        // irrelevant: getRowCount(); // make sure we have a row count AND XSB tables are loaded
        int mapKeyInt       = (int) (Math.floor(((double) r) / ((double) REFRESH_PAGE)));
        Integer mapKey      = new Integer(mapKeyInt);
        Vector<TermModel> cacheVector  = rowTermsCache.get(mapKey);
        if(cacheVector == null) {
            // cache does not contain row r
            int first         = mapKeyInt * REFRESH_PAGE;
            int newLast       = Math.min(first + REFRESH_PAGE, getRowCount()) - 1;
            //System.out.println("Fetching rows "+first+" to "+newLast);
            TermModel[] rows  = (TermModel[]) engine.deterministicGoal(
                "recoverTermModels([Gmodel,Tmodel,OCmodel],[G,T,OC]), " +
                (context == null ? "(var(OC)->OC=null;true), " : "recoverTermModel(Cmodel,C), C=OC, ") +
                (isSorted()
                ? "recoverTermModel(SortIndexListModel, Sort), get_sorted_solution_models(G,T," + first + "," + newLast + ",Sort,Array)"
                : unorderedPageFetcherPred +"(G,T," + first + "," + newLast + ",Array)"),
                "[Gmodel,Tmodel,OCmodel,Cmodel,SortIndexListModel]",
                new Object[]{G, T, originalContext, context, getSortTerm()},
                "[Array]"
                )[0];
            if(rows.length != (newLast - first + 1)) {
                throw new XJException("Inconsistent row page fetch:"+rows.length);
            }
            cacheVector = new Vector<TermModel>(REFRESH_PAGE);
            for(int i = 0; i < rows.length; i++) {
                cacheVector.addElement(rows[i]);
            }
            rowTermsCache.put(mapKey, cacheVector);
        }
        return cacheVector.elementAt(r - mapKeyInt * REFRESH_PAGE);
    }

    public Map<Integer,Vector<TermModel>> getRowCache() {
        return rowTermsCache;
    }

    public void destroy() {
        rowCountCache = -1;
        rowTermsCache.clear();
        // for now rowTermsCache = null;
    }

    static final Boolean FALSE = new Boolean(false);

    // This might be amalgamated with the code in XJChangeManager... or may be not:
    public class UpdateRowAction extends PrologAction {
        UpdateRowAction() {
            super(LazyListModel.this.engine, LazyListModel.this.someComponent, "xjSaveTerm(Old,New,IsNew)", "Save List Row");
            setThreaded(false);// we want to check actionConcluded() afterwards
        }

        // not using row index now, but may do so in the future
        void setRowToSave(int row, TermModel oldTerm, TermModel newTerm) {
            setArguments(
                "[Old,New,IsNew]",
                new Object[]{oldTerm, newTerm, FALSE}
                );
        }
    }

    public boolean setTerm(int row, TermModel value) {
        if(doSetTerm(row, value)) {
            // rowTermsCache.setElementAt(value,row): WRONG! solution ordering no longer valid
            invalidateAndRefresh();
            return true;
        } else return false;
    }

    protected boolean doSetTerm(int row, TermModel value) {
        if(updateAction == null) {
            // probably the cell is not declared readonly or opaque
            // it was entered for editing and then exited
            // even if no changes were made ...
            throw new XJException("No updatable property or xj_update_... predicate for lazy list goal term template");
        }
        updateAction.setRowToSave(row, getTerm(row), value);
        updateAction.doit();
        return updateAction.actionSucceeded();
    }

    /**
     * Adds to predicate using xj_insert
     *
     *@param terms  The feature to be added to the Terms attribute
     */
    public boolean addTerms(TermModel[] terms) {
        if(doAddTerms(terms)) {
            invalidateAndRefresh();
            return true;
        } else return false;
        /*
         *  WRONG: solution ordering no longer valid
         *  {
         *  rowCountCache=oldCount+terms.length;
         *  fireTableRowsInserted(oldCount,rowCountCache-1);
         *  }
         */
    }

    protected boolean doAddTerms(TermModel[] terms) {
        if(insertAction == null) {
            throw new XJException("No xj_insert_... predicate for lazy list goal term template");
        }
        if(terms.length == 0) {
            return true;
        }
        // irrelevant: int oldCount = getRowCount();
        insertAction.setRowsToAdd(terms);
        insertAction.doit();
        return insertAction.actionSucceeded();
    }

    class InsertRowsAction extends PrologAction {
        InsertRowsAction() {
            super(LazyListModel.this.engine, LazyListModel.this.someComponent, "xjInsertTerms(TermArray)", "Add List Rows");
            setThreaded(false);// we want to check actionConcluded() afterwards
        }

        void setRowsToAdd(TermModel[] newTerms) {
            setArguments(
                "[TermArray]",
                new Object[]{newTerms}
                );
        }
    }

    public boolean deleteTerms(int[] indexes) {
        throw new XJException("use deleteTerms(TermModel[]) instead");
    }

    public boolean deleteTerms(TermModel[] terms) {
        if(doDeleteTerms(terms)) {
            invalidateAndRefresh();
            return true;
        } else return false;
        /*
         *  WRONG: solution ordering no longer valid
         *  {
         *  rowCountCache = rowCountCache-terms.length;
         *  rowTermsCache.clear();
         *  fireTableDataChanged();
         *  }
         */
    }

    protected boolean doDeleteTerms(TermModel[] terms) {
        if(deleteAction == null) {
            throw new XJException("No xj_delete_... predicate for lazy list goal term template");
        }
        if(terms.length == 0) {
            return true;
        }
        deleteAction.setRowsToDelete(terms);
        deleteAction.doit();
        return deleteAction.actionSucceeded();
    }

    public class DeleteRowsAction extends PrologAction {
        DeleteRowsAction() {
            super(LazyListModel.this.engine, LazyListModel.this.someComponent, "xjDeleteTerms(TermArray)", "Delete List Rows");
            setThreaded(false);// we want to check actionConcluded() afterwards
        }

        void setRowsToDelete(TermModel[] newTerms) {
            setArguments(
                "[TermArray]",
                new Object[]{newTerms}
                );
        }
    }

}
