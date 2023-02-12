/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2013
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
** Based on Fifesoft's rsyntaxtextarea editor example
*/

package com.declarativa.fiji;
public interface EditorLifecycleListener{
	void didCreate(LogicProgramEditor editor);
	void willDestroy(LogicProgramEditor editor);
}