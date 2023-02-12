package com.xsb.xj;
import com.declarativa.interprolog.*;
import com.xsb.xj.util.*;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.dnd.*;

@SuppressWarnings("serial")
public class XJTree extends JScrollPane implements XJTemplateComponent, DnDCapable, LazyRefreshListener{
	static Icon repeatArrow = new ImageIcon(XJTree.class.getResource("images/arrow.png"));
	
	GUITerm gt;
	PrologEngine engine;
	XJTreeView theJTree;
	LazyTreeModel model;
	private TermModel[] oldSelection=null;
	/** TermModels aggregating either one or several templates each */
	TermModel rendererCopy, editorCopy;
	/**
	 * GuiTerm property that informs Java that all tree nodes are of the same
	 * height. May significantly improve performance for large
	 * trees and reduce memory usage.
	 */
	public static final String FIXEDHEIGHT = "fixedheight";
	/**
	 * Make the tree auto expand "some" on creation, to a reasonable extent
	 */
	public static final String EXPAND = "expand";
    private boolean DRY;

	public XJTree(PrologEngine engine,GUITerm gt){
		this.gt=gt; this.engine=engine;
		DRY = gt.findProperty(GUITerm.DRY)!=null;
		if (gt.findProperty(GUITerm.PROLOGCACHED)!=null) {
			if (DRY) throw new XJException("DRY not supported for prologcached trees");
			model= new PrologCachedTreeModel(engine,gt);
		} else model = new LazyTreeModel(engine,gt);
		model.addLazyRefreshListener(this);
		
		TreeCellRenderer renderer;
		TreeCellEditor editor;
		TermModel templateObject = getTemplate();
		if (templateObject.isList()){
			// we have possibly more than one template / node type
			GUITerm[] renderingTemplates = new GUITerm[templateObject.getChildCount()];
			GUITerm[] editingTemplates = new GUITerm[renderingTemplates.length];
			for (int t=0;t<renderingTemplates.length;t++){
				GUITerm oneTemplate = (GUITerm)templateObject.getChild(t);
				renderingTemplates[t] = (GUITerm)oneTemplate.clone();
				editingTemplates[t] = (GUITerm)oneTemplate.clone();
			}
			rendererCopy = new TermModel(".",renderingTemplates,true);
			editorCopy = new TermModel(".",editingTemplates,true);
			renderer = new PolimorphicRenderer(engine,renderingTemplates,DRY);
			editor = new PolimorphicEditor(engine,editingTemplates);
		} else{
			// all nodes are similar
			GUITerm rendererTemplate = (GUITerm)templateObject.clone();
			rendererCopy = rendererTemplate;
			renderer = new XJCellTupleRenderer(engine,rendererTemplate,DRY);
			GUITerm editorTemplate = (GUITerm)templateObject.clone();
			editorCopy = editorTemplate;
			editor = new TreeNodeEditor(engine,editorTemplate);
		}
		// (see comments on XJTable constructor regarding cloning)
		
		theJTree = new XJTreeView(this, model, renderer, editor);
		if (gt.findProperty(FIXEDHEIGHT) != null){
			setFixedHeight(theJTree, model, renderer);
		}
		setViewportView(theJTree);
		// not anymore???? theJTree.addNotify();
		ToolTipManager.sharedInstance().registerComponent(this);
        new TreeExcelAdapter(theJTree);
        if (gt.findProperties(EXPAND)!=null)
        	expandOne();
	}
        
    public XJTree(){}
    
    boolean isDRY(){return DRY;}

	public JTree getJTree(){
		return theJTree;
	}
	
	public Dimension getPreferredSize(){
		return gt.getPreferredSize(super.getPreferredSize());
	}
	
	public TermModel getTemplate(){
		return gt.lazyTreeTemplate();
	}
	
	public void constructionEnded(){
		rendererCopy.assignTermChanges(getTemplate());
		editorCopy.assignTermChanges(getTemplate());
	}
        
	public void destroy(){
		if(model instanceof PrologCachedTreeModel) {
			// TV: Putting cache cleanup in a separate thread
			// - not to wait for it 
			// (for example, when a window is closing)
			// If this causes a problem - remove
			// from the thread - run and wait
			Thread closingThread = new Thread("XJTree closer") {
				public void run() {
					// System.out.println("Destroy Start");
					((PrologCachedTreeModel)model).destroy();
					// System.out.println("Destroy End");
				}
			};
			SwingUtilities.invokeLater(closingThread);
			
		}
	}
	
	public void setEnabled(boolean yes){
		theJTree.setEnabled(yes);
	}
        
