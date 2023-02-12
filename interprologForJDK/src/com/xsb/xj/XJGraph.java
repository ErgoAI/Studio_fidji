package com.xsb.xj;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.apache.commons.collections15.Transformer;

import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.LazyGraphModel.Edge;
import com.xsb.xj.LazyGraphModel.Node;
import com.xsb.xj.util.XJException;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.visualization.FourPassImageShaper;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.DefaultVertexIconTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.EllipseVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.VertexIconShapeTransformer;
import edu.uci.ics.jung.visualization.renderers.VertexLabelAsShapeRenderer;

@SuppressWarnings("serial")
public class XJGraph extends JPanel implements XJComponent {

/*
A XJComponent showing a graph defined by an initial node set, plus an edge expansion relation and a node "rendering" relation.
Node IDs are assumed unique.

Since in the first version nodes and edges will be rendered as atoms, rather than GTs we'll use a simplified properties notation:

gt('XJ$LAZY', [
	class='com.xsb.xj.XJGraph', layout=..., ...overall attributes...,
	% nodes and edges are "atomic", but their decoration may vary
	nodeGTs([atomtype1-[...node properties...],atomtype2-[...]]),
	edgeGTs([atomtype3-[...edge properties...],type4-[...]]),
	],
	lazy( edgeRelation(NodeID1,NodeID2,ID,Type,LabelTerm,TooltipTerm), nodeRelation(ID,Type,LabelTerm,TooltipTerm), InitialNodeIDsList)
	)

Each Type-Properties item in nodeGTs and edgeGTs corresponds to an implicit template specification gt(_,[typename=Type,opaque|properties],_)

In addition to XJ-style properties, node and edge labels can use html simply by starting their atoms with <html>, eg.
'<html>This <b>word</b> was in bold'
No need to close the </html>

Example:
GT = gt('XJ$LAZY',[class='com.xsb.xj.XJGraph', layout=isomLayout, undirected, labelsInNodes, cubicCurve,
	myGUI(Graph), root, 
	operation(term(T,_P),javaMessage(Graph,setPickingMode),menu('Pick Mode')),
	operation(term(T,_P),javaMessage(Graph,setTransformingMode),menu('Scroll Mode')),
	operation(term(T,_P),javaMessage(Graph,expandAllVertices),menu('Expand More')),
	operation(term(T,_P),javaMessage(Graph,expandAll),menu('Expand All')),
	operation(term(T,_P),javaMessage(Graph,reapplyLayout),menu('Rearrange')),
	operation(term(T,_P),writeln(double-Graph),doubleclick),
	operation(term(T,_P), (P=[_,Layout], javaMessage(Graph,applyLayout(string(Layout)))), menu(P,'Layouts'('frlayout2','daglayout','isomlayout','springlayout2'))),
	nodeTypes([ 
		accessible=[icon='http://www.berklee.edu/sites/default/files/images/BPC/wheelchair-icon.jpg',
			operation(term(T,_P),writeln(id-T/_P),menu('Show ID')),
			operation(term(T,_P),(buildTermModel(T,TM),javaMessage(Graph,expandVertex(TM))),doubleclick)
			], 
		normal=[icon='/com/coherentknowledge/fidji/flora.gif',
			operation(term(T,_P),(buildTermModel(T,TM),javaMessage(Graph,expandVertex(TM))),doubleclick)] ]),
	edgeTypes([
		blueLine=[color=blue,dotted,operation(term(T,P),writeln(id-T/P),menu('Show ID'))], 
		greenLine=[color=green, dashed], 
		yellowLine=[color=yellow]
	])],
	lazy( segment(_NodeID1,NodeID2,_ID,Type,_Term,_Tip), station(ID,Type,Term,Tip), ['Avenida','Restauradores'])
	), createXJcomponent(GT,GUI), javaMessage('com.xsb.xj.XJDesktop',testGUI(GUI)).


station(S,T,S,Tip) :- station(S,T,Tip).
station('Avenida',normal,'You''d better be into shape!') :- !.
station(_Name,accessible,'May have elevators').

segment(N1,N2,N1-N2,Type,Term,Tip) :- segment(N1,N2,Type,Term,Tip).
segment('Avenida','Restauradores',blueLine,1,'500 meters').
segment('Avenida','Marquês de Pombal',blueLine,2,'600 meters').
segment('Rato','Marquês de Pombal',yellowLine,3,'400 meters').
segment('Chiado','Restauradores',blueLine,4,'700 meters').
segment('Chiado','Cais do Sodré',greenLine,5,null).
segment('Chiado','Terreiro do Paço',greenLine,'<html><h1>SIX',null).
*/
	/** This may change over time, because of expand/collapse */
	LazyGraphModel model; 
	XJGraphCollapser collapser;
    GUITerm gt;
    PrologEngine engine;
    /** These constants must be in lower case here, although for the Prolog programmer they don't need to */
    public static final String LAYOUT = "layout"; // use simple names of layout classes
    public static final String LABELSINNODES = "labelsinnodes"; // global property; do not use for nodes with icons or circles
    public static final String ICON = "icon";
    public static final String NOICONSHAPE = "noiconshape"; // node will NOT be shaped as per the opaque part of the icon (usually bad!)
    /** This is ignored if LABELSINNODES*/
    public static final String SIZE = "size"; 
    public static final String DASHED = "dashed";
    public static final String DOTTED = "dotted";
    /** Straight edges */
    public static final String LINE = "line";
    public static final String CUBICCURVE = "cubiccurve";

    
    GraphElementAccessor<LazyGraphModel.Node,LazyGraphModel.Edge> pickSupport;
    VisualizationViewer<LazyGraphModel.Node,LazyGraphModel.Edge> vv;
    DefaultModalGraphMouse<LazyGraphModel.Node,LazyGraphModel.Edge> graphMouse;
    
