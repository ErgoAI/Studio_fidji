package com.xsb.xj;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.util.OutOfBandTermResource;
import com.xsb.xj.util.XJException;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

@SuppressWarnings("serial")
public class LazyGraphModel extends SparseMultigraph<LazyGraphModel.Node,LazyGraphModel.Edge>{
	TermModel edgeGoal, nodeGoal, initialIDs;
	PrologEngine engine;
	boolean directed;
	/** directed graphs are the default */
    public static final String UNDIRECTED = "undirected";
    public static final String NODETYPES = "nodetypes";
    public static final String EDGETYPES = "edgetypes";
	static final String COLLAPSED_TYPE = "xjCollapsed";
    public static final String MAXITEMS = "maxitems";
    public static final int MAXITEMS_DEFAULT = 10000;
    Hashtable<String,TermModel[]> nodeTypes= new Hashtable<String,TermModel[]>();
    Hashtable<String,TermModel[]> edgeTypes= new Hashtable<String,TermModel[]>();
    int maxItems;
    /** Determines use of out of band term resources */
    static final int MIN_ITEMS_FOR_OOB = 500;
	HashMap<TermModel,HashSet<TermModel>> expansions = new HashMap<TermModel,HashSet<TermModel>>();
	
	/** Make a clean copy of the prototype graph, without nodes nor edges */
	public LazyGraphModel(LazyGraphModel proto){
		edgeGoal=proto.edgeGoal; nodeGoal=proto.nodeGoal; initialIDs=proto.initialIDs;
		engine=proto.engine; directed=proto.directed;
		nodeTypes=proto.nodeTypes; edgeTypes=proto.edgeTypes;
		maxItems=proto.maxItems;
	}
	
	public LazyGraphModel(PrologEngine engine, GUITerm gt){
            this.engine=engine;
            directed = true;
            if(gt.findProperty(UNDIRECTED)!=null) 
                directed=false;
            maxItems = MAXITEMS_DEFAULT;
            if(gt.findProperty(MAXITEMS)!=null) 
                maxItems=gt.findProperty(MAXITEMS).intValue();
            if(!gt.node.equals(GUITerm.XJ_LAZY))
                throw new XJException("bad graph GT:" + gt);
            TermModel lazy = (TermModel) gt.getChild(0);
            if (!lazy.node.equals("lazy") || lazy.getChildCount()!=3)
                throw new XJException("bad lazy term:" + lazy);
            edgeGoal = GUITerm.floraPreprocessWithArgs((TermModel)lazy.getChild(0),6);
            /*
              if (edgeGoal.getChildCount()==3)
        	edgeGoal = new TermModel(edgeGoal.node,new TermModel[]{
        		edgeGoal.children[0], edgeGoal.children[1], 
        		new TermModel("-",new TermModel[]{edgeGoal.children[0],edgeGoal.children[1]}),
        		edgeGoal.children[2], new TermModel("null")
        	});
            */
        //if (edgeGoal.getChildCount()!=6)
        //    throw new XJException("bad edge relation:" + edgeGoal);
        if (!edgeGoal.children[0].isVar() || !edgeGoal.children[1].isVar())
            throw new XJException("node args must be vars in " + edgeGoal);
        nodeGoal = GUITerm.floraPreprocessWithArgs((TermModel)lazy.getChild(1),4);
        /*
        if (nodeGoal.getChildCount()==2)
        	nodeGoal = new TermModel(nodeGoal.node, new TermModel[]{
        		nodeGoal.children[0],nodeGoal.children[1],nodeGoal.children[0]
        	});*/
        //if (nodeGoal.getChildCount()!=4)
        //    throw new XJException("bad node relation:" + nodeGoal);
        
        if (!nodeGoal.children[0].isVar())
            throw new XJException("node arg must be var in " + nodeGoal);

        // TermModel[] initialIDs = ((TermModel) lazy.getChild(2)).flatList();
        initialIDs = (TermModel) lazy.getChild(2);
        
        // Extract type properties into our two hash tables
        TermModel nodeTypesTerm = gt.findProperty(NODETYPES);
 		if (nodeTypesTerm==null)
			throw new XJException("Missing nodeTypes([type1-Properties1, ...]) term in " + gt);
		nodeTypesTerm = (TermModel)nodeTypesTerm.getChild(0);		
		TermModel[] nodeTypesArray = nodeTypesTerm.flatList();
       	for (int t=0; t<nodeTypesArray.length; t++){
       		TermModel typeDeclaration = nodeTypesArray[t];
       		String typename = typeDeclaration.children[0].toString();
       		//if (!typename.isAtom())
			//	throw new XJException("node type must be an atom: " + typename);
       		TermModel typeProps = typeDeclaration.children[1];
       		nodeTypes.put(typename,typeProps.flatList());
       	}

        TermModel edgeTypesTerm = gt.findProperty(EDGETYPES);
 		if (edgeTypesTerm==null)
			throw new XJException("Missing edgeTypes([type1-Properties1, ...]) term in " + gt);
		edgeTypesTerm = (TermModel)edgeTypesTerm.getChild(0);		
		TermModel[] edgeTypesArray = edgeTypesTerm.flatList();
       	for (int t=0; t<edgeTypesArray.length; t++){
       		TermModel typeDeclaration = edgeTypesArray[t];
       		TermModel typename = typeDeclaration.children[0];
       		if (!typename.isAtom())
				throw new XJException("edge type must be an atom: " + typename);
       		TermModel typeProps = typeDeclaration.children[1];
       		edgeTypes.put(typename.toString(),typeProps.flatList());
       	}
       	
        addInitialNodes();
	}
	
