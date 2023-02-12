/* 
** Author(s): Miguel Calejo
** Contact:   miguel@calejo.com, www.calejo.com
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.fiji;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.LanguageAwareCompletionProvider;
import org.fife.ui.autocomplete.VariableCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;

import com.declarativa.interprolog.AbstractPrologEngine;

/** This requires making LanguageAwareCompletionProvider.getProviderFor() protected */
public class LogicCompletionProvider extends LanguageAwareCompletionProvider{
	private AbstractPrologEngine engine;
	protected LogicVariableCompletionProvider variableProvider;
	private PredicateCompletionProvider predicateProvider;
	
	protected LogicCompletionProvider(AbstractPrologEngine engine){
		this.engine = engine;
		DefaultCompletionProvider test = new DefaultCompletionProvider();
		setDefaultCompletionProvider(test);
		setDocCommentCompletionProvider(null) ;
		setCommentCompletionProvider(null);
		setStringCompletionProvider(null);
		variableProvider = new LogicVariableCompletionProvider();
		predicateProvider = new PredicateCompletionProvider();
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
				return predicateProvider;
			default:
				return null; // In a token type we can't auto-complete from.
		}

	}
	
	class TermCompletionProvider extends DefaultCompletionProvider{
		public java.util.List<Completion> getCompletionsAt(JTextComponent tc, Point p) {
			System.err.println("getCompletionsAt???");
			return new ArrayList<Completion>();
		}
		public String getAlreadyEnteredText(JTextComponent comp) {
			RSyntaxTextArea rsta = (RSyntaxTextArea)comp;
			int end = rsta.getCaretPosition();
			Token T = LogicProgramEditor.tokenEndingAtCaret(rsta);
			// System.out.println("T:"+T);
			if (T!=null && T.isPaintable()) {
				try{
					return rsta.getText(T.getOffset(),end-T.getOffset());
				} catch (BadLocationException ex){
					System.err.println(ex);
				}
			}
			return "";
		}
		
	}

	class LogicVariableCompletionProvider extends TermCompletionProvider{
		/**
		 * Does the dirty work of creating a list of completions.
		 *
		 * @param comp The text component to look in.
		 * @return The list of possible completions, or an empty list if there
		 *         are none.
		 */
		public java.util.List<Completion> getCompletions(javax.swing.text.JTextComponent comp){
			RSyntaxTextArea rsta = (RSyntaxTextArea)comp;
			clear();
			Token current = LogicProgramEditor.tokenEndingAtCaret(rsta);
			//System.out.println("current:"+current);
			if (current!=null && current.getType()==Token.VARIABLE) {
				final char[] thisvar = current.getLexeme().toCharArray();
				final HashSet<String> vars = new HashSet<String>();
				LogicProgramEditor.visitVariablesOfClauseWith((RSyntaxDocument)rsta.getDocument(),current,new LogicProgramEditor.TokenVisitor(){
					public void doSomething(Token T){
						if (!(T.is(Token.VARIABLE,thisvar))){ // consider only vars different from the current (incomplete...) one
							String varname = T.getLexeme(); 
							if (!(varname.equals("_")))
								vars.add(varname);
						}
					}
				});
				java.util.List<Completion> vcs = new ArrayList<Completion>();
				for (String v : vars)
					vcs.add(new VariableCompletion(this,v,"No type!"));
				addCompletions(vcs);
			}
			return super.getCompletions(comp);
		}
	}
	
	class PredicateCompletionProvider extends TermCompletionProvider{
		public java.util.List<Completion> getCompletions(javax.swing.text.JTextComponent comp){
			RSyntaxTextArea rsta = (RSyntaxTextArea)comp;
			clear();
			Token current = LogicProgramEditor.tokenEndingAtCaret(rsta);
			//System.out.println("current:"+current);
			if (current!=null && current.getType()==Token.IDENTIFIER) {
				final char[] partialPredicate = current.getLexeme().toCharArray();
				java.util.List<Completion> vcs = new ArrayList<Completion>();
				Object[] bindings = engine.deterministicGoal("fjPredicateCompletions("+ new String(partialPredicate) +",Templates), stringArraytoList(SM,Templates)","[SM]");
				if (bindings!=null){
					String[] signatures = (String[]) bindings[0];
					for (String S : signatures)
						vcs.add(new BasicCompletion(this,S));
				}
				addCompletions(vcs);
			}
			return super.getCompletions(comp);
		}
	}
}