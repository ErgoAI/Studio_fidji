package com.coherentknowledge.fidji;

import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;

import com.declarativa.fiji.LogicCompletionProvider;
import com.declarativa.fiji.LogicProgramEditor;
import com.declarativa.interprolog.AbstractPrologEngine;

public class FloraCompletionProvider extends LogicCompletionProvider {

	FloraCompletionProvider(AbstractPrologEngine engine) {
		super(engine);
	}

	/** Sligthly tweaked version of the superclass's */
	protected CompletionProvider getProviderFor(JTextComponent comp) {

		RSyntaxTextArea rsta = (RSyntaxTextArea)comp;
		RSyntaxDocument doc = (RSyntaxDocument)rsta.getDocument();

		Token curToken = LogicProgramEditor.tokenEndingAtCaret(rsta);
		//System.out.println("curToken in getProviderFor:"+curToken);
		if (curToken==null) return null;
		
		int type = curToken.getType();
		if (type<0)
			type = doc.getClosestStandardTokenTypeForInternalType(type);
				
		switch (type) {
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
			case Token.ERROR_STRING_DOUBLE:
			case Token.LITERAL_CHAR:
				return getStringCompletionProvider();
			case Token.COMMENT_EOL:
			case Token.COMMENT_MULTILINE:
				return getCommentCompletionProvider();
			case Token.COMMENT_DOCUMENTATION:
				return getDocCommentCompletionProvider();
			case Token.VARIABLE:
				return variableProvider;
			case Token.NULL:
			case Token.WHITESPACE:
			case Token.IDENTIFIER:
				// return predicateProvider; Prolog only
			default:
				return null; // In a token type we can't auto-complete from.
		}

	}
	
}
