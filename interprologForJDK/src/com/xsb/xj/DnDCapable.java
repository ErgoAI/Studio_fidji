package com.xsb.xj;
import java.awt.dnd.DragGestureListener;

import javax.swing.JComponent;

/** An XJComponent that can behave as a drag and drop source and/or as a DnD target. 
To implement just the target part it suffices to define the methods as follows:

public DragGestureListener createDragGestureListener(){return null;}
public JComponent getRealJComponent(){return this;}
*/
public interface DnDCapable{
	/** Returns null if it cannot be a drag source. The DragGestureListener has the responsibility
	to initiate the dragging if it sees fit. */
	public DragGestureListener createDragGestureListener();
	/* Returns true if the drop succeeds, false if it fails for any reason */
	// No longer needed, folded into makeGUI: public boolean handleDrop(TransferableXJSelection data,XJAction da);
	/** The following allows an XJComponent to have a subcomponent handle DnD, as in lists and trees.
	Simpler XJComponents should simply return themselves */
	public JComponent getRealJComponent();
}
