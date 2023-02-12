/*
 *  XJComboBoxModel.java
 *
 *  Created on January 23, 2002, 12:05 PM
 */
package com.xsb.xj;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.XJException;

import java.io.Serializable;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *@author    tanya
 *@version
 */
@SuppressWarnings("serial")
public class LazyComboListModel extends XJAbstractListModel implements ComboBoxModel<Object>, Serializable {

    protected EventListenerList listenerList  = new EventListenerList();
    PrologEngine engine;
    int rowCountCache;// -1 means invalid row count
    Vector<TermModel> rowTermsCache;// size zero means empty cache...
    TermModel T, G, originalContext, context;
    String iAndRgoal;

    /**
     * Creates new XJComboBoxModel
     *
     *@param engine   Description of the Parameter
     *@param gt       Description of the Parameter
     *@param cellGTs  Description of the Parameter
     */
    public LazyComboListModel(PrologEngine engine, GUITerm gt, GUITerm[] cellGTs) {
        super(gt, cellGTs);
        // System.out.println("gt -"+gt);
        this.engine = engine;
        TermModel lazy  = gt.findProperty("lazy");
        if(!(lazy.node.equals("lazy")) || lazy.getChildCount() != 3) {
            throw new XJException("bad lazy term:" + lazy);
        }
        T = (TermModel) lazy.getChild(0);
        G = (TermModel) lazy.getChild(1);
        originalContext = (TermModel) lazy.getChild(2);
        context = null;
        iAndRgoal = "javaMessage(" + engine.registerJavaObject(this) + ",invalidateAndRefresh)";
        checkSortProperty();
        invalidateCache();
    }

    public synchronized void setContext(TermModel c) {
        context = c;
        invalidateAndRefresh();
    }

    public TermModel getContext() {
        return context;
    }

    // cache management
    synchronized void invalidateCache() {
        rowCountCache = -1;
        rowTermsCache = new Vector<TermModel>();
    }

    public void invalidateAndRefresh() {
//		System.out.println("Let's invalidate and refresh a combo box");
//		fireWillRefresh(); // here and not after, to make sure we get old selected values before a change
        invalidateCache();
        fireContentsChanged(this, 0, getSize());
//		fireDidRefresh();
    }

    private TermModel getSelectedTerm() {
        return gt.getTermModel();
    }

    public TermModel getTerm(int row) {
        // IRRELEVANT: getRowCount(); // make sure we have a row count AND XSB tables are loaded
        if(row >= rowTermsCache.size()) {
            // cache does not contain row r
            int newLast       = Math.min(row + REFRESH_PAGE, getRowCount()) - 1;
            int first         = rowTermsCache.size();
            TermModel[] rows  = (TermModel[]) engine.deterministicGoal(
                "recoverTermModels([Gmodel,Tmodel,OCmodel],[G,T,OC]), " +
                (context == null ? "(var(OC)->OC=null;true), " : "recoverTermModel(Cmodel,C), C=OC, ") +
                (sorted ? "get_sort_term(SortIndexListModel, G, SortTerm), " : "SortTerm = none,") +
                "get_cached_solution_models(G,T,SortTerm," + iAndRgoal + "," + first + "," + newLast + ",Array)",
                "[Gmodel,Tmodel,OCmodel,Cmodel,SortIndexListModel]", new Object[]{G, T, originalContext, context, getSortTerm()},
                "[Array]"
                )[0];
            if(rows.length != (newLast - first + 1)) {
                throw new XJException("Inconsistent row page fetch");
            }
            for(int i = 0; i < rows.length; i++) {
                rowTermsCache.addElement(rows[i]);
            }
        }
        return rowTermsCache.elementAt(row);
    }

    public int getRowCount() {
        if(rowCountCache == -1) {
            // System.out.println("Computing rowCount in PrologCachedListModel...");
            Integer N  = (Integer) engine.deterministicGoal(
                "recoverTermModels([Gmodel,OCmodel],[G,OC]), " +
                (context == null ? "(var(OC)->OC=null;true), " : "recoverTermModel(Cmodel,C), C=OC, ") +
                (sorted ? "get_sort_term(SortIndexListModel, G, SortTerm)," : "SortTerm = none,") +
                " cache_count(G,SortTerm," + iAndRgoal + ",N), ipObjectSpec('java.lang.Integer',Int,[N],_)",
                "[Gmodel,OCmodel,Cmodel,SortIndexListModel]", new Object[]{G, originalContext, context, getSortTerm()},
                "[Int]"
                )[0];
            rowCountCache = N.intValue();
            // System.out.println("...computed:"+rowCountCache);
        }
        return rowCountCache;
    }

