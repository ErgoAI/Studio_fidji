package com.xsb.xj;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.util.IPAbortedException;
import com.declarativa.interprolog.util.IPInterruptedException;
import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.IPPrologError;
import com.xsb.xj.util.XJException;
import com.declarativa.interprolog.util.VariableNode;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * A PrologAction encapsulating an XJ operation. Prior to performing the Prolog
 * action a target is bound to a "lambda" variable. It knows perhaps a bit too
 * much about lists, this may or not change during some future code streamlining
 *
 *@version   $Id: XJAction.java,v 1.24 2005/03/27 17:12:16 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJAction extends PrologAction {
	GUITerm originalTarget;
	TermModel originalRoot, originalOperation;
	TermModel lambda;
	TermModel target;
	TermModel D;

	/**
	 * Used only for hierarchical menus. The Prolog subterm to which XJActionHelper
	 * paths will be matched
	 */
	TermModel lambdaHM;

	/**
	 * Fourth arg, for 'dropped' operations
	 */
	TermModel lambdaDT;

	/**
	 * Fourth arg value to be used when doing the operation
	 */
	TermModel DT;

	/**
	 * Action attached to a list root (not to the template), requires special
	 * treatment
	 */
	boolean forList;

	/**
	 * Action attached to a tree root (not to the template), requires special
	 * treatment
	 */
	boolean forTree;

	/**
	 * true means that either we're listening or we know that we don't care
	 */
	boolean listeningListSelections;

	/**
	 * Used only for list and tree operations
	 */
	TermModel[] selectedTerms;

	final static String DROPPED      = "dropped";
	final static String THREADED     = "threaded";
	final static String THREADEDINAWT     = "threadedInAWT";
	final static String DISABLING = "disabling";

	/**
	 * parent must be an XJComponent, and it should enclose the root
	 *
	 *@param engine     Prolog Engine
	 *@param parent     xjcomponent, must enclose root
	 *@param target     Description of the Parameter
	 *@param root       Description of the Parameter
	 *@param operation  Description of the Parameter
	 */
	public XJAction(PrologEngine engine, Component parent, GUITerm target, TermModel root, TermModel operation) {
		super(engine, parent, null, operationDescription(operation));

        this.originalTarget = target;
		this.originalRoot = root;
		this.originalOperation = operation;

		// sequence forTree then forList is important for XJTreeWithSelections
		forTree = target.isLazyTree();
		forList = (target.isLazyList() || target.isList()) && !target.isOpaque() && !forTree;
		if(parent != null && !(parent instanceof XJComponent)) {
			throw new XJException("Expected XJcomponent in XJAction constructor:" + parent);
		}

		D = (TermModel) operation.getChild(2);
		if(isHierarchicalMenuOperation() || isDroppedMenuOperation()) {
			lambdaHM = (TermModel) D.getChild(0);
		} else {
			lambdaHM = null;
		}

		TermModel opTargetArg  = (TermModel) operation.getChild(0);
		if(forList || forTree || target.isComboField()) {
			if(!opTargetArg.node.equals("terms") || opTargetArg.getChildCount() != 2) {
				throw new XJException("Bad operation target argument for list:" + opTargetArg);
			}
		} else if(!opTargetArg.node.equals("term") || opTargetArg.getChildCount() != 2) {
			throw new XJException("Bad operation target argument:" + opTargetArg);
		}

		if(root == null) {
			throw new XJException("Root of target is null for operation " + operation);
		}

		lambda = opTargetArg;
		this.target = target;
		goal = GUITerm.floraPreprocessWithVarList((TermModel) operation.getChild(1));

		// Cater for goals to be run in background
		TermModel tmgoal       = (TermModel) goal;
		if(tmgoal.node.equals(THREADED) || tmgoal.node.equals(THREADEDINAWT)) {
			if(tmgoal.getChildCount() != 1) {
				throw new XJException(THREADED + " operation functors must have 1 child goal only:" + tmgoal);
			}
			setThreaded(true); 
			if (tmgoal.node.equals(THREADEDINAWT)) setInAWTThread(true);
			else setInAWTThread(false);
			goal = tmgoal.getChild(0);
			tmgoal = (TermModel)goal;
		}

		if(tmgoal.node.equals(DISABLING)) {
			if(tmgoal.getChildCount() != 1) {
				throw new XJException(DISABLING + " operation functors must have 1 child goal only:" + tmgoal);
			}
			disabling = true; 
			goal = tmgoal.getChild(0);
		}

		// Drag and drop stuff:
		if(isDroppedOperation()) {
			lambdaDT = (TermModel) operation.getChild(3);
		} else {
			lambdaDT = null;
		}
		DT = null;

		listeningListSelections = false;
		mayListenListSelections();
	}

	/**
	 * Make sure some list root actions (menu and gui) are disabled when the list
	 * has no selection. Implementation is a bit convoluted as we wish to avoid the
	 * need for an explicit XJAction setup method
	 */
	void mayListenListSelections() {
		if(listeningListSelections || (!forList && !forTree)) {
			return;
		}
		if(context == null) {
			return;
		}// no gui object yet
		if(context instanceof XJComboBox) {
			return;
		}// very similar to lists, but no selection-dependent action enabling
		if(isMenuOperation() || isGUIOperation()) {
			if(forList) {
				JTable table  = ((XJTable) context).getJTable();
				if(table == null) {
					return;
				}// not yet our time
				setEnabled(!table.getSelectionModel().isSelectionEmpty());
				table.getSelectionModel().addListSelectionListener(
					new ListSelectionListener() {
						public void valueChanged(ListSelectionEvent e) {
							if(e.getValueIsAdjusting()) {
								return;
							}
							ListSelectionModel lsm  = (ListSelectionModel) e.getSource();
							setEnabled(!lsm.isSelectionEmpty());
						}
					});
			} else {
				// trees
				final JTree tree  = ((XJTree) context).getJTree();
				if(tree == null) {
					return;
				}// not yet our time
				setEnabled(tree.getSelectionCount() != 0);
				tree.addTreeSelectionListener(
					new TreeSelectionListener() {
						public void valueChanged(TreeSelectionEvent e) {
							setEnabled(tree.getSelectionCount() != 0);
						}
					});
			}
			listeningListSelections = true;
		} else {
			listeningListSelections = true;
		}

	}

	static String operationDescription(TermModel operation) {
		TermModel D  = (TermModel) operation.getChild(2);
		if(D.getChildCount() == 0) {
			return D.node.toString();
		}
		// if (D.getChildCount()!=1) throw new XJException("Operation " +operation+" third argument "+D+ " must have 0 or 1 args");
		if(D.node.equals("menu")) {
			if(D.getChildCount() == 2 && ((TermModel)D.getChild(1)).isList()) {
				return D.getChild(0).toString();
			} else if(D.getChildCount() == 1) {
				return D.getChild(0).toString();
			} else {
				return ((TermModel) D.getChild(1)).node.toString();
			}
		} else if(D.node.equals("gui")) {
			return "some gui object:" + ((TermModel) D.getChild(0)).node.getClass();
		} else if(D.node.equals("dropped")) {
			return "Drag and Drop";
		} else {
			throw new XJException("Unexpected term for third argument in operation");
		}
	}


	static XJAction findDoubleClick(XJAction[] ops) {
		for(int a = 0; a < ops.length; a++) {
			if(ops[a].isDoubleClick()) {
				return ops[a];
			}
		}
		return null;
	}

	static XJAction findSelectionChanged(XJAction[] ops) {
		for(int a = 0; a < ops.length; a++) {
			if(ops[a].isSelectionChanged()) {
				return ops[a];
			}
		}
		return null;
	}

	static XJAction findRowCountChanged(XJAction[] ops) {
		for(int a = 0; a < ops.length; a++) {
			if(ops[a].isRowCountChanged()) {
				return ops[a];
			}
		}
		return null;
	}

	static XJAction[] findDropped(XJAction[] ops) {
	    XJAction[] droppedOpsArray;
	    Vector<XJAction> droppedOps = new Vector<XJAction>();
		for(int a = 0; a < ops.length; a++) {
			if(ops[a].isDroppedOperation()) {
			    droppedOps.add(ops[a]);
			}
		}
		if(droppedOps.size() > 0){
		    droppedOpsArray = new XJAction[droppedOps.size()];
		    int i = 0;
		    for(Iterator<XJAction> iterator = droppedOps.iterator() ; iterator.hasNext() ; ){
			droppedOpsArray[i++] = iterator.next();
		    }
		    return droppedOpsArray;
		} else {
		    return null;
		}
	}

	boolean isDroppedOperation() {
		return originalOperation.getChildCount() == 4 && D.node.toString().toLowerCase().equals(DROPPED);
	}

	public boolean isDoubleClick() {
		return D.node.equals(GUITerm.DOUBLECLICK) && D.isLeaf();
	}

	public boolean isMenuOperation() {
		return (D.node.equals("menu"));
	}

	public boolean isHierarchicalMenuOperation() {
		return isMenuOperation() && D.getChildCount() == 2 && !((TermModel)D.getChild(1)).isList();
	}

    public boolean isDroppedMenuOperation() {
		return isDroppedOperation() && (D.getChildCount() == 2);
	}

	public boolean isGUIOperation() {
		return (D.node.equals(GUITerm.GUI));
	}

	public boolean isSelectionChanged() {
		return (D.node.equals(GUITerm.SELECTIONCHANGED));
	}
	
	public boolean isRowCountChanged() {
		return (D.node.equals(GUITerm.ROWCOUNTCHANGED));
	}

	public void attachToGUI() {
		if(!isGUIOperation()) {
			throw new XJException("this ain't no GUI operation");
		}
		if(D.getChildCount() != 1) {
			throw new XJException("wrong arguments in gui(_)");
		}
		TermModel ref         = (TermModel) D.getChild(0);
		if(!(ref.node instanceof Integer)) {
			throw new XJException("operation with bad gui(Ref):" + ref + ",node class==" + ref.node.getClass()+"\ngoal:"+goal);
		}
		int refNumber         = ((Integer) ref.node).intValue();
		JComponent component  = (JComponent) engine.getRealJavaObject(refNumber);
		try {
			// Let's try to grab properties from the component:
			putValue(SHORT_DESCRIPTION, component.getToolTipText());
			if(component instanceof AbstractButton) {
				putValue(NAME, ((AbstractButton) component).getText());
				putValue(SMALL_ICON, ((AbstractButton) component).getIcon());
			}// JTextField and JComboBox catered for above

			boolean attached  = false;
			// first let's try to set this as the Action for the component
			Method getAction  = null;
			Method setAction  = null;
			try {
				getAction = AbstractPrologEngine.findMethod(component.getClass(), "getAction", new Class[0]);
				setAction = AbstractPrologEngine.findMethod(component.getClass(), "setAction", new Class[]{Action.class});
			} catch(NoSuchMethodException e) {}
			if(getAction != null && setAction != null) {
				Object currentAction  = getAction.invoke(component, new Object[0]);
				if(currentAction == null) {
					Object[] newAction  = {this};
					setAction.invoke(component, newAction);
					attached = true;
				}// else let's not screw up what was setup elsewhere
			}
			if(!attached) {
				// If the component does not accept Actions, let's try to make this an ActionListener to it:
				Class<?>[] actionListener  = {ActionListener.class};
				Method remove           = AbstractPrologEngine.findMethod(component.getClass(), "removeActionListener", actionListener);
				if(remove == null) {
					throw new XJException("could not attach to gui because it originates no Action events: " + component);
				}
				Method add              = AbstractPrologEngine.findMethod(component.getClass(), "addActionListener", actionListener);
				if(add == null) {
					throw new XJException("could not attach to gui because it originates no Action events: " + component);
				}
				Object[] actual         = {this};
				add.invoke(component, actual);
				attached = true;
			}
			// Now that this action is in business, setup its component if not already there
			if(context == null) {
				context = (Component) originalTarget.getARenderer();
			}
			mayListenListSelections();
		} catch(Exception e) {
			throw new XJException("Problems attaching XJAction to component:" + e);
		}
	}

	public JMenuItem buildMenu() {
            if((D.getChildCount() == 3) && isMenuTerm(D)){ // new way
                return buildHMenuTree(D, new Stack<String>());
            } else if(!isHierarchicalMenuOperation()) {
			JMenuItem item  = new JMenuItem((String) description);
			item.addActionListener(this);

			if(D.getChildCount() == 2) {//menu(Text,[mnemonic(M), image(I)|_])
				TermModel [] props  = ((TermModel) D.getChild(1)).flatList();

				for(int i = 0; props.length > 0 && i < props.length; i++) {
					TermModel prop = props[i];
					if(prop == null) {
						continue;
					}

					if(prop.node.equals("mnemonic") && prop.getChildCount() == 1) {
						TermModel mnemonic  = (TermModel)prop.getChild(0);

						if(mnemonic.isAtom()) {
							char[] m = mnemonic.node.toString().toCharArray();

							if(m.length > 0) {
								item.setMnemonic(m[0]);
							}
						}
					} else if(prop.node.equals("image") && prop.getChildCount() == 1) {
						TermModel image     = (TermModel)prop.getChild(0);
						if(image.isAtom()) {
							File file    = new File(image.node.toString());
							URL iconURL  = null;

							if(file.exists()) {
								try {
									iconURL = file.toURI().toURL();
									if(iconURL != null) {
										item.setIcon(new ImageIcon(iconURL));
									}
								} catch(MalformedURLException e) {
									throw new XJException("XJAction: bad url for image");
								}
							}
						}
					}
				}
			}

			return item;
		} else {
			return buildHMenuTree((TermModel) D.getChild(1), new Stack<String>());
		}
	}

	JMenuItem buildHMenuTree(TermModel tree, Stack<String> path) {
            if(isMenuTerm(tree)){
                Hashtable<Object,Object> guiRefsBag = new Hashtable<Object,Object>();
                GUITerm menuGt = makeMenuGt(tree, guiRefsBag);
                XJComponent menuGui = menuGt.makeGUI(engine);
                for(Iterator<java.util.Map.Entry<Object,Object>> it = guiRefsBag.entrySet().iterator(); it.hasNext();){
                    java.util.Map.Entry<Object,Object> e = it.next();
                    originalRoot.assignToVar((VariableNode)e.getKey(), ((TermModel)e.getValue()).node);
                }
                menuGt.refreshRenderers();
                // Collect all leaf menu items to listen to their actions
                if(menuGui instanceof XJMenuItemComponent){
                    Collection<?> leafs = ((XJMenuItemComponent)menuGui).getLeafMenuItems();
                    for(Iterator<?> i = leafs.iterator(); i.hasNext(); ){
                        XJMenuItemComponent item = (XJMenuItemComponent)i.next();
                        item.addActionListener(new ActionListener(){
                            public void actionPerformed(ActionEvent e) {
                                description = ((XJMenuItemComponent)e.getSource()).getPath();
                                doit();
                            }
                        });
                    }
                } 
                return (JMenuItem)menuGui;
            } else {
                // old way of creating hierarchical menus
		String label  = tree.node.toString();
		path.push(label);

		if(tree.isLeaf()) {
			JMenuItem item  = new JMenuItem(label);
			item.addActionListener(new XJActionHMHelper(path));
			path.pop();
			return item;
		} else {
			JMenu submenu  = new JMenu(label);
			// no action attached to intermediate nodes
			for(int c = 0; c < tree.getChildCount(); c++) {
				submenu.add(buildHMenuTree((TermModel) tree.getChild(c), path));
			}
			path.pop();
			return submenu;
		}
            }
	}
        
        /**
         * Given menu term of form menu(Label, PropList, Submenus)
         * converts it to a similar GUITerm
         * guiRefsBag returns a list of variables from myGUI property
         * to use them later when they are grounded to replace other occurrences
         * of them in the rest of GT
         */
        protected GUITerm makeMenuGt(TermModel menu, Hashtable<Object,Object> guiRefsBag){
                TermModel children = ((TermModel)menu.getChild(2));
		if(children.isListEnd()) {
                    GUITerm gt = new GUITerm(menu.getChild(0),
                                       ((TermModel)menu.getChild(1)).flatList(),
                                       children.flatList(), 
                                       false);
                    if(gt.findProperty("class") == null){
                        if(gt.findProperty("checkbox") == null){
                            gt.addProperty(new TermModel("=",
                                                         new TermModel[]{new TermModel("class"),
                                                                         new TermModel("com.xsb.xj.XJMenuItem")}));
                        } else {
                            gt.addProperty(new TermModel("=",
                                                         new TermModel[]{new TermModel("class"),
                                                                         new TermModel("com.xsb.xj.XJCheckBoxMenuItem")}));
                        }
                    }
                    // colect all myGUI variables
                    TermModel [] guiRefs = gt.findProperties("myGUI");
                    for(int i=0; i<guiRefs.length; i++){ 
                        guiRefsBag.put(((TermModel)guiRefs[i].getChild(0)).node, guiRefs[i].getChild(0)); 
                    }
                    return gt;
		} else {
                    TermModel [] submenus = children.flatList();
                    GUITerm [] submenuGts = new GUITerm [submenus.length];
                    for(int c = 0; c < submenus.length; c++) {
                        submenuGts[c] = makeMenuGt(submenus[c], guiRefsBag);
                    }
                    GUITerm gt = new GUITerm(menu.getChild(0),
                                       ((TermModel)menu.getChild(1)).flatList(),
                                       submenuGts,
                                       false);
                    if(gt.findProperty("class") == null){
                        gt.addProperty(new TermModel("=", 
                                                     new TermModel[]{new TermModel("class"), 
                                                                     new TermModel("com.xsb.xj.XJMenu")}));
                    }
                    // colect all myGUI variables
                    TermModel [] guiRefs = gt.findProperties("myGUI");
                    for(int i=0; i<guiRefs.length; i++){ 
                        guiRefsBag.put(((TermModel)guiRefs[i].getChild(0)).node, guiRefs[i].getChild(0)); }
                    return gt;
		}
        }
                
    protected boolean isMenuTerm(TermModel menu){
        return ((menu.getChildCount() == 3) && (menu.node.toString().equals("menu")));
    }

	public static boolean hasMenuActions(XJAction[] actions) {
		for(int i = 0; actions.length > 0 && i < actions.length; i++) {
			if(actions[i].isMenuOperation()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param menu
	 * @param actions may include non menu actions
	 * @return Number of menu actions found and added
	 */
	public static int addMenuActions(JMenu menu, XJAction[] actions) {
		int count = 0;
		for(int a = 0; a < actions.length; a++) {
			if(actions[a].isMenuOperation()) {
				menu.add(actions[a].buildMenu());
				count ++;
			}
		}
		return count;
	}

	public static void addMenuActions(JPopupMenu menu, XJAction[] actions) {
		for(int a = 0; a < actions.length; a++) {
			if(actions[a].isMenuOperation()) {
				menu.add(actions[a].buildMenu());
			}
		}
	}

	/**
	 * This should be invoked just prior to doing a DnD operation
	 *
	 *@param term  The new droppedTerm value
	 */
	public void setDroppedTerm(TermModel term) {
		DT = term;
	}
        
    public JMenuItem[] buildDropActionMenus(){
        TermModel[] menus = ((TermModel)D.getChild(1)).flatList();
        JMenuItem[] menuItems = new JMenuItem[menus.length];
        for(int i = 0; i < menus.length; i++){
            menuItems[i] = buildHMenuTree(menus[i], new Stack<String>());
        }
        return menuItems;
    }

	/**
	 * This should be invoked just prior to doing a DnD operation dropping on a
	 * list or tree
	 *
	 *@param selectedTerms  The new selectedTerms value
	 */
	public void setSelectedTerms(TermModel[] selectedTerms) {
		this.selectedTerms = selectedTerms;
	}

    public boolean isAcceptableDnDTerm(TermModel selectedTerm){
	return lambdaDT.unifies(selectedTerm);
    }

	public void doit() {
		if(callIfQuickMode
			 && !isEnabled()) {
			return;
		}
		if((context instanceof XJComponent)) {
			if(!((XJComponent) context).getGT().loadAllFromGUI()) {
				return;
			}
		}
		super.doit();
	}

    public void actionPerformed(ActionEvent e) {
        //System.out.println("Entered XJAction.actionPerformed, this class=="+this.getClass()+", isEnabled()=="+isEnabled());
        if(!isEnabled()) {
            return;
        }
        description = e.getActionCommand();
        // Object w = XJDesktop.findWindowOrSimilar(e.getSource());
        // XJDesktop.setWaitCursor((Component)w);
        super.actionPerformed(e);
        // XJDesktop.restoreCursor((Component)w);
    }

    public void run() {
        /*
          System.out.println("Entered XJAction.run");
          System.out.println("forList=="+forList);
          System.out.println("target:"+target);
          System.out.println("lambda:"+lambda);
          System.out.println("goal:"+goal);
          System.out.println("running description:"+description);
        */
        String objSpecs;
        Object[] objects;
        String dg;
        IPException exception = null;
        
        if(isDroppedOperation() && DT == null) {
            throw new XJException("running an operation without DT set:" + this);
        }
        
        if(forList || forTree) {
            if(!(context instanceof XJTable) && !(context instanceof XJTree) && !(context instanceof XJComboBox)) {
                // this can not be checked earlier, as actions may have their parents set up after construction
                throw new XJException("Bad component in XJAction:" + context);
            }
            Integer listRef  = new Integer(engine.registerJavaObject(context));
            if(isDroppedOperation()) {
                objects = new Object[]{listRef, selectedTerms, lambda, goal, lambdaDT, DT, lambdaHM, description};
                objSpecs = "[ListIntObj,SelectedTermsObj,Lambda,Goal,LambdaDT,DTspec,LambdaHMspec,DescriptionSpec]";
                dg = "recoverTermModels([Lambda,Goal,LambdaDT" + (lambdaHM == null ? "" : ",LambdaHMspec") + "]," +
                    "[terms(ListRef,SelectedTerms),G,DT" + (lambdaHM == null ? "" : ",LambdaHM") + "]), " +
                    "ipObjectSpec('java.lang.Integer',ListIntObj,[ListRef],_), recoverTermModelArray(SelectedTermsObj,SelectedTerms), " +
                    "recoverTermModel(DTspec,DT)," +
                    (lambdaHM == null ? "" : "stringArraytoList(DescriptionSpec,LambdaHM), ") +
                    (callIfQuickMode ? "xjCallIfQuick(G,Result)" : "G");
            } else {
                if(context instanceof XJComboBox) {
                    selectedTerms = ((XJComboBox) context).getSelectedTerms();
                } else if(forList) {
                    selectedTerms = ((XJTable) context).getSelectedTerms();
                } else {
                    selectedTerms = ((XJTree) context).getSelectedTerms();
                }
                objects = new Object[]{listRef, selectedTerms, lambda, goal, lambdaHM, description};
                objSpecs = "[ListIntObj,SelectedTermsObj,Lambda,Goal,LambdaHMspec,DescriptionSpec]";
                dg = "recoverTermModels([Lambda,Goal" + (lambdaHM == null ? "" : ",LambdaHMspec") + "]," +
                    "[terms(ListRef,SelectedTerms),G" + (lambdaHM == null ? "" : ",LambdaHM") + "]), " +
                    "ipObjectSpec('java.lang.Integer',ListIntObj,[ListRef],_), " +
                    "recoverTermModelArray(SelectedTermsObj,SelectedTerms), " +
                    (lambdaHM == null ? "" : "stringArraytoList(DescriptionSpec,LambdaHM), ") +
                    (callIfQuickMode ? "xjCallIfQuick(G,Result)" : "G");
            }
        } else {
            GUITerm.PathSearch path  = new GUITerm.PathSearch();
            TermModel rootTerm       = ((GUITerm) (originalRoot)).getTermModel(originalTarget, path);
            
            if(isDroppedOperation()) {
                objects = new Object[8];
                objSpecs = "[Root,Path,Lambda,Goal,LambdaDT,DTspec,LambdaHMspec,DescriptionSpec]";
                dg = "recoverTermModels([Lambda,Goal,LambdaDT" + (lambdaHM == null ? "" : ",LambdaHMspec") + 
                    "],[term(TT,PP),G,DT" + (lambdaHM == null ? "" : ",LambdaHM") + "]), " +
                    "recoverTermModel(Root,TT), recoverTermModel(Path,PP), " +
                    "recoverTermModel(DTspec,DT), " +
                    (lambdaHM == null ? "" : "stringArraytoList(DescriptionSpec,LambdaHM), ") +
                    (callIfQuickMode ? "xjCallIfQuick(G,Result)" : "G");
                objects[4] = lambdaDT;
                objects[5] = DT;
                objects[6] = lambdaHM;
                objects[7] = description;
            } else {
                objects = new Object[6];
                objSpecs = "[Root,Path,Lambda,Goal,LambdaHMspec,DescriptionSpec]";
                dg = "recoverTermModels([Lambda,Goal" + (lambdaHM == null ? "" : ",LambdaHMspec") + "]," +
                    "[term(TT,PP),G" + (lambdaHM == null ? "" : ",LambdaHM") + "]), " +
                    "recoverTermModel(Root,TT), recoverTermModel(Path,PP), " +
                    (lambdaHM == null ? "" : "stringArraytoList(DescriptionSpec,LambdaHM), ") +
                    (callIfQuickMode ? "xjCallIfQuick(G,Result)" : "G");
                objects[4] = lambdaHM;
                objects[5] = description;
            }
            objects[0] = rootTerm;
            if(context instanceof XJTree) {
                TreePath[] paths        = ((XJTree) context).getJTree().getSelectionPaths();
                if(paths.length != 1) {
                    throw new XJException("XJTree selection inconsistent with XJAction");
                }
                Object[] objectsInPath  = paths[0].getPath();
                Vector<TermModel> temp  = new Vector<TermModel>();
                for(int o = 0; o < objectsInPath.length; o++) {
                    // temp.addElement(((LazyTreeModel.LazyTreeNode)objectsInPath[o]).getID());
                    // let's instead add the nodes themselves:
                    temp.addElement(new TermModel(new Integer(engine.registerJavaObject(objectsInPath[o]))));
                }
                objects[1] = TermModel.makeList(temp);
            } else {
                objects[1] = path.getPath();
            }
            objects[2] = lambda;
            objects[3] = goal;
        }
        Object[] bindings;
        goalWasInterrupted = false;
        goalAborted = false;
        try {
            /*
              System.out.println("Calling XJAction goal:"+rememberMyRef + dg);
              System.out.println("objSpecs:"+objSpecs);
              //System.out.println("objects:"+Arrays.toString(objects));
              System.out.println("thread:"+Thread.currentThread());
            */
            bindings = engine.deterministicGoal(rememberMyRef + dg, objSpecs, objects, (callIfQuickMode ? "[string(Result)]" : "[]"));
        } catch(IPInterruptedException e) {
            e.printStackTrace();
            bindings = null;
            goalWasInterrupted = true;
            exception = e;
        } catch(IPAbortedException e) {
            bindings = null;
            goalWasInterrupted = true;
            exception = e;
            e.printStackTrace();
        } catch (IPPrologError e){ 
            bindings=null;
            if (e.t instanceof TermModel && ((TermModel)e.t).toString().equals(XJPrologEngine.ERGO_USER_ABORT_HACK))
            	goalWasInterrupted = true;
            else goalAborted=true;
            exception = e;
            e.printStackTrace();
        } catch (IPException e){ 
            bindings=null;
            goalAborted=true;
            exception = e;
            e.printStackTrace();
        }
        goalEnded(bindings, exception);// bindings is either null or Object{}
    }
    
    public String getDescription() {
        if(description instanceof String[]) {
            String[] descriptions  = (String[]) description;
            return descriptions[descriptions.length - 1];
        } else {
            return super.getDescription();
        }
    }

    /**
     * To support hierarchical menus
     */
    class XJActionHMHelper implements ActionListener {
        String[] path;
        
        XJActionHMHelper(Vector<String> p) {
            path = new String[p.size()];
            for(int i = 0; i < path.length; i++) {
                path[i] = p.elementAt(i);
            }
        }
        
        public void actionPerformed(ActionEvent e) {
            description = path;
            doit();
        }
    }
}
