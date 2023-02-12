/* File:   ErgoStudioPreferencesDialog.java
**
** Author(s): Miguel Calejo
**
** Contact:   mc@interprolog.com
**
** Copyright (C) Coherent Knowledge Systems, LLC, 2013 - 2016.
** All rights reserved.
**
*/

package com.coherentknowledge.fidji;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.declarativa.fiji.FijiSubprocessEngineWindow;
import com.declarativa.fiji.PreferencesDialog;

@SuppressWarnings("serial")
public class ErgoStudioPreferencesDialog extends PreferencesDialog {
	public ErgoStudioPreferencesDialog(FijiSubprocessEngineWindow listener){
		super(listener);
		setTitle("Ergo Studio Preferences");
	}
	protected void addParseableSize(JPanel box) {
		String psTip = "The Ergo editor will open larger files, but will not provide as-you-type warnings and errors";
		JLabel parseableSizeLabel = new JLabel("File size limit for real-time syntax checks in the editor ");
		JLabel parseableSizeUnit = new JLabel(" characters");
		parseableSizeLabel.setToolTipText(psTip);
		final JFormattedTextField parseableSize = new JFormattedTextField(new Integer(getInt(FloraProgramEditor.MAX_PARSEABLE_SIZE_ERGO_PREF)));
		parseableSize.setToolTipText(psTip);
		parseableSize.setHorizontalAlignment(SwingConstants.RIGHT);
		parseableSize.addPropertyChangeListener("value",new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String V = parseableSize.getValue()+"";
				userPreferences.put(FloraProgramEditor.MAX_PARSEABLE_SIZE_ERGO_PREF, V);
			}
		});
		JPanel psp = new JPanel();
		psp.setLayout(new BoxLayout(psp, BoxLayout.X_AXIS));
		psp.add(parseableSizeLabel); psp.add(parseableSize);
		psp.add(parseableSizeUnit);
		psp.setAlignmentX(LEFT_ALIGNMENT);
		box.add(psp);
	}
	@Override
	protected void addVerbosity(JPanel box) {
		final JCheckBox verbosityCB = new JCheckBox("Verbose Ergo listener",getBoolean(FijiSubprocessEngineWindow.VERBOSE_LISTENER));
		verbosityCB.setToolTipText("Tells the listener to show all Ergo output");
		verbosityCB.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				userPreferences.put(FijiSubprocessEngineWindow.VERBOSE_LISTENER, verbosityCB.isSelected()+"");
			}
		});
		verbosityCB.setAlignmentX(LEFT_ALIGNMENT);
		box.add(verbosityCB);
		box.add(new JLabel(" ",JLabel.CENTER));
	}
	@Override
	protected void overrideUserPrefsFrom(Properties target, Properties source) {
		super.overrideUserPrefsFrom(target, source);
		assignPref(FloraProgramEditor.MAX_PARSEABLE_SIZE_ERGO_PREF,target,source,FloraProgramEditor.defaultParseableSize()+"");
	}
	
}