	public boolean isDirected(){
		return directed;
	}
	
	void removeNode(TermModel ID){
		Node dummy = Node.makeDummy(ID);
		Collection<Node> neighbors = getNeighbors(dummy);
		if (!removeVertex(dummy))
			throw new XJException("Failed to remove node" + ID);
		for (Node node:neighbors)
			node.expanded=false;
	}
	
	void addInitialNodes(){
		addNodes(initialIDs);
	}
	
	void addNodes(TermModel IDs){
		addNodes( IDs, false, false);
	}
	void addNodes(TermModel IDs,boolean expandIncoming,boolean expandOutcoming){
		//System.out.println("Entering addNodes with "+IDs);
		//System.out.println("Class of IDs:"+IDs.getClass());
		int nNodes = IDs.flatList().length;
		if (nNodes==0) return;
		if(nNodes>maxItems)
			throw new XJException("Trying to add "+nNodes+" graph nodes; maximum is "+maxItems);
 		boolean useOutOfBand = nNodes > MIN_ITEMS_FOR_OOB; 
		OutOfBandTermResource oobr = null;
		if (useOutOfBand) oobr = new OutOfBandTermResource(engine);
       	Object[] bindings = engine.deterministicGoal(
            "recoverTermModel(NodeGoalModel,NodeGoal), recoverTermModel(IDsModel,IDs), " +
            "findall(n(ID,Type,Term,Tip) , (basics:member(ID,IDs),arg(1,NodeGoal,ID), NodeGoal, arg(2,NodeGoal,Type), arg(3,NodeGoal,Term), arg(4,NodeGoal,Tip)), Nodes), " +
            // "buildTermModelArray(Nodes,NodesArray) ", 
            (useOutOfBand ?
             "ipPutTermList(Nodes,"+oobr.prologFileAtom()+"), NodesFTM=null "
             :
             "buildInitiallyFlatTermModel(Nodes,NodesFTM) "
             ), 
            //	"[NodeGoalModel,IDsModel]", new Object[]{nodeGoal,IDs}, "[NodesArray]");
            "[NodeGoalModel,IDsModel]", new Object[]{nodeGoal,IDs}, "[NodesFTM]");
        if (bindings==null)
            throw new XJException("Failed obtaining more info about nodes " + IDs);
        
        // TermModel[] initialNodes = (TermModel[])bindings[0];
        TermModel[] initialNodes = null;
        if (useOutOfBand) initialNodes = oobr.getTermList();
        else initialNodes = ((TermModel)bindings[0]).flatList(); 
        
        TermModel[] addedIDs = new TermModel[initialNodes.length];
        for (int t=0; t<initialNodes.length; t++){
        	Node node = new Node(initialNodes[t],this);
        	addVertex(node);
        	addedIDs[t] = node.ID;
        }
        // TermModel.destroy(initialNodes);
        if (expandIncoming||expandOutcoming)
        	expandVertices(addedIDs,expandIncoming,expandOutcoming);
	}
	
