package com.xsb.xj;
import com.declarativa.interprolog.*;
import com.declarativa.interprolog.util.*;
import com.xsb.xj.util.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.awt.dnd.*;
import java.lang.reflect.*;

import javax.swing.border.Border;

/** A GUITerm represents a Prolog term annotated with extra information to make it editable. 
Whereas a Prolog term is a tree with nodes, representable by TermModel, GUITerm represents a tree
where each node is annotated with a list of properties; each property is a Prolog term. 
A checker of some sort should be run on GTs... checking for example that no gui(..) actions are
defined under invisible subtrees!*/
public class GUITerm extends TermModel {
	private static final long serialVersionUID = 9027166989380943408L;
	/** COMMENTS ON THE FOLLOWING ARE IN OTHER DOCS */
	public static final String TYPENAME = "typename";
	public static final String OPTIONAL = "optional";
	public static final String NULLABLE = "null";
	public static final String DEFAULT = "default";
	public static final String ATOM = "atom";
	public static final String ATOMUPPER = "atomUPPER";
	public static final String COMBOBOX = "comboBox";
	public static final String PLAINCOMBO = "plainCombo";
	public static final String EDITABLE = "editable";
	public static final String INTEGER = "integer";
	public static final String NUMBER = "number";
	public static final String FLOAT = "float";
	public static final String MINSIZE = "minSize";
	public static final String MAXSIZE = "maxSize";
	public static final String AUTOSIZE = "autosize";
	public static final String LARGE = "large";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	public static final String BORDERLESS = "borderless";
	public static final String BORDER = "border";
	public static final String FONT = "font";
	public static final String CAPTION = "caption";
	public static final String COLOR = "color";
	public static final String TYPICALSIZE = "typicalSize";
	public static final String DISABLED = "disabled";	
	public static final String EXAMPLE = "example";
	public static final String TIP = "tip";
	public static final String EMPTYTIP = "emptytip";
	public static final String LIST = "list";
	public static final String LAZYTREE = "lazytree";
	public static final String LAZYLIST = "lazylist";
	public static final String XJ_LAZY = "XJ$LAZY";
	public static final String PROLOGCACHED = "prologcached";
	public static final String HIDDENROOT = "hiddenroot";
	public static final String JAVACOMPONENT = "javaComponent";
	public static final String CLASS = "class";
	public static final String MYGUI = "myGUI";
	public static final String OPAQUE = "opaque";
	public static final String NONPERSISTENT = "nonpersistent";
	public static final String INVISIBLE = "invisible";
	public static final String CONSTANT = "constant";
	public static final String READONLY = "readonly";
	public static final String ROOT = "root";
	public static final String OPERATION = "operation";
	public static final String FUNCTION = "function";
	public static final String CALLIFQUICK = "callifquick";
	public static final String INSERTABLE = "insertable";	
	public static final String UPDATABLE = "updatable";	
	public static final String DELETABLE = "deletable";	
	public static final String ISNEWTERM = "isNewTerm";	
	public static final String DRAGSOURCE = "dragsource";	
	public static final String SINGLESELECTIONS = "singleselections";
    public static final String BACKGROUND = "background";
    public static final String ADJUSTTOFIRSTROW = "adjustToFirstRow";
    public static final String RENDERTOPNODE = "renderTopNode";
    public static final String RENDERALLNODES = "renderAllNodes";
    public static final String DRY = "dry";
    public static final String SELECTIONCHANGED = "selectionChanged";
    public static final String ROWCOUNTCHANGED = "rowCountChanged";
    public static final String DOUBLECLICK = "doubleclick";
    public static final String GUI = "gui";
	
	/** String=Value and String terms */
	TermModel[] properties;
    String module = null;
        
	private transient Vector<UndoableEditListener> undoListeners=null /*new Vector() here doesn't work on deserialization...*/;
	private transient boolean notifyUndoListeners=true;
	/** defines data context for operations */
	transient GUITerm oproot;
	/** flag to avoid recreating XJActions whose Description refers a gui(R) object */
	transient boolean guiOperationsCreated = false;
	/** XJComponent rendering this node, or this subtree if it is a XJComponentTree. This may later evolve to a Vector */
	transient private XJComponent renderer=null;
	
	static TermModel[] TMA = new TermModel[]{ new TermModel("uh")};
	public static ObjectExamplePair example1(){
		
		return new ObjectExamplePair("GUITerm",
			new GUITerm(new Integer(1),null,null,false),
			new GUITerm(".",TMA,new TermModel[0],true)
			);
	}
        
	public static ObjectExamplePair example2(){
		return new ObjectExamplePair("GUITerm",
			new GUITerm("basics",new Integer(1),null,null,false),
			new GUITerm("usermod",".",TMA,null,true)
		);
	}
	
	GUITerm(Object n){this(n,null,null,false);}
	
	public GUITerm(Object n,TermModel[] p,TermModel[] c,boolean isAlist){
		super(n,c,isAlist);
		if (p==null) properties = new TermModel[0];
		else properties=p;
	}
        
	public GUITerm(String module, Object n,TermModel[] p,TermModel[] c,boolean isAlist){
		this(n, p, c, isAlist);
		this.module = module;
	}
	
	/** Factory method to construct a JComponent, simply following what is stipulated in the 'class' property,
	if there is one. Otherwise it applies a simple default policy, using standard XJComponents; this policy 
	refuses to digest a non-leaf GUITerm 
	 */
	public XJComponent makeGUI(PrologEngine engine){
		return makeGUI(engine,false,null);
	}
        
	/**
	 * Same as makeGUI(PrologEngine) but binds bindVars and returns a term model
	 * with a created XJComponent as a first child and bound bindVars as a
	 * second child
	 */
	public TermModel makeGUI(PrologEngine engine, TermModel bindVars){
		TermModel [] oldChildren = children;
		if(bindVars != null){
			GUITerm gtForVars = 
				new GUITerm(bindVars.node,
							new TermModel[]{new TermModel(OPAQUE), 
											new TermModel(INVISIBLE), 
											new TermModel(NONPERSISTENT)},
							bindVars.children,false);
			if(children == null){
				children = new GUITerm[]{gtForVars};
			} else {
				 TermModel[] newChildren = new TermModel [children.length + 1];
				 System.arraycopy(children, 0, newChildren, 0, children.length);
				 newChildren[children.length] = gtForVars;
				 children = newChildren;
		   }
		}
		
		XJComponent comp = makeGUI(engine,false,null);
		children = oldChildren;
		return new TermModel("", 
							 new TermModel[]{new TermModel(engine.registerJavaObject(comp)), 
											 bindVars});
	}

	public XJComponent makeGUIforNode(PrologEngine engine,Class<?> toAvoid){
		return makeGUI(engine,true,toAvoid);
	}
	