	public XJGraph(PrologEngine engine, GUITerm gt) {
		this.gt=gt; this.engine=engine;
        setLayout(new BorderLayout());
        setBackground(java.awt.Color.lightGray);
        // FIDJI FONT HERE!!!!!
        setFont(new Font("Serif", Font.PLAIN, 12));

		model = new LazyGraphModel(engine,gt);
		collapser = new XJGraphCollapser(model);
		// model.expandAllVertices();
		
		// Collect global attributes...
		TermModel layoutTerm = gt.findProperty(LAYOUT);
		String layoutName = (layoutTerm==null?null:layoutTerm.toString());
		Layout<LazyGraphModel.Node,LazyGraphModel.Edge> graphLayout = layoutForName(layoutName,model);
		
		boolean labelsInNodes = gt.findProperty(LABELSINNODES)!=null;			

		// probably use a GraphZoomScrollPane containing a
		///edu.uci.ics.jung.visualization.VisualizationViewer<V,E>

		vv = new VisualizationViewer<LazyGraphModel.Node,LazyGraphModel.Edge>(graphLayout, new Dimension(600,600));
		pickSupport = vv.getPickSupport();
		vv.setForeground(Color.black);
		//vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
		
		// Set renderContext transformers:
		RenderContext<LazyGraphModel.Node,LazyGraphModel.Edge> RC =  vv.getRenderContext();
		RC.setEdgeDrawPaintTransformer(edgePaint);
		//RC.setEdgeFillPaintTransformer(edgePaint);
		RC.setEdgeLabelTransformer(edgeLabel);
		vv.setEdgeToolTipTransformer(edgeTooltip);
		RC.setEdgeStrokeTransformer(edgeStroke);
		if (gt.findProperty(LINE)!=null)
			RC.setEdgeShapeTransformer(new EdgeShape.Line<LazyGraphModel.Node,LazyGraphModel.Edge>());
		else if (gt.findProperty(CUBICCURVE)!=null)
			RC.setEdgeShapeTransformer(new EdgeShape.CubicCurve<LazyGraphModel.Node,LazyGraphModel.Edge>());
		
		RC.setVertexFillPaintTransformer(nodePaint);
		RC.setVertexLabelTransformer(nodeLabel);
		vv.setVertexToolTipTransformer(nodeTooltip);
		
		if (labelsInNodes){
			VertexLabelAsShapeRenderer<LazyGraphModel.Node,LazyGraphModel.Edge> vlasr = 
				new VertexLabelAsShapeRenderer<LazyGraphModel.Node,LazyGraphModel.Edge>(vv.getRenderContext());
			RC.setVertexShapeTransformer(vlasr);
			vv.getRenderer().setVertexLabelRenderer(vlasr);
		} else RC.setVertexShapeTransformer(new SizableVertexShapeFunction(10)); // default width/height
		// Actually NodeIconShapeTransformer will act as a "broker", providing also collapsed shapes:
		RC.setVertexShapeTransformer(new NodeIconShapeTransformer(RC.getVertexShapeTransformer()));
		RC.setVertexIconTransformer(new NodeIconTransformer());

		GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);		
		add(panel);
		
