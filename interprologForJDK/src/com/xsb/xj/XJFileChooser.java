/*
    XJFileChooser.java
    Created on February 28, 2002, 10:01 AM
  */
package com.xsb.xj;

import com.xsb.xj.util.ExampleFileFilter;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;

/**
 *@author    tanya
 *@version
 */
@SuppressWarnings("serial")
public class XJFileChooser extends JFileChooser {
	private String approveText;
	private Component parent;

	/**
	 * Creates new XJFileChooser
	 *
	 *@param parent              XJFileChooser's parent window
	 *@param approveText         Button text for ok button
	 *@param extensions          List of allowable extensions
	 *@param allowMultipleFiles  true/false allow multiple files
	 */
	public XJFileChooser(Component parent, String approveText, String[] extensions, boolean allowMultipleFiles) {
		super();
		if(extensions.length != 0) {
			setFileFilter(new ExampleFileFilter(extensions));
		}
		this.setMultiSelectionEnabled(allowMultipleFiles);
		this.approveText = approveText;
		this.parent = parent;
	}

	public XJFileChooser(Component parent, String startDirectory, String approveText, String[] extensions, boolean allowMultipleFiles) {
		super(startDirectory);
		if(extensions.length != 0) {
			setFileFilter(new ExampleFileFilter(extensions));
		}
		this.setMultiSelectionEnabled(allowMultipleFiles);
		this.approveText = approveText;
		this.parent = parent;
	}

	public String pickFile() {
		int returnVal  = showDialog(parent, approveText);
		if(returnVal == APPROVE_OPTION) {
			return getSelectedFile().getPath();
		} else {
			return null;
		}
	}

	public String pickDir() {
		int returnVal  = showDialog(parent, approveText);

		if(returnVal == APPROVE_OPTION) {
			return getSelectedFile().getPath();
		} else {
			return null;
		}
	}

	public String[] pickDirs() {
		int returnVal  = showDialog(parent, approveText);

		if(returnVal == APPROVE_OPTION) {

			File[] Dirs  = getSelectedFiles();
			return getArrayofStrings(Dirs);
		} else {
			return null;
		}
	}

	private String[] getArrayofStrings(File[] Dirs) {
		String[] S_Dirs  = new String[Dirs.length];
		for(int i = 0; i < Dirs.length; i++) {
			S_Dirs[i] = Dirs[i].getPath();
		}

		return S_Dirs;
	}

}
