package com.declarativa.fiji.project;

import java.io.File;

import com.declarativa.fiji.FijiSubprocessEngineWindow;
import com.declarativa.fiji.LogicProgramEditor;
import com.declarativa.interprolog.TermModel;

/**
 * Maintains a "project" cache based on the currently loaded/added/included Ergo files, as introspected from the Flora registry and include dependencies.
 * To refresh the cash message this... refresh(), which in addition to rebuilding the cache will notify all editors of the relevant information,
 * so these can update their UI.
 * Typically there will be a single instance, owned by a FijiSubprocessEngineWindow
 * To visualize it use a VirtualProjectWindow.
 *
 */
@SuppressWarnings("serial")
public class ErgoMemoryProjectModel extends AbstractProjectModel {
	String title, name;

	public ErgoMemoryProjectModel(FijiSubprocessEngineWindow listener) {
		super(listener);
		title = "Loaded Ergo files";
		name = "com.declarativa.fiji.project.ErgoInMemory";
		refresh();
	}
	
	@Override
	public String getTitle(){
		return title;
	}
	@Override
	public String getName(){
		return name;
	}
	
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	/** 
	 * 	refresh method to update items: insert/delete/update...loses items loaded in the past. Notifies editors.
	 */
	public void refresh(){
		if (!listener.getEngine().isAvailable()) // this needs to be commented to test with java(....) from the listener
			return;
		Object[] bindings = listener.getEngine().deterministicGoal(
			"findall(t(FilePath,Module,Status,Includer),fjFileInMemory(FilePath,Module,Status,Includer),L), buildTermModelArray(L,LM)",
			"[LM]");
		if(bindings == null){
			System.err.println("Failed to refresh items for "+getTitle());
			return;
		}
		TermModel[] terms = (TermModel[])bindings[0];
		boolean[] stillThere = new boolean[items.size()];
		for (TermModel term:terms){
			File logicFile = new File(term.children[0].toString());
			String newAction = ProjectItem.NOP;
			String newModule = term.children[1].toString();
			String newStatus = term.children[2].toString();
			String newIncluder = term.children[3].toString(); // aka "Hosting file"
			File newIncluderF = (newIncluder.equals("null") ? null : new File(newIncluder));
			
			LogicProgramEditor LPE = LogicProgramEditor.getExistingEditor(logicFile);
			if (newStatus.equals("load")){
				newStatus = ProjectItem.LOADED;
				newAction = ProjectItem.LOAD;
				if (LPE!=null)
					LPE.setWasLoadedAddedIncluded(false, true, false, false, newIncluderF, newModule);
			} else if (newStatus.equals("add")){
				newStatus = ProjectItem.ADDED;
				newAction = ProjectItem.ADD;
				if (LPE!=null) 
					LPE.setWasLoadedAddedIncluded(false, false, true, false, newIncluderF, newModule);
			} else if (newStatus.equals("included")) {
				if (LPE!=null) 
					LPE.setWasLoadedAddedIncluded(false, false, false, true, newIncluderF, newModule);
			} else 
				System.err.println("Unknown status in ErgoMemoryProject:"+term);
			
			int index = getIndexFor(logicFile);
			if (index==-1){
				items.add(new ProjectItem(this, logicFile, newAction, newModule, newStatus));
			} else {
				stillThere[index] = true;
				ProjectItem item = items.get(index);
				item.action = newAction;
				item.module = newModule;
				item.status = newStatus;
			}
		}
		for (int i=stillThere.length-1;i>=0;i--)
			if (!stillThere[i]){
				ProjectItem unloaded = items.get(i);
				LogicProgramEditor LPE = LogicProgramEditor.getExistingEditor(unloaded.file);
				if (LPE!=null)
					LPE.setWasLoadedAddedIncluded(false,false, false, false, null, null);
				items.remove(i);
			}
		this.fireTableDataChanged(); // could be less brutal...
	}
	

}
