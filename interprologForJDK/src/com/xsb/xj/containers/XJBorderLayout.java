package com.xsb.xj.containers;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JPanel;


/**
 * Displays values of a term in border layout
 *
 *@version   $Id: XJBorderLayout.java,v 1.11 2004/07/08 19:43:22 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJBorderLayout extends JPanel implements XJComponent, XJComponentTree {
    GUITerm gt;
    PrologEngine engine;
    private boolean dirty;

    private HashMap<String,XJComponent> layoutComponents  = new HashMap<String,XJComponent>(5);

    public XJBorderLayout(PrologEngine engine, GUITerm gt) {
        this.gt = gt;
        this.engine = engine;
        dirty = false;

        BorderLayout b  = new BorderLayout();
        setLayout(b);

        //horizontal gap
        TermModel hgap  = gt.findProperty("hgap");
        if(hgap != null) {
            int h  = ((Integer) hgap.node).intValue();
            b.setHgap(h);
        }

        //vertical gap
        TermModel vgap  = gt.findProperty("vgap");
        if(vgap != null) {
            int v  = ((Integer) vgap.node).intValue();
            b.setVgap(v);
        }

        for(int c = 0; c < gt.getChildCount(); c++) {

            GUITerm child  = (GUITerm) gt.children[c];

            if(!child.isInvisible()) {

                XJComponent line    = child.makeGUI(engine);
                TermModel bounds    = child.findProperty("bounds");
                TermModel location  = child.findProperty("layout");

                if(bounds != null) {
                    int childcount  = bounds.getChildCount();
                    if(childcount == 4) {
                        TermModel x1  = (TermModel) bounds.getChild(0);
                        TermModel y1  = (TermModel) bounds.getChild(1);
                        TermModel x2  = (TermModel) bounds.getChild(2);
                        TermModel y2  = (TermModel) bounds.getChild(3);
                        ((JComponent) line).setPreferredSize(
                            new Dimension((x2.intValue() - x1.intValue()),
                            (y2.intValue() - y1.intValue())));
                    }
                }

                if(location != null) {
                    String layout  = (String) location.node;
                    if(layout.equals("north")) {
                        add((JComponent) line, BorderLayout.NORTH);
                        layoutComponents.put(BorderLayout.NORTH, line);
                    } else if(layout.equals("south")) {
                        add((JComponent) line, BorderLayout.SOUTH);
                        layoutComponents.put(BorderLayout.SOUTH, line);
                    } else if(layout.equals("east")) {
                        add((JComponent) line, BorderLayout.EAST);
                        layoutComponents.put(BorderLayout.EAST, line);
                    } else if(layout.equals("west")) {
                        add((JComponent) line, BorderLayout.WEST);
                        layoutComponents.put(BorderLayout.WEST, line);
                    } else {
                        add((JComponent) line, BorderLayout.CENTER);
                        layoutComponents.put(BorderLayout.CENTER, line);
                    }
                } else {
                    add((JComponent) line, BorderLayout.CENTER);
                    layoutComponents.put(BorderLayout.CENTER, line);
                }
            }
        }
    }

	public TermModel[] getMyGUIs(){
		return gt.getMyGUIs(); 
	}
	
    public void setDefaultValue(TermModel dv) { }

    public PrologEngine getEngine() {
        return engine;
    }

	public void setGT(GUITerm gt){
		this.gt=gt;
	}
    public GUITerm getGT() {
        return gt;
    }

    public void refreshGUI() { }

    public boolean loadFromGUI() {
        dirty = false;
        return true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void selectGUI(Object[] parts) {
        GUITerm.typicalContainerSelect(this, parts);
    }

    public void setNorthComponent(GUITerm gt) {
        updateComponent(gt, BorderLayout.NORTH);
    }

    public void setSouthComponent(GUITerm gt) {
        updateComponent(gt, BorderLayout.SOUTH);
    }

    public void setEastComponent(GUITerm gt) {
        updateComponent(gt, BorderLayout.EAST);
    }

    public void setWestComponent(GUITerm gt) {
        updateComponent(gt, BorderLayout.WEST);
    }

    public void setCenterComponent(GUITerm gt) {
        updateComponent(gt, BorderLayout.CENTER);
    }

    public void setNorthComponent(XJComponent north) {
        updateComponent(north, BorderLayout.NORTH);
    }

    public void setSouthComponent(XJComponent south) {
        updateComponent(south, BorderLayout.SOUTH);
    }

    public void setEastComponent(XJComponent east) {
        updateComponent(east, BorderLayout.EAST);
    }

    public void setWestComponent(XJComponent west) {
        updateComponent(west, BorderLayout.WEST);
    }

    public void setCenterComponent(XJComponent center) {
        updateComponent(center, BorderLayout.CENTER);
    }
    
    private void updateComponent(GUITerm newgt, String layoutLocation) {
        XJComponent newComponent = newgt.makeGUI(engine);
        updateComponent(newComponent, layoutLocation);
    }
    private void updateComponent(XJComponent newComponent, String layoutLocation) {
        GUITerm newgt = newComponent.getGT();
        XJComponent layoutComponent  = (XJComponent) layoutComponents.get(layoutLocation);

        if(layoutComponent != null) {
            GUITerm oldgt  = layoutComponent.getGT();

            remove((JComponent) layoutComponent);
            //replace old child with new child
            for(int i = 0; i < gt.getChildCount(); i++) {
                if(gt.children[i] == oldgt) {
                    gt.children[i] = newgt;
                    break;
                }
            }
        } else {
            //add new child ???
            gt.addChildren(new TermModel[]{newgt});
        }

        layoutComponent = newComponent;
        add((JComponent) layoutComponent, layoutLocation);
        layoutComponents.put(layoutLocation, layoutComponent);

        newgt.refreshRenderers();
        revalidate();
    }
    
    public void destroy() {
    }
    public Dimension getPreferredSize(){
    	Dimension D = super.getPreferredSize();
    	return gt.getPreferredSize(D);
    }
    
}
