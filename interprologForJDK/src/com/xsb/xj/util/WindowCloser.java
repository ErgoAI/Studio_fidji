/*
 * InternalFrameCloser.java
 *
 * Created on March 27, 2002, 9:34 AM
 */

package com.xsb.xj.util;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
/**
 *
 * @author  tanya
 * @version 
 */
public class WindowCloser extends WindowAdapter {

    Object closeGoal;
    PrologEngine engine;
    
    /** Creates new WindowCloser */
    public WindowCloser(PrologEngine engine, Object closeGoal) {
        super();
        this.closeGoal = closeGoal;
        this.engine = engine;
    }
    
    public void windowClosing(WindowEvent e){
        if((this.closeGoal != null) && (this.engine != null)){
            if (!engine.isAvailable()) {
			System.out.println("PrologEngine is busy; make sure your last top goal has ended.");
		} else {
                    Thread thread = new Thread("XJ Window closer"){
                        public void run(){
                            if(closeGoal instanceof String){
                                boolean bindings = engine.deterministicGoal((String)closeGoal);
                                if(bindings){
                                    System.exit(0);
                                }
                            } else if(closeGoal instanceof TermModel){
                                String extraVar = "MustBeUnique_XJ";
                                String extraVar2 = extraVar+"2";
                                String realGoal = "recoverTermModel("+extraVar+","+extraVar2+"),"+extraVar2;
                                Object[] realObjects=new Object[]{closeGoal};
                                String realVariables="["+ extraVar +"]";
            
                                boolean bindings = engine.deterministicGoal(realGoal, realVariables,realObjects);
                                if(bindings){
                                    System.exit(0);
                                }
                            } else throw new IllegalArgumentException("Window listeners can be only of type String or TermModel");
                        }
                    };
                    thread.start();
		}
        } else {
            System.exit(0);
        }
    }
     
}
