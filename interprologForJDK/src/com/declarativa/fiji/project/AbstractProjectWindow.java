/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2013
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/

package com.declarativa.fiji.project;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.fife.ui.rsyntaxtextarea.TextEditorPane;

import com.declarativa.fiji.FijiPreferences;
import com.declarativa.fiji.FijiSubprocessEngineWindow;
import com.declarativa.fiji.LogicProgramEditor;
import com.declarativa.fiji.project.AbstractProjectModel.ProjectItem;
import com.xsb.xj.XJTable;

@SuppressWarnings("serial")
public abstract class AbstractProjectWindow extends JFrame{
	// ipPrologEngine(E), javaMessage('com.declarativa.fiji.ProjectWindow',W,'ProjectWindow'(E)).
	AbstractProjectModel model;
	FijiSubprocessEngineWindow listener;
	JTable table;
	final static int maxToOpenWithoutMinimizing = 4;
	static final String PREF_PREFIX = "com.declarativa.fiji.ProjectWindow.";
	static final String FILEITEM_PREFIX = "file.";
	public static final String FILE_COMMENT = " A Ergo project, in Java Properties format\n";
	
	/** pairs <model name, this object> */
	static HashMap<String,AbstractProjectWindow> projects = new HashMap<String,AbstractProjectWindow>();
	protected JScrollPane scrollpane; 
	
	public static Iterator<AbstractProjectWindow> getProjects(){
		return projects.values().iterator();
	}
	
	static void rememberPreferences(FijiPreferences Prefs){
		for (AbstractProjectWindow P : projects.values())
			P.rememberPreference(Prefs);
	}

	public static boolean someDirtyProject(){
		for (AbstractProjectWindow P : projects.values())
			if (P.model.isDirty()) return true;
		return false;
	}

	static String preferenceName(String filename){
		return PREF_PREFIX+filename;
	}
	
	public static String preferenceName2Filename(String prefName){
		return prefName.substring(PREF_PREFIX.length());
	}

	public static boolean hasPreferencePrefix(String preference){
		return preference.startsWith(PREF_PREFIX);
	}
	
	public String preferenceName(){
		return preferenceName(model.getName());
	}
	

	/** If it can't find a preferred window size and position for a present screen, returns null */
	static protected Rectangle getPreferredBounds(String name,FijiSubprocessEngineWindow listener){
		String preference = listener.preferences.getProperty(preferenceName(name));
		return FijiPreferences.pref2Rectangle(preference);
	}
	
	String window2pref(){
		return FijiPreferences.window2pref(this,false);
	}

	void rememberPreference(FijiPreferences P){
		P.setProperty(preferenceName(model.getName()), window2pref()); 
	}
	
	
	protected AbstractProjectWindow(FijiSubprocessEngineWindow LW, AbstractProjectModel model){
		super(model.getTitle());
		this.listener=LW;
		setLayout(new BorderLayout());
		this.model = model;
      	//table = new MyJTable(model);
      	table = new JTable(model);
      	table.setAutoCreateRowSorter(true);
		JComboBox<String> comboBox = new JComboBox<String>();
		comboBox.addItem(ProjectItem.ADD);
		comboBox.addItem(ProjectItem.LOAD);
		comboBox.addItem(ProjectItem.NOP);
		table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(comboBox));
		new XJTable.SizeAdjusterToFirstRow(table);
		table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer(){
    		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				String S = (String)value;
				if (S.equals(ProjectItem.ERRORS)) c.setForeground(Color.RED);
				//else if (S.equals(ProjectItem.LOADED) || S.equals(ProjectItem.ADDED)) c.setForeground(Color.GREEN);
				else c.setForeground(Color.BLACK);
        		return c;
    		}
		});
		
		table.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e) {
				int row        = table.rowAtPoint(e.getPoint());
				int viewColumnIndex  = table.columnAtPoint(e.getPoint());
				if(e.getClickCount() == 2 && row >= 0 && viewColumnIndex != -1 && row < table.getRowCount()) {
					row = table.convertRowIndexToModel(row);
					table.getSelectionModel().setSelectionInterval(row, row);
					ProjectItem item = AbstractProjectWindow.this.model.items.get(row);
					e.consume();
					if (item.file.exists()){
						LogicProgramEditor LPE = LogicProgramEditor.showEditor(item.file.getAbsolutePath(),null,listener);
						LPE.editor.addPropertyChangeListener(TextEditorPane.FULL_PATH_PROPERTY,item);
						LPE.maySetPreferredModule(item.module);
					} else listener.errorMessage("That file vanished: "+item.file.getAbsolutePath());
				}
				
			}
		});
		
	 	installKeyCommands();
		      	
      	scrollpane = new JScrollPane(table);

		getContentPane().add(scrollpane,BorderLayout.CENTER);
		/*
		model.items.add(new ProjectItem(new File ("foo.flr"), "\\add", "main"));
		model.items.add(new ProjectItem(new File ("bar.flr"), "\\add", "main"));
		model.items.add(new ProjectItem(new File ("barrrrr.flr"), "\\load", "baril"));
		model.items.get(1).status="errors";*/
		JMenuBar mb = new JMenuBar();
		JMenu fileMenu = new JMenu("File"); mb.add(fileMenu);
		
		

		populateFileMenu(fileMenu);
		
		setJMenuBar(mb);	
		listener.addWindowsMenuTo(this);
			
		pack();
		setFont(getFont().deriveFont((float)LW.preferredFontSize));
      	table.setFont(table.getFont().deriveFont((float)LW.preferredFontSize));
      	table.setRowHeight(LW.preferredFontSize+2);
      	
		
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				doClose();
			}
		});
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		projects.put(model.getName(),this);
		Rectangle rect = getPreferredBounds(model.getName(), LW);
		if (rect!=null)
			setBounds(rect);
		setVisible(true);
		listener.updateBoundsPreference(preferenceName(model.getName()),this);
	}

	protected void installKeyCommands() {
	}

	protected void populateFileMenu(JMenu fileMenu) {
		fileMenu.add(new OpenAllAction());
		fileMenu.add(new LoadAllAction());
		
		//fileMenu.addSeparator();
	}

	/** Returns false if the window is not closed (user cancelled). */
	public boolean doClose(){
		rememberPreference(listener.preferences);
		projects.remove(model.getName());
		model=null;
		dispose();
		return true;
	}
	
	class OpenAllAction extends AbstractAction{
		OpenAllAction(){
			super("Open all");
			putValue(Action.SHORT_DESCRIPTION,"Open editor windows for all files; if more than "+ maxToOpenWithoutMinimizing +
			", all the new windows will be minimized");
		}
		public void actionPerformed(ActionEvent e){
			setEnabled(false);
			model.openAll(this);
		}
	}
	
	class LoadAllAction extends AbstractAction{
		LoadAllAction(){
			super("Load all");
			putValue(Action.SHORT_DESCRIPTION,"Load or add all files");
		}
		public void actionPerformed(ActionEvent e){
			if (!listener.checkFloraShell()) return;
			setEnabled(false);
			model.loadAll(this);
		}
	}
			
}
