package com.xsb.xj;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.XJException;

/**
 * Lazy tree which keeps eager list of selection paths. Useful for cases when 
 * initial selections are to be setup before presenting it to the user and when it
 * is necessary to get the user selection back after the term representation was edited
 * by the user. So the tree GT is not non-persistent - it keeps selection information,
 * but it is lazy.
 * Example:
 * <pre>
 * guiTerm(testTree1/1,null,null,gt(testTree1,[class='com.xsb.xj.containers.XJBorderLayout',updatable],
 *	[ gt('.',
 *	   [class='com.xsb.xj.XJTreeWithSelections',lazytree( [
 *			gt(_,[root,typename=complexid,typicalSize=1,class='com.xsb.xj.ValueRow',tip='M if multiple parents',
 *			      borderless,color=orange,readonly],[ 
 *				gt(_,[borderless,class='com.xsb.xj.XJLabel',tip='Class'],[]), % class names assumed atomic
 *				gt(_,[invisible,borderless,opaque],[])
 *				]),
 *			gt(_,[typename=simpleid,root,typicalSize=1,tip='M if multiple parents',class='com.xsb.xj.ValueRow',
 *			      borderless,color=orange,readonly],[ 
 *				gt(_,[borderless,class='com.xsb.xj.XJLabel',tip='Class'],[]),
 *				gt(_,[borderless,integer,invisible],[])
 *				])
 *			] 
 *			 ),lazy(classTreeSc(_,_,1),classTreeName(_,_,_),1)],
 *	   _
 *	   )
 *      ])).
 * </pre>
 * if classTreeSc/3 and classTreeName/3 goals are defined for this example,
 * <code>editTermGetResult(testTree1([[1],[1,2,3],[1,11]]),null,X)</code>
 * will display a tree with nodes with ids by paths [1], [1,2,3], [1,11] 
 * (paths from root) selected. On the user selection X will unify with the new paths
 * of the selections.
 * As seen in the example above the differences between XJTree and XJTreeWithSelections are the following:
 * <ul>
 * <li>For XJTree the node value is 'XJ$LAZY' and for XJTreeWithSelections it is '.'(since the list is passed)
 * <li>For XJTree <code>lazy(SubclassGoal,LabelGoal,Context)</code> term is in 
 * the value part of the GT whenever for XJTreeWithSelections it is moved to 
 * GT properties, since the value for GT is a list of selection paths.
 * </ul>
 *
 */
@SuppressWarnings("serial")
public class XJTreeWithSelections extends XJTree{
    
    
    public XJTreeWithSelections(PrologEngine engine, GUITerm gt){ // TODO factor this with the constructor in XJTree
        //super(engine, gt);
        
        this.gt=gt;
        this.engine=engine;
        boolean DRY = gt.findProperty(GUITerm.DRY)!=null;
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
        
        theJTree = new XJTreeViewWithSelections(this, model, renderer, editor);
        
        setViewportView(theJTree);
        // not anymore???? theJTree.addNotify();
        ToolTipManager.sharedInstance().registerComponent(this);
        
        
        
        if(gt.getChildCount() > 0){
           theJTree.selectNodes(gt.getChildren());
        }
        
    }

    // Overwritten this method to set appropriate tree selections
    public void refreshGUI(){
        theJTree.selectNodes(gt.getChildren());
    }
    
    static class  XJTreeViewWithSelections extends XJTreeView{
        XJTreeViewWithSelections(XJTree theXJTree, LazyTreeModel model, TreeCellRenderer renderer, TreeCellEditor editor){
            super(theXJTree, model, renderer, editor);
            
            XJAction[] topOps = theXJTree.getGT().operations(theXJTree.getEngine(),theXJTree);
            final XJAction selectionChangedAction = XJAction.findSelectionChanged(topOps);
            if (selectionChangedAction == null) {
                addTreeSelectionListener(new TreeSelectionListener() {
                    public void valueChanged(TreeSelectionEvent  e){
                        loadFromGUI();
                    }
                });
            }
        }
        
        // overloading some methods
        public boolean loadFromGUI(){
            //System.out.println("In loadFromGUI");
            if (!isEditing()) {
                TermModel[] newSelections = theXJTree.getSelectedNodeIDpaths();
                rememberTermSelection(newSelections);
                return true;
            }
            else if(stopEditing()){
                TermModel[] newSelections = theXJTree.getSelectedNodeIDpaths();
                rememberTermSelection(newSelections);
                return true;
            } else {
                return false;
            }
        }
                
        public void rememberTermSelection(TermModel[] selectionsList){
            GUITerm treeGT = theXJTree.getGT();
            //System.out.println("rememberTermSelection: old gt " + treeGT);
            //int oldCount = treeGT.getChildCount();
            //System.out.println("rememberTermSelection: oldCount " + oldCount);
            TermModel[] oldChildren = treeGT.getChildren();
            treeGT.setChildren(selectionsList);
            //treeGT.assign(selectionList);
            //System.out.println("rememberTermSelection: new gt " + treeGT);
            //int newCount = getRowCount();
            //if (newCount>oldCount) {
            XJChangeManager.UndoableTermEdit ute = new XJChangeManager.UndoableTermEdit(
            this,treeGT,XJChangeManager.ADDCHILDREN_EDIT,-1,oldChildren,treeGT.getChildren());
            treeGT.fireUndoableEditUpdate(new UndoableEditEvent(this,ute));
            //}
        }
        
        /** Used to implement undo/redo */
        public void setChildren(TermModel[] terms){
            GUITerm treeGT = theXJTree.getGT();
            treeGT.setChildren(terms);
            
            //TermModel[] selectedPaths = TermModel.flatList(terms[0]);
            System.out.println("setChildren:Flat list of Children  is " + treeGT);
            //TermModel[] selectedPaths = TermModel.flatList(gt);
            selectNodes(treeGT.getChildren());
        
            // a bit abrupt, this may get refined later as we diversify each model's edition methods:
            //fireTableDataChanged();
        }
    }
}
