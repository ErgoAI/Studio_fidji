package com.xsb.xj.util;

import java.util.HashSet;

import javax.swing.Action;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.declarativa.interprolog.EngineController;
import com.declarativa.interprolog.SubprocessEngine;

public class XJEngineController extends EngineController {
	HashSet<HintTextAreaUI> hinters = new HashSet<HintTextAreaUI>();
	public XJEngineController(SubprocessEngine pausableEngine, boolean autoHints) {
            super(pausableEngine,autoHints);
	}
    /** Declare a field to have hints injected as the logic engine changes state
     * Assumes that initially engine is idle; next state change will correct it */ 
    public void hintWhenIdleOrPaused(JTextArea field){
        // always show the prompt in background (false);
        // true erases the prompt on forcus, which is not that intuitive
        HintTextAreaUI hinter = new HintTextAreaUI(idleOrPausedHint, false);
        field.setUI(hinter);
        hinters.add(hinter);
        fields.add(field);
        originalColors.put(field, field.getBackground());
        darkerColors.put(field, makeBusyColorFrom(field.getBackground()));
    }
    /** change the GUI hint and color to show that the engine is busy; this will be called automatically if autoHints==true */
    public void setUItoBusy(){
    	SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
		        engineStateAction.putValue(Action.NAME, busyLabel);
		        for (HintTextAreaUI hinter:hinters){
		            hinter.setHint(busyHint);
		        }
		        for (JTextArea field:fields)
		            if (busyColor!=null) field.setBackground(busyColor);
		            else field.setBackground(darkerColors.get(field));
			}
    	});
    }
    /** change the GUI hint and color to show that the engine is awaiting user input; this will be called automatically if autoHints==true */
    public void setUItoPausedOrIdle(){
    	SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
		        engineStateAction.putValue(Action.NAME, idleOrPausedLabel);
		        for (HintTextAreaUI hinter:hinters)
		            hinter.setHint(idleOrPausedHint);
		        for (JTextArea field:fields)
		            if (idleOrPausedColor!=null) field.setBackground(idleOrPausedColor);
		            else field.setBackground(originalColors.get(field));
			}
    	});
    }
    public void setUItoNeedsMoreInput() {
    	SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
		        engineStateAction.putValue(Action.NAME, needsMoreInputLabel);
		        for (HintTextAreaUI hinter:hinters)
		            hinter.setHint(needsMoreInputHint);
		        for (JTextArea field:fields)
		            if (needsMoreInputColor!=null) field.setBackground(needsMoreInputColor);
		            else field.setBackground(originalColors.get(field));		
			}
    	});
    }
}