    static final int REFRESH_PAGE = 30;

    public boolean setTerm(int row, TermModel value) {
        //System.out.println("in setTerm row "+new Integer(row)+" value "+value);
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
        TermModel selectedT  = getSelectedTerm();
        if((selectedT != null) ||
            (selectedT == null && anObject != null)) {
            gt.assign((TermModel) anObject);
        }
    }


    public void removeListDataListener(javax.swing.event.ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }

    protected void fireContentsChanged(Object source, int index0, int index1) {
        Object[] listeners  = listenerList.getListenerList();
        ListDataEvent e     = null;

        for(int i = listeners.length - 2; i >= 0; i -= 2) {
            if(listeners[i] == ListDataListener.class) {
                if(e == null) {
                    e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).contentsChanged(e);
            }
        }
    }

    protected void fireIntervalAdded(Object source, int index0, int index1) {
        Object[] listeners  = listenerList.getListenerList();
        ListDataEvent e     = null;

        for(int i = listeners.length - 2; i >= 0; i -= 2) {
            if(listeners[i] == ListDataListener.class) {
                if(e == null) {
                    e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).intervalAdded(e);
            }
        }
    }

    protected void fireIntervalRemoved(Object source, int index0, int index1) {
        Object[] listeners  = listenerList.getListenerList();
        ListDataEvent e     = null;

        for(int i = listeners.length - 2; i >= 0; i -= 2) {
            if(listeners[i] == ListDataListener.class) {
                if(e == null) {
                    e = new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).intervalRemoved(e);
            }
        }
    }

    /*
     *  public EventListener[] getListeners(Class listenerType) {
     *  return listenerList.getListeners(listenerType);
     *  }
     *  public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
     *  return listenerList.getListeners(listenerType);
     *  }
     */
    public int getSize() {
        return this.getRowCount();
    }

    public java.lang.Object getElementAt(int param) {
        return getTerm(param);
    }

    public boolean deleteTerms(int[] indexes) {
    	return true;
    }

    public boolean deleteTerms(TermModel[] less) {
    	return true;
    }

    public boolean addTerms(TermModel[] terms) {
    	return true;
    }

    public void setSortTerm(TermModel newSortTerm) {
        System.out.println("Sorting is not yet implemented for combo boxes");
    }

    /*
     *  public void removeElement(java.lang.Object obj) {
     *  }
     *  public void insertElementAt(java.lang.Object obj, int param) {
     *  }
     *  public void addElement(java.lang.Object obj) {
     *  }
     *  public void removeElementAt(int param) {
     *  }
     */
    // see comments for PrologCachedListModel.removeOldNotifiers()
    protected synchronized void removeOldNotifiers() {
        if(rowCountCache != -1) {
            // cache is already filled
            engine.deterministicGoal("recoverTermModels([Gmodel,OCmodel],[G,OC]), " +
                (context == null ? "(var(OC)->OC=null;true), " : "recoverTermModel(Cmodel,C), C=OC, ") +
                (sorted ? "get_sort_term(SortIndexListModel, G, SortTerm), " : "SortTerm = none,") +
                "(cache_remove_notifier(G, SortTerm, " + iAndRgoal + ", cache) -> true ; true)",
                "[Gmodel,OCmodel,Cmodel,SortIndexListModel]",
                new Object[]{G, originalContext, context, getSortTerm()});
        }
    }

    public void destroy() {
        removeOldNotifiers();
        // following might be slow - unregisterJavaObject looks searches
        // through a vector of registered objects
        // to speed up - remember its int id in constructor
        // and unregister by id
        //engine.unregisterJavaObject(this);
        rowCountCache = -1;
        rowTermsCache = new Vector<TermModel>();
    }
}
