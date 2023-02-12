package com.declarativa.fiji.project;

import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.fife.ui.rsyntaxtextarea.TextEditorPane;

import com.declarativa.fiji.FijiSubprocessEngineWindow;
import com.declarativa.fiji.LogicProgramEditor;

@SuppressWarnings("serial")
/** This is actually a bit more than a model, offers some UI dialogs */
abstract class AbstractProjectModel extends AbstractTableModel{
    FijiSubprocessEngineWindow listener;
    ArrayList<ProjectItem> items = new ArrayList<ProjectItem>();
    static final String DIRTY_PROPERTY = "dirty project";
    private ArrayList<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();
    
    AbstractProjectModel(FijiSubprocessEngineWindow listener){
        this.listener = listener;
    }
    
    public String getName(){
        return super.toString();
    }
    
    public String getTitle(){
        return "???";
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener){
        listeners.add(listener);
    }
    protected void firePropertyChanged(PropertyChangeEvent e){
        for (PropertyChangeListener L : listeners)
            L.propertyChange(e);
    }
	
    public int getColumnCount() { return 4; }
    public int getRowCount() { return items.size();}
    public Object getValueAt(int row, int col) { 
        ProjectItem item = items.get(row);
        if (col==0) return item.file.getName();
        else if (col==1) return item.action;
        else if (col==2) return item.module;
        else return item.status;
    }
    public String getColumnName(int col) {
        if (col==0) return "Logic file";
        else if (col==1) return "Action";
        else if (col==2) return "Module";
        else return "Status";
    }
    public boolean isCellEditable(int rowIndex, int columnIndex){
        if (columnIndex==1||columnIndex==2) return true;
        else return false;
    }
    void openAll(final Action B){
        Thread background = new Thread("Project opener"){
                public void run(){
                    boolean ok = true;
                    boolean shouldMinimize = items.size() > AbstractProjectWindow.maxToOpenWithoutMinimizing;
                    for (int i=0; i<items.size(); i++){
                        ProjectItem item = items.get(i);
                        if (item.file.exists()){
                            LogicProgramEditor LPE =
                                LogicProgramEditor.showEditor(
                                                              LogicProgramEditor.prologFilename(item.file,AbstractProjectModel.this.listener),
                                                              null,
                                                              AbstractProjectModel.this.listener);
                            if (shouldMinimize) LPE.setState(Frame.ICONIFIED);
                        }
                        else ok = false;
                    }
                    if (!ok)
                        try{ SwingUtilities.invokeAndWait(new Runnable(){
                                public void run(){
                                    JOptionPane.showMessageDialog(null,"Some files were not opened.",
                                                                  "Error(s) opening",JOptionPane.ERROR_MESSAGE);
                                }
                            });
                        } catch(Exception e){throw new RuntimeException(e);}
                    B.setEnabled(true);
                    
                }
            };
        background.start();
    }
    
    void loadAll(final Action actionToEnableB){
        Thread background = new Thread("Project loader"){
                public void run(){
                    boolean ok = true;
                    try{
                        for (int i=0; i<items.size(); i++){
                            ProjectItem item = items.get(i);
                            final int ii = i;
                            if (item.action.equals(ProjectItem.ADD)) item.status=ProjectItem.ADDING;
                            else if (item.action.equals(ProjectItem.LOAD)) item.status=ProjectItem.LOADING;
                            SwingUtilities.invokeAndWait(new Runnable(){
                                    public void run(){
                                        fireTableRowsUpdated(ii,ii);
                                    }
                                });
                            boolean fileok = true;
                            if (item.file.getName().endsWith(".P")||item.file.getName().endsWith(".pl")||item.file.getName().endsWith(".plt")) 
                                fileok = AbstractProjectModel.this.listener.engine.consultAbsolute(item.file);
                            else 
                                if (!(item.action.equals(ProjectItem.NOP))) 
                                    fileok = !AbstractProjectModel.this.listener.loadOrAdd(item.file,item.action,item.module).getHasErrors();
                            if (!fileok) item.status=ProjectItem.ERRORS;
                            else {
                                if (item.action.equals(ProjectItem.ADD)) 
                                    item.status=ProjectItem.ADDED;
                                else if (item.action.equals(ProjectItem.LOAD))
                                    item.status=ProjectItem.LOADED;
                                LogicProgramEditor LPE = LogicProgramEditor.getExistingEditor(item.file,AbstractProjectModel.this.listener);
                                if (LPE!=null) 
                                        LPE.maySetPreferredModule(item.module);
                            }
                            SwingUtilities.invokeAndWait(new Runnable(){
                                    public void run(){
                                        fireTableRowsUpdated(ii,ii);
                                    }
                                });
                            ok = ok && fileok;
                        }
                        if (!ok)
                            SwingUtilities.invokeAndWait(new Runnable(){
                                    public void run(){
                                        JOptionPane.showMessageDialog(null,"Some files were not loaded.",
                                                                      "Error(s) loading",JOptionPane.ERROR_MESSAGE);
                                    }
                                });
                        actionToEnableB.setEnabled(true);
                        AbstractProjectModel.this.listener.refreshProblemsPanel(!ok);
                    } catch(Exception e){
                        // waiting for a weird one to manifest again...:
                        com.declarativa.interprolog.AbstractPrologEngine.printAllStackTraces();
                        throw new RuntimeException("Bad exception:"+e);
                    }
                    
                }
            };
        background.start();
    }
    
    /** This implementation returns false */
    public boolean isDirty() {
        return false;
    }
    
    
    /**
     * @param F an item's file
     * @return -1 if not found
     */
    public int getIndexFor(File F){
        int found = -1;
        for (int i=0; i<items.size(); i++){
            ProjectItem item = items.get(i);
            if (item.file.equals(F)){
                found = i;
                break;
            }
        }
        return found;
    }
    
    /** Somehow updates the project model. This implementation does nothing */
    public void refresh(){}
    
    static class ProjectItem implements PropertyChangeListener{
        /** For action */
        static final String ADD="\\add", LOAD="\\load";
        /** For status*/
        static final String NONE="none", /* OPEN="open", DIRTY="dirty", */ LOADING = "loading", ADDING="adding";
        static final String ERRORS="errors", LOADED="loaded", ADDED="added", INCLUDED="included";
        public static final String NOP = "";
        File file; String action, module, status;
        AbstractProjectModel model;
        
        ProjectItem(AbstractProjectModel model, File file, String action, String module){
            this.file=file; this.action=action; this.module=module; status=NONE;
            this.model=model;
        }
        ProjectItem(AbstractProjectModel model, File file, String action, String module, String status){
            this.file=file; this.action=action; this.module=module; this.status=status;
            this.model=model;
        }
        public void propertyChange(PropertyChangeEvent evt){
            if (evt.getPropertyName().equals(TextEditorPane.FULL_PATH_PROPERTY)){
                TextEditorPane editor = (TextEditorPane)evt.getSource();
                file = new File(editor.getFileFullPath());
                int ii = model.items.indexOf(this);
                model.fireTableRowsUpdated(ii,ii);
            }
        }
        
        
        
    }
}
