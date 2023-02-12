package com.xsb.xj.util;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJComponent;

import java.awt.Dimension;

import javax.swing.JProgressBar;


/**
 * XJ class for javax.swing.JProgressBar. The properties that can be supplied
 * are as follows:
 * <ul>
 *   <li> <code>PROP_MIN</code>: the progress bar's minimum value, which is
 *   stored in the progress bar's BoundedRangeModel. By default, the minimum
 *   value is 0. The value of his property must be of type integer.</li>
 *   <li> <code>PROP_MAX</code>: the progress bar's maximum value, which is
 *   stored in the progress bar's BoundedRangeModel. By default, the maximum
 *   value is 100. The value of his property must be of type integer.</li>
 *   <li> <code>PROP_PAINT_STRING</code>: Sets the value of the stringPainted
 *   property, which determines whether the progress bar should render a
 *   progress string. The default is false: no string is painted. Some look and
 *   feels might not support progress strings or might support them only when
 *   the progress bar is in determinate mode.</li>
 *   <li> <code>PROP_VERTICAL</code>: Sets the progress bar's orientation to
 *   vertical. The default orientation is horizontal.</li>
 * </ul>
 * <p>
 *
 * Example GT Code:<br>
 * <code>gt(_, [class='com.xsb.xj.XJProgressBar', min=0, max=10, paintString, myGUI(Bar)], [])</code>
 * </p>
 *
 *@author    Harpreet Singh
 *@version   $Id: XJProgressBar.java,v 1.3 2004/07/08 19:43:23 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJProgressBar extends JProgressBar implements XJComponent {

    /**
     */
    public static final String PROP_MIN           = "min";
    /**
     */
    public static final String PROP_MAX           = "max";
    /**
     */
    public static final String PROP_PAINT_STRING  = "paintString";
    /**
     */
    public static final String PROP_VERTICAL      = "vertical";

    GUITerm gt;
    PrologEngine engine;

    public XJProgressBar(PrologEngine engine, GUITerm gt) {
        super();

        this.gt = gt;
        this.engine = engine;

        TermModel minimum      = gt.findProperty(PROP_MIN);
        if(minimum != null) {
            setMinimum(minimum.intValue());
        }

        TermModel maximum      = gt.findProperty(PROP_MAX);
        if(maximum != null) {
            setMaximum(maximum.intValue());
        }

        TermModel paintString  = gt.findProperty(PROP_PAINT_STRING);
        if(paintString != null) {
            setStringPainted(true);
        }

        TermModel vertical     = gt.findProperty(PROP_VERTICAL);
        if(vertical != null) {
            setOrientation(JProgressBar.VERTICAL);
        }
    }

    public void incValue() {
        if(getValue() < getMaximum()) {
            setValue(getValue() + 1);
        }
    }
    public void setValue(int value) {
        super.setValue(value);
        repaint();
    }

    public Dimension getPreferredSize() {
        return gt.getPreferredSize(super.getPreferredSize());
    }

    public PrologEngine getEngine() {
        return engine;
    }

    public GUITerm getGT() {
        return gt;
    }
	public void setGT(GUITerm gt){
		this.gt=gt;
	}

    public void refreshGUI() { }

    /**
     * This implementation does nothing
     *
     *@param dv  The new defaultValue value
     */
    public void setDefaultValue(TermModel dv) { }

    /**
     * This implementation does nothing
     *
     *@return   Description of the Return Value
     */
    public boolean loadFromGUI() {
        return true;
    }

    /**
     * This implementation returns false
     *
     *@return   The dirty value
     */
    public boolean isDirty() {
        return false;
    }

    public void selectGUI(Object[] parts) {
        GUITerm.typicalAtomicSelect(this, parts);
    }
    
    public void destroy() {
    }
    
}
