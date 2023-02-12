package com.xsb.xj.util;

import com.declarativa.interprolog.PrologEngine;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * java.awt.event.ComponentListener for XJ. Goals must be string/atomic.
 *
 *@author    Harpreet Singh
 *@version   $Id: XJComponentListener.java,v 1.2 2003/08/08 20:45:12 hsingh Exp $
 */
public class XJComponentListener implements ComponentListener {
	String resizedGoal;
	String movedGoal;
	String shownGoal;
	String hiddenGoal;

	PrologEngine engine;

	public XJComponentListener(PrologEngine engine) {
		super();
		this.engine = engine;
	}

	public void setResizeGoal(String resizeGoal) {
		if(resizeGoal == null) {
			return;
		}

		this.resizedGoal = resizeGoal;
	}

	public void setMoveGoal(String moveGoal) {
		if(moveGoal == null) {
			return;
		}
		this.movedGoal = moveGoal;
	}

	public void setShowGoal(String showGoal) {
		if(showGoal == null) {
			return;
		}
		this.shownGoal = showGoal;
	}

	public void setHideGoal(String hideGoal) {
		if(hideGoal == null) {
			return;
		}
		this.hiddenGoal = hideGoal;
	}

	public void componentResized(ComponentEvent e) {
		if(resizedGoal != null) {
			notifyProlog(resizedGoal);
		}
	}

	public void componentMoved(ComponentEvent e) {
		if(movedGoal != null) {
			notifyProlog(movedGoal);
		}
	}

	public void componentShown(ComponentEvent e) {
		if(shownGoal != null) {
			notifyProlog(shownGoal);
		}
	}

	public void componentHidden(ComponentEvent e) {
		if(hiddenGoal != null) {
			notifyProlog(hiddenGoal);
		}
	}

	private void notifyProlog(String goal) {
		if(engine == null) {
			System.err.println("XJComponentListener: PrologEngine is null.");
			return;
		}

		//needs to be final because its being accessed from inner class below
		final String prologGoal  = goal;

		if(!engine.isAvailable()) {
			System.err.println("XJComponentListener: PrologEngine is busy.");
		} else {
			Thread thread  =
				new Thread("XJ notifier") {
					public void run() {
						boolean bindings  = engine.deterministicGoal(prologGoal);
						if(bindings) {
							return;
						}
					}
				};
			thread.start();
			return;
		}
	}
}