		graphMouse = new DefaultModalGraphMouse<LazyGraphModel.Node,LazyGraphModel.Edge>();
		graphMouse.add(new ItemMouseListener());
        vv.setGraphMouse(graphMouse);
        setPickingMode(); // until our initial layouts improve...
		
		//System.out.println(model);
	}
	
	static Layout<LazyGraphModel.Node,LazyGraphModel.Edge>  layoutForName(String layoutName, LazyGraphModel model){
		if (layoutName==null) 
			return new FRLayout2<LazyGraphModel.Node,LazyGraphModel.Edge>(model);
		layoutName = layoutName.toLowerCase();
		if (layoutName.equals("frlayout2")) 
			return new FRLayout2<LazyGraphModel.Node,LazyGraphModel.Edge>(model);
		else {
			if (layoutName.equals("daglayout"))
				return new DAGLayout<LazyGraphModel.Node,LazyGraphModel.Edge>(model);
			else if (layoutName.equals("isomlayout"))
				return new ISOMLayout<LazyGraphModel.Node,LazyGraphModel.Edge>(model);
			else if (layoutName.equals("springlayout2"))
				return new SpringLayout2<LazyGraphModel.Node,LazyGraphModel.Edge>(model);
			else if (layoutName.equals("circlelayout"))
				return new CircleLayout<LazyGraphModel.Node,LazyGraphModel.Edge>(model);
			else throw new XJException("unknown graph layout:"+layoutName);
			// More require Forest (delegate??)
		}
	}
	
		
	Transformer<LazyGraphModel.Node,String> nodeLabel = new Transformer<LazyGraphModel.Node,String>(){
		public String transform(LazyGraphModel.Node node){
			String R = node.term.toString();
			return (R.equals("null")?null:R);
		}
	};
		
	Transformer<LazyGraphModel.Node,String> nodeTooltip = new Transformer<LazyGraphModel.Node,String>(){
		public String transform(LazyGraphModel.Node node){
			String R = node.tip.toString();
			return (R.equals("null")?null:R);
		}
	};
		
	Transformer<LazyGraphModel.Node,Paint> nodePaint = new Transformer<LazyGraphModel.Node,Paint>(){
		public Paint transform(LazyGraphModel.Node node){
			TermModel color = GUITerm.findProperty(GUITerm.COLOR, node.properties);
			if (color==null) return null;
			//if (color==null) return Color.GRAY;
			return GUITerm.termToColor(color);
		}
	};
	
	class ItemMouseListener extends AbstractGraphMousePlugin implements MouseListener{
		ItemMouseListener(){
			super(0); // we'll ignore modifiers per se, see below...
		}
		// MouseListener methods:
		public void mousePressed(MouseEvent e) {
			GUITerm GT = findGT(e);
			if (e.isPopupTrigger() /* Unix systems here*/){
				e.consume();
				JPopupMenu pm = XJAtomicField.operationsPopup(GT,engine,XJGraph.this);
				if(pm.getComponentCount() > 0){
					pm.show(XJGraph.this,e.getX(),e.getY());
				}
			} else if (e.getClickCount()==2){
				e.consume();
				//System.out.println("gt:"+GT);
				//System.out.println("properties:"+Arrays.toString(GT.properties));
				XJAction[] actions=GT.operations(engine,XJGraph.this,null);
				XJAction todo = XJAction.findDoubleClick(actions);
				if (todo!=null)
					todo.doit();
			} // else System.out.println("Some click with modifiers "+e.getModifiers()+" :"+node+","+edge);
		}
		public void mouseClicked(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {
			if (!AbstractPrologEngine.isWindowsOS())
				return;
			GUITerm GT = findGT(e);
			if (e.isPopupTrigger() /* Windows systems here*/){
				e.consume();
				JPopupMenu pm = XJAtomicField.operationsPopup(GT,engine,XJGraph.this);
				if(pm.getComponentCount() > 0){
					pm.show(XJGraph.this,e.getX(),e.getY());
				}
			}
		}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		
		GUITerm findGT(MouseEvent e){
			Point2D ip = e.getPoint();
			LazyGraphModel.Node node = pickSupport.getVertex(vv.getGraphLayout(), ip.getX(), ip.getY());
			LazyGraphModel.Edge edge = null;
			if (node==null) 
				edge = pickSupport.getEdge(vv.getGraphLayout(), ip.getX(), ip.getY());
			LazyGraphModel.GraphItem item = (node!=null?node:edge);
			
			GUITerm GT = null;
			if (item==null) 
				GT = gt; // an event somewhere outside nodes and edges, assume the whole graph visualizer
			else
				GT = item.buildGT();
			return GT;
		}
    }
	
	// Adapted from VertexIconShapeTransformer; also handles collapsed nodes
	class NodeIconShapeTransformer extends VertexIconShapeTransformer<LazyGraphModel.Node>{
		ClusterVertexShapeFunction<LazyGraphModel.Node> collapsedHandler;
		NodeIconShapeTransformer(Transformer<LazyGraphModel.Node,Shape> delegate){
			super(delegate);
			collapsedHandler = new ClusterVertexShapeFunction<LazyGraphModel.Node>(30);
		}
		public Shape transform(LazyGraphModel.Node v) {
			if (v instanceof LazyGraphModel.CollapsedNode)
				return collapsedHandler.transform(v);
			if (GUITerm.findProperty(NOICONSHAPE,v.properties)!=null)
				return delegate.transform(v);
			Icon icon = null;
			if (iconMap!=null)
				icon = iconMap.get(v);
			else{
				TermModel iconLocation = GUITerm.findProperty(ICON,v.properties);
				if(iconLocation != null)
					icon = XJDesktop.fetchIcon(this,iconLocation);
			}
			if (icon != null && icon instanceof ImageIcon) {
				Image image = ((ImageIcon) icon).getImage();
				Shape shape = (Shape) shapeMap.get(image);
				if (shape == null) {
					shape = FourPassImageShaper.getShape(image, 30);
					if(shape.getBounds().getWidth() > 0 && 
							shape.getBounds().getHeight() > 0) {
						// don't cache a zero-sized shape, wait for the image
					   // to be ready
						int width = image.getWidth(null);
						int height = image.getHeight(null);
						AffineTransform transform = AffineTransform
							.getTranslateInstance(-width / 2, -height / 2);
						shape = transform.createTransformedShape(shape);
						shapeMap.put(image, shape);
					}
				}
				return shape;
			} else {
				return delegate.transform(v);
			}
		}
	}
	
    class NodeIconTransformer extends DefaultVertexIconTransformer<LazyGraphModel.Node>
    	implements Transformer<LazyGraphModel.Node,Icon> {
        
        boolean fillImages = true;
        boolean outlineImages = false;

        public boolean isFillImages() {
            return fillImages;
        }
        public void setFillImages(boolean fillImages) {
            this.fillImages = fillImages;
        }

        public boolean isOutlineImages() {
            return outlineImages;
        }
        public void setOutlineImages(boolean outlineImages) {
            this.outlineImages = outlineImages;
        }
        
        public Icon transform(LazyGraphModel.Node v) {
            if(fillImages) {
                Icon icon = (Icon)iconMap.get(v);
                if (icon==null){
					TermModel iconLocation = GUITerm.findProperty(ICON,v.properties);
					if(iconLocation != null)
						icon = XJDesktop.fetchIcon(this,iconLocation);
                	}
                return icon;
            } else {
                return null;
            }
        }
    }
    
    static class SizableVertexShapeFunction extends EllipseVertexShapeTransformer<LazyGraphModel.Node> {
    	/** size is default size */
    	SizableVertexShapeFunction(int size){
            setSizeTransformer(new VertexSizeFunction(size));
    	}
        public Shape transform(LazyGraphModel.Node v) {
        	return factory.getEllipse(v);
        }
    }

	/** Adapted from JUNG's VertexCollapseDemo */
    static class ClusterVertexShapeFunction<V> extends EllipseVertexShapeTransformer<LazyGraphModel.Node> {

        ClusterVertexShapeFunction(int size) {
            setSizeTransformer(new VertexSizeFunction(size));
        }
        public Shape transform(LazyGraphModel.Node v) {
            if(v instanceof LazyGraphModel.CollapsedNode) {
                int size = ((LazyGraphModel.CollapsedNode)v).getClusterGraph().getVertexCount();
                if (size < 8) {   
                    int sides = Math.max(size, 3);
                    return factory.getRegularPolygon(v, sides);
                }
                else {
                    return factory.getRegularStar(v, size);
                }
            }
            throw new XJException("inconsistent non-CollapsedNode handling:"+ v);
            // return super.transform(v); // This actually never runs...
        }
    }
    static class VertexSizeFunction implements Transformer<LazyGraphModel.Node,Integer> {
    	Integer defaultSize;
        public VertexSizeFunction(Integer size) {
            defaultSize = size;
        }
        public Integer transform(LazyGraphModel.Node v) {
			TermModel sizeTerm = GUITerm.findProperty(SIZE,v.properties);
			if (sizeTerm != null) return (Integer) sizeTerm.node;
            else return defaultSize;
        }
    }    	
	
	Transformer<LazyGraphModel.Edge,String> edgeLabel = new Transformer<LazyGraphModel.Edge,String>(){
		public String transform(LazyGraphModel.Edge edge){
			String R = edge.term.toString();
			return (R.equals("null")?null:R);
		}
	};
		
	Transformer<LazyGraphModel.Edge,String> edgeTooltip = new Transformer<LazyGraphModel.Edge,String>(){
		public String transform(LazyGraphModel.Edge edge){
			String R = edge.tip.toString();
			return (R.equals("null")?null:R);
		}
	};
		
	Transformer<LazyGraphModel.Edge,Paint> edgePaint = new Transformer<LazyGraphModel.Edge,Paint>(){
		public Paint transform(LazyGraphModel.Edge edge){
			TermModel color = GUITerm.findProperty(GUITerm.COLOR, edge.properties);
			if (color==null) return Color.GRAY;
			return GUITerm.termToColor(color);
		}
	};
	
	final Stroke dashedStroke = 
		new BasicStroke(1.0f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f);
	final Stroke dottedStroke = 
		new BasicStroke(1.0f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f}, 0.0f);
		
	Transformer<LazyGraphModel.Edge,Stroke> edgeStroke = new Transformer<LazyGraphModel.Edge,Stroke>(){
		public Stroke transform(LazyGraphModel.Edge edge){
			if (GUITerm.findProperty(DASHED, edge.properties)!=null)
				return dashedStroke;
			else if (GUITerm.findProperty(DOTTED, edge.properties)!=null)
				return dottedStroke;
			else return null;
		}
	};
		
	public void setPickingMode(){
		graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
	}
	public void setTransformingMode(){
		graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
	}
	public void addNodes(TermModel IDs){
		addNodes(IDs,false,false);
	}
	
	public void addNodes(TermModel IDs,boolean expandIncoming,boolean expandOutcoming){
		XJDesktop.setWaitCursor(this);
		model.addNodes(IDs,expandIncoming,expandOutcoming);
		vv.repaint();
		XJDesktop.restoreCursor(this);
	}
	
	public void expandAll(){
		XJDesktop.setWaitCursor(this);
		model.expandAll();
		vv.repaint();
		XJDesktop.restoreCursor(this);
	}
	public void expandAllVertices(){
		XJDesktop.setWaitCursor(this);
		model.expandAllVertices();
		vv.repaint();
		XJDesktop.restoreCursor(this);
	}
	public void expandVertices(TermModel[] IDs){
		model.expandVertices(IDs);
		vv.repaint();
	}
	public void expandVertex(TermModel ID){
		expandVertices(new TermModel[]{ID});
	}
	public void unexpandVertex(TermModel ID){
		model.unexpandVertex(ID);
		vv.repaint();
	}
	public void removeNode(TermModel ID){
		model.removeNode(ID);
		vv.repaint();
	}
	
	public void reapplyLayout(){
		Layout<Node, Edge> L = vv.getGraphLayout();
		XJDesktop.setWaitCursor(this);
		L.setSize(vv.getSize());
		L.initialize();
		XJDesktop.restoreCursor(this);
	}
	public void applyLayout(String name){
		Layout<LazyGraphModel.Node,LazyGraphModel.Edge> graphLayout = layoutForName(name,model);
        graphLayout.setInitializer(vv.getGraphLayout());
        graphLayout.setSize(vv.getSize());
        graphLayout.initialize();
        
        for (LazyGraphModel.Node node : model.getVertices())
        	if (vv.getGraphLayout().isLocked(node))
        		graphLayout.lock(node,true);
        		
		//LayoutTransition<LazyGraphModel.Node,LazyGraphModel.Edge> lt =
		//	new LayoutTransition<LazyGraphModel.Node,LazyGraphModel.Edge>(vv, vv.getGraphLayout(), graphLayout);
		//Animator animator = new Animator(lt);
		//animator.start();
		//vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
		vv.setGraphLayout(graphLayout);
		vv.repaint();
	}
	public TermModel[] getSelectedNodeIDs(){
		Collection<LazyGraphModel.Node> picked = vv.getPickedVertexState().getPicked();
		if (picked.size()==0) return new TermModel[0];
		else {
			TermModel[] R = new TermModel[picked.size()];
			int t=0;
			for (Iterator<LazyGraphModel.Node> i = picked.iterator(); i.hasNext();)
				R[t++] = i.next().ID;
			return R;
		}
	}
	public void collapseSelectedNodes(){
		collapseNodes(getSelectedNodeIDs());
	}
	public void collapseNodes(TermModel[] IDs){
	
		Collection<LazyGraphModel.Node> picked = model.IDsToNodes(IDs);
		collapseNodes(picked);
	}
	void collapseNodes(Collection<LazyGraphModel.Node> picked){
		if(picked.size() > 1) {
			if (!model.allExpanded(picked,false)) // weird bug here!
				throw new XJException("Only fully expanded nodes can be collapsed");
			
			LazyGraphModel clusterGraph = (LazyGraphModel)collapser.getClusterGraph(model, picked);
			
			Layout<LazyGraphModel.Node,LazyGraphModel.Edge> layout = vv.getGraphLayout();
			
			LazyGraphModel.CollapsedNode collapsedNode = new LazyGraphModel.CollapsedNode(clusterGraph,model);

			model = collapser.collapse(model, collapsedNode);
			
			double sumx = 0;
			double sumy = 0;
			for(LazyGraphModel.Node v : picked) {
				Point2D p = (Point2D)layout.transform(v);
				sumx += p.getX();
				sumy += p.getY();
			}
			Point2D cp = new Point2D.Double(sumx/picked.size(), sumy/picked.size());
			vv.getRenderContext().getParallelEdgeIndexFunction().reset();
			
			layout.setGraph(model);
			layout.setLocation(collapsedNode, cp);
			
			vv.getPickedVertexState().clear();
			vv.repaint();
		}
	}
	
	public void uncollapse(TermModel ID){
		LazyGraphModel.Node N = model.idToNode(ID);
		if (N==null) return;
		if (!(N instanceof LazyGraphModel.CollapsedNode))
			throw new XJException("Bad node to uncollapse:"+ID);
		Layout<LazyGraphModel.Node,LazyGraphModel.Edge> layout = vv.getGraphLayout();
		model = collapser.expand(model, (LazyGraphModel.CollapsedNode)N);
		vv.getRenderContext().getParallelEdgeIndexFunction().reset();
		layout.setGraph(model);
		vv.getPickedVertexState().clear();
	   	vv.repaint();		
	}
	
	public void lock(TermModel ID){
		LazyGraphModel.Node node = model.idToNode(ID);
		vv.getGraphLayout().lock(node,true);
	}
	
	public void unlock(TermModel ID){
		LazyGraphModel.Node node = model.idToNode(ID);
		vv.getGraphLayout().lock(node,false);
	}
	
	public void collapseWeakComponents(){
		WeakComponentClusterer<LazyGraphModel.Node,LazyGraphModel.Edge> clusterer = new WeakComponentClusterer<LazyGraphModel.Node,LazyGraphModel.Edge>();
		Set<Set<LazyGraphModel.Node>> clusters = clusterer.transform(model);
		//System.out.println(clusters);
		// System.out.println("after:"+model.allExpanded(model.getVertices(),false)); // all expanded...
		for (Set<LazyGraphModel.Node> cluster : clusters)
			if (model.allExpanded(cluster,false)) {
				collapseNodes(cluster);
				System.out.println("Collapsed:"+cluster);
			} else System.err.println("Bad cluster:"+cluster); // some are not!!!!
		System.out.println("even later:"+model.allExpanded(model.getVertices(),false)); // all expanded...
		// System.out.println("even later2:"+model.allExpanded(new java.util.HashSet<LazyGraphModel.Node>(model.getVertices()),false)); // all expanded...
	}
	
	// XJComponent methods:
	public PrologEngine getEngine(){ return engine; }
	public GUITerm getGT(){ return gt; }
	public void setGT(GUITerm gt){
        throw new XJException("Inconsistent setGT:" + gt);
	}
	public void refreshGUI(){} // done by JUNG logic
	
	/** Load the GUITerm with latest data edited by the user; typically this will be messaged only for nodes, not XJComponentTrees. 
	Can ignore the request if !isDirty(), and should clear the dirty flag if it processes the request*/
	public boolean loadFromGUI(){
		return true; // no data editing in graphs
	}
	/** The XJComponent has one node changed by the user that is currently different from its gt (GUITerm) node value, typically because
	the user edited it. GUITerm intrinsic changes do not reflect into this state variable*/
	public boolean isDirty(){
		return false; // no data editing in graphs
	}
	public void setDefaultValue(TermModel d){} // pointless for graphs
	
	/** XJComponents unable to have more than one selection should throw an exception if selection.length>1 */
	public void selectGUI(Object[] selectionParts){
        throw new XJException("missing selectGUI");
	}
        
    public void destroy(){
    	//TODO: cleanup JUNG objects and model
    }
	
}