	static class XJTreeView extends JTree implements Autoscroll{
		XJTree theXJTree;
        private int scrollMargin = 12;
        Cursor previousCursor = null;
        
		XJTreeView(XJTree theXJTree, LazyTreeModel model, TreeCellRenderer renderer, TreeCellEditor editor){
			super(model);
			this.theXJTree = theXJTree;
			setCellRenderer(renderer);
			setCellEditor(editor);
			if (theXJTree.getGT().findProperty(FIXEDHEIGHT) == null){
				setRowHeight(0);
			}
			setEditable(true); // CHANGE HERE!!!
			setRootVisible(model.shouldShowRoot);
			setShowsRootHandles(true);
			if (model.shouldShowRoot) putClientProperty("JTree.lineStyle", "Angled");
			// else the tree would crash
			ToolTipManager.sharedInstance().registerComponent(this); // why aren't tips appearing ???
			getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
			
			if (theXJTree.getGT().findProperty(GUITerm.SINGLESELECTIONS)!=null)
				getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			else getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

			setInvokesStopCellEditing(true);
            setAccessibleComponentName();

			/* Handle selectionChanged operations (not functions); code similar to XJTable */
			XJAction[] topOps = theXJTree.getGT().operations(theXJTree.getEngine(),theXJTree);
			final XJAction selectionChangedAction = XJAction.findSelectionChanged(topOps);
			if (selectionChangedAction!=null) {
				// let's keep it light, and assume no modal interactions occur:
				selectionChangedAction.setInAWTThread(true);
				selectionChangedAction.setCursorChanges(false);
				addTreeSelectionListener(new TreeSelectionListener() {
					public void valueChanged(TreeSelectionEvent  e){
						// System.out.println("valueChanged in tree selection, "+Thread.currentThread());
						if (loadFromGUI()) 
							selectionChangedAction.doit();
					}
				});
			}
			addTreeWillExpandListener(new TreeWillExpandListener(){
				public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException{}
				public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException{
					//System.out.println("Expanding "+event.getPath().getLastPathComponent());
					setWaitCursor();
				}
			});
			addTreeExpansionListener(new TreeExpansionListener(){
				public void treeCollapsed(TreeExpansionEvent event){}
				public void treeExpanded(TreeExpansionEvent event){
					restoreCursor();
				}
			});
		}
		
		public boolean isDRY(){
			return theXJTree.isDRY();
		}

		public void setWaitCursor() {
			previousCursor = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}

		public void restoreCursor() {
			if(previousCursor != null) {
				setCursor(previousCursor);
			}
		}

		protected void setAccessibleComponentName(){
			if(theXJTree.getGT().tipDescription().equals("")){
					if(!theXJTree.getGT().getTitle().equals("")){
						getAccessibleContext().setAccessibleName(theXJTree.getGT().getTitle());
					}
				}else{
					getAccessibleContext().setAccessibleName(theXJTree.getGT().tipDescription());
				}
		}
                
		/** Handle popup menu operations  */
		protected void processMouseEvent(MouseEvent e){
			
			if (e.isPopupTrigger()){
				int selRow = getRowForLocation(e.getX(), e.getY());
				if (selRow!=-1 && theXJTree.loadFromGUI()) {
					e.consume();
					TreePath selPath = getPathForLocation(e.getX(), e.getY());
					setSelectionPath(selPath);
					LazyTreeModel.LazyTreeNode node = (LazyTreeModel.LazyTreeNode)selPath.getLastPathComponent();
					// System.out.println("popup event on "+node);
					getEditor().setCurrentTuple(node);
					// System.out.println(getEditor().getGT());
					JPopupMenu pm  = XJAtomicField.operationsPopup(
						getEditor().getGT(),theXJTree.getEngine(),theXJTree,null
						);
                                        if(pm.getComponentCount() > 0){
                                            pm.show(this,e.getX(),e.getY());
                                        }
				} else super.processMouseEvent(e);
			} else super.processMouseEvent(e);
		}
		public boolean loadFromGUI(){
			if (!isEditing()) return true;
			else return stopEditing();
		}
		/** Delegate to our cell editor */
		public boolean isDirty(){
			return getEditor().isDirty();
		}
		
		TreeNodeEditorInterface getEditor(){
			return (TreeNodeEditorInterface)getCellEditor();
		}

		/** parts are the NodeID terms */
		public void selectGUI(Object[] parts){
			GUITerm.typicalCommonSelect(this);
			selectNodes(parts);
		}
		
