package com.declarativa.fiji.project;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.declarativa.fiji.FijiPreferences;
import com.declarativa.fiji.FijiSubprocessEngineWindow;
import com.declarativa.fiji.project.AbstractProjectModel.ProjectItem;
import com.declarativa.interprolog.FloraSubprocessEngine;

@SuppressWarnings("serial")
public class EditableProjectWindow extends AbstractProjectWindow {
	SaveAction saveAction;

	public static EditableProjectWindow showProject(File F,FijiSubprocessEngineWindow listener){
		EditableProjectWindow PW = getExistingProject(F);
		if (PW==null) PW = new EditableProjectWindow(listener, F);
		else {
			PW.setVisible(true); PW.toFront(); PW.requestFocus(); 
		}
		return PW;
	}

	public static EditableProjectWindow getExistingProject(File file){
		// If it has a file, it's an editable project
		return (EditableProjectWindow)projects.get(file.getAbsolutePath());
	}
	
	EditableProjectWindow(FijiSubprocessEngineWindow LW, File F){
		super(LW,new EditableProjectModel(LW, F));
		DropTargetListener dropHandler = new DropTargetListener(){
			public void dragOver(DropTargetDragEvent dtde){}
			public void dropActionChanged(DropTargetDragEvent dtde){}
			public void dragExit(DropTargetEvent dte){}
			public void drop(DropTargetDropEvent dtde){
				handleFileDnD(dtde);
			}
			public void dragEnter(DropTargetDragEvent dtde){}
		};
		new DropTarget(scrollpane,dropHandler);
		new DropTarget(this,dropHandler);
	}
	class SaveAction extends AbstractAction implements PropertyChangeListener{
		SaveAction(){
			super("Save");
			putValue(Action.SHORT_DESCRIPTION,"Save the project file; this does NOT save the individual source files");
			setEnabled(model.isDirty());
			model.addPropertyChangeListener(this);
		}
		public void actionPerformed(ActionEvent e){
			doIt();
		}
		public void doIt(){
			((EditableProjectModel)model).saveProject();
		}
		public void propertyChange(PropertyChangeEvent evt) {
			setEnabled(model.isDirty());
		}
	}
	protected void populateFileMenu(JMenu fileMenu){
		saveAction = new SaveAction();
		fileMenu.add(saveAction).setAccelerator(
        	KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		fileMenu.addSeparator();
		FijiSubprocessEngineWindow.addItemToMenu(fileMenu,"Include...",new ActionListener(){
			public void actionPerformed(ActionEvent e){
				((EditableProjectModel)model).includeFile();
			}
		}).setToolTipText("Include an additional logic file into this project");		

		fileMenu.addSeparator();
		super.populateFileMenu(fileMenu);
	}
	protected void installKeyCommands() {
		InputMap inputMap = table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = table.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "Delete");
		actionMap.put("Delete", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int selected = table.getSelectedRow();
				if (selected>=0) ((EditableProjectModel)model).remove(selected);
			}
		});
		table.setToolTipText("To delete one item select its file name and press the Delete key");
	}
	public boolean doClose(){
		if (model.isDirty()){
			int choice = JOptionPane.showConfirmDialog(
					this,"Save changes ?","Save project file before closing?", JOptionPane.YES_NO_CANCEL_OPTION);
			if (choice==JOptionPane.CANCEL_OPTION) 
				return false;
			if (choice==JOptionPane.YES_OPTION)
				saveAction.doIt();
		}
		return super.doClose();
	}
	// this is too similar to a method in ListenerWindow, should be refactored
	void handleFileDnD(DropTargetDropEvent dtde){
		//System.out.println("drop:"+dtde);
		try{
			Transferable transferable = dtde.getTransferable();
			/*
			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			for (int f=0;f<flavors.length;f++)
				System.out.println("Flavor:"+flavors[f]);*/
			int action = dtde.getDropAction();
			if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){	
				dtde.acceptDrop(action);
				final java.util.List<?> files = (java.util.List<?>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
				dtde.getDropTargetContext().dropComplete(true);
				boolean allOK = true;
				for (int f=0;f<files.size();f++){
					if (!listener.droppableFile((File)files.get(f))) {
						allOK=false; break;
					}
				}
				if(!allOK) 
					JOptionPane.showMessageDialog(this,listener.badFilesDroppedMessage(),"Error including",JOptionPane.ERROR_MESSAGE);	
				else {
					listener.prologOutput.append("\nIncluding "+((files.size()>1 ? files.size()+" files into project...\n" : files.size()+" file into project...\n")));
					Runnable r = new Runnable(){
						public void run(){
							boolean crashed = false;
							Toolkit.getDefaultToolkit().sync();
							for (int f=0;f<files.size() && !crashed;f++){
								File file = (File)files.get(f);
								if (FloraSubprocessEngine.isFloraSourceFile(file) || FloraSubprocessEngine.isErgoSourceFile(file)) {
									((EditableProjectModel)model).add(new ProjectItem(model,file,ProjectItem.ADD,"main"));
								} else if (file.getName().endsWith(".P") || file.getName().endsWith(".pl") || file.getName().endsWith(".plt")){
									((EditableProjectModel)model).add(new ProjectItem(model,file,ProjectItem.LOAD," "));
								}
							}
							listener.prologOutput.append("...done.\n");
							listener.scrollToBottom();
						}
					};
					SwingUtilities.invokeLater(r);
				}	
			} else dtde.rejectDrop();
		} catch (Exception e){
			throw new RuntimeException("Problem dropping:"+e);
		}
	}

}
