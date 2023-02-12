package com.xsb.xj.util;
import com.xsb.xj.*;
public class GUIError {
	String programmerMessage;
	GUITerm term;
	public GUIError(Object message,GUITerm rejector){
		this(message.toString(),rejector);
	}
	public GUIError(String programmerMessage,GUITerm rejector){
		term=rejector;
		this.programmerMessage = programmerMessage;
	}
	public GUIError(String programmerMessage){
		this(programmerMessage,null);
	}
	
	public String toString(){
		if (term!=null && term.getUserTitle().length()>0) 
			return programmerMessage+" in "+term.getUserTitle();
		else return programmerMessage;
	}
}
