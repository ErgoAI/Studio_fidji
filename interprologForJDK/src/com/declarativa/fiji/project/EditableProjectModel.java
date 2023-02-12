package com.declarativa.fiji.project;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.declarativa.fiji.FijiPreferences;
import com.declarativa.fiji.FijiSubprocessEngineWindow;
import com.declarativa.fiji.FijiSubprocessEngineWindow.FileAndModule;
import com.declarativa.interprolog.FloraSubprocessEngine;

@SuppressWarnings("serial")
class EditableProjectModel extends AbstractProjectModel {
	File file;
	/** This project had either a file, action or module changed since the last save */
	private boolean dirty = false;
	EditableProjectModel(FijiSubprocessEngineWindow listener, File F){
		super(listener);
		file = F;
		loadProject();
	}

	public String getName(){
		return file.getAbsolutePath();
	}
	
	public String getTitle(){
		return file.getName()+" (project)";
	}
	void loadProject(){
		Properties P = new Properties();
		try{
			FileReader FR = new FileReader(file);
			P.load(FR);
			FR.close();
		} catch (IOException ex){
			throw new RuntimeException("Problems loading Fidji project:"+ex);
		}
		items.clear();
		Set<Object> keys = P.keySet();
		try{
			for (Object key : keys){
				String name = (String)key;
				if (name.startsWith(AbstractProjectWindow.FILEITEM_PREFIX)){
					File fileItem = new File(name.substring(5));
					String property = (String)P.get(key);
					Scanner S = new Scanner(property);
					S.useDelimiter(FijiPreferences.PREF_SEPARATOR);
					String action = S.next();
					String module = S.next();
					S.close();
					add(new ProjectItem(this,fileItem,action,module));
				}
			}
		} catch(Exception e){
			System.err.println("Bad project file:"+file);
		}
		setDirty(false);
	}
	void saveProject(){
		Properties P = new Properties();
		for (ProjectItem item: items)
			P.put(AbstractProjectWindow.FILEITEM_PREFIX+item.file.getAbsolutePath(),item.action+FijiPreferences.PREF_SEPARATOR+(item.module.length()==0?" ":item.module));

		try{
			FileWriter FW = new FileWriter(file);
			P.store(FW,AbstractProjectWindow.FILE_COMMENT);
			FW.close();
			setDirty(false);
		} catch (IOException ex){
			throw new RuntimeException("Problems saving Fidji project:"+ex);
		}
	}
	protected void setDirty(boolean dirty){
		boolean old = this.dirty;
		this.dirty=dirty;
		firePropertyChanged(new PropertyChangeEvent(this, AbstractProjectModel.DIRTY_PROPERTY, new Boolean(old), new Boolean(dirty) ));
	}
	public boolean isDirty() {
		return dirty;
	}
	
	public void setValueAt(Object aValue, int rowIndex, int columnIndex){
		ProjectItem item = items.get(rowIndex);
		if (columnIndex==1) {
			item.action = (String)aValue;
			setDirty(true);
		} else if (columnIndex==2) {
			String M = (String)aValue;
			if (M.indexOf("'")==-1) {
				item.module=M;
				setDirty(true);
			}
		}
	}
	public void add(ProjectItem item){
		items.add(item);
		setDirty(true);
		fireTableRowsInserted(items.size()-1,items.size());
	}
	public ProjectItem remove(int index){
		ProjectItem R = items.remove(index);
		setDirty(true);
		fireTableRowsDeleted(index,index);
		return R;
	}			
	synchronized boolean doImportSilkProject(File F){
		int initialSize = items.size();
		boolean importSucceeded = true;
		Document document = null;
		if (!F.getName().endsWith(".silkprj"))
			throw new RuntimeException("Silk project files must have extension .silkprj");
		File projectBase = F.getParentFile();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(F);
		} catch (Exception e){
			throw new RuntimeException("Problems parsing XML:"+e);
		}
		NodeList loads = document.getElementsByTagName("load");
		for (int f=0; f<loads.getLength(); f++){
			String silkname = ((Element)loads.item(f)).getAttribute("file");
			if (!silkname.endsWith(".silk"))
				throw new RuntimeException("Silk project files must have the .silk extension. This one does not:"+silkname);
			File silkFile = new File(projectBase,silkname);
		
			String silkbase = silkFile.getName().substring(0,silkFile.getName().length()-5);
	
			File compiled = new File(silkFile.getParent(),"compiled/"+silkbase+"/");
			if (!compiled.exists()){
				importSucceeded = false;
				//throw new RuntimeException("Silk files must have their compiled/ subdirectory together. This one does not:"+silkname);
				System.err.println("Missing compiled/....flr:"+silkname);
				continue;
			}
			File[] compiledFiles = compiled.listFiles();
			for (File compiledFile:compiledFiles)
				if (FloraSubprocessEngine.isFloraSourceFile(compiledFile) || FloraSubprocessEngine.isErgoSourceFile(compiledFile))
					add(new ProjectItem(this,compiledFile,ProjectItem.ADD,"main"));
		}
		if (items.size()>initialSize)
			fireTableRowsInserted(initialSize,items.size());
		return importSucceeded;
	}
	void includeFile(){
		FileAndModule FM = null;
		if ((FM = listener.pickFileAndModule("Include Ergo or Prolog file..."))!=null){
			if (FloraSubprocessEngine.isFloraSourceFile(FM.file) || FloraSubprocessEngine.isErgoSourceFile(FM.file)) {
				add(new ProjectItem(this,FM.file,ProjectItem.ADD,FM.module));
			} else if (FM.file.getName().endsWith(".P")||FM.file.getName().endsWith(".pl")||FM.file.getName().endsWith(".plt")){
				add(new ProjectItem(this,FM.file,ProjectItem.LOAD," "));
			} else
				JOptionPane.showMessageDialog(null,"Only Ergo, Flora or XSB files can be included into a project",
						"Error including",JOptionPane.ERROR_MESSAGE);	
		}	
	}

}
