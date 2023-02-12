package com.xsb.xj;
import java.awt.Component;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.XJException;

/** An object providing several useful Action objects related to data editing and undoing, whose enabled/disabled state 
will follow the basic XJ data changing logic. Therefore using them for menu items etc. guarantees these will be 
enabled/disabled appropriately; if more customized behavior is desired, some other Action object may invoke 
these and test actionConcluded() for success, and act accordingly; also, it may choose 
to implement PropertyChangeListener, and by adding itself as a PropertyChangeListener 
it will have its own enabling state kept automatically */
@SuppressWarnings("serial")
public class XJChangeManager extends UndoManager{
	public final static KeyStroke undoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z,Event.CTRL_MASK);
	public final static KeyStroke redoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z,Event.CTRL_MASK+Event.SHIFT_MASK);
	public Action undoAction,redoAction;
	public PrologAction saveAction,deleteAction;
	public CloseAction closeAction;
	GUITerm gt;
	PrologEngine engine;
	Component component;
	boolean isNewTerm=false, insertable=false, updatable=false, deletable=false;
	
	public XJChangeManager(XJComponent c){
		gt = c.getGT();
		component = (Component)c;
		undoAction = new UndoAction(); 
		redoAction = new RedoAction();
		gt.addUndoableEditListener(this);
		engine = ((XJComponent)component).getEngine();
                boolean gtWithNoPersistentChildren = gt.isLazyList()  ||
                                         (gt.isLazyTree() && !gt.isList()); //not a lazy tree with eager selections
		isNewTerm = gt.findProperty(GUITerm.ISNEWTERM) != null;
		insertable = !((gt.findProperty(GUITerm.INSERTABLE)==null) || gtWithNoPersistentChildren);
		updatable = !((gt.findProperty(GUITerm.UPDATABLE)==null) || gtWithNoPersistentChildren);
		deletable = !((gt.findProperty(GUITerm.DELETABLE)==null) || gtWithNoPersistentChildren);
		if (insertable||updatable) saveAction = new SaveAction();
		else saveAction=null;
		if(deletable) deleteAction = new DeleteAction();
		else deleteAction=null;
		closeAction = new CloseAction();
	}
	
	// UndoableEditListener interface:

	public void undoableEditHappened(UndoableEditEvent e){
		UndoableEdit edit = e.getEdit();
		addEdit(edit);
		if(edit instanceof UndoableTermEdit){
			UndoableTermEdit ute = (UndoableTermEdit)edit;
			if ((ute.model instanceof GUITerm) && gt!= ((GUITerm)ute.model).root) throw new XJException("Unexpected gt");
		} else throw new XJException("Unexpected UndoableEdit");
		//System.out.println("got UndoableEditEvent:"+e.getEdit());
		updateActions();
	}
	
	public boolean isDirty(){
		return canUndo(); // something can be undone <=> there have been changes!
	}

    protected void updateActions() {
        if (canUndo()) {
            undoAction.setEnabled(true);
            undoAction.putValue(Action.NAME, getUndoPresentationName());
        } else {
            undoAction.setEnabled(false);
            undoAction.putValue(Action.NAME, "Undo");
        }
        if (canRedo()) {
            redoAction.setEnabled(true);
            redoAction.putValue(Action.NAME, getRedoPresentationName());
        } else {
            redoAction.setEnabled(false);
            redoAction.putValue(Action.NAME, "Redo");
        }
        if (saveAction!=null) saveAction.setEnabled(isDirty());
        if (deleteAction!=null) deleteAction.setEnabled(!isNewTerm);
    }      
	
    /** Uses the undo machinery to avoid keeping redundant "old term" info elsewhere */
	synchronized GUITerm getOldTerm(){
		if (!isDirty()) return gt;
        UndoableEdit next = editToBeUndone();
		while(canUndo()) undo();
		GUITerm old = (GUITerm)gt.clone();
		if (next==null) while(canRedo()) redo();
        else 
        	try{ redoTo(next); } 
        	catch(CannotRedoException e){throw new XJException("Inconsistent undo fiddling:"+e);};
        return old;
	}
	/* redundant with GUITerm.loadAllFromGUI():
	public boolean loadAllGUIs(){
		XJComponent[] renderers = gt.collectSignificantRenderers();
		System.out.println("loadAllGUIs():"+renderers.length);
		for (int c=0;c<renderers.length;c++){
			System.out.println(renderers[c]);
			if (!renderers[c].loadFromGUI()) return false;
		}
		return true;
	}*/
	
	/** Save this term to Prolog */
	public class SaveAction extends PrologAction{
		SaveAction(){
            super(XJChangeManager.this.engine, XJChangeManager.this.component, "xjSaveTerm(Old,New,IsNew)", "Save");
            setEnabled(false);
            setThreaded(false); // we want to check actionConcluded() afterwards
		}
        public void actionPerformed(ActionEvent e) {
        	if (!gt.loadAllFromGUI()) return; // first error was reported by its XJComponent
        	TermModel newTerm = gt.getTermModel();
        	// should abort here if there is one non optional node without a value
        	setArguments(
        		"[Old,New,IsNew]",
        		new Object[]{(!isNewTerm?getOldTerm().getTermModel():null),newTerm,new Boolean(isNewTerm)}
        		);
      		super.actionPerformed(e);
        	if (actionSucceeded()) {
        		discardAllEdits();
        		isNewTerm=false;
	            updateActions();
        	}
        }          
	}
	
	/** Whoever invokes this must destroy the XJComponent rendering the term if actionConcluded() */
	public class DeleteAction extends PrologAction{
		DeleteAction(){
            super(XJChangeManager.this.engine, XJChangeManager.this.component, "xjDeleteTerm(OldTerm)", "Delete");
           	setEnabled(!isNewTerm);
            setThreaded(false); // we want to check actionConcluded() afterwards
		}
        public void actionPerformed(ActionEvent e) {
        	setArguments(
        		"[OldTerm]",
        		new Object[]{getOldTerm().getTermModel()}
        		);
        	super.actionPerformed(e);
        	if (actionSucceeded()) {
        		discardAllEdits();
        		isNewTerm=false;
	            updateActions();
        	}
        }          
	}
	
	/** Whoever invokes this should destroy the XJComponent rendering the term if actionConcluded() */
	public class CloseAction extends AbstractAction{
		boolean completed=false;
		boolean showConfirmation;
		CloseAction(boolean showConfirmation){
            super("Close");
            setEnabled(true);
            this.showConfirmation=showConfirmation;
		}
		CloseAction(){
			this(true);
		}
        public void actionPerformed(ActionEvent e) {
        	completed=false;
	 		if (saveAction!=null && isDirty()) {
				int result;
				if (showConfirmation) result = JOptionPane.showConfirmDialog(
					component,
					"Save "+gt.getUserTitle()+" before closing?",
					"Closing...",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
				else result = JOptionPane.YES_OPTION;
				if (result==JOptionPane.CANCEL_OPTION) return;
				if (result==JOptionPane.YES_OPTION){
					saveAction.actionPerformed(e);
					completed = saveAction.actionSucceeded();
				} else completed=true;
			} else completed=true;
        }
        public boolean actionConcluded(){return completed;} 
	}
	
	public class UndoAction extends AbstractAction{
        UndoAction() {
            super("Undo");
            setEnabled(false);
            // no impact on the menu items... putValue(ACCELERATOR_KEY,undoKey);
        }
        
        public void actionPerformed(ActionEvent e) {
            try {
                undo();
                //gt.refreshRenderers();
            } catch (CannotUndoException ex) {
                throw new XJException("Unable to undo: " + ex);
            }
            updateActions();
        }          
	}
	
    public class RedoAction extends AbstractAction {
        public RedoAction() {
            super("Redo");
            setEnabled(false);
       }

        public void actionPerformed(ActionEvent e) {
            try {
                redo();
            } catch (CannotRedoException ex) {
                throw new XJException("Unable to redo: " + ex);
            }
            updateActions();
        }
    }   
    
	public static final int SETNODEVALUE_EDIT = 1, SETTERM_EDIT = 2, ADDCHILDREN_EDIT=3, DELETECHILDREN_EDIT=4;
	
	public static class UndoableTermEdit extends AbstractUndoableEdit{
		Object oldValue,newValue; 
		Object model; 
		GUITerm gt;
		int childIndex;
		int editType;

		public UndoableTermEdit(Object model, GUITerm gt, int editType, int childIndex, Object oldValue,Object newValue){
			this.editType = editType;
			this.gt=gt;
			if (editType <1 || editType>4) throw new XJException("bad editType");
			this.oldValue = oldValue; this.newValue = newValue;
			this.model=model; this.childIndex = childIndex;
		}
		public void undo() throws CannotUndoException{
			boolean notifying = gt.getNotifyUndoListeners();
			gt.setNotifyUndoListeners(false);
			super.undo();
			if (editType==SETNODEVALUE_EDIT) {
				((GUITerm)model).setNodeValue(oldValue);
				((GUITerm)model).refreshRenderers();
			} 
			else if (editType==SETTERM_EDIT) ((XJAbstractListModel)model).setTerm(childIndex,(TermModel)oldValue);
                        else if (editType==ADDCHILDREN_EDIT) {
                            try{
                                java.lang.reflect.Method setChildren = model.getClass().getMethod("setChildren",new Class[]{TermModel[].class});
                                setChildren.invoke(model,new Object[]{(TermModel[])oldValue});
                            } catch (Exception e){
                                System.out.println("Warning: "+model.getClass()+" does not have setChildren method with arguments "+(new TermModel[0]).getClass());
                                e.printStackTrace();
                            }
                            //((EagerListModel)model).setChildren((TermModel[])oldValue);
                        }
			else if (editType==DELETECHILDREN_EDIT) {
                            try{
                                java.lang.reflect.Method setChildren = model.getClass().getMethod("setChildren",new Class[]{TermModel[].class});
                                setChildren.invoke(model,new Object[]{(TermModel[])oldValue});
                            } catch (Exception e){
                                System.out.println("Warning: "+model.getClass()+" does not have setChildren method with arguments "+(new TermModel[0]).getClass());
                                e.printStackTrace();
                            }
//                            ((EagerListModel)model).setChildren((TermModel[])oldValue);
                        }
			gt.setNotifyUndoListeners(notifying);
		}
		public void redo() throws CannotRedoException{
			boolean notifying = gt.getNotifyUndoListeners();
			gt.setNotifyUndoListeners(false);
			super.redo();
			if (editType==SETNODEVALUE_EDIT){
				((GUITerm)model).setNodeValue(newValue);
				((GUITerm)model).refreshRenderers();
			} 
			else if (editType==SETTERM_EDIT) ((XJAbstractListModel)model).setTerm(childIndex,(TermModel)newValue);
                        else if (editType==ADDCHILDREN_EDIT) {
                            try{
                                java.lang.reflect.Method setChildren = model.getClass().getMethod("setChildren",new Class[]{TermModel[].class});
                                setChildren.invoke(model,new Object[]{(TermModel[])newValue});
                            } catch (Exception e){
                                System.out.println("Warning: "+model.getClass()+" does not have setChildren method");
                                e.printStackTrace();
                            }
                            //((EagerListModel)model).setChildren((TermModel[])newValue)
                        }
			else if (editType==DELETECHILDREN_EDIT){ 
                            try{
                                java.lang.reflect.Method setChildren = model.getClass().getMethod("setChildren",new Class[]{TermModel[].class});
                                setChildren.invoke(model,new Object[]{(TermModel[])newValue});
                            } catch (Exception e){
                                System.out.println("Warning: "+model.getClass()+" does not have setChildren method");
                                e.printStackTrace();
                            }
                            //((EagerListModel)model).setChildren((TermModel[])newValue); 
                        }
			gt.setNotifyUndoListeners(notifying);
		}
		public String toString(){
			return "UndoableTermEdit: oldValue=="+oldValue+",newValue=="+newValue;
		}
	}
}