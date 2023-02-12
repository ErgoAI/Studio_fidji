package com.xsb.xj;
import com.declarativa.interprolog.*;
/** A GUI object that can be specified from a GUITerm tree. 
Objects of some class C implementing this interface must obbey the following conditions: 
- C instanceof JComponent
- C must have a constructor C(PrologEngine,GUITerm) 
After construction XJ will send a refreshGUI() message to this component, so the constructor should not invoke it.
This will be preceded by a setDefaultValue(TermModel) message if a new term is being edited. 
*/
public interface XJComponent{
	/** The PrologEngine used for operations and persistence etc. */
	public PrologEngine getEngine();
	/** The GUITerm which, or whose root, this XJComponent renders */
	public GUITerm getGT();
	/** Sets the GUITerm for this component; should be called only by XJ itself */
	public void setGT(GUITerm gt);
	/** Refresh the GUI with the data in my GUITerm; this method has the responsability to invoke repaint(), 
	and to clear the dirty flag */
	public void refreshGUI();
	/** Load the GUITerm with latest data edited by the user; typically this will be messaged only for nodes, not XJComponentTrees. 
	Can ignore the request if !isDirty(), and should clear the dirty flag if it processes the request*/
	public boolean loadFromGUI();
	/** The XJComponent has one node changed by the user that is currently different from its gt (GUITerm) node value, typically because
	the user edited it. GUITerm intrinsic changes do not reflect into this state variable*/
	public boolean isDirty();
	/** Sets the GUITerm node to a default value. This is invoked when a "new term" is being created. 
	Implementations should interpret d as they see fit,
	 which may be null if no 'default=d' property was defined.
	Value setting should occur quietly, namely without firing undo events as we 
	don't want this "editing" to be deemed relevant */
	public void setDefaultValue(TermModel d);
	
	/** XJComponents unable to have more than one selection should throw an exception if selection.length>1 */
	public void selectGUI(Object[] selectionParts);
        
    /** This allows XJComponents to clean up after themselves - interprolog registry, cache notifiers, etc.*/
    public void destroy();
}