package com.declarativa.fiji;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.declarativa.interprolog.AbstractPrologEngine;

@SuppressWarnings("serial")
/** A modal dialog to edit some preferences in listener.preferences */
public class PreferencesDialog extends JDialog {
	/** preferences being edited here */
	protected Properties userPreferences;
	
	public static PreferencesDialog makePreferencesDialog(FijiSubprocessEngineWindow owner){
		if (FijiPreferences.floraSupported){
			try{
				Constructor<?> C = AbstractPrologEngine.findConstructor(FijiPreferences.otherPreferencesDialogClass,new Class[]{FijiSubprocessEngineWindow.class});
			return (PreferencesDialog)C.newInstance(owner);
			} catch (Exception e){
				e.printStackTrace(System.err);
				throw new RuntimeException("Trouble making preferences:"+e);
			}
		} else return new PreferencesDialog(owner);
		
	}
	
	protected PreferencesDialog(final FijiSubprocessEngineWindow owner){
		super(owner,"Preferences",true);
		userPreferences = new Properties();
		setLayout(new BorderLayout());
		//Box box = new Box(BoxLayout.Y_AXIS);
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		overrideUserPrefsFrom(userPreferences,owner.preferences);
		
		addParseableSize(box);
		
		final JLabel fontSample = new JLabel(" Sample");
		fontSample.setFont(owner.prologInput.getFont().deriveFont((float)getInt(FijiSubprocessEngineWindow.FONT_SIZE_PREF)));
		SpinnerNumberModel fontSizes = new SpinnerNumberModel(getInt(FijiSubprocessEngineWindow.FONT_SIZE_PREF), 7, 44, 1);
		final JSpinner fontSpinner = new JSpinner(fontSizes);
		fontSpinner.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				userPreferences.put(FijiSubprocessEngineWindow.FONT_SIZE_PREF, fontSpinner.getValue()+"");
				fontSample.setFont(fontSample.getFont().deriveFont((float)getInt(FijiSubprocessEngineWindow.FONT_SIZE_PREF)));
			}
		});
		fontSpinner.setSize(50, 18);
		fontSpinner.setToolTipText("Set font size for editor, listener, query window, justifications etc.");
		JLabel fontSizeLabel = new JLabel("Font size ");
		JPanel fsp = new JPanel();
		fsp.setLayout(new BoxLayout(fsp, BoxLayout.X_AXIS));
		fsp.add(fontSizeLabel); fsp.add(fontSpinner); fsp.add(fontSample); fsp.add(Box.createGlue());
		fsp.setAlignmentX(LEFT_ALIGNMENT);
		box.add(new JLabel(" ",JLabel.CENTER));
		box.add(fsp);
		box.add(new JLabel(" ",JLabel.CENTER));
		
		final JButton inputBackgroundB = new JButton("Input Background Color...");
		inputBackgroundB.setOpaque(true);
		inputBackgroundB.setToolTipText("Set color background for listener bottom (input) field, query panel input");
		inputBackgroundB.setBackground(getColor(FijiSubprocessEngineWindow.INPUT_BACKGROUND_PREF));
		inputBackgroundB.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				Color picked = JColorChooser.showDialog(PreferencesDialog.this, "Input Field Background", getColor(FijiSubprocessEngineWindow.INPUT_BACKGROUND_PREF));
				if (picked != null){
					userPreferences.setProperty(FijiSubprocessEngineWindow.INPUT_BACKGROUND_PREF, picked.getRGB()+"");
					inputBackgroundB.setBackground(picked);
				}
				
			}
		});
		inputBackgroundB.setAlignmentX(LEFT_ALIGNMENT);
		box.add(inputBackgroundB);
		box.add(new JLabel(" ",JLabel.CENTER));
		
		addVerbosity(box);
		
		final JCheckBox reopenWindowsCB = new JCheckBox("Reopen last windows configuration on startup",getBoolean(FijiSubprocessEngineWindow.REOPEN_WINDOWS_SET_PREF));
		reopenWindowsCB.setToolTipText("This saves the last window configuration on exit and reopens it on startup");
		reopenWindowsCB.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				userPreferences.put(FijiSubprocessEngineWindow.REOPEN_WINDOWS_SET_PREF, reopenWindowsCB.isSelected()+"");
			}
		});
		reopenWindowsCB.setAlignmentX(LEFT_ALIGNMENT);
		box.add(reopenWindowsCB);
		box.add(new JLabel(" ",JLabel.CENTER));
		
		JLabel warning = new JLabel("Changes will take effect only after a restart",JLabel.CENTER);
		//warning.setEnabled(false);
		warning.setForeground(Color.RED);
		box.add(warning);
		box.add(new JLabel(" ",JLabel.CENTER));
		box.add(new JLabel(" ",JLabel.CENTER));
		box.add(new JLabel(" ",JLabel.CENTER));
		
		box.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		add(box,BorderLayout.CENTER);
		
		Box south = new Box(BoxLayout.X_AXIS);
		
		south.add(Box.createHorizontalGlue());
		
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		south.add(cancel);
		
		JButton closer = new JButton("OK");
		closer.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				overrideUserPrefsFrom(owner.preferences,userPreferences);
			}
		});
		south.add(closer);
		
		add(south,BorderLayout.SOUTH);
		add(Box.createHorizontalStrut(20),BorderLayout.EAST);
		pack();
	}

	/** This implementation does nothing
	 * @param box container for the gadgets
	 */
	protected void addVerbosity(JPanel box) {
	}
	
	/** This implementation does nothing
	 * @param box container for the gadgets
	 */
	protected void addParseableSize(JPanel box) {
	}
	
	protected Color getColor(String prefName) {
		return new Color(Integer.parseInt(userPreferences.getProperty(prefName)));
	}

	protected boolean getBoolean(String prefName) {
		return userPreferences.getProperty(prefName).equals("true");
	}

	protected int getInt(String prefName) {
		return Integer.parseInt(userPreferences.getProperty(prefName));
	}

	// All prefs here MUST be preset in the owner preferences before creating PreferencesDialog
	protected void overrideUserPrefsFrom(Properties target,Properties source) {
		assignPref(FijiSubprocessEngineWindow.FONT_SIZE_PREF, target, source);
		assignPref(FijiSubprocessEngineWindow.INPUT_BACKGROUND_PREF, target, source);
		assignPref(FijiSubprocessEngineWindow.VERBOSE_LISTENER, target, source);
		assignPref(FijiSubprocessEngineWindow.REOPEN_WINDOWS_SET_PREF,target,source);
	}
	static protected void assignPref(String prefName,Properties target,Properties source){
		assignPref(prefName, target, source, null);
	}
	static protected void assignPref(String prefName,Properties target,Properties source, String defaultValue){
		Object value = source.get(prefName);
		if (value == null) {
			if (defaultValue!=null)
				target.put(prefName, defaultValue);
			else {
				System.err.println("Missing property "+prefName+" in "+source);
				target.remove(prefName);
			}
		}
		else target.put(prefName, value);
	}

}