	private XJComponent makeGUI(PrologEngine engine, boolean onlyForNode,Class<?> toAvoid){
		// System.out.println("entering makeGUI for GUITerm "+this+", node "+node+" avoid "+toAvoid);
		checkRoots();
		Vector<XJAction> guiActions = null;
		if (!guiOperationsCreated){
			guiActions = new Vector<XJAction>();
			markAndCollectGUIActions(engine,guiActions);
		}
		XJComponent component = null;
		
		
		boolean useClassProperty=false;
		Class<?> theClass=null;
		TermModel cl = findProperty(CLASS);
		if (cl!=null){
			String className = (String)cl.node;
			try{theClass = Class.forName (className);} 
			catch (ClassNotFoundException e){
				throw new XJException("Could not find class "+className+":"+e);
			}
			useClassProperty = (toAvoid==null) || !(toAvoid.equals(theClass));
		}
		TermModel JC = findProperty(JAVACOMPONENT);
		if (JC!=null){
			if (properties.length!=1)
				throw new XJException("A component specified by javaComponent can have no more properties");
			if (!JC.isInteger())
				throw new XJException("A javaComponent must be referenced by its int ref");
			Object X = engine.getRealJavaObject(JC.intValue());
			if (!(X instanceof XJComponent))
				throw new XJException("javaComponent properties must indicate a preexisting XJComponent");
			component = (XJComponent)X;
			component.setGT(this);
			// return immediately, this is indeed a special case:
			return component;
		} else if (useClassProperty){
			try{
	 			int nargs = cl.getChildCount();
	       		Class<?> formalArguments[] = new Class[nargs+2];
	        	formalArguments[0] = PrologEngine.class;
	        	formalArguments[1] = GUITerm.class;
	        	for (int a=0;a<nargs; a++) 
	        		formalArguments[a+2]=TermModel.class;
	            Constructor<?> constructor = AbstractPrologEngine.findConstructor (theClass,formalArguments);
	        	Object[] localArguments = new Object[formalArguments.length];
	        	localArguments[0] = engine;
	        	localArguments[1] = this;
	         	for (int a=0;a<nargs; a++) 
	        		localArguments[a+2]=(TermModel)cl.getChild(a);
	           	component = (XJComponent) constructor.newInstance(localArguments);
           	} catch(InvocationTargetException e){
           		e.printStackTrace();
           		throw new XJException("Could not invoke 'class' constructor for "+theClass+":"+e.getTargetException());
           	} catch (Exception e){
           		throw new XJException("Could not execute 'class' property: "+e);
           	}
		} 
		else if (findProperty(LARGE)!=null) component = new XJTextArea(engine,this);
		else if (isOpaque()) component = new XJAtomicField(engine,this);
		else if (isComboField()) component = new XJComboField(engine,this);
		else if (isList()||isLazyList()) component = new XJTable(engine,this); 
		else if (isLazyTree()) component = new XJTree(engine,this); 
		else if (!isLeaf() && !onlyForNode) 
			// Non-leaves must be digested as per specific class properties
			// throw new XJException("Inconsistency in makeGUI for "+this+"\nProperties:"+propsDescription());
			component = new ValueRow(engine,this); // default policy
		else if (isConstant()||isReadOnly()) component = new XJLabel(engine,this);
		else component = new XJAtomicField(engine,this);

		// a component needs to have a renderer before attaching operations
		// since gt.getARenderer() is called from action.attachToGUI()
		addRenderer(component);

		bindMyGUIvars(engine);

		if (guiActions!=null){ 
			// this makeGUI invocation is responsible for binding the actions;
			// so this must have been sent to the root of this term
			if (!isRoot()) throw new XJException("Action attachments should happen at the root");
			for(int a=0;a<guiActions.size();a++){
				XJAction action = (XJAction)guiActions.elementAt(a);
				action.attachToGUI();
			}
		}
		
		if (findProperty(ISNEWTERM)!=null)
			setAllDefaultValues();
		// if (isInvisible()) System.out.println("invisible node !!!");
                
        TermModel disabled = findProperty(DISABLED);
		if(disabled != null && component instanceof JComponent) {
			((JComponent)component).setEnabled(false);
		}
		
        TermModel color = findProperty(COLOR);
		if (color!=null){
			Color f = termToColor(color);
			if (f==null) 
				throw new XJException("Unknown color:"+color);
			((Component)component).setForeground(f);
		}
                
                
        TermModel background = findProperty(BACKGROUND);
		if (background != null){
			Color f = termToColor(background);
			if (f == null) 
				throw new XJException("Unknown color:" + background);
			((Component)component).setBackground(f);
		}
		
		TermModel border = findProperty(BORDER);
		if(border != null && component instanceof JComponent) {
			((JComponent)component).setBorder(getXJBorder(border, findProperty(CAPTION)));
		}
		
		TermModel font = findProperty(FONT);
		if(font != null && component instanceof JComponent) {
			setGtFont(font, (JComponent)component);
		}
                
		if (isRoot()){
			// message all XJTemplateComponents:
			XJTemplateComponent[] tcs = collectTemplateRenderers();
			for (int c=0;c<tcs.length;c++)
				tcs[c].constructionEnded();
		}
		// handle DnD source properties:
		if (findProperty(DRAGSOURCE)!=null){
			if (!(component instanceof DnDCapable))
				throw new XJException("'dragsource' can only be used in a DnDCapable component:"+this);
			DnDCapable dndComponent = (DnDCapable)component;
			DragGestureListener dgl = dndComponent.createDragGestureListener();
			if (dgl==null) 
				throw new XJException("'dragsource' requires a DnDCapable component implementing createDragGestureListener:"+this);
			new DragSource().createDefaultDragGestureRecognizer(dndComponent.getRealJComponent(),DnDConstants.ACTION_COPY,dgl);
		}
		// handle DnD destination operations:
		XJAction[] dropped = XJAction.findDropped(operations(engine,(JComponent)component));
		if (dropped!=null){
			if (!(component instanceof DnDCapable))
				throw new XJException("'dropped' operations can only be defined in nodes rendered by a DnDCapable component:"+this);
			DnDCapable dndComponent = (DnDCapable)component;
			for(int i = 0; i < dropped.length ; i++){
			    // Consider the operation as an integral part of the DnD interaction:
			    dropped[i].setThreaded(false); // commenting this line  allows subsequent modal interactions... but I'm afraid of allowing them 
             }
			XJDropTargetListener dropHandler = new XJDropTargetListener(component,dropped);
			// Drop will be into the "real", possibly contained, JComponent:
			new DropTarget(dndComponent.getRealJComponent(),dropHandler);
		}
		// System.out.println("...leaving makeGUI for node "+node);
		// Thread.yield(); // (failed) attempt to fix some lag finishing xj actions
		
		if (component instanceof Component){
			Component C = (Component)component;
			Dimension CD = C.getPreferredSize();
			Dimension GTD = getPreferredSize(CD);
			//System.out.println("CD=="+CD);
			//System.out.println("GTD=="+GTD);
			//System.out.println("properties=="+Arrays.toString(properties));
			if (CD!=null && !CD.equals(GTD)){
				C.setPreferredSize(GTD);
				//System.out.println("Set dimension of "+this+" to "+GTD);
			}
		}
		return component;
	}
	