	void expandAllVertices(){
		expandVertices(collectCurrentNonExpandedNodeIDs());
	}
	void expandVertices(TermModel[] IDs){
		expandVertices(IDs,true,true);
	}
	/** Include (or not) outgoing and incoming edges */
	void expandVertices(TermModel[] IDs,boolean outgoingEdges,boolean incomingEdges){
            if (IDs.length==0) return;
            boolean useOutOfBand = IDs.length > MIN_ITEMS_FOR_OOB; 
            OutOfBandTermResource oobr = null;
            if (useOutOfBand) oobr = new OutOfBandTermResource(engine);
            
            String outgoing = (outgoingEdges?"true":"fail");
            String incoming = (incomingEdges?"true":"fail");
            Object[] bindings = engine.deterministicGoal(
             "recoverTermModelArray(IDsModel,IDs), recoverTermModel(NodeGoalModel,NodeGoal1), recoverTermModel(EdgeGoalModel,EdgeGoal), copy_term(NodeGoal1,NodeGoal2), " +
             "findall(e(ID,Type,Term,Tip,NodeID1,Node1Type,Node1Term,Node1Tip,NodeID2,Node2Type,Node2Term,Node2Tip) , "+
             "(basics:member(NodeID,IDs),  ("+outgoing+",arg(1,EdgeGoal,NodeID);"+incoming+",arg(2,EdgeGoal,NodeID)),  EdgeGoal, "+
             "arg(1,EdgeGoal,NodeID1), arg(2,EdgeGoal,NodeID2), arg(3,EdgeGoal,ID), arg(4,EdgeGoal,Type), arg(5,EdgeGoal,Term), arg(6,EdgeGoal,Tip), "+
             "arg(1,NodeGoal1,NodeID1), NodeGoal1, arg(2,NodeGoal1,Node1Type), arg(3,NodeGoal1,Node1Term), arg(4,NodeGoal1,Node1Tip), "+
             "arg(1,NodeGoal2,NodeID2), NodeGoal2, arg(2,NodeGoal2,Node2Type), arg(3,NodeGoal2,Node2Term), arg(4,NodeGoal2,Node2Tip) ),"+
             " Edges), " +
             // "buildTermModelArray(Edges,EdgesArray) "
             "basics:length(Edges,Nedges), (Nedges>"+maxItems+"->xj_failError('Trying to add too many graph edges:%ld',args(Nedges));true)," +
             (useOutOfBand ? "ipPutTermList(Edges,"+oobr.prologFileAtom()+"), EdgesFTM=null " : "buildInitiallyFlatTermModel(Edges,EdgesFTM) ")
             // , "[IDsModel,EdgeGoalModel,NodeGoalModel]", new Object[]{IDs,edgeGoal,nodeGoal}, "[EdgesArray]");
             , "[IDsModel,EdgeGoalModel,NodeGoalModel]", new Object[]{IDs,edgeGoal,nodeGoal}, "[EdgesFTM]");
            if (bindings==null)
                throw new XJException("Failed obtaining more info about  nodes " + Arrays.toString(IDs));
            TermModel[] edges = null;
            if (useOutOfBand) edges = oobr.getTermList();
            else edges = ((TermModel)bindings[0]).flatList(); // redundant... can be optmized in tandem with a InitiallyFlatTermModel subclass to keep the array flat

            for (int e=0; e<edges.length; e++){
        	TermModel T = edges[e];
        	addEdge(
        		new Edge(T.children[0], T.children[1], T.children[2], T.children[3], this), 
        		new Pair<Node>(
        			new Node(T.children[4], T.children[5], T.children[6], T.children[7], this),  
        			new Node(T.children[8], T.children[9], T.children[10], T.children[11], this) ), 
        		(directed?EdgeType.DIRECTED:EdgeType.UNDIRECTED)
        		);
            }		
            // TermModel.destroy(edges);
            ArrayList<Node> expandedNodes = IDsToNodes(IDs);
            for (Node node:expandedNodes)
        	node.expanded=true;
	}
    void expandAll(){
        while(!isExpanded())
            expandAllVertices();
    }
    void unexpandVertex(TermModel ID){
	/*TODO:
          in expandVertices, build expansions tree
          collect (recursively; apparently no loops) all IDs in
          expansions into a list, and delete expansions remove the nodes
          
          on xjGenericGraph: first implement unexpandVertex, then toggleVertex
        */
    }

