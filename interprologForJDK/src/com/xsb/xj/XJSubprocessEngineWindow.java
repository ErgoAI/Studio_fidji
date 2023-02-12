/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2000-2012
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.xsb.xj;
import com.declarativa.interprolog.XSBSubprocessEngine;
import com.declarativa.interprolog.gui.ListenerWindow;
import com.declarativa.interprolog.gui.XSBSubprocessEngineWindow;

@SuppressWarnings("serial")
public class XJSubprocessEngineWindow extends XSBSubprocessEngineWindow{
	public XJSubprocessEngineWindow(XSBSubprocessEngine e){
		this(e,true,false);
	}
	public XJSubprocessEngineWindow(XSBSubprocessEngine e,boolean autoDisplay){
		this(e,true,false);
	}
	public XJSubprocessEngineWindow(XSBSubprocessEngine e,boolean autoDisplay, boolean mayExitApp){
		super(e,autoDisplay,mayExitApp);
		XJPrologEngine.initPrologLayer(e);
                // MK: the below is bogus crap - commented out
		//e.consultAbsolute(new java.io.File("/Users/mc/Dropbox/declarativa/projectos/SILK/DeclarativeDebugging/debugging.P"));
	}	
	/** Useful for launching the system, by passing the full Prolog executable path and 
	optionally extra arguments, that are passed to the Prolog command */
	public static void main(String[] args){
		commonMain(args);
		new XJSubprocessEngineWindow(new XSBSubprocessEngine(prologStartCommands,debug,loadFromJar));
	}

	/** Try it with ipPrologEngine(E), javaMessage('com.xsb.xj.XJSubprocessEngineWindow',init(E)). 
	Silk: 
	?-silk:flora(" ipPrologEngine(?E)@_prologall, javaMessage('com.xsb.xj.XJSubprocessEngineWindow',init(?E))@_prologall ");
	*/
	public static void init(XSBSubprocessEngine engine){
		String VF = engine.getImplementationPeer().visualizationFilename();
		if (engine.getLoadFromJar()) engine.consultFromPackage(VF,ListenerWindow.class,true);
		else engine.consultRelative(VF,ListenerWindow.class);
		engine.teachMoreObjects(ListenerWindow.guiExamples());
		new XJSubprocessEngineWindow(engine);
	}
}