	public void bindMyGUIvars(PrologEngine engine){
		
		// bind myGUI property variables for this node, and their occurrences all over the term
		TermModel[] myGUIs = findProperties(MYGUI);
		for(int g=0;g<myGUIs.length;g++) {
			TermModel guiVar = (TermModel)myGUIs[g].getChild(0); // myGUI(G)
			if (!(guiVar.node instanceof VariableNode) && !(guiVar.node instanceof Integer)) 
				throw new XJException("myGUI property argument should be a variable or an Integer object");
			/* too strong a test:
            if (guiVar.node instanceof Integer){
            	if (!engine.getRealJavaObject(guiVar.intValue()).equals(component)){
            		System.err.println("already knew:"+engine.getRealJavaObject(guiVar.intValue()));
            		throw new XJException("Inconsistency in myGUI handling, component:"+component);
            	}
            }*/
            if (renderer!=null && !(guiVar.node instanceof Integer)) {
            	root.assignToVar((VariableNode)guiVar.node, new Integer(engine.registerJavaObject(renderer)));
            }
		}
		
	}
	
	/**
	 * Creates a border using BorderFactory. In gui term user should specify
	 * border=(line|etched|raised|lowered|titled|empty) or border(top,left,bottom,right).
	 * Note : Titled border takes the title from caption property.
	 * Note : Empty border has preset insets.
	 * Note : Using border(t,l,b,r), creates an emtpy border with custom insets.
	 * 
	 * @param  border  	Term border
	 * @param  caption 	Term caption
	 * @return 			Returns a border.
	*/
	public static Border getXJBorder(TermModel border, TermModel caption) {
		if(border.getChildCount() == 4) {	//border(top,left,bottom,right)
			int top     = ((TermModel) border.getChild(0)).intValue();
			int left    = ((TermModel) border.getChild(1)).intValue();
			int bottom  = ((TermModel) border.getChild(2)).intValue();
			int right   = ((TermModel) border.getChild(3)).intValue();
			return BorderFactory.createEmptyBorder(top, left, bottom, right);
		} else {
			String borderType  = (String) border.node;
			if(borderType.equals("line")) {
				return BorderFactory.createLineBorder(Color.black);
			} else if(borderType.equals("etched")) {
				return BorderFactory.createEtchedBorder();
			} else if(borderType.equals("raised")) {
				return BorderFactory.createRaisedBevelBorder();
			} else if(borderType.equals("lowered")) {
				return BorderFactory.createLoweredBevelBorder();
			} else if(borderType.equals("empty")) {
				return BorderFactory.createEmptyBorder(12, 11, 12, 11);	
			} else if(borderType.equals("titled")) {
				String title = "";
				if(caption != null) {
					title  = (String) caption.node;
				}
				return BorderFactory.createTitledBorder(title);
			} else {
				return null;
			}
		}
	}
	
	/** 
	 * Changes the font for a XJComponent provided it is an instance of JComponent.
	 * In prolog the font is specified as a term in the gt as 
	 * font(Name, Style, Size). Name is a string, and can refer to any font 
	 * available on the system. Be aware font availability changes from system to
	 * system. Style is a string with values b for bold, i for italic and bi for
	 * bold and italic. If no value is specified plain is assumed. Size has to be
	 * an integer. If the name of the font is not specified the default font for
	 * the component is kept and specified values are changed. If the font name 
	 * is specified a new font is created for the component.
	 * 
	 * @param  font		 font prolog term of the form font(Name, Style, Size).
	 * @param  component component whose font is to be changed.
	 */
	public static void setGtFont(TermModel font, JComponent component) {
		if(font.getChildCount() != 3) {
			System.out.println("Font needs to be specified as font(Name, Style, Size)");
			return;
		}
		
		if (component instanceof JScrollPane){
			JViewport v = ((JScrollPane)component).getViewport();
			if (v!=null && v.getView()!=null)
				component = (JComponent)v.getView();
		}
	
		TermModel fontName = (TermModel) font.getChild(0);
		TermModel fontStyle= (TermModel) font.getChild(1);
		TermModel fontSize = (TermModel) font.getChild(2);
		
		int font_size	   = 0;
		int font_style	   = Font.PLAIN;
		
		if(fontSize.isInteger()) {
			try {
				font_size = fontSize.intValue();
			} catch(Exception e) {
				e.printStackTrace();
				System.err.println("font size must be an integer");
			}
		}

		if(fontStyle.isAtom()) {
			String fs = fontStyle.toString();
			if(fs.equalsIgnoreCase("b")) {			//bold
				font_style = Font.BOLD;
			} else if(fs.equalsIgnoreCase("i")) {	//italic
				font_style = Font.ITALIC;
			} else if(fs.equalsIgnoreCase("bi")		//bold + italic 
				|| fs.equalsIgnoreCase("ib")) {
				font_style = Font.ITALIC + Font.BOLD;
			}
		} 
	
		if(fontName.isVar()) {
			if(font_size > 0) {
				//change size and style
				component.setFont(component.getFont().deriveFont(font_style, font_size));
			} else {
				//change style
				component.setFont(component.getFont().deriveFont(font_style));
			}
		} else {
			String font_name = fontName.toString();
			System.out.println("name : " + font_name);
			//change font,style and size
			component.setFont(new Font(font_name, font_style, font_size));
		}
	}
	
	public static Color termToColor(TermModel color){
		if (!(color.node instanceof String))
			throw new XJException("color must be an atom or of a form rgb(int,int,int)");
		String c = (String)color.node;
		if (c.equals("green")) return Color.green;
		else if (c.equals("red")) return Color.red;
		else if (c.equals("black")) return Color.black;
		else if (c.equals("blue")) return Color.blue;
		else if (c.equals("cyan")) return Color.cyan;
		else if (c.equals("darkGray")) return Color.darkGray;
		else if (c.equals("gray")) return Color.gray;
		else if (c.equals("lightGray")) return Color.lightGray;
		else if (c.equals("magenta")) return Color.magenta;
		else if (c.equals("orange")) return Color.orange;
		else if (c.equals("pink")) return Color.pink;
		else if (c.equals("white")) return Color.white;
		else if (c.equals("yellow")) return Color.yellow;
		else if (c.equals("rgb") && (color.getChildCount()==3)) {
                    TermModel red = (TermModel)color.getChild(0);
                    TermModel green = (TermModel)color.getChild(1);
                    TermModel blue = (TermModel)color.getChild(2);
                    return new Color(red.intValue(),green.intValue(),blue.intValue());
                } else return null;
	}
	
	/** First come, first considered. Typically recursive makeGUI calls will cause the "simpler" XJComponent
	to be taken as renderer for the node. */
	// we hope
	public void addRenderer(XJComponent r){
		if (r==null) throw new XJException("Null renderer");
		if (renderer!=null) return; // throw new XJException("Only one renderer allowed");
		renderer=r;
	}
	
