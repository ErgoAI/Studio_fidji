package com.xsb.xj;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.LazyRefreshListener;
import com.xsb.xj.util.XJException;

/** A TreeModel for XJ lazy trees, based on a tree of LazyTreeNodes. The tree cache filling is actually
 * managed by LazyTreeNode, so improved implementations (e.g. with an explicit cache on the Prolog side)
 * should probably subclass both this class and LazyTreeNode. */
public class LazyTreeModel implements TreeModel{
    protected Vector<TreeModelListener> treeListeners = null;
    PrologEngine engine;
    /** Cache for the root of the tree, discovered automatically; root==null means unknown/invalid root */
    LazyTreeNode root;
	/** This defines the tree structural (binary) relationship. It is assumed to have at least 2 arguments: 
	nodeID, parentNodeID */
    TermModel subclassGoal;
	/** This defines the tree node relationship, specifying node details. It is assumed to have at least 3 arguments: 
	NodeID, NodeType, NodeTerm */
    TermModel nameGoal;
	/** originalContext is the tree context/parameter term. It may share variables with both goals.*/
    TermModel originalContext;
    /** the current context */
    TermModel context;
    boolean hasPolimorphicNodes;
    final boolean shouldShowRoot;
    protected Vector<LazyRefreshListener> refreshListeners;
        
    protected boolean sorted = false;
    protected TermModel sortTerm = null;
    static final int REFRESH_PAGE = 60;
    
    /** Future feature:
    If DRY is true, the model will help the UI not to repeat itself, e.g. will serve values annotated with the path of the original */
    public LazyTreeModel(PrologEngine engine,GUITerm gt){
        this.engine=engine;
        TermModel lazy = null;
        if(gt.getChildCount() > 0){
            lazy = (TermModel)gt.getChild(0);
            // it might also be defined in properties like for XJTreeWithSelections
            if(!(lazy.node.equals("lazy"))){
                lazy = gt.findProperty("lazy");
            }
        } else {
            lazy = gt.findProperty("lazy");
        }
        
        if(lazy == null) {
            throw new XJException("missing lazy term");
        }
        if (lazy.getChildCount()!=3){
            throw new XJException("bad lazy term:"+lazy);
        }
        // Check whether the goals are wrapped Flora:
        subclassGoal = GUITerm.floraPreprocessWithArgs((TermModel)lazy.getChild(0),3);
        nameGoal = GUITerm.floraPreprocessWithArgs((TermModel)lazy.getChild(1),3);
        
        originalContext = (TermModel)lazy.getChild(2);
        context=null;
        if (gt.lazyTreeTemplate().isList()) hasPolimorphicNodes=true;
        else hasPolimorphicNodes=false;
        shouldShowRoot = gt.findProperty(GUITerm.HIDDENROOT)==null;
        root = null;
        refreshListeners = new Vector<LazyRefreshListener>();
        setSortedProperty(gt);   
    }
    
	public void invalidateAndRefresh(){
		((LazyTreeNode)getRoot()).invalidateAndRefresh();
	}

    public boolean isSorted(){
        return sorted;
    }
    
    public TermModel getSortTerm(){
        return this.sortTerm;
    }

    protected void setSortedProperty(GUITerm gt){
        TermModel sortedProperty = gt.findProperty("sorted");
        if(sortedProperty != null){
            sorted = true;
            if(sortedProperty.getChildCount() == 1){
                sortTerm = ((TermModel)sortedProperty.getChild(0));
                if(!sortTerm.isList()){
                    System.out.println("Incorrect sort specification: " +
                    "expecting list of form [asc(Index1), desc(Index2), ...]");
                }
            } else if(sortedProperty.getChildCount() == 2){
                String sortDirection = (String)((TermModel)sortedProperty.getChild(0)).node;
                Integer sortIndex = (Integer)((TermModel)sortedProperty.getChild(1)).node;
                sortTerm = buildSortTerm(sortDirection, sortIndex);
            } else {
                sortTerm = buildSortTerm(new String("asc"), new Integer(3));
            }
        }
    }
    
    protected TermModel buildSortTerm(String sortDirection, Integer sortIndex){
        TermModel indexModel = new TermModel(sortIndex);
        TermModel sortTermModel = new TermModel(sortDirection, new TermModel[]{indexModel});
        return new TermModel(".", new TermModel[]{sortTermModel, new TermModel("[]", true)}, true);
    }

