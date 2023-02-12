/* File:   FloraParser.java
**
** Author(s): Miguel Calejo
**
** Contact:   mc@interprolog.com
**
** Copyright (C) Coherent Knowledge Systems, LLC, 2014 - 2016.
** All rights reserved.
**
*/

package com.coherentknowledge.fidji;
import java.io.File;
import java.util.Iterator;

import org.fife.ui.rsyntaxtextarea.Token;

import com.declarativa.fiji.FijiPreferences;
import com.declarativa.fiji.LogicParser;
import com.declarativa.fiji.LogicProgramEditor;
import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.FloraSubprocessEngine;
import com.declarativa.interprolog.TermModel;
public class FloraParser extends LogicParser{
	FloraParser(AbstractPrologEngine engine,LogicProgramEditor lpe){
		super(engine,lpe);
	}
	protected TermModel[] getNoticeTerms(String filename){
		FijiPreferences.ImpatientBugReporter ibr = new FijiPreferences.ImpatientBugReporter(engine);
		Object[] bindings;

                String fileExt = "";
                int i = filename.lastIndexOf('.');
                if (i >= 0) {
                    fileExt = filename.substring(i);
                }

                // if .ergotxt file, call checkFloraFile with WithCompiler=fail
                if (fileExt.equals(FloraSubprocessEngine.ERGOTEXT_EXTENSION))
                    bindings = engine.deterministicGoal("checkFloraFile('"+ filename +"',fail,Status), buildTermModelArray(Status,SM)","[SM]");
                else
                    bindings = engine.deterministicGoal("checkFloraFile('"+ filename +"',true,Status), buildTermModelArray(Status,SM)","[SM]");

		ibr.dontWorry();
		if (bindings!=null) return (TermModel[])bindings[0];
		else return null;
	}
	
	protected boolean shouldReport(Token firstInNotice){
		//return firstInNotice.getType()!=Token.PREPROCESSOR;
		return true; // we're now pre processing before parsing
	}

	static final char[] DIRECTIVE_FUNCTOR = ":-".toCharArray();
	static final char[] ERGOTEXT = "ergotext".toCharArray();
	static final char[] LEFT_BRACKET = "{".toCharArray();
	static final char[] RIGHT_BRACKET = "}".toCharArray();
	static final char[] DOT = ".".toCharArray();
	
	protected Token checkForInclusions(Token T, Iterator<Token> it, File originalDir) {
		if (T.getType()==Token.RESERVED_WORD && T.is(DIRECTIVE_FUNCTOR)){
			int startPos = T.getOffset();
			if (!(it.hasNext()))
				return null;
			T = it.next();
			if (!(T.is(Token.IDENTIFIER, ERGOTEXT)))
				return T;

			if (!(it.hasNext()))
				return null;
			T = it.next();
			if (!(T.is(Token.SEPARATOR, LEFT_BRACKET)))
				return T;

			if (!(it.hasNext()))
				return null;
			T = it.next();
			if (T.getType()!=Token.IDENTIFIER)
				return T;
			
			String included = T.getLexeme();
			if (included.startsWith("\'") && included.endsWith("\'") && included.length()>=2)
				included = included.substring(1,included.length()-1);

			if (!(it.hasNext()))
				return null;
			T = it.next();
			if (!(T.is(Token.SEPARATOR, RIGHT_BRACKET)))
				return T;

			if (!(it.hasNext()))
				return null;
			T = it.next();
			if (!(T.is(Token.OPERATOR, DOT)))
				return T;
			
			if (!(FloraSubprocessEngine.isErgotextSourceFile(included)))
				included = included + FloraSubprocessEngine.ERGOTEXT_EXTENSION;

			File I = new File(included);
			File original = I.isAbsolute() ? I : new File(originalDir,included);
			if (shouldCopyIncluded() && original.exists())
				includes.put(included,new Inclusion(startPos,included, I.isAbsolute(), original, new File(tempDirectory,included)));
			
			
			return T;
		} else 
			return super.checkForInclusions(T, it, originalDir) ;
	}

}