	/** Send setDefaultValue to the GUI renderers for this subtree, top down, left to right */
	public void setAllDefaultValues(){
		if (renderer!=null) {
			renderer.setDefaultValue(findProperty(DEFAULT));
			if (!isLeaf())
				for (int i=0;i<getChildCount();i++)
					((GUITerm)getChild(i)).setAllDefaultValues();
		}
	}
	
	/** Redraw the GUI renderers for this subtree, top down, left to right */
	public void refreshRenderers(){
		if (renderer!=null) {
			renderer.refreshGUI();
			if (!isLeaf())
				for (int i=0;i<getChildCount();i++)
					((GUITerm)getChild(i)).refreshRenderers();
		}
	}
	
	/** Ask all renderers to loadFromGUI, return false if any fails */
	
	public boolean loadAllFromGUI(){
		if (renderer!=null) {
			if (!renderer.loadFromGUI()) return false;
			if (!isLeaf()){
				for (int i=0;i<getChildCount();i++){
					if (getChild(i) instanceof GUITerm)
						if (!((GUITerm)getChild(i)).loadAllFromGUI()) return false;
                }
            }
		}
		return true;
	}
        
        /** Call destroy() method for the GUI renderers for this subtree, top down, left to right */
	public void destroyRenderers(){
            if (renderer!=null) {
                renderer.destroy();
                if (!isLeaf())
                    for (int i=0;i<getChildCount();i++)
                        ((GUITerm)getChild(i)).destroyRenderers();
            }
	}
	
	/** Returns an array with all XJComponents rendering nodes in this term, ordered as per a depth
	first left to right visit, i.e. the makeGUI order. List and tree template components are not returned */
	// Not anymore: Renderers below an XJComponentTree renderer were not considered
	public XJComponent[] collectSignificantRenderers(){
		Vector<XJComponent> rv = new Vector<XJComponent>();
		collectSignificantRenderers(rv);
		XJComponent[] ra = new XJComponent[rv.size()];
		for (int i=0;i<ra.length;i++)
			ra[i]=(XJComponent)rv.elementAt(i);
		return ra;
	}
	
	void collectSignificantRenderers(Vector<XJComponent> rv){
		if (renderer != null && !(rv.contains(renderer))){
			rv.addElement(renderer);
			//if (renderer instanceof XJComponentTree) return;
		}
		// if (renderer instanceof XJComponentTree) 
		//throw new XJException("bad place for a XJComponentTree");
		if (!isLeaf())
			for(int i=0;i<getChildCount();i++){
				GUITerm child = (GUITerm)getChild(i);
				if (child.isLazyList()||child.isLazyTree()) continue;
				child.collectSignificantRenderers(rv);
			}
	}
	
	public XJTemplateComponent[] collectTemplateRenderers(){
		Vector<XJComponent> rv = new Vector<XJComponent>();
		collectTemplateRenderers(rv);
		XJTemplateComponent[] ra = new XJTemplateComponent[rv.size()];
		for (int i=0;i<ra.length;i++)
			ra[i]=(XJTemplateComponent)rv.elementAt(i);
		return ra;
	}
	
	void collectTemplateRenderers(Vector<XJComponent> rv){
		if (renderer != null && !(rv.contains(renderer)) && renderer instanceof XJTemplateComponent){
			rv.addElement(renderer);
			TermModel tm = ((XJTemplateComponent)renderer).getTemplate();
			if (tm instanceof GUITerm) ((GUITerm)tm).collectTemplateRenderers(rv);
			else // polimorphic tree; assumes propertiers TermModel has a flat structure... this seems to be working:
				for (int t=0;t<tm.getChildCount();t++) 
					((GUITerm)tm.getChild(t)).collectTemplateRenderers(rv);
		} else if (!isLeaf())
			for(int i=0;i<getChildCount();i++){
				if (getChild(i) instanceof GUITerm)
					((GUITerm)getChild(i)).collectTemplateRenderers(rv);				
			}
	}
	
	/** Until we decide we really have only one renderer, let's call this "get A renderer" */ 
	public XJComponent getARenderer(){
		return renderer;
	}
	
	public boolean isComboField(){
		return findProperty(GUITerm.COMBOBOX) != null;
	}
	
	public boolean isLazyTree(){
		return (node.equals(XJ_LAZY) || node.equals(".")) && findProperty(LAZYTREE)!=null;
	}
	
	public boolean isLazyList(){
		return node.equals(XJ_LAZY) && findProperty(LAZYLIST)!=null;
	}
		
	public boolean isGraph(){
		if (!node.equals(XJ_LAZY)) return false;
		TermModel P = findProperty(CLASS);
		return P!=null && P.node.equals("com.xsb.xj.XJGraph");
	}
		
	/* too strong:
	public boolean isList(){
		return super.isList() && findProperty(LIST)!=null;
	}*/
	
	void markAndCollectGUIActions(PrologEngine engine,Vector<XJAction> GUIActions){
		// Component will be set afterwards for GUI actions, and others will be thrown away
		XJAction[] actions = operations(engine,null); 
		for (int a = 0; a<actions.length; a++)
			if(actions[a].isGUIOperation()) 
                GUIActions.addElement(actions[a]);
		// Do not collect/mark actions in templates:
		if (isLazyList()) ;// lazyListTemplate().markAndCollectGUIActions(engine,GUIActions);
		else if (isLazyTree()) ;// lazyTreeTemplate().markAndCollectGUIActions(engine,GUIActions);
		else if (isList()) ;// listTemplate().markAndCollectGUIActions(engine,GUIActions);
		else if (!isLeaf()) {
			for (int c=0;c<getChildCount();c++)
				if (getChild(c) instanceof GUITerm)
					((GUITerm)getChild(c)).markAndCollectGUIActions(engine,GUIActions);
		}
		guiOperationsCreated = true;
	}
	
	/** Visits GUITerm propagating operation roots (oproot) to descendents, processing 'root' 
	properties along the way. It also
	defines and propagates the root variable; this works on the assumption that the first GUITerm 
	receiving checkRoots() is the whole term*/
	public void checkRoots(){
		setRoot();
		setOpRoot(oproot);
	}	

	protected void propagateRoot(){
		super.propagateRoot();
		for(int i=0;i<properties.length;i++)
			properties[i].setRoot(root);
	}
		
	/** Sets this GUITerm to be the oproot for its descendents, unless 'root' properties are found; each such property
	defines a new oproot. Furthermore, template-based multi tuple sub terms (lists and trees) establish
	new oproot scopes: their ancestor 'root' properties will not be considered. So operations and functions 
	defined in templates require in-template root properties */
	
