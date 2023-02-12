package com.xsb.xj.util;
import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class WindowSaver extends WindowAdapter {

    TermModel goal;
    PrologEngine engine;
    
    public WindowSaver(PrologEngine engine, TermModel goal, Window window) {
        this.goal = goal;
        this.engine = engine;
        window.addWindowListener(this);
    }
    
    public void windowClosing(WindowEvent e){
    	if (!engine.isAvailable()){
    		JOptionPane.showMessageDialog(e.getWindow(),"Can not close window. Please finish the ongoing computation first.","Error",
    			JOptionPane.ERROR_MESSAGE);	
    		return;
    	}
    	if (engine.deterministicGoal("recoverTermModel(GM,G), call(G)","[GM]",new Object[]{goal})) 
    		e.getWindow().dispose();
    	else return;
    } 
}