    boolean isExpanded(){
        /*
          for (Iterator<Node> i = getVertices().iterator(); i.hasNext();){
              Node node = i.next();
              if (!node.expanded) 
              return false;
          }
          return true;
        */
        return allExpanded(getVertices(),false);
    }
    boolean allExpanded(Collection<Node> nodes, boolean andNeighbors){
        for (Node N: nodes){
            if (!N.expanded) {
                System.err.println("Unexpanded node: "+N);
                return false;
            }
            if (andNeighbors){
                for (Node NB:getNeighbors(N)){
                    if (!NB.expanded){
                        System.out.println("Neighbor not expanded:"+NB);
                        return false;	
                    }
                }
            }
        }
        return true;
    }
    
    TermModel[] collectCurrentNonExpandedNodeIDs(){
        Collection<Node> vertices = getVertices();
        ArrayList<TermModel> IDs = new ArrayList<TermModel>();
        for (Iterator<Node> i = vertices.iterator(); i.hasNext();){
            Node node = i.next();
            if (!node.expanded) {
                IDs.add(node.ID);
            }
        }
        return IDs.toArray(new TermModel[0]);
    }
    
    /** Very unefficient, does linear search for each vertice...*/
    ArrayList<Node> IDsToNodes(TermModel[] IDs){
        ArrayList<Node> R = new ArrayList<Node>();
        if (IDs==null) return R;
        for (Iterator<Node> i = getVertices().iterator(); i.hasNext();){
            Node N = i.next();
            for (int j = 0; j<IDs.length; j++)
                if (IDs[j].equals(N.ID))
                    R.add(N);
        }
        return R;
    }
    
    Node idToNode(TermModel ID){
        ArrayList<Node> C = IDsToNodes(new TermModel[]{ID});
        if (C.size()==0) return null;
        else return C.get(0);
    }
    
    
    public String toString(){
        return "Edge relation:"+edgeGoal+"\nNode relation:"+nodeGoal+
            "\nCurrent graph:"+super.toString();
    }
    
    static class GraphItem{
        TermModel ID;
        String term, tip;
        String type;
        /** Properties declared for the type */
        TermModel[] properties;
        
        protected GraphItem(){}
        