	public void setOpRoot(){
		setOpRoot(this);
	}
	private void setOpRoot(GUITerm r){
		if (oproot!=null && r!=null && oproot!=r) throw new XJException("Op Roots must not be changed");
		if (oproot!=null) return; // subtree already set
		if (findProperty(ROOT)!=null) oproot = this;
		else oproot=r;
		
		// multiple tuple subterms get new operation scopes:
        if (isLazyList()) {
            TermModel listTemplate = lazyListTemplate();
			if (listTemplate.isList()){
				for(int i=0;i<listTemplate.getChildCount();i++)
					((GUITerm)(listTemplate.children[i])).setOpRoot(oproot);
			}
			else //((GUITerm)(listTemplate.children[0])).setOpRoot();
                            ((GUITerm)listTemplate).setOpRoot();
        } else if (isLazyTree()) {
			TermModel treeTemplate = lazyTreeTemplate();
			if (treeTemplate.isList()){
				for(int i=0;i<treeTemplate.getChildCount();i++)
					((GUITerm)(treeTemplate.children[i])).setOpRoot(oproot);
			}
			else ((GUITerm)treeTemplate).setOpRoot();
		} else if (isList()) {
            TermModel listTemplate = listTemplate();
			if (listTemplate.isList()){
				for(int i=0;i<listTemplate.getChildCount();i++)
					((GUITerm)(listTemplate.children[i])).setOpRoot(oproot);
			}
			else ((GUITerm)listTemplate).setOpRoot();
        } else if (!isLeaf()) 
			for(int i=0;i<children.length;i++)
				if (children[i] instanceof GUITerm) 
					((GUITerm)(children[i])).setOpRoot(oproot);
	}
	
	/** Returns the data inherited from TermModel, encapsulated in a new TermModel. 
	Lists will be flat, not binary trees. Ignores lazy sub terms, and skips nonpersistent nodes,
	obtaining a tree structure alike to the processing done by the removeGTExtras/2 Prolog predicate */
	public TermModel getTermModel(){
        return getTermModel((TermModel)null, new PathSearch());
	}
	
	/** Like getTermModel(), but in addition computes the path, in the new TermModel, 
	to the TermModel sub tree resulting from transforming 'subtree'. 'subtree' must be contained in this GUITerm, 
	must not be under lazy subtrees, and be a nonpersistent node (???) */
	public TermModel getTermModel(TermModel subtree, PathSearch path){
		Vector<TermModel> v = new Vector<TermModel>();
		getTermModel(v,subtree,path);
		if (subtree!=null && !path.hasFound()) throw new XJException("subtree not contained in term");
		if (v.size()!=1) throw new XJException("Bad term ");

        TermModel term = (v.elementAt(0));
		return (module == null || module.equals(""))? 
                       term
                       : new TermModel(":", new TermModel[]{new TermModel(module), term});
	}
	
	static class PathSearch{
		Stack<TermModel> currentPath=new Stack<TermModel>(); // Should use an int array, lots of objects created along the way unnecessarily
		TermModel thePath=null;
		boolean hasFound(){return thePath!=null;}
		void found(){
			if (hasFound()) throw new XJException("inconsistency in PathSearch");
			// hack to remove first path element:
			TermModel[] cc;
			if (currentPath.size()>1) {
				cc = new TermModel[currentPath.size()-1];
				for (int c=0;c<cc.length;c++)
					cc[c] = currentPath.elementAt(c+1);
			}
			else cc = null;
			// thePath=new TermModel(".",cc);
			thePath = TermModel.makeList(cc);
		}
		void push(int i){currentPath.push(new TermModel(new Integer(i)));}
		void pop(){currentPath.pop();}
		TermModel getPath(){ return thePath; }
	}
	
	void getTermModel(Vector<TermModel> newChildren, TermModel subtree, PathSearch path){
		if (subtree==this) {
			path.push(newChildren.size()+1); path.found(); path.pop(); 
		}
        if (isLazyTree() && isList()) { // for lazy trees with eager selections
			newChildren.addElement(TermModel.makeList(children));
			return;
		}
		if (isLazyTree() || isLazyList()) return;
		if (isList()) {
			newChildren.addElement(TermModel.makeList(children));
			return;
		}
		boolean inp = isNonPersistent();
		/* This was allowing GUITerms to be included in the result:
		if (isOpaque()) newChildren.addElement(new TermModel(node,children));
		else */
		if (super.isLeaf()) {
			if (!inp) newChildren.addElement(new TermModel(node));
		} else{
			if (inp) {
				for (int c=0;c<getChildCount();c++)
					((GUITerm)children[c]).getTermModel(newChildren, subtree, path);
			} else{
				Vector<TermModel> nc2 = new Vector<TermModel>();
				for (int c=0;c<getChildCount();c++){
					path.push(newChildren.size()+1);
					if (children[c] instanceof GUITerm) 
						((GUITerm)children[c]).getTermModel(nc2, subtree, path);
					else nc2.addElement(children[c]); // we assume no GUITerms are under
					path.pop();
				}
				newChildren.addElement(new TermModel(node,nc2));
			}
			// 
		}
	}
	
	/** Returns sub term reachable from this through path, ignoring lazy sub terms and nonpersistent nodes.
	path should contain Integer objects, with values 1..N, and not 0..N-1 */
	public GUITerm subTerm(Vector<Integer> path){
		return subTerm(path,0);
	}
	
	protected GUITerm subTerm(Vector<Integer> path, int level){
		if (path.size()==level) return this;
		int N = ((Integer)path.elementAt(level)).intValue();
		Object x = getRealChild(N-1);
		GUITerm xx = (GUITerm)x;
		GUITerm R = xx.subTerm(path,level+1);
		return R;
	}
	
	/** Ignores lazy subterms and dives over nonpersistent nodes, does not dive into eager list templates; 
	returns a GUITerm if the child is found under this,
	otherwise returns an Integer with the number of children under this*/
	Object getRealChild(int index){
		int initialIndex = index;
		//if (children==null) throw new XJException("inconsistency in getRealChild, node=="+node+",index=="+index);
		
		for (int c=0;c</*children.length*/getChildCount();c++){
			GUITerm child = (GUITerm)children[c];
			if ((child.isLazyTree() && node.equals(XJ_LAZY)) || child.isLazyList()) {  //tv, should be child.node.equals(XJ_LAZY) ??
				continue;
			} 
			if (child.isNonPersistent()){
				Object sub = child.getRealChild(index);
				if (sub instanceof GUITerm) return sub;
				index = index - ((Integer)sub).intValue();
				continue;
			} 
			if (index==0) return child;
			index --;
		}
		return new Integer(initialIndex-index);
	}
	
	/* Old version without lazy/nonpersistent stripping:
	public void assign(TermModel data){
		setNodeValue(data.node);
		if (isList()) {
			if (!data.isList()) throw new XJException("No list in data term");
			if (data instanceof GUITerm) children=data.children;
			else {
				// We need to flatten the list
				children = flatList(data);
			}
		}else {
			if (data.isLeaf()) return;
			// the above allows for example assigning hello --> hi(there), resulting in hello(there)
			for (int i=0; i<getChildCount();i++) 
				((GUITerm)children[i]).assign(data.children[i]);
		}
	}*/
	
