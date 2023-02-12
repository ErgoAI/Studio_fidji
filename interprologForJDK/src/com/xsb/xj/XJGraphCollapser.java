package com.xsb.xj;
import edu.uci.ics.jung.visualization.subLayout.*;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import java.util.Collection;

/** Similar to super class, but instead of Graph vertices uses LazyGraphModel.CollapsedNodes
to represent collapsed clusters */

public class XJGraphCollapser extends GraphCollapser{
	LazyGraphModel myOriginalGraph; // hack to avoid changing superclass...
	
	public XJGraphCollapser(Graph<?, ?> originalGraph){
		super(originalGraph);
		myOriginalGraph = (LazyGraphModel)originalGraph;
	}
    private Graph<?, ?> createGraph_()  {
        return new LazyGraphModel(myOriginalGraph);
    }
    public LazyGraphModel collapse(LazyGraphModel inGraph, LazyGraphModel.CollapsedNode clusterNode) {
        LazyGraphModel clusterGraph = clusterNode.getClusterGraph();
        if(clusterGraph.getVertexCount() < 2) return inGraph;

        LazyGraphModel graph = (LazyGraphModel)createGraph_();
        
        Collection<LazyGraphModel.Node> cluster = clusterGraph.getVertices();
        
        // add all vertices in the delegate, unless the vertex is in the
        // cluster.
        for(LazyGraphModel.Node v : inGraph.getVertices()) {
            if(cluster.contains(v) == false) {
                graph.addVertex(v);
            }
        }
        // add the clusterGraph as a vertex
        graph.addVertex(clusterNode);
        
        //add all edges from the inGraph, unless both endpoints of
        // the edge are in the cluster
        for(LazyGraphModel.Edge e : inGraph.getEdges()) {
            Pair<LazyGraphModel.Node> endpoints = inGraph.getEndpoints(e);
            // don't add edges whose endpoints are both in the cluster
            if(cluster.containsAll(endpoints) == false) {

                if(cluster.contains(endpoints.getFirst())) {
                	graph.addEdge(e, clusterNode, endpoints.getSecond(), inGraph.getEdgeType(e));

                } else if(cluster.contains(endpoints.getSecond())) {
                	graph.addEdge(e, endpoints.getFirst(), clusterNode, inGraph.getEdgeType(e));

                } else {
                	graph.addEdge(e,endpoints.getFirst(), endpoints.getSecond(), inGraph.getEdgeType(e));
                }
            }
        }
        return graph;
    }
    
    public LazyGraphModel expand(LazyGraphModel inGraph, LazyGraphModel.CollapsedNode clusterNode) {
    
   	 	LazyGraphModel clusterGraph = clusterNode.getClusterGraph();
    
        LazyGraphModel graph = new LazyGraphModel(inGraph);

        Collection<LazyGraphModel.Node> cluster = clusterGraph.getVertices();

        // put all clusterGraph vertices and edges into the new Graph
        for(LazyGraphModel.Node v : cluster) {
            graph.addVertex(v);
            for(LazyGraphModel.Edge edge : clusterGraph.getIncidentEdges(v)) {
                Pair<LazyGraphModel.Node> endpoints = clusterGraph.getEndpoints(edge);
                graph.addEdge(edge, endpoints.getFirst(), endpoints.getSecond(), clusterGraph.getEdgeType(edge));
            }
        }
        // add all the vertices from the current graph except for
        // the cluster we are expanding
        for(LazyGraphModel.Node v : inGraph.getVertices()) {
            if(!v.equals(clusterNode)) {
                graph.addVertex(v);
            }
        }

        // now that all vertices have been added, add the edges,
        // ensuring that no edge contains a vertex that has not
        // already been added
        for(LazyGraphModel.Node v : inGraph.getVertices()) {
            if(!v.equals(clusterNode)) {
                for(LazyGraphModel.Edge edge : inGraph.getIncidentEdges(v)) {
                    Pair<LazyGraphModel.Node> endpoints = inGraph.getEndpoints(edge);
                    LazyGraphModel.Node v1 = endpoints.getFirst();
                    LazyGraphModel.Node v2 = endpoints.getSecond();
                     if(!cluster.containsAll(endpoints)) {
                        if(clusterNode.equals(v1)) {
                            // i need a new v1
                            LazyGraphModel.Node originalV1 = clusterNode.getPreviousGraph().getEndpoints(edge).getFirst();
                            LazyGraphModel.Node newV1 = graph.findVertex(originalV1);
                            graph.addEdge(edge, newV1, v2, inGraph.getEdgeType(edge));
                        } else if(clusterNode.equals(v2)) {
                            // i need a new v2
                            LazyGraphModel.Node originalV2 = clusterNode.getPreviousGraph().getEndpoints(edge).getSecond();
                            LazyGraphModel.Node newV2 = graph.findVertex(originalV2);
                            graph.addEdge(edge, v1, newV2, inGraph.getEdgeType(edge));
                        } else {
                        	graph.addEdge(edge, v1, v2, inGraph.getEdgeType(edge));
                        }
                    }
                }
            }
        }
        return graph;
    }
    // Here just because of weird bug......:
    public LazyGraphModel getClusterGraph(LazyGraphModel inGraph, Collection<LazyGraphModel.Node> picked) {
        LazyGraphModel clusterGraph = new LazyGraphModel(inGraph);
        for(LazyGraphModel.Node v : picked) {
        	clusterGraph.addVertex(v);
            Collection<LazyGraphModel.Edge> edges = inGraph.getIncidentEdges(v);
            for(LazyGraphModel.Edge edge : edges) {
                Pair<LazyGraphModel.Node> endpoints = inGraph.getEndpoints(edge);
                LazyGraphModel.Node v1 = endpoints.getFirst();
                LazyGraphModel.Node v2 = endpoints.getSecond();
                if(picked.containsAll(endpoints)) {
                    clusterGraph.addEdge(edge, v1, v2, inGraph.getEdgeType(edge));
                }
            }
        }
        return clusterGraph;
    }
    
}