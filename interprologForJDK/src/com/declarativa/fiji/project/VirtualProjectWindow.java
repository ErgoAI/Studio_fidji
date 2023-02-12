package com.declarativa.fiji.project;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import com.declarativa.fiji.FijiSubprocessEngineWindow;

/** A project window which is not user edited, but rather reflects some existing project structure, such as Ergo's or Prolog's modules loaded in memory*/
@SuppressWarnings("serial")
public class VirtualProjectWindow extends AbstractProjectWindow {
	public VirtualProjectWindow(FijiSubprocessEngineWindow LW, AbstractProjectModel model){
		super(LW,model);
		// Hide Action column:
		table.getColumnModel().getColumn(1).setMinWidth(0);
		table.getColumnModel().getColumn(1).setMaxWidth(0);
		table.getColumnModel().getColumn(1).setResizable(false);
	}
	public static VirtualProjectWindow createErgoMemory(FijiSubprocessEngineWindow LW){
		return new VirtualProjectWindow(LW,new ErgoMemoryProjectModel(LW));
	}
	public void refresh(){
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				if (model!=null)
					model.refresh();				
			}
		});
	}
	/** This implementation does not provide a Load All menu: items assumes in memory */
	protected void populateFileMenu(JMenu fileMenu) {
		fileMenu.add(new OpenAllAction());
		//fileMenu.addSeparator();
	}

	/** Returns false if the window is not closed (user cancelled). */
	public boolean doClose(){
		rememberPreference(listener.preferences);
		setVisible(false);
		return true;
	}
	
	//TODO: build EditableProject from snapshot
}
