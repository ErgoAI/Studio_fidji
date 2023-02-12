package com.xsb.xj;
import java.awt.Component;
import java.util.Vector;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.XJException;

/** A LazyListModel implementation which does not trust on Prolog solution ordering, and instead uses David's
cache to access goal solutions */

@SuppressWarnings("serial")
public class PrologCachedListModel extends LazyListModel{
	String iAndRgoal; // for use by Prolog javaMessages
    boolean removed;

    public PrologCachedListModel(PrologEngine engine,Component c, GUITerm gt,GUITerm[] cellGTs){
		super(engine,c,gt,cellGTs);
		iAndRgoal = "javaMessage("+engine.registerJavaObject(this)+",invalidateAndRefresh)";
        removed = false;
    }
        
    protected void setCountSolutionsPred(String S){
    	throw new XJException("Can not (yet...) use custom row counting predicates in PrologCachedLists");
    }

    protected void setUnorderedPageFetcherPred(String S){
    	throw new XJException("Can not (yet...) use custom paging predicates in PrologCachedLists");
  	}
  	 
	public synchronized void setSortTerm(TermModel newSortTerm){
		if(newSortTerm == null){
			sorted = false;
		} else {
			sorted = true;
		}
		if(((sortTerm == null) && (newSortTerm != null)) || !sortTerm.equals(newSortTerm)){
			removeOldNotifiers();
			sortTerm = newSortTerm;
			invalidateAndRefresh();
		}
	}

	// same as super.setContext(TermModel c)
	// but removes old notifiers (with old context) in prolog cache first
	public synchronized void setContext(TermModel c){
		removeOldNotifiers();
		super.setContext(c);
	}

	protected synchronized void removeOldNotifiers(){
		if(rowCountCache != -1){ 
			// cache is already filled
			engine.deterministicGoal("recoverTermModels([Gmodel,OCmodel],[G,OC]), " +
			(context==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
			(sorted ? "get_sort_term(SortIndexListModel, G, SortTerm), ":"SortTerm = none,") +
			"(cache_remove_notifier(G, SortTerm, " + iAndRgoal + ", cache) -> true ; true)",
			"[Gmodel,OCmodel,Cmodel,SortIndexListModel]",
			new Object[]{G,originalContext,context,getSortTerm()});
		}
	}
	
	public synchronized int getRowCount(){
		if(removed){
			return 0;
		}
		if (rowCountCache==-1){
			// System.out.println("Computing rowCount in PrologCachedListModel...");
			Integer N = (Integer)engine.deterministicGoal(
			"recoverTermModels([Gmodel,OCmodel],[G,OC]), " +
			(context==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
			(sorted ? "get_sort_term(SortIndexListModel, G, SortTerm), ":"SortTerm = none,") +
			"cache_count(G,SortTerm,"+iAndRgoal+",N), ipObjectSpec('java.lang.Integer',Int,[N],_)" ,
			"[Gmodel,OCmodel,Cmodel,SortIndexListModel]",
			new Object[]{G,originalContext,context,getSortTerm()},
			"[Int]"
			)[0];
			rowCountCache = N.intValue();
			// System.out.println("...computed:"+rowCountCache);
		}
		return rowCountCache;
	}

	// similar to DefaultTableModel:
	// throws ArrayIndexOutOfBoundsException - if an invalid row was given
	public synchronized TermModel getTerm(int r){
		if(removed){
			return null;
		}
		// IRRELEVANT: getRowCount(); // make sure we have a row count AND XSB tables are loaded
		int mapKeyInt = (int)(Math.floor(((double)r)/((double)REFRESH_PAGE)));
		Integer mapKey = new Integer(mapKeyInt);
		Vector<TermModel> cacheVector = rowTermsCache.get(mapKey);
		if(cacheVector == null){
			// cache does not contain row r
			int first = mapKeyInt*REFRESH_PAGE;
			int newLast = Math.min(first+REFRESH_PAGE,getRowCount()) - 1;
			TermModel[] rows = (TermModel[])engine.deterministicGoal(
			"recoverTermModels([Gmodel,Tmodel,OCmodel],[G,T,OC]), "+
			(context==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
			(sorted ? "get_sort_term(SortIndexListModel, G, SortTerm), ":"SortTerm = none,") +
			"get_cached_solution_models(G,T,SortTerm,"+iAndRgoal+","+first+","+newLast+",Array)",
			"[Gmodel,Tmodel,OCmodel,Cmodel,SortIndexListModel]", 
			new Object[]{G,T,originalContext,context,getSortTerm()},
			"[Array]"
			)[0];
			if (rows.length!=(newLast-first+1))
				throw new XJException("Inconsistent row page fetch");
			cacheVector = new Vector<TermModel>(REFRESH_PAGE);
			for(int i=0; i<rows.length; i++)
				cacheVector.addElement(rows[i]);
			rowTermsCache.put(mapKey,cacheVector);
		}
		return cacheVector.elementAt(r-mapKeyInt*REFRESH_PAGE);
	}
	
	/** This method does not invoke invalidateAndRefresh directly: the Prolog cache does it */
	public boolean setTerm(int row,TermModel value){
		return doSetTerm(row,value);
	}
	/** This method does not invoke invalidateAndRefresh directly: the Prolog cache does it */
	public boolean deleteTerms(TermModel[] terms){
		return doDeleteTerms(terms);
	}
	/** This method does not invoke invalidateAndRefresh directly: the Prolog cache does it */
	public boolean addTerms(TermModel[] terms){
		return doAddTerms(terms);
	}
        
	public void destroy(){
		removeOldNotifiers();
		synchronized(this){
			removed = true;
		}
		// following might be slow - unregisterJavaObject looks searches 
		// through a vector of registered objects
		// to speed up - remember its int id in constructor
		// and unregister by id
		//engine.unregisterJavaObject(this);
		super.destroy();
	}
}