	/** Assigns nodes in data term to this term, assuming identical structures modulo lazy subterms and nonpersistent
	nodes, which are ignored. The data term
	may be smaller than this (assigned) term, to cater for situations where we want to assign
	new values to intermediate nodes only, e.g. assigning hello --> hi(there), resulting in hello(there).
	The data term can also be larger, when 'this' is opaque.
	The root node must not be nonPersistent */
	// This may need some flag caching
	public void assign(TermModel data){
		//System.err.println("Assigning:"+data+"\nto:"+this);
		if (isOpaque()){
        	setOpaqueNodeValue(data);
        	return;
		}
        setNodeValue(data.node);
		if (isList()) {
			if (!data.isList()) throw new XJException("No list in data term");
			if (data instanceof GUITerm) children=data.children;
			else {
				// We need to flatten the list
				children = flatList(data);
                hasListFunctor = true;
			}
		} else {
			for (int i=0;i<data.getChildCount();i++){
				Object temp = getRealChild(i);
				if (!(temp instanceof GUITerm))
					throw new XJException("Bad child "+i+" for node "+data.node+":"+temp+"\nMissing 'opaque' property...?");
				GUITerm child = (GUITerm)temp; // must be so
				child.assign(data.children[i]);
			}
		}
	}
	
	public void assignToVar(VariableNode v,Object value){
		for (int p=0;p<properties.length;p++)
			properties[p].assignToVar(v,value);
		super.assignToVar(v,value);
	}
	
	/** Similar implementation in TermModel, except that it also processes properties. 
	Mechanism used by lazy components to update their local template copies with changes in the global template,
	typically performed after myGUI variables are bound at the end of makeGUI */
	public void assignTermChanges(TermModel other){
		super.assignTermChanges(other);
		for (int p=0;p<properties.length;p++)
			properties[p].assignTermChanges(((GUITerm)other).properties[p]);
	}
	