        GraphItem(TermModel ID, TermModel type, TermModel term, TermModel tip){
            this.ID=ID;
            this.term=term.toString().intern();
            this.type=type.toString().intern();
            this.tip=tip.toString().intern();
        }
        public int hashCode(){
            // System.out.println("hashCode asked of "+ID+":"+ID.hashCode());
            return ID.hashCode();
        }
        public boolean equals(Object X){
            if (!X.getClass().equals(getClass())) return false;
            GraphItem N = (GraphItem)X;
            boolean F = N.ID.variant(ID); // beware of TermModel.hashcode()...
            //System.out.println(F+":"+ID+","+N.ID);
            return F /* && N.term.equals(this.term) && N.type.equals(this.type)*/;
        }
        public String toString(){
            return getClass().getSimpleName()+" "+term+" with ID "+ID+" has type "+type;
        }
        GUITerm buildGT(){
            if (GUITerm.findProperty(GUITerm.FUNCTION, properties)!=null)
            	throw new XJException("Graph item properties can not contain functions: " + Arrays.toString(properties));				
            // add root and opaque to properties, for (at least aesthetical...) consistency
            TermModel root=null,opaque=null;
            int N = properties.length;
            if (GUITerm.findProperty(GUITerm.ROOT, properties)==null){
                root = new TermModel(GUITerm.ROOT);
                N++;
            }
            if (GUITerm.findProperty(GUITerm.OPAQUE, properties)==null){
                opaque = new TermModel(GUITerm.OPAQUE);
                N++;
            }
            TermModel[] extendedProperties = Arrays.copyOf(properties,N);
            if (root!=null)
                extendedProperties[N-2] = root;
            if (opaque!=null)
                extendedProperties[N-1] = opaque;
            GUITerm GT = new GUITerm(ID.node,extendedProperties,ID.children,ID.isList());
            GT.setOpRoot();
            return GT;
        }
    }
    
    static class Node extends GraphItem{
        /** whether the graph edgeRelation has been computed focused on this node*/
        boolean expanded; 
        Node(TermModel ID, TermModel type, TermModel term, TermModel tip, LazyGraphModel model){
            super(ID,type,term,tip);
            expanded=false;
            properties = model.nodeTypes.get(this.type); // Super class already toString()'d it...
            if (properties==null)
                throw new XJException("Missing declaration for type "+type+" in node with ID "+ID);
        }
        Node(TermModel N, LazyGraphModel model){
            this( (TermModel)N.getChild(0), (TermModel)N.getChild(1), (TermModel)N.getChild(2), (TermModel)N.getChild(3), model);
        }
        /** For some local hacking */
        Node(){
        }
        static Node makeDummy(TermModel ID){
            Node dummy = new Node();
            dummy.ID=ID;
            return dummy;
        }
    }
    
    static int collapsedCounter = 0;
    static final String COLLAPSED_PREFIX = "fj_unique_collapsed";
    static class CollapsedNode extends Node{
        LazyGraphModel clusterGraph;
        /** Reference to the larger graph (before the node group collapse) */
        LazyGraphModel previousGraph;
        
        CollapsedNode(LazyGraphModel clusterGraph, LazyGraphModel mainGraph){
            super(
                  new TermModel(COLLAPSED_PREFIX + (collapsedCounter ++)), new TermModel(COLLAPSED_TYPE), 
                  new TermModel("..."), new TermModel("represents "+clusterGraph.getVertices().size()+" collapsed nodes"), mainGraph );
            this.clusterGraph=clusterGraph; previousGraph=mainGraph;
            expanded=true;
        }
        
        LazyGraphModel getClusterGraph(){ return clusterGraph; }
        LazyGraphModel getPreviousGraph(){ return previousGraph; }
    }
    
    static class Edge extends GraphItem{
        // Node one,two;   unnecessary, Jung keeps that
        Edge(TermModel ID, TermModel type, TermModel term, TermModel tip, LazyGraphModel model){
            super(ID,type,term,tip);
            properties = model.edgeTypes.get(this.type);
            if (properties==null)
                throw new XJException("Missing declaration for type "+type+" in edge with ID "+ID);
        }
        /*
          public boolean equals(Object X){
          if (!(X instanceof Edge)) return false;
              Edge N = (Edge)X;
              return N.ID.variant(this.ID) && N.term.equals(this.term) && N.type.equals(this.type);
          }
        */
    }
    Node findVertex(Node vertex) {
        Collection<Node> vertices = getVertices();
        if(vertices.contains(vertex)) {
            return vertex;
        }
        for(Node v : vertices) {
            if(v instanceof CollapsedNode) {
                LazyGraphModel g = ((CollapsedNode)v).getClusterGraph();
                Node N = g.findVertex(vertex);
                if (N!=null) return N;
            }
        }
        return null;
    }
    /*
      rebuildGraph(){
		restart from scratch; later might be incremental preserving vertices
      }
    */
}
