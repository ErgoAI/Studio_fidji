package com.xsb.xj;
import com.declarativa.interprolog.TermModel;
/** A XJComponent able to visualize a set of terms, each somehow compatible with a template 
*/
public interface XJTemplateComponent extends XJComponent{
	/** Returns the component template term */ 
	public TermModel getTemplate();
	/** Sent by XJ after all other components are generated from the term 
	enclosing the subterm visualized by this component. */
	public void constructionEnded();
}