	/** Single node changes get the undo treatment in this class; more structural (children variable) changes get treated
	in model classes such as EagerListModel */
	public void setNodeValue(Object v){
		Object oldValue;
		if(isOpaque() && (v instanceof TermModel)){
			setOpaqueNodeValue((TermModel)v);
        } else {
			oldValue = node;
			super.setNodeValue(v);
            XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
				this,this,XJChangeManager.SETNODEVALUE_EDIT,-1,oldValue,v);
			fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
        }
		
	}
	
	private void setOpaqueNodeValue(TermModel data){
		if (isOpaque()){
			Object oldValue = getTermModel();
			// opaque subterms should have nothing interesting (e.g. renderer, root variables) in them.
			// so we don't worry about losing anything from the current children:
			if (data.isList() && !(data instanceof GUITerm)) {
				super.setNodeValue(data.node);
				hasListFunctor = true;
				setChildren(flatList(data));
			} else {
				super.setNodeValue(data.node);
				setChildren(data.children);
			}
			XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
					this,this,XJChangeManager.SETNODEVALUE_EDIT,-1,oldValue,data);
			fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
			return;
		}
	}
        
	public TermModel[] getProperties(){
		return properties;
	}
	
	// In this class lists are flat, while in TermModel they're binary trees
	// We can not have [a,b|X]
	public String listToString(PrologOperatorsContext ops,boolean quoted){
		int i;
		StringBuffer s = new StringBuffer("[");
		for( i = 0 ; i < children.length ; i++ ){
			if (i>listMaxLength) break;
			if (i>0) s.append(',') ;
			s.append(children[i].toString(ops,quoted));
		}
		if( i == listMaxLength ) s.append("...");
		return s + "]";
	}
	
	public boolean isNumber(){
		if (!isVar()) return super.isNumber(); // let's assume that consistency is mandatory with explicit type
		return nodeIsNumber();
	}
	
	public boolean nodeIsNumber(){
		return findProperty(NUMBER)!=null;
	}
	
	public boolean nodeIsFloat(){
		return findProperty(FLOAT)!=null;
	}
	
	public boolean isInteger(){
		if (!isVar()) return super.isInteger(); // let's assume that consistency is mandatory with explicit type
		return nodeIsInteger();
	}
	
	public boolean nodeIsInteger(){
		return findProperty(INTEGER)!=null;
	}
	
	public boolean isAtom(){
		if (!isVar()) return super.isAtom();
		return nodeIsAtom();
	}
	
	public boolean nodeIsAtom(){
		return findProperty(ATOM)!=null || findProperty(ATOMUPPER)!=null;
	}
	
	public boolean isLeaf(){
		return super.isLeaf()||(isList()&&!isComboField())||isOpaque()||isLazyList()||isLazyTree()||isGraph(); 
	}
	
	public boolean isOpaque(){
		return findProperty(OPAQUE)!=null;
	}
	
	public boolean isNonPersistent(){
		return findProperty(NONPERSISTENT)!=null;
	}
	
	public boolean isVisibleList(){
		return isList() && !isOpaque();
	}
	
	public boolean isConstant(){
		return findProperty(CONSTANT)!=null; 
	}
	
	public boolean isReadOnly(){
		return findProperty(READONLY)!=null;
	}
	
	public boolean isInvisible(){
		return findProperty(INVISIBLE)!=null; 
	}
	
	public boolean isOptional(){
		return findProperty(OPTIONAL)!=null; 
	}
	
	public boolean containsOpaque(){
		return findDeepProperty(OPAQUE)!=null;
	}
	
	public boolean containsReadonly(){
		return findDeepProperty(READONLY)!=null;
	}
	
	public boolean containsInvisible(){
		return findDeepProperty(INVISIBLE)!=null;
	}
		
	TermModel findDeepProperty(String name){
		TermModel here = findProperty(name);
		if (here!=null) return here;
		// remove this to avoid diving through list templates:
		if (isList() && findProperty(LIST)!=null && !listTemplate().isList()) // this probably needs cleaning up
			return ((GUITerm)listTemplate()).findDeepProperty(name);
		if (isLeaf()) return null;
		return findPropertyBelow(name);
	}
	
	TermModel findPropertyBelow(String name){
		if (isLeaf()) return null;
		// not a list nor atomic nor ...
		for (int c=0; c<children.length; c++){
			GUITerm child = (GUITerm)children[c];
			TermModel there = child.findDeepProperty(name);
			if (there!=null) return there;
		}
		return null;
	}
	

	public TermModel findProperty(String name){
		return findProperty(name,properties);
	}

	public static TermModel findProperty(String name,TermModel[] props){
		if (props==null) return null;
		name=name.toLowerCase();
		int p=0;
		while(p<props.length){
			if(props[p].node.toString().toLowerCase().equals(name) /* && properties[p].isLeaf()*/)
				return props[p];
			if(props[p].node.equals("=") && props[p].children[0].node.toString().toLowerCase().equals(name))
				return props[p].children[1] ;
			p++;
		}
		return null;
	}
	
	public TermModel[] findProperties(String name){
		return findProperties(name,properties);
	}
	
	public static TermModel[] findProperties(String name, TermModel[] properties){
		Vector<TermModel> temp = new Vector<TermModel>();
		findProperties2(name.toLowerCase(),temp, properties);
		return vectorToArray(temp);
	}
	void findProperties2(String name,Vector<TermModel> temp){
		findProperties2(name,temp,properties);
	}
	
	static void findProperties2(String name,Vector<TermModel> temp, TermModel[] properties){
		int p=0;
		if (properties==null) return;
		while(p<properties.length){
			if(properties[p].node.equals("=") && properties[p].children[0].node.toString().toLowerCase().equals(name))
				temp.addElement(properties[p].children[1]) ;
			if(properties[p].node.toString().toLowerCase().equals(name) /*&& properties[p].isLeaf()*/)
				temp.addElement(properties[p]);
			p++;
		}
	}
	private static TermModel[] vectorToArray(Vector<TermModel> temp){
		TermModel[] result = new TermModel[temp.size()];
		for (int i=0;i<result.length;i++)
			result[i] = temp.elementAt(i);
		return result;		
	}
	
	/** Collects all property terms with this name, in this GUITerm and its descendents */
	public TermModel[] findRecursiveProperties(String name){
		Vector<TermModel> temp = new Vector<TermModel>();
		findRecursiveProperties2(name.toLowerCase(),temp);
		return vectorToArray(temp);		
	}
	
	void findRecursiveProperties2(String name,Vector<TermModel> temp){
		findProperties2(name,temp);
		for (int c=0; c<getChildCount(); c++)
			if (getChild(c) instanceof GUITerm)
				((GUITerm)getChild(c)).findRecursiveProperties2(name,temp);
	}
		
	public void addProperty(TermModel property){
		TermModel[] newProperties = new TermModel [properties.length + 1];
		System.arraycopy(properties, 0, newProperties, 0, properties.length);
		newProperties[properties.length] = property;
		properties = newProperties;
	}
        
	public String getUserTitle(){
		TermModel t = findProperty(CAPTION);
		if (t!=null) return t.toString();
		else if (isVisibleList() && findProperty(LIST)!=null && !listTemplate().isList()) return "List of "+((GUITerm)listTemplate()).getUserTitle();
		else return "";
	}
	
	public String getTitle(){ 
		TermModel t = findProperty(CAPTION);
		if (t!=null) return t.toString();
		else if (isVisibleList() && findProperty(LIST)!=null && !listTemplate().isList()) 
            return "List of "+((GUITerm)listTemplate()).getTitle()+ " ("+getChildCount()+" items)";
		else return node.toString();
	}
		
	public int getCharWidth(){ 
		TermModel t = findProperty(TYPICALSIZE);
		if (t!=null) return t.intValue();
		TermModel minT = findProperty(MINSIZE);
		TermModel maxT = findProperty(MAXSIZE);
		int min,max;
		if (minT!=null) min = minT.intValue(); else min = Integer.MIN_VALUE;
		if (maxT!=null) max = maxT.intValue(); else max = Integer.MAX_VALUE;
		if (min==max) return max;
		else {
			int width = node.toString().length();
			if (width <8) {
				if (max==0) return 1;
				if (max<8) return max; else	return 8;
			}
			if (width>35) {
				if (min>35) return min; else return 35;
			}
			return width;
		}
	}
	
	public Dimension getPreferredSize(Dimension defaultD){
		TermModel specifiedWidth = findProperty(WIDTH);
		TermModel specifiedHeight = findProperty(HEIGHT);
		if (defaultD==null) defaultD = new Dimension(20,20);
		if (specifiedWidth!=null) defaultD.setSize(specifiedWidth.intValue(),defaultD.height);
		if (specifiedHeight!=null) defaultD.setSize(defaultD.width,specifiedHeight.intValue());
		return defaultD;
	}
	
	
	/** Fire undoableEditHappened messages to undo listeners, by asking the term root to do it once for all*/
	public void fireUndoableEditUpdate(UndoableEditEvent e){
		if (isRoot()){
			if (getNotifyUndoListeners() && undoListeners!=null) 
				for (int l=0;l<undoListeners.size();l++)
					((UndoableEditListener)(undoListeners.elementAt(l))).undoableEditHappened(e);
		} else if (root!=null) ((GUITerm)root).fireUndoableEditUpdate(e);
	}
	
	/** Determines whether undoableEditHappened messages are or not sent, necessary for undo action processing; 
	only the root of the term matters */
	
	void setNotifyUndoListeners(boolean notify){
		if (!isRoot()) ((GUITerm)root).setNotifyUndoListeners(notify);
		else notifyUndoListeners = notify;
	}
	
	boolean getNotifyUndoListeners(){
		if (!isRoot()) return ((GUITerm)root).getNotifyUndoListeners();
		else return notifyUndoListeners;
	}
	
	public void addUndoableEditListener(UndoableEditListener l){
		if (!isRoot()) throw new XJException("addUndoableEditListener should be sent to term roots only"); 
		if (undoListeners==null) undoListeners = new Vector<UndoableEditListener>();
		setNotifyUndoListeners(true);
		undoListeners.addElement(l);
	}
	
	public void removeUndoableEditListener(UndoableEditListener l){
		undoListeners.removeElement(l);
	}
		
	static boolean objectInArray(Object x,Object[] array){
		for (int i=0; i<array.length; i++)
			if (x==array[i]) return true;
		return false;
	}
			
	TermModel listTemplate(){ // uses property list(Template)
		/* removed because of findDeepProperty:
		if (!isVisibleList()) throw new XJException("inconsistent use of listTemplate"); */
		TermModel p = findProperty(LIST);
		if (p==null) throw new XJException("lists require a list property");
		// One could display without a declared list template, but that would probably be a bad idea
		return p.children[0]; 
	}

	TermModel lazyListTemplate(){ // uses property lazylist(Template)
		TermModel p = findProperty(LAZYLIST);
		if (p==null) throw new XJException("lazy lists require a lazylist property");
		return p.children[0]; 
	}

	/** Returns a single GUITerm template, or a list of */
	TermModel lazyTreeTemplate(){ // uses property lazytree(Template)
		TermModel p = findProperty(LAZYTREE);
		if (p==null) throw new XJException("lazy trees require a lazytree property");
		return p.children[0]; 
	}

	/** Returns null or a GUIError if s unacceptable */
	Object coerceNodeText(String s){
		if (s.length()==0 && ! isOptional()) return message("Mandatory field");
		if (isVar() && s.length()==0) return node; 
			// vars with empty input remain vars; perhaps maxSize==0...
		else if (nodeIsAtom()) {
			TermModel minSize = findProperty(MINSIZE);
			if (minSize!=null && s.length()<minSize.intValue())
				return message("A minimum of "+minSize.intValue()+" characteres is needed");
			TermModel maxSize = findProperty(MAXSIZE);
			if (maxSize!=null && s.length()>maxSize.intValue())
				return message("No more than "+maxSize.intValue()+" characteres are admissible");
			return s;
		} else if (nodeIsInteger() || nodeIsNumber() || nodeIsFloat()){
			// numbers
			if (s.equals("null") && findProperty(NULLABLE)!=null) return s;
			if (nodeIsInteger())
				try {
					return Integer.valueOf(s); 
				} catch (NumberFormatException e){return message("Bad Integer number format");}
			else 
				try {
					return Float.valueOf(s); 
				} catch (NumberFormatException e){return message("Bad Float number format");}
		} else return message("Weird node type:\n"+this);
	}
	
	public GUIError message(String s){
		return new GUIError(s,this);
	}
	
	public String tipDescription(){
		TermModel tip = findProperty(TIP);
		if (tip!=null) return tip.toString();
		else return "";
		/*
		StringBuffer temp = new StringBuffer(getTitle()+" is "+typeDescription());
		TermModel ex = findProperty(EXAMPLE);
		if (ex!=null) temp.append(" such as "+ex);
		return temp.toString();*/
	}
	/** Tips for fields still empty (aka prompts) */
	public String emptyTipDescription(){
		TermModel tip = findProperty(EMPTYTIP);
		if (tip!=null) return tip.toString();
		else return "";
	}
	
	String typeDescription(){
		if (nodeIsInteger()) return "an " + INTEGER;
		else if (nodeIsNumber()) return "a "+NUMBER;
		else if (nodeIsAtom()) return "an " + ATOM;
		else return "a ?subterm?";
	}
	
	String propsDescription(){
		StringBuffer temp = new StringBuffer("Node has class "+node.getClass());
		for (int p=0;p<properties.length;p++){
			if (p>0) temp.append(", ");
			else temp.append(" ");
			temp.append(properties[p].toString());
		}
		temp.append(",getCharWidth()="+getCharWidth());
		return temp.toString();
	}

	XJAction[] operations(PrologEngine engine,Component parent){
		return operations(engine,parent,null);
	}
	
	public XJAction[] operations(PrologEngine engine,Component parent,Runnable rememberFunctionResults){
		return operations(properties, this, oproot, engine, parent, rememberFunctionResults);
	}
	public static XJAction[] operations(TermModel[] properties, GUITerm gt, TermModel oproot, PrologEngine engine,Component parent,Runnable rememberFunctionResults){
		TermModel[] ops = findProperties(OPERATION, properties);
		TermModel[] funcs = findProperties(FUNCTION, properties);
		XJAction[] result = new XJAction[ops.length+funcs.length];
		for (int op = 0; op<ops.length; op++)
			result[op] = new XJAction(engine, parent,gt,oproot,ops[op]);
		for (int op = 0; op<funcs.length; op++)
			result[ops.length+op] = new XJFunction(engine, parent,gt,oproot,funcs[op],rememberFunctionResults);
		return result;
	}
	
	
	public static void typicalCommonSelect(Component c){
		Container window = SwingUtilities.getAncestorOfClass((Class<?>)RootPaneContainer.class,c);
		if (window instanceof JInternalFrame) {
			JInternalFrame internalWindow = (JInternalFrame)window;
			try{internalWindow.setIcon(false);}
			catch(java.beans.PropertyVetoException e){throw new XJException("Problem de-iconifying internal frame:"+e);}
			JDesktopPane desktop = internalWindow.getDesktopPane();
			if (desktop!=null) {
				Container topwindow = SwingUtilities.getAncestorOfClass((Class<?>)RootPaneContainer.class,desktop);
				topwindow.setVisible(true);
				if (topwindow instanceof Frame) ((Frame)topwindow).setState(Frame.NORMAL);
			}
		}
		window.setVisible(true);
		if (window instanceof Frame) ((Frame)window).setState(Frame.NORMAL);
		c.requestFocus();
	}
	
	public void typicalPartsSelect(Object[] parts){
		if (parts==null || parts.length==0) return;
		// if (parts.length>1) throw new XJException("Containers can not have a multiple selection");
		// Vector path = ((TermModel)parts[0]).makeIntegerVector();
		Vector<Integer> path = new Vector<Integer>();
		for (int i=0;i<parts.length;i++){
			TermModel term = (TermModel)parts[i];
			path.addElement(new Integer(term.intValue()));
		}
		XJComponent component = subTerm(path).getARenderer(); // CHANGE VISIBILITY OF IT AGAIN!
		if (component==null) throw new XJException("Inexistent component part to select");
		// System.out.println("SHOULD SELECT:"+component);
		component.selectGUI(null);
	}
	
	/** Implementation of XJComponent.selectGUI for containers, which allow a subcomponent to be selected by a path */
	public static void typicalContainerSelect(XJComponent gui,Object[] parts){
		typicalCommonSelect((Component)gui);
		gui.getGT().typicalPartsSelect(parts);
	}
	
	/** Implementation of XJComponent.selectGUI for atomic components,
	 which do not allow subcomponents to be selected  */
	public static void typicalAtomicSelect(XJComponent gui,Object[] parts){
		if (parts!=null) throw new XJException("Cannot select a part of atomic component "+gui);
		typicalCommonSelect((Component)gui);
	}

	/** Allows Flora to be wrapped in GTs. See comments in xjLazyFloraGoal predicate */
	public static TermModel floraPreprocessWithVarList(TermModel goal){
		if (goal.node.equals("flora") && goal.getChildCount()==2){
			//Not really:
			// if (!inPrologShell)
			//	throw new XJException("flora goals can only be used when the system is in the Prolog shell");
			TermModel floraGoalArg = (TermModel)goal.getChild(0);
			if (!floraGoalArg.isAtom() || !floraGoalArg.toString().endsWith(".")) 
				throw new XJException("Flora goals must be in an atom ended by .");
			TermModel fv = (TermModel)goal.getChild(1);
			TermModel R = new TermModel("xjLazyFloraGoal",new TermModel[]{floraGoalArg,fv});
			return R;
		} else return goal;
	}
	
	/** Allows Flora to be wrapped in GTs, encapsulating the Flora query into a Prolog predicate call with the first N arguments explicit. 
	See comments in xjLazyFloraGoal predicate */
	public static TermModel floraPreprocessWithArgs(TermModel goal, int N){
		if (goal.node.equals("flora") && goal.getChildCount()==2){
			//if (!inPrologShell) why was this not commented...??
			//	throw new XJException("flora goals can only be used when the system is in the Prolog shell");
			TermModel floraGoalArg = (TermModel)goal.getChild(0);
			if (!floraGoalArg.isAtom() || !floraGoalArg.toString().endsWith(".")) 
				throw new XJException("Flora goals must be in an atom ended by .");
			TermModel fv = (TermModel)goal.getChild(1);
			TermModel[] flatVars = fv.flatList();
			if (flatVars.length<N)
				throw new XJException("Flora goals must come with a list of at least 3 ?Var=PrologVar elements");
			// fetch first 3 Prolog vars:
			
			TermModel[] fGoalArgs = new TermModel[N+2];
			
			for (int i=0; i<N; i++)
				fGoalArgs[i] = (TermModel)flatVars[i].getChild(1);
			
			fGoalArgs[N] = floraGoalArg;
			fGoalArgs[N+1] = fv;
			
			TermModel R = new TermModel("xjLazyFloraGoal",fGoalArgs);
			return R;
		} else return goal;
	}

	public TermModel[] getMyGUIs(){
		return findRecursiveProperties(MYGUI);
	}
}