    /** Discover the root for the current tree definition */
    LazyTreeNode findRoot(){
        String goal = "recoverTermModels([ScGmodel,NameGModel,OCmodel],[ScG,NameG,OC]), " +
        (context==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
        "xj_discoverTreeRoot(ScG,NameG,RootID,RootTypeObj,RootTerm), " +
        "buildTermModel(RootID,RootIDmodel), (RootTerm=null->RootTermModel=null;buildTermModel(RootTerm,RootTermModel))";
        Object[] bindings = engine.deterministicGoal(
        goal,
        "[ScGmodel,NameGModel,OCmodel,Cmodel]",
        new Object[]{subclassGoal,nameGoal,originalContext,context},
        "[RootIDmodel,RootTypeObj,RootTermModel]"
        );
        if (bindings==null)
        	throw new XJException("Failed to find tree root for context "+context);
        LazyTreeNode R = new LazyTreeNode(this, (TermModel) bindings[0], (TermModel)bindings[2], -1, (String)bindings[1], null);
        //System.err.println("findRoot:"+R);
        return R;
    }
    
    static TreePath findPath(LazyTreeNode node){
    	ArrayList<LazyTreeNode> P = new ArrayList<LazyTreeNode>();
    	P.add(node);
    	while(node.parent!=null){
    		node=node.parent;
    		P.add(node);
    	}
    	TreePath path = new TreePath(P.get(P.size()-1));
    	for (int n=P.size()-2; n>=0; n--)
    		path = path.pathByAddingChild(P.get(n));
    	return path;
    }
    
    // Swing TreeModel methods:
    
    public Object getRoot()	{
        if (root==null) root = findRoot();
        return root;
    }
    public Object getChild(Object parent,int index) {
    	LazyTreeNode X = (LazyTreeNode)((LazyTreeNode)parent).getChild(index);
    	return X;
    }
    public int getChildCount(Object parent){
        return ((LazyTreeNode)parent).getChildCount();
    }
    public boolean isLeaf(Object node){
        return ((LazyTreeNode)node).isLeaf();
    }
    public void valueForPathChanged(TreePath path,Object newValue) {
        // System.out.println("valueForPathChanged:"+newValue);
    }
    public int getIndexOfChild(Object parent,Object child){
        if(parent == null){
            return -1;
        } else {
            return ((LazyTreeNode)parent).getIndexOfChild(child);
        }
    }
    public void addTreeModelListener(TreeModelListener l){
        if (treeListeners==null) treeListeners = new Vector<TreeModelListener>();
        treeListeners.addElement(l);
        // System.out.println("added tree model listener "+l);
    }
    public void removeTreeModelListener(TreeModelListener l){
        if (!treeListeners.removeElement(l))
            throw new XJException("Bad removal of listener");
    }
    
    public void fireTreeStructureChanged(){
        TreeModelEvent e = new TreeModelEvent(this, new Object[] {getRoot()});
        fireTreeStructureChanged(e);
    }
    
    public void fireTreeStructureChanged(TreeModelEvent e){
        for (int l=0;l<treeListeners.size();l++)
            (treeListeners.elementAt(l)).treeStructureChanged(e);
    }

    public void fireTreeNodesChanged(TreeModelEvent e){
        for (int l=0;l<treeListeners.size();l++)
            treeListeners.elementAt(l).treeNodesChanged(e);
    }

    public void invalidateCache(){
        if (root!=null) root.invalidateSubtreeCaches();
        root = null;
    }
    
    public TermModel getContext(){
        return context;
    }
    
    public void setContext(TermModel c){
        context = c;
        invalidateCache();
        fireTreeStructureChanged();
    }
    
	public void addLazyRefreshListener(LazyRefreshListener l){
		refreshListeners.addElement(l);
	}
	
	public void removeLazyRefreshListener(LazyRefreshListener l){
		refreshListeners.removeElement(l);
	}
	
	protected void fireWillRefresh(){
		for(int i=0;i<refreshListeners.size();i++)
			refreshListeners.elementAt(i).willRefresh();
	}
	
	protected void fireDidRefresh(){
		for(int i=0;i<refreshListeners.size();i++)
			refreshListeners.elementAt(i).didRefresh();
	}
	
	public TreePath getPathToNode(TermModel[] nodeIDpath){
		/* now nodes are specified by their NodeID paths, so this is no longer needed:
		String goal = "recoverTermModels([ScGmodel,OCmodel],[ScG,OC]), recoverTermModel(NodeIDModel,NodeID), " +
		(context==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), ") +
		"xjTreePathFromRoot(NodeID,ScG,OC,C,Path), buildTermModelArray(Path,PathObject)";
		
		// get path of NodeIDs:
		Object[] bindings = engine.deterministicGoal(
			goal,
			"[ScGmodel,OCmodel,Cmodel,NodeIDModel]",
			new Object[]{subclassGoal,originalContext,context,nodeID},
			"[PathObject]"
			);
		if (bindings==null) return null; // Node not (possibly no longer...) in tree
		TermModel[] path = (TermModel[])bindings[0];
		if (path.length<1) throw new XJException("Bad result from xjTreePathFromRoot/5");
		// get LazyTreeNode path, expanding the model scope if necessary:
		*/
		TreePath tp = ((LazyTreeNode)getRoot()).buildExpandPath(nodeIDpath);
		return tp;
	}
	    
    /** The Java representation for a node in a XJ tree. Instances of this class will be
     * passed to our own tree cell editor and renderer.
     * Tree nodes are fetched lazily from Prolog as they're needed, one children group range at a time.
     * The current caching policy assumes constant ordering of Prolog solutions,
     * e.g. no abolish tables during the life of this model */
    public static class LazyTreeNode{
        LazyTreeModel myModel;
        protected TermModel nodeID;
        protected String nodeType; // null for mono-typed trees
        protected int childCountCache;  // -1: nothing in cache
        protected TermModel nodeTermCache;
        private Map<Integer,Vector<LazyTreeNode>> childrenCaches; // An ArrayList might do, but in principle a JTree may require middle ranges too
        LazyTreeNode parent;
        protected Boolean isLeafNode; // a trick to reduce number of roundtrips to Prolog
        /** Used to avoid repeated nodes; a bit of a hack with XJCellTupleRenderer */
        private TreePath originalPath = null;
        
        LazyTreeNode(LazyTreeModel model, TermModel nodeID, TermModel nodeTerm, int childCount, String nodeType,LazyTreeNode parent){
            myModel = model;
            this.nodeID=nodeID;
            this.nodeType=nodeType;
            childCountCache = childCount;
            nodeTermCache = nodeTerm;
            childrenCaches = null;
            /* premature, as in general the type is set after node creation
            if (myModel.hasPolimorphicNodes && nodeType==null)
                throw new XJException("Missing type for nodeID "+nodeID);*/
            this.parent=parent;
            this.isLeafNode = null;
        }
        
        LazyTreeNode(LazyTreeModel model, TermModel nodeID,LazyTreeNode parent){
        	this(model,nodeID,null,-1,null,parent);
            /*
            myModel = model;
            this.nodeID=nodeID;
            this.nodeType=null;
            childCountCache = -1;
            nodeTermCache = null;
            childrenCache = null;*/
        }
        
        boolean isRepeated(){
        	return originalPath!=null;
        }
        
        void youAreArepetition(TreePath P){
        	if (originalPath!=null)
        		throw new XJException("This node already has originaPath set: "+this+"\noriginalPath:"+originalPath);
        	originalPath = P;
        }
        
        TreePath getOriginalPath(){
        	return originalPath;
        }
               
        synchronized int getChildCount(){
            if (childCountCache==-1) {
				String goal =
				"recoverTermModels([ScGmodel,OCmodel],[ScG,OC]), recoverTermModel(NodeIDModel,NodeID)," +
				"goal_arg(2,ScG,NodeID), " +
				(getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
				"countSolutions(ScG,N), ipObjectSpec('java.lang.Integer',Int,[N],_)";
				
				Integer N = (Integer)getEngine().deterministicGoal(
					goal,"[ScGmodel,OCmodel,Cmodel,NodeIDModel]",
					new Object[]{getSubclassGoal(),getOC(),getC(),nodeID},
					"[Int]"
				)[0];
				childCountCache = N.intValue();
				isLeafNode = (childCountCache==0);
            }
            // System.out.println("getChildCount():"+childCountCache);
            return childCountCache;
        }
        synchronized boolean isLeaf(){
            if (childCountCache==-1) {
                // children cache for the node is not filled yet
                if(isLeafNode == null) { // no information available yet
                    // do not get all the children (might take a long time),
                    // just check whether they exist
                    String goal =
                    "recoverTermModels([ScGmodel,OCmodel],[ScG,OC]), recoverTermModel(NodeIDModel,NodeID)," +
                    "goal_arg(2,ScG,NodeID), " +
                    (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                    //"GoalFlag = '$xj_scgoalsucceeded'(0), writeln(scG-ScG), flrdecode:flora_decode_goal_as_atom(ScG,AtomScG), writeln(atomScG-AtomScG), (once(ScG), writeln(cucu), machine:term_set_arg(GoalFlag,1,1,1), fail ; true), GoalFlag = '$xj_scgoalsucceeded'(1) ";
                    "GoalFlag = '$xj_scgoalsucceeded'(0), (once(ScG), machine:term_set_arg(GoalFlag,1,1,1), fail ; true), GoalFlag = '$xj_scgoalsucceeded'(1) ";
                    
                    // System.out.println("isLeaf(): "+nodeID);
                    
                    boolean hasChildren = getEngine().deterministicGoal(
                    goal,"[ScGmodel,OCmodel,Cmodel,NodeIDModel]",
                    new Object[]{getSubclassGoal(),getOC(),getC(),nodeID}
                    );
                    this.isLeafNode = new Boolean(!hasChildren); // TODO: avoid creating all these Boolean objects
                }
                return isLeafNode.booleanValue();
            } else {
                // get the number from cache without calling Prolog
                return getChildCount()==0;
            }
        }
            	
        synchronized Object getChild(int index){
            if (childrenCaches==null)
				childrenCaches= Collections.synchronizedMap(new HashMap<Integer,Vector<LazyTreeNode>>());
        	int mapKeyInt       = (int) (Math.floor(((double) index) / ((double) REFRESH_PAGE)));
        	Integer mapKey      = new Integer(mapKeyInt);
        	Vector<LazyTreeNode> cacheRangeVector  = childrenCaches.get(mapKey);
        	if (cacheRangeVector==null){
        		//System.out.println("Fetching children for "+this+" because of "+index);
        		// cache does not contain range with this node
        		cacheRangeVector = new Vector<LazyTreeNode>();
            	int first         = mapKeyInt * REFRESH_PAGE;
            	int newLast       = Math.min(first + REFRESH_PAGE, getChildCount()) - 1; // this fills childCountCache
            	            	
				String goal = "recoverTermModels([ScGmodel,OCmodel],[ScG,OC]), recoverTermModel(NodeIDModel,NodeID), goal_arg(2,ScG,NodeID), " +
				(getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
				((myModel.isSorted())
				?"recoverTermModel(SortModel, Sort), goal_arg(1, ScG, ChildTemplate), get_sorted_solution_models(ScG,ChildTemplate,"+first+","+newLast+",Sort,Children)" 
				:"get_solution_models((ScG,goal_arg(1,ScG,ChildID)),ChildID,"+ first + "," + newLast + ",Children)");      
			
				Object[] childrenRange = (Object[])getEngine().deterministicGoal(
					goal,"[ScGmodel,OCmodel,Cmodel,NodeIDModel,SortModel]",
					new Object[]{getSubclassGoal(),getOC(),getC(),nodeID,myModel.getSortTerm()},
				"[Children]")[0];
							
				for (int i=0;i<childrenRange.length;i++) {
					TermModel c = (TermModel)childrenRange[i];
					// System.out.println("Storing child "+i+":"+c);
					cacheRangeVector.addElement(new LazyTreeNode(myModel,c,this));
				}
				childrenCaches.put(mapKey,cacheRangeVector);
			};
            
            // we got this range in cache, let's proceed:
            LazyTreeNode child = cacheRangeVector.elementAt(index - mapKeyInt * REFRESH_PAGE);
            
            if((child.nodeTermCache == null) || (myModel.hasPolimorphicNodes && child.nodeType==null)){
            	// System.err.println("getNameGoal():"+getNameGoal()+"getOC():"+getOC());
            	
                String goal = "recoverTermModels([NameGmodel,OCmodel],[NameG,OC]), recoverTermModel(NodeIDModel,NodeID), "+
                "goal_arg(1,NameG,NodeID), " +
                (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                "NameG, xj_getTreeNodeType(NameG,NodeTypeModel), goal_arg(3,NameG,NodeTerm)," +
                "(NodeTerm=null->NodeTermModel=null;buildTermModel(NodeTerm,NodeTermModel))";
                                
                Object[] bindings = (Object[])getEngine().deterministicGoal(
                	goal,"[NameGmodel,OCmodel,Cmodel,NodeIDModel]",
                	new Object[]{getNameGoal(),getOC(),getC(),child.nodeID},
                	"[NodeTypeModel, NodeTermModel]");
                if (bindings==null){
                	System.err.println("Failed to present node with ID "+child.nodeID);
                	System.err.println("Other arguments:"+Arrays.toString(new Object[]{getNameGoal(),getOC(),getC()}));
                }
                child.setNodeTerm((TermModel) bindings[1]);
                //System.out.println("Got node term for "+child.nodeID+":");
                //System.out.println(bindings[1]);
                child.setType((String)bindings[0]);
                
               // System.out.println("Got child "+index + "(ID "+child.nodeID+") of "+ this.nodeID+ ": "+child);
                
                if (myModel.hasPolimorphicNodes && child.nodeType==null)
                                throw new XJException("Missing type for nodeID "+child.nodeID);
                if(child.nodeTermCache == null){
                    throw new XJException("Missing label for nodeID "+child.nodeID);
                }

            }
            // System.out.println("Returning child "+index+":"+child);
            return child;
        }
        
        synchronized int getIndexOfChild(Object child){
            // System.out.println("Requested child of "+this.nodeID);
            /* if (childrenCache==null) {
                fillChildrenCache();
            }  
             * no need to fill out childrenCache: the comparison (equals in indexOf())
             * is by object reference (no equals method is implemented so far),
             * if the cache is not filled for a parent 
             * (or the cache is partially filled and the object is not there) 
             * - such object was not yet created
             */
            int index = -1;
            if((childrenCaches != null) && (child != null)){
            	//Collection<Vector<LazyTreeNode>> rangeCaches = childrenCaches.values();
            	Set<Integer> ints = childrenCaches.keySet();
            	
            	for (Iterator<Integer> i = ints.iterator(); i.hasNext(); ){
            		Integer I = i.next();
            		Vector<LazyTreeNode> rangeCache = childrenCaches.get(I);
            		index = rangeCache.indexOf(child);
            		if (index!=-1){
            			index = index + I.intValue()*REFRESH_PAGE;
            		 	break;
            		}
            	}
            } 
            if (index==-1) throw new XJException("Inconsistent index");
            return index;
                        /* Prolog-side old approach:
                        LazyTreeNode childLTN = (LazyTreeNode)child;
                        String G = "Nodes=[Node,Child], get_solutionN("+ getRelationFunctor()+"(_,_,Node), N, "+getRelationFunctor()+"(Child,_,Node)), ";
                        G += "ipObjectSpec('java.lang.Integer',Int,[N],_)";
                        Integer N = (Integer)getEngine().deterministicGoal(
                                G,"Nodes",new Object[]{node,childRTN.node},"[Int]"
                                )[0];
                        return N.intValue(); */
        }
        
        public TermModel getNodeTerm(){
            return nodeTermCache;
        }
        // should check if it can, and act on it: save to Prolog!
        public void setNodeTerm(TermModel term){
            nodeTermCache=term;
        }
        public String toString(){
            if (nodeTermCache==null) return "treenode:null";
            else return "treenode:"+nodeTermCache.toString();
        }
        
        PrologEngine getEngine(){
            return myModel.engine;
        }
        public TermModel getSubclassGoal(){
            return myModel.subclassGoal;
        }
        public TermModel getNameGoal(){
            return myModel.nameGoal;
        }
        public TermModel getC(){
            return myModel.context;
        }
        public TermModel getOC(){
            return myModel.originalContext;
        }
        boolean isPolimorphic(){
            return myModel.hasPolimorphicNodes;
        }
        public String getType(){
            return nodeType;
        }
        
        public void setType(String type){
            nodeType = type;
        }
        
        public TermModel getID(){
            return nodeID;
        }
        synchronized void invalidateSubtreeCaches(){
            if (childrenCaches!=null){
            	Collection<Vector<LazyTreeNode>> rangeCaches = childrenCaches.values();
             	for (Iterator<Vector<LazyTreeNode>> i = rangeCaches.iterator(); i.hasNext(); ){
            		Vector<LazyTreeNode> rangeCache = i.next();
            		for (int c=0; c<rangeCache.size(); c++){
            			LazyTreeNode node = rangeCache.elementAt(c);
            			if (node!=null) node.invalidateSubtreeCaches();
            		}
            	}
            }
            childrenCaches = null;
            childCountCache = - 1;
            isLeafNode = null;
        }

            	

        
        public void fireSubTreeStructureChanged(){
            //System.out.println("about to notify JTree of new subtree under "+this);
            TreeModelEvent e = new TreeModelEvent(this, getPathFromRoot());
            myModel.fireTreeStructureChanged(e);
        }
            
        public void fireSubTreeNodesChanged(){
            //System.out.println("about to notify JTree of node change "+this);
            //TreeModelEvent e = new TreeModelEvent(this, new Object[] {this});
            TreeModelEvent e = new TreeModelEvent(this, getPathFromRoot());
            //System.out.println("TreeModelEvent path:"+getPathFromRoot());
            myModel.fireTreeNodesChanged(e);
        }
        
        Object[] getPathFromRoot(){
            Vector<LazyTreeNode> v = new Vector<LazyTreeNode>();
            collectPathFromRoot(v);
            return v.toArray();
        }
        
        void collectPathFromRoot(Vector<LazyTreeNode> v){
            if(parent!=null) parent.collectPathFromRoot(v);
            v.addElement(this);
        }

        public void invalidateAndRefresh(){
            //System.out.println("Let's invalidate and refresh subtree "+this);
            myModel.fireWillRefresh();
            invalidateSubtreeCaches();
            fireSubTreeStructureChanged();
            myModel.fireDidRefresh();
           /*if(this==myModel.root){
                // resend request to Prolog to notify if name cache changed,
                // because this method could be involved because goal
                // is invalidated, and thus previous notifier is forgotten
                
                String goal = "recoverTermModels([NameGmodel,OCmodel],[NameG,OC]), recoverTermModel(NodeIDModel,NodeID)," +
                " arg(1,NameG,NodeID)," +
                (getC()==null?"(var(OC)->OC=null;true), " :"recoverTermModel(Cmodel,C), C=OC, ") +
                "cache_range(NameG,none,"+getInvalidateAndRefreshGoal()+",1,1)," +
                "xj_getTreeNodeType(NameG,Type), "+
                "arg(3,NameG,ChildTerm), buildTermModel(ChildTerm, ChildTermModel)";
                
                Object [] bindings=getEngine().deterministicGoal(goal,
                "[NameGmodel,OCmodel,Cmodel,NodeIDModel]",new Object[]{getNameGoal(),getOC(),getC(),nodeID},
                "[Type,ChildTermModel]");
                setType((String)bindings[0]);
                setNodeTerm((TermModel) bindings[1]);
            }*/
        }
        
        LazyTreeNode getChildByNodeID(TermModel nodeID){
            /* getChild(i) will fill as much children cache as necessary
             * if (childrenCache==null) fillChildrenCache();
             */
            for (int i=0;i<getChildCount();i++){
                LazyTreeNode child = (LazyTreeNode)getChild(i); // so we get node labels, even from subclasses with subtler policies...
                if (child.getID().equals(nodeID)) return child;
            }
            return null;
        }
        TreePath buildExpandPath(TermModel[] nodeIDs){
            Vector<LazyTreeNode> bag = new Vector<LazyTreeNode>();
            boolean found = buildExpandPath(nodeIDs,0,bag);
            if (!found) return null;
            Object[] objects = new Object[bag.size()];
            for (int i=0;i<objects.length;i++)
                objects[i] = bag.elementAt(i);
            return new TreePath(objects);
        }
        boolean buildExpandPath(TermModel[] nodeIDs, int current, Vector<LazyTreeNode> bag){
            if (!nodeID.equals(nodeIDs[current]))
                throw new XJException("inconsistency expanding path:"+nodeID+","+nodeIDs[current]);
            bag.addElement(this);
            if (current<nodeIDs.length-1){
                LazyTreeNode childInPath = getChildByNodeID(nodeIDs[current+1]);
                // if (childInPath==null) return false;
                // changed from above as we want to get as much of path as possible
                // if node is deleted we want to go down to its parent
                if (childInPath==null) return true;
                else return childInPath.buildExpandPath(nodeIDs,current+1,bag);
            }
            return true;
        }
        
        protected void setLeafNode(Boolean isLeafNode){
            this.isLeafNode = isLeafNode;
        }
    }
}