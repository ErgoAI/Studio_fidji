package com.xsb.xj.util;

import com.declarativa.interprolog.PrologEngine;

/**
 *  Description of the Class
 *
 *@author     HSingh
 *@created    October 28, 2002
 */
public class InternalFrameCloser implements javax.swing.event.InternalFrameListener {

	String closeGoal;
	PrologEngine engine;


	/**
	 *  Creates new InternalFrameCloser
	 *
	 *@param  engine     Interprolog Engine
	 *@param  closeGoal  Prolog called when event occurs.
	 */
	public InternalFrameCloser(PrologEngine engine, String closeGoal) {
		super();
		this.closeGoal = closeGoal;
		this.engine = engine;
	}

	
	public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
		if((this.closeGoal != null) && (this.engine != null)) {
			if(!engine.isAvailable()) {
				System.out.println("PrologEngine is busy; make sure your last top goal has ended.");
			} else {
				Thread thread =
					new Thread("XJ InternalFrame closer") {
						public void run() {
							boolean bindings = engine.deterministicGoal(closeGoal);
							if(bindings) {
								//System.exit(0);
							}
						}
					};
				thread.start();
			}
		} else {
			//System.exit(0);
		}
	}
	
	
	public void internalFrameOpened(javax.swing.event.InternalFrameEvent e) {
		;
	}


	public void internalFrameActivated(javax.swing.event.InternalFrameEvent e) {
		;
	}


	public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
		;
	}


	public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent e) {
		;
	}


	public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent e) {
		;
	}


	public void internalFrameIconified(javax.swing.event.InternalFrameEvent e) {
		;
	}

}

