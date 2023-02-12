package com.xsb.xj.util;
import java.io.*;
@SuppressWarnings("serial")
public class XJException extends RuntimeException{
	Exception originalException;
	public XJException(String d,Exception e){
		super(d);
		originalException = e;
		//System.err.println("XJException about to be thrown:"+this);
	}
	public XJException(String d){
		this(d,null);
	}
	public void printStackTrace(PrintStream s){
		if (originalException==null) super.printStackTrace(s);
		else originalException.printStackTrace(s);
	}
	public void printStackTrace(PrintWriter s){
		if (originalException==null) super.printStackTrace(s);
		else originalException.printStackTrace(s);
	}
	public String toString(){
		if (originalException==null) return super.toString();
		else return super.toString()+" original:"+originalException.toString();
	}
}