		void selectNodes(Object[] parts){
			if (parts==null) return;
			if (parts.length==0){
			clearSelection();
				return;
			}
			// some nodes may have disappeared:
			Vector<TreePath> bag = new Vector<TreePath>();
			for (int p=0; p<parts.length; p++){
				TermModel[] nodeIDpath = ((TermModel)parts[p]).flatList();
				TreePath temp = theXJTree.getModel().getPathToNode(nodeIDpath);
				if (temp!=null) bag.addElement(temp);
			}
			
			final TreePath[] paths = new TreePath[bag.size()];
			for (int p=0; p<paths.length; p++){
				paths[p] = (TreePath)bag.elementAt(p);
			}
			
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
                                    if(paths.length > 0){
					setSelectionPaths(paths);
                                        // find topmost node:
                                        TreePath firstPath=null;
                                        int firstRow = Integer.MAX_VALUE;
                                        for (int p=0; p<paths.length; p++){
                                            //System.out.println("path "+paths[p]);
                                            Rectangle r = getPathBounds(paths[p]);
                                            //if (r==null) throw new XJException("invisible node in path??? :"+paths[p]);
                                            if(r != null){
                                            int temp = r.y;
                                            if (temp<firstRow){
                                                firstRow=temp;
                                                firstPath=paths[p];
                                            }
                                            }
                                        }
                                        
                                        if(firstPath != null){
                                        Rectangle rect = getPathBounds(firstPath);
                                        scrollRectToVisible(rect);
                                        }
                                    }
				}
			});
		}
                
		public DragGestureListener createDragGestureListener(){			
			DragGestureListener dgl = new DragGestureListener(){
				public void dragGestureRecognized( DragGestureEvent event) {
					if (event.getDragAction()!=DnDConstants.ACTION_COPY) return;

					Point origin = event.getDragOrigin();
					TreePath path = getClosestPathForLocation(origin.x,origin.y);
					if(path==null) return;
					if(!getSelectionModel().isPathSelected(path)){
						System.err.println("Data must be selected just before being dragged");
						return;
					}
					DragSourceListener dsl = new com.xsb.xj.util.DragSourceAdapter(){
						public void dragDropEnd(DragSourceDropEvent dsde){
							//System.out.println("Drop successful ? : "+dsde.getDropSuccess());
						}
				    };
				    TransferableXJSelection txjs = new TransferableXJSelection(theXJTree,theXJTree.getSelectedTerms());
				    event.getDragSource().startDrag (event, DragSource.DefaultCopyDrop, txjs, dsl);
				    // System.out.println("Started drag");
				}
			};
			return dgl;
		}
		TermModel point2Term(Point where){
			TreePath path = getClosestPathForLocation(where.x,where.y);
			if(path==null) return null;
			LazyTreeModel.LazyTreeNode node = (LazyTreeModel.LazyTreeNode)path.getLastPathComponent();
			return node.getNodeTerm();
		}
                
                // autoscroll as per
                // http://www.oreilly.com/catalog/jswing/chapter/dnd.beta.pdf
                // We've been told to scroll because the mouse cursor is in our
                // scroll zone.
                public void autoscroll(Point cursorLocn) {
                    // Figure out which row we're on.
                    int realrow = getRowForLocation(cursorLocn.x, cursorLocn.y);
                    Rectangle outer = getBounds();
                    // Now decide if the row is at the top of the screen or at the
                    // bottom. We do this to make the previous row (or the next
                    // row) visible as appropriate. If we're at the absolute top or
                    // bottom, just return the first or last row respectively.
                    realrow = (cursorLocn.y + outer.y <= scrollMargin ?
                    realrow < 1 ? 0 : realrow - 1 :
                        realrow < getRowCount() - 1 ? realrow + 1 : realrow);
                    scrollRowToVisible(realrow);
                }
                
                // Calculate the insets for the *JTREE*, not the viewport
                // the tree is in. This makes it a bit messy.
                public Insets getAutoscrollInsets() {
                    Rectangle outer = getBounds();
                    Rectangle inner = getParent().getBounds();
                    return new Insets(
                    inner.y - outer.y + scrollMargin, inner.x - outer.x + scrollMargin,
                    outer.height - inner.height - inner.y + outer.y + scrollMargin,
                    outer.width - inner.width - inner.x + outer.x + scrollMargin);
                }
	}

	public LazyTreeModel getModel(){ return model;}

	public void setContext(TermModel c){
		model.setContext(c);
	}
	
	public TermModel getContext(){
		return ((LazyTreeModel)model).getContext();
	}
	
	// XJComponent interface methods
	public PrologEngine getEngine(){return engine;}
	
	public GUITerm getGT(){return gt;}
	
	public void setGT(GUITerm gt){
		this.gt=gt;
	}
	
	/** No op: GUI is refreshed automatically using the standard JTree machinery */
	public void refreshGUI(){
        }
	
	public boolean loadFromGUI(){
		return theJTree.loadFromGUI();
	}
	
	/** Delegates into our XJTableView */
	public boolean isDirty(){
		return theJTree.isDirty();
	}

 	/** This implementation does nothing. */
	public void setDefaultValue(TermModel dv){}
	
	public TermModel[] getSelectedTerms(){
		TreePath[] paths = theJTree.getSelectionPaths();
		if (paths==null) paths = new TreePath[0];
		TermModel[] selectedTerms = new TermModel[paths.length];
		for (int p=0;p<paths.length;p++) {
			LazyTreeModel.LazyTreeNode node = (LazyTreeModel.LazyTreeNode)paths[p].getLastPathComponent();
			selectedTerms[p] = node.getNodeTerm();
		}
		return selectedTerms;
	}

	public TermModel[] getSelectedNodeIDpaths(){
		TreePath[] paths = theJTree.getSelectionPaths();
		if (paths==null) paths = new TreePath[0];
		TermModel[] selectedNodeIDpaths = new TermModel[paths.length];
		for (int p=0;p<paths.length;p++) {
			Object[] nodes = paths[p].getPath();
			Vector<TermModel> nodeIDs = new Vector<TermModel>();
			for(int i=0;i<nodes.length;i++)
				nodeIDs.addElement(((LazyTreeModel.LazyTreeNode)nodes[i]).getID());
			selectedNodeIDpaths[p] = TermModel.makeList(nodeIDs);
		}
		return selectedNodeIDpaths;
	}

	/** Make the tree visible, and may select nodes */
	public void selectGUI(Object[] parts){
		theJTree.selectGUI(parts);
	}
	// do we need synchronization on oldSelection? probably not
 	public void willRefresh(){
 		oldSelection = getSelectedNodeIDpaths();
 		//System.out.println("will refresh, "+oldSelection.length+" items selected");
 	}
	public void didRefresh(){
 		//System.out.println("did refresh, current selection now has "+theJTree.getSelectionPaths()+" items");
 		//System.out.println("attempting to select "+oldSelection.length+" old items");
 		theJTree.selectNodes(oldSelection);
 		oldSelection=null;
	}

    /** Returns the term item at the location in (contained) JTree coordinates, or null if none is there */
    public TermModel point2Term(Point where){
    	return theJTree.point2Term(where);
    }
    
    // DnDCapable methods
    public DragGestureListener createDragGestureListener(){
        return theJTree.createDragGestureListener();
    }
    
    public JComponent getRealJComponent(){
        return theJTree;
    }
    
    /**
     */
    protected void setFixedHeight(JTree tree, LazyTreeModel model, TreeCellRenderer renderer){
       // get height of one of the nodes
        LazyTreeModel.LazyTreeNode aNode = null;
        LazyTreeModel.LazyTreeNode root = (LazyTreeModel.LazyTreeNode)model.getRoot();
        // for trees where the root is invisible get the first child of the root
        // for other trees get the root
        if(!tree.isRootVisible() && (root != null)){
            if(model.getChildCount(root) > 0){
                aNode = (LazyTreeModel.LazyTreeNode)model.getChild(root, 0);
            }
        }
        if(aNode == null){
            aNode = root;
        }
        if(aNode != null){
            Component comp = renderer.getTreeCellRendererComponent(tree, aNode, true, true, false, 1, true); // values on this line are randomly chosen
            if(comp != null){
                tree.setRowHeight((int)comp.getPreferredSize().getHeight());
                tree.setLargeModel(true);
            }
        }
    }
    
    /** Returns number of nodes actually expanded. Test with (ipObjectSpec(int,?I,[200],?), javaMessage(TreeID,?R,expandAll(?I)))@\plg. */
    public int expandAll(int nMax){ // not really! no more than a few nodes
    	int i = 0;
		for (; (i<nMax && i < theJTree.getRowCount()); i++) {
         	theJTree.expandRow(i);
		}
		return i;
    }
    /** Expand one level of nodes, assumed at the same initial level */
    public void expandOne(){
    	int initialRows = theJTree.getRowCount();
    	for (int r = initialRows-1; r>=0; r--)
    		theJTree.expandRow(r);
    }

}
