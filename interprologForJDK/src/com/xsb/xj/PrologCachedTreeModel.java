package com.xsb.xj;
import java.util.Iterator;
import java.util.Vector;

import com.declarativa.interprolog.ObjectExamplePair;
import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.util.BasicTypeWrapper;
import com.xsb.xj.util.XJException;

/** A LazyTreeModel using David's Prolog cache. */
public class PrologCachedTreeModel extends LazyTreeModel {

    protected int cachedNamesRange;

	public PrologCachedTreeModel(PrologEngine engine,GUITerm gt){
            super(engine,gt);
            if(gt.findProperty(XJTree.FIXEDHEIGHT) != null){
                cachedNamesRange = 75;
            } else {
                cachedNamesRange = 500;
            }
	}
        
        public int getCachedNamesRange(){
            return this.cachedNamesRange;
        }
        
	/** Just like the superclass implementation, except that it returns a PrologCachedTreeNode */
        LazyTreeNode findRoot(){
            String goal = "recoverTermModels([ScGmodel,OCmodel],[ScG,OC]), " +
            (context==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
            "xj_discoverTreeRoot(ScG,RootID), " +
            "buildTermModel(RootID,RootIDmodel)";
            
            Object[] bindings = engine.deterministicGoal(
            goal, "[ScGmodel,OCmodel,Cmodel]",
            new Object[]{subclassGoal,originalContext,context}, "[RootIDmodel]");
            if(bindings == null){throw new XJException("Can not find tree root");}
        	PrologCachedTreeNode cachingRoot = new PrologCachedTreeNode(this, (TermModel) bindings[0],null);
            
            // Now cache the root name
            goal = "recoverTermModels([NameGmodel,OCmodel],[NameG,OC]), recoverTermModel(NodeIDModel,NodeID), goal_arg(1,NameG,NodeID)," +
            (context==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
            "once(nocache_but_notify(NameG, javaMessage("+String.valueOf(cachingRoot.getObjectNumber())+",redrawNode))), " +
            "xj_getTreeNodeType(NameG,Type), "+
            "goal_arg(3,NameG,ChildTerm), buildTermModel(ChildTerm, ChildTermModel)";
            
            bindings = engine.deterministicGoal(goal,
            "[NameGmodel,OCmodel,Cmodel,NodeIDModel]",
            new Object[]{nameGoal,originalContext,context,cachingRoot.getID()},
            "[Type,ChildTermModel]");
            if(bindings == null){throw new XJException("Can not find label for tree root");}
            cachingRoot.setType((String)bindings[0]);
            cachingRoot.setNodeTerm((TermModel) bindings[1]);
            return cachingRoot;
	}
        
        public synchronized void destroy(){
            PrologCachedTreeNode root = (PrologCachedTreeNode)getRoot();
            root.removeCacheForChildren();
	    root.removeCacheForNodeOnly();
            // System.gc();
        }
        
        public synchronized void setContext(TermModel c){
            destroy();
            context = c;
            invalidateCache();
            fireTreeStructureChanged();
            // System.gc();
        }

        public static class NodeNotifier implements java.io.Serializable{
			private static final long serialVersionUID = -7223094586706214411L;
			TermModel nodeID;
            int ind;
            int nodeRegNumber;
            
            public NodeNotifier(TermModel nodeID, int index, int nodeRegNumber){
                NodeNotifier.this.nodeID = nodeID;
                NodeNotifier.this.ind = index;
                NodeNotifier.this.nodeRegNumber = nodeRegNumber;
            }
         }

         public static ObjectExamplePair notifierExample() {
            return new ObjectExamplePair(new NodeNotifier(new TermModel(),1,0));
         }

         public static class NodeLabelInfo implements java.io.Serializable{
			private static final long serialVersionUID = 8732785192357555021L;
		//TermModel nodeID;
            int ind;
            TermModel nodeTerm;
            String nodeType;
            Boolean isLeaf;
         }

         public static ObjectExamplePair labelExample() {
            return new ObjectExamplePair(new NodeLabelInfo());
         }

         /** A LazyTreeNode using the Prolog cache */
    public static class PrologCachedTreeNode extends LazyTreeModel.LazyTreeNode{
    	/** The superclass uses cache ranges, this one doesn't */
    	protected Vector<LazyTreeNode> childrenCache;
        public static final int IP_UNREGISTERED = -1;
        public static final int IP_DELETED = -2;
        // for use by Prolog javaMessages
        // objectNumber == -1 : not yet registered in interprolog
        // objectNumber == -2 : unregistered from interprolog as a result of invalidation of a parent, to be deleted
        // objectNumber > -1 : registered
        int objectNumber;
            
        PrologCachedTreeNode(LazyTreeModel model, TermModel nodeID, LazyTreeNode parent){
            super(model,nodeID,parent);
            objectNumber=IP_UNREGISTERED;
            childrenCache = null;
        }
            
            public int getObjectNumber(){
                if(objectNumber==IP_UNREGISTERED){
                    objectNumber = getEngine().registerJavaObject(this);
                }
                return objectNumber;
            }
            
            public void setObjectNumber(int objectNumber){
                this.objectNumber = objectNumber;
            }
               
            synchronized int getChildCount(){
                // System.out.println("Getting count for node " + nodeID);
                if(getObjectNumber() == IP_DELETED){
                    childCountCache = 0;
                    System.out.println("Warning: Getting count for deleted node " + nodeID);
                } else if (childCountCache==-1) {
                    //System.out.println("Prolog call");
                    String goal = "recoverTermModels([ScGmodel,OCmodel],[ScG,OC]), recoverTermModel(NodeIDModel,NodeID), goal_arg(2,ScG,NodeID), " +
                    (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                    ((myModel.isSorted())? "get_sort_term(SortIndexListModel, ScG, SortTerm), ":"SortTerm = none,") +
                    "cache_count(ScG,SortTerm,javaMessage("+String.valueOf(getObjectNumber())+",invalidateAndRefresh),Nchildren)," +
                    "ipObjectSpec('int',ChildCount,[Nchildren],_)";
                    
                    Object[] bindings = (Object[])getEngine().deterministicGoal(
                    goal,"[ScGmodel,OCmodel,Cmodel,NodeIDModel,SortIndexListModel]",
                    new Object[]{getSubclassGoal(),getOC(),getC(),nodeID, myModel.getSortTerm()},
                    "[ChildCount]");
                    
                    childCountCache = ((Integer)((com.declarativa.interprolog.util.BasicTypeWrapper)bindings[0]).wrapper).intValue();
                    
                }
                return childCountCache;
            }
            
            synchronized boolean isLeaf(){
                if(getObjectNumber() == IP_DELETED){
                    //Java Tree itself caches nodes also,
                    //might call isLeaf() from there after invalidation
                    //System.out.println("Warning: checking isLeaf for deleted node " + nodeID);
                    return true;
                } else if (childCountCache==-1) {
                    // children cache for the node is not filled yet
                    if(isLeafNode == null) { // no information available yet
                        // do not get all the children (might take a long time),
                        // juts check whether they exist
                        String goal =
                        "recoverTermModels([ScGmodel,OCmodel],[ScG,OC]), recoverTermModel(NodeIDModel,NodeID), " +
                        "goal_arg(2,ScG,NodeID), " +
                        (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                        "GoalFlag = '$xj_scgoalsucceeded'(0), " +
                        "(once(ScG), machine:term_set_arg(GoalFlag,1,1,1), fail ; true), " +
                        "prologCache:just_notify(ScG, javaMessage("+String.valueOf(getObjectNumber())+",invalidateAndRefresh)), "+
                        " GoalFlag = '$xj_scgoalsucceeded'(1)";
                        
                        boolean hasChildren = getEngine().deterministicGoal(
                        goal,"[ScGmodel,OCmodel,Cmodel,NodeIDModel]",
                        new Object[]{getSubclassGoal(),getOC(),getC(),nodeID}
                        );
                        this.isLeafNode = new Boolean(!hasChildren);
                    }
                    return isLeafNode.booleanValue();
            } else {
                // get the number from cache without calling Prolog
                return getChildCount()==0;
            }
        }
        // Redefine the superclass method, as this does NOT use cache ranges. This is identical to the older (pre range caching, r107) method in the superclass  
        synchronized int getIndexOfChild(Object child){
            int index;
            if((childrenCache != null) && (child != null)){
                index = childrenCache.indexOf(child);
            } else {
                index = -1;
            }
            return index;
        }
	public void redrawNode(){
                if(getObjectNumber() != IP_DELETED){
                    // System.out.println("Let's redrawNode "+this);
                    String goal = "recoverTermModels([NameGmodel,OCmodel],[NameG,OC]), recoverTermModel(NodeIDModel,NodeID), "+
                    "goal_arg(1,NameG,NodeID), " +
                    (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                    "once(nocache_but_notify(NameG, javaMessage("+String.valueOf(this.getObjectNumber())+",redrawNode))), " +
                    "xj_getTreeNodeType(NameG,NodeTypeModel), goal_arg(3,NameG,NodeTerm)," +
                    "(NodeTerm=null->NodeTermModel=null;buildTermModel(NodeTerm,NodeTermModel))";
                    
                    Object[] bindings = (Object[])getEngine().deterministicGoal(
                    goal,"[NameGmodel,OCmodel,Cmodel,NodeIDModel]",new Object[]{getNameGoal(),getOC(),getC(),this.nodeID},
                    "[NodeTypeModel, NodeTermModel]");
                    if(bindings != null){ // if such node still exists
                        setType((String)bindings[0]);
                        setNodeTerm((TermModel) bindings[1]);
                        
                        if (myModel.hasPolimorphicNodes && this.nodeType==null)
                            throw new XJException("Missing type for nodeID "+this.nodeID);
                        if(this.nodeTermCache == null){
                            throw new XJException("Missing label for nodeID "+this.nodeID);
                        }
                    }
                    fireSubTreeNodesChanged();
                } else {
                    System.out.println("Warning: calling redrawNode for deleted node " + nodeID);
                }
            }

            /** Differs from the superclass in that the tree relation goal is called through the Prolog cache; first a solution
             * count is requested, effectively filling the cache, and then all solutions are fetched, collecting all children of this node */
            synchronized void fillChildrenCache(){
                // System.out.println("Filling Java-side cache for node "+this);
                if(this.objectNumber != IP_DELETED){
                    getChildCount();
                    childrenCache = new Vector<LazyTreeNode>(childCountCache);
                    childrenCache.setSize(childCountCache);
                    int range = 500;
                    
                    for(int lowerBound = 0; lowerBound < childCountCache; lowerBound+= range){
                        int size = ((lowerBound + range) > childCountCache)?
                        (childCountCache - lowerBound) : range;
                        
                        fillChildrenCache(lowerBound, lowerBound + size - 1);
                        
                    }
                    
                } else {System.out.println("Warning: fillChildrenCache for deleted object"+nodeID);}
            }
                
            /**
             * Populates children of this node (their ids) in range [lowerBound, upperBound]
             */
            private void fillChildrenCache(int lowerBound, int upperBound){
                
                // System.out.println("Filling Java-side cache for node "+this+" in range ["+lowerBound +", "+upperBound+"] ");
                String goal = "recoverTermModels([ScGmodel,OCmodel],[ScG,OC]), recoverTermModel(NodeIDModel,NodeID), " +
                "goal_arg(2,ScG,NodeID), " +
                (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                ((myModel.isSorted())? "get_sort_term(SortIndexListModel, ScG, SortTerm), ":"SortTerm = none,") +
                "findall(ChildID,("+
                "cache_range(ScG,SortTerm,javaMessage("+String.valueOf(getObjectNumber())+",invalidateAndRefresh),"+String.valueOf(lowerBound+1)+","+String.valueOf(upperBound+1)+")," +
                "goal_arg(1,ScG,ChildID)),L), "+
                "buildTermModelArray(L,Children)";
                
                Object[] children = (Object[])getEngine().deterministicGoal(goal,
                "[ScGmodel,OCmodel,Cmodel,NodeIDModel,SortIndexListModel]",
                new Object[]{getSubclassGoal(),getOC(),getC(),nodeID,myModel.getSortTerm()},
                "[Children]")[0];
                
                if (children.length != (upperBound - lowerBound + 1))
                    throw new XJException("Inconsistency filling PrologCachedTreeNode cache: "+children.length+"!="+(upperBound - lowerBound + 1)+" in node "+this);
                
                for (int i = 0 ; i < children.length ; i++) {
                    TermModel c = (TermModel)children[i];
                    childrenCache.setElementAt(new PrologCachedTreeNode(myModel,c,this), i + lowerBound);
                }
            }
            
            /**
             * Populates names of children of this node in range [lowerBound, upperBound]
             */
            private void fillChildrenCacheNames(int lowerBound, int upperBound) {
                // name cache for the range is not filled up yet, so get children names first
                NodeNotifier [] nodes=new NodeNotifier[upperBound - lowerBound + 1];
                for(int i=0; i < nodes.length; i++){
                    PrologCachedTreeNode anode = ((PrologCachedTreeNode)childrenCache.elementAt(i + lowerBound));
                    nodes[i] = new NodeNotifier(anode.getID(), i + lowerBound, anode.getObjectNumber());
                }
                
                String goal =
                "recoverTermModels([NameGmodel,ScGmodel,OCmodel],[NameG,ScG,OC])," +
                (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                " getTreeLabels(NameG,ScG,NodesModel,Children)";
                Object[] bindings = (Object[])getEngine().deterministicGoal(
                goal,"[NameGmodel,ScGmodel,OCmodel,Cmodel,NodesModel]",
                new Object[]{getNameGoal(),getSubclassGoal(),getOC(),getC(),nodes},
                "[Children]")[0];
                
                for(int i = 0 ; i < bindings.length ; i++){
                    NodeLabelInfo label = (NodeLabelInfo)bindings[i];
                    LazyTreeNode element = (LazyTreeNode)childrenCache.elementAt(label.ind);
                    element.setType(label.nodeType);
                    element.setNodeTerm(label.nodeTerm);
                    element.setLeafNode(label.isLeaf);
                    if (myModel.hasPolimorphicNodes && (element.nodeType == null))
                        throw new XJException("Missing type for nodeID " + element.nodeID);
                    if(element.nodeTermCache == null){
                        throw new XJException("Missing label for nodeID " + element.nodeID);
                    }
                }
            }

                /**
                 * Will throw ArrayIndexOutOfBoundsException if index is incorrect
                 */
                synchronized Object getChild(int index){
                    if(getObjectNumber() == IP_DELETED){
                        System.out.println("Warning: getChild for deleted node "+nodeID);
                        return null;
                    }
                    if (childrenCache == null) {
                        getChildCount(); 
                        childrenCache = new Vector<LazyTreeNode>(childCountCache);
                        childrenCache.setSize(childCountCache);
                    }
                    LazyTreeNode child = (LazyTreeNode)childrenCache.elementAt(index);
                    if(child == null){
                        int cachingRange = ((PrologCachedTreeModel)myModel).getCachedNamesRange();
                        // lets increase range for subclass goal, 
                        // since cachingRange is the range for names,
                        // but process of populating children is faster
                        // (need to execute just one subclass call, not many label calls)
                        // subclass range should, however, be divisible by (cover) name range
                        int subclassRange = greatestNumberDivisibleBy(500, cachingRange); //might increase or decrease chunk limit of 500 if necessary
                        int rangeCount = index / subclassRange; // int division
                        int rangeStart = subclassRange * rangeCount;
                        int size = ((rangeStart + subclassRange) > childCountCache)?
                                (childCountCache - rangeStart) : subclassRange;
                        fillChildrenCache(rangeStart, rangeStart + size - 1);

                    }
                    child = (LazyTreeNode)childrenCache.elementAt(index);
                    if((child.nodeTermCache == null) || (myModel.hasPolimorphicNodes && child.nodeType==null)){
                        
                        // names for children in that range were not cached yet
                        // find the range to cache
                        int cachingRange = ((PrologCachedTreeModel)myModel).getCachedNamesRange();
                        int rangeCount = index/cachingRange; // int division
                        int rangeStart = cachingRange*rangeCount;
                        int size = ((rangeStart + cachingRange) > childCountCache)? 
                                   (childCountCache - rangeStart) : cachingRange ;

                        fillChildrenCacheNames(rangeStart, rangeStart + size - 1);
                        
                    }
                   return child;
                }
                
                /**
                 * Returns the greatest number below the limit
                 * that is divisible by div (limit and div are positive)
                 * If that number is 0, returns div
                 */
                private int greatestNumberDivisibleBy(int limit, int div){
                    if((div < limit) && (div != 0)){
                        int count = limit/div;
                        return div*count;
                    } else {
                        return div;
                    }
                }
              

        /**
         * Using LazyTreeModel's getChildByNodeID() method would mean
         * loading all children nodes into Java side in case the node is not found (for example, after deletion)
         * Using prologCache to get the node:
         * check nodes already cached  in Java
         * if not found, first find out the index of the node,
         * then getChild(index) - to cache only that area
         */
        LazyTreeNode getChildByNodeID(TermModel childNodeID){
            // first try to find among already cached nodes
            synchronized(this){
            if(childrenCache != null){
                for(Iterator<LazyTreeNode> i = childrenCache.iterator(); i.hasNext(); ){
                    LazyTreeNode next = (LazyTreeNode)i.next();
                    if(next != null){
                        if(next.getID().equals(childNodeID)){
                            return next;
                        }
                    }
                }
            }
            }
            BasicTypeWrapper intWrapper;
            synchronized(this){
                if(getObjectNumber() == IP_DELETED){
                    System.out.println("Warning: getChildIndex for deleted node "+nodeID);
                    return null;
                }
                String goal = "recoverTermModels([ScGmodel,OCmodel], [ScG,OC]), "+
                "recoverTermModel(NodeIDModel, NodeID), " +
                "recoverTermModel(ChildNodeIDModel, ChildNodeID), " +
                (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                ((myModel.isSorted())? "get_sort_term(SortIndexListModel, ScG, SortTerm), ":"SortTerm = none,") +
                "find_child_index(ScG, SortTerm, NodeID, ChildNodeID, Index)";
                
                intWrapper = (BasicTypeWrapper)getEngine().deterministicGoal(goal,
                "[ScGmodel,OCmodel,Cmodel,NodeIDModel,ChildNodeIDModel,SortIndexListModel]",
                new Object[]{getSubclassGoal(),getOC(),getC(),nodeID,childNodeID,myModel.getSortTerm()},
                "[Index]")[0];
            }
            if(intWrapper != null){
                Integer indexInteger = (Integer)intWrapper.getObject();
                if(indexInteger != null){
			int index = indexInteger.intValue();
                        // System.out.println("Index "+ (index-1));
                        if(index > 0){
                            LazyTreeNode node;
                            try{
                                node = (LazyTreeNode)getChild(index - 1);
                                if(!node.getID().equals(childNodeID)){
                                    // prolog cache has changed between finding index and getting a child
                                    node = null;
                                }
                            } catch(ArrayIndexOutOfBoundsException e){
                                // prolog cache has changed between finding index and getting a child
                                return null;
                            }
                            return node;
                        }
                }
            }
            
            return null;
        }
 
	    synchronized void invalidateSubtreeCaches(){
        	removeCacheForChildren();
            if (childrenCache!=null){
                for (int c=0;c<childrenCache.size();c++){
                    LazyTreeNode node = (LazyTreeNode)childrenCache.elementAt(c);
                    if(node != null){
                        node.invalidateSubtreeCaches();
                    }
                }
            }
	        childrenCache = null;
	        childCountCache = - 1;
	        isLeafNode = null;
	    }

      protected void removeCacheForNodeOnly(){
            if(getObjectNumber() != IP_DELETED){
                removeScGNotifiers();
                // remove name and leaf info cache
                String goal =
                "recoverTermModels([NameGmodel,ScGmodel,OCmodel],[NameG,ScG,OC])," +
                (getC() == null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                "recoverTermModel(NodeIDModel,NodeID), goal_arg(1,NameG,NodeID), "+
                "(cache_remove_notifier(NameG, none, javaMessage("+String.valueOf(getObjectNumber())+",redrawNode), nocache) -> true ; true), "+
                "goal_arg(2,ScG,NodeID), " +
                "(cache_remove_notifier(ScG, none, javaMessage("+String.valueOf(getObjectNumber())+", invalidateAndRefresh), nocache) -> true ; true)";
                getEngine().deterministicGoal(goal,
                "[NameGmodel,ScGmodel,OCmodel,Cmodel,NodeIDModel]",
                new Object[]{getNameGoal(),getSubclassGoal(),getOC(),getC(),getID()},
                "[]");
                
                // remove pointer from interprolog cache
                getEngine().unregisterJavaObject(this);
                setObjectNumber(IP_DELETED);
            }
        }

                
        protected void removeCacheForChildren(){
            if (childrenCache != null) {
                
                // find children for which name cache is filled
                Vector<NodeNotifier> tempNodeNotifiers = new Vector<NodeNotifier>();
                int index = 0;
                for (Iterator<LazyTreeNode> i = childrenCache.iterator(); i.hasNext(); ){
                    PrologCachedTreeNode child = (PrologCachedTreeNode)i.next();
                    if(child != null){
                        synchronized(child){
                        if((child.nodeTermCache != null) && (child.getObjectNumber() != IP_DELETED)){
                            tempNodeNotifiers.add(new NodeNotifier(child.getID(), index, child.getObjectNumber()));
                            if(child.removeScGNotifiers()){ 
                                child.removeCacheForChildren();
                            }
                        }
                        }
                    }
                    index++;
                }
                
                int cachingRange = ((PrologCachedTreeModel)myModel).getCachedNamesRange();
                for(int j = 0; j < tempNodeNotifiers.size(); j+= cachingRange){
                    int size = ((j + cachingRange) > tempNodeNotifiers.size())?
                                (tempNodeNotifiers.size() - j) : cachingRange;
                        NodeNotifier [] nodes = new NodeNotifier[size];
                        for(int k = 0; k < size; k++){
                            nodes[k] = tempNodeNotifiers.elementAt(j+k);
                        }
                        
                        String goal =
                        "recoverTermModels([NameGmodel,ScGmodel,OCmodel],[NameG,ScG,OC])," +
                        (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                        " removeTreeLabelCache(NameG,ScG,NodesModel)";
                        getEngine().deterministicGoal(goal,
                        "[NameGmodel,ScGmodel,OCmodel,Cmodel,NodesModel]",
                        new Object[]{getNameGoal(),getSubclassGoal(),getOC(),getC(),nodes},
                        "[]");
                }
                for (Iterator<LazyTreeNode> i = childrenCache.iterator(); i.hasNext(); ){
                    PrologCachedTreeNode child = (PrologCachedTreeNode)i.next();
                    if(child != null){
                        if(child.nodeTermCache != null){
                            if(child.objectNumber >= 0){
                                getEngine().unregisterJavaObject(child.objectNumber);
                                child.setObjectNumber(IP_DELETED);
                            }
                        }
                    }
                }
            }
        }
        
        /* 
         * Removes a notifier for cached subclass goal for a node. That
         * notifier is set when the node gets a list of its children 
         * (i.e. through fillChildrenCache())
         * Succeeds if notifier was in cache and removed, fails if notifier
         * was not in cache (may be was removed before)
         **/
        protected boolean removeScGNotifiers(){ 
            if(childCountCache != -1){
                // cache is already filled
                return getEngine().deterministicGoal("recoverTermModels([ScGmodel,OCmodel],[ScG,OC]), recoverTermModel(NodeIDModel,NodeID), goal_arg(2,ScG,NodeID), " +
                (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                ((myModel.isSorted()) ? "get_sort_term(SortIndexListModel, ScG, SortTerm), ":"SortTerm = none,") +
                "cache_remove_notifier(ScG, SortTerm, javaMessage("+ String.valueOf(getObjectNumber())+",invalidateAndRefresh), cache)",
                "[ScGmodel,OCmodel,Cmodel,NodeIDModel,SortIndexListModel]",
                new Object[]{getSubclassGoal(),getOC(),getC(),nodeID, myModel.getSortTerm()});
            } else return false;
        }
        
        protected void finalize(){
          // System.out.println("Finalize obj "+this);
        }
    }
}