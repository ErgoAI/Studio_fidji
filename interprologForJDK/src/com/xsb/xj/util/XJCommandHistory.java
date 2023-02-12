package com.xsb.xj.util;

import java.io.File;

import javax.swing.JMenu;

import com.declarativa.interprolog.gui.CommandHistory;
import com.xsb.xj.XJTextArea;

public class XJCommandHistory extends CommandHistory {
	public XJCommandHistory(File file) {
		super(file);
	}
	public void addField(XJTextArea text){
		addField(text.getTextArea());
	}
	public void addMenuAndField(JMenu menu,int firstItem,XJTextArea text){
		addMenuAndField(menu,firstItem,text.getTextArea());
	}

}
