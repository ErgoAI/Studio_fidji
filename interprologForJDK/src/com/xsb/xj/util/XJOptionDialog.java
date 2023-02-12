/*
 * XJOptionDialog.java
 *
 * Created on October 1, 2002, 8:54 AM
 */

package com.xsb.xj.util;

import java.awt.Component;
import java.awt.HeadlessException;
import javax.swing.*;

/**
 * <code>XJOptionDialog</code> displays a window with a question message and a set of 
 * buttons to click on to answer that message.
 * @author  tanya
 */
public class XJOptionDialog {
    protected String title;
    protected String message;
    protected String[] buttons;
    protected Component parentContainer;
    
    /** Creates a new instance of XJOptionDialog */
    public XJOptionDialog(String title, String message, String[] buttons) {
        this.title = title;
        this.message = message;
        this.buttons = buttons;
    }
    
    public XJOptionDialog(String title, String message, String[] buttons, Component parentContainer) {
        this.title = title;
        this.message = message;
        this.buttons = buttons;
        this.parentContainer = parentContainer;
    }

    public String show() throws HeadlessException{
        /*Object[] possibleValues = new Object[buttons.length];
        for (int i=0; i<buttons.length; i++){
            possibleValues[i] = buttons[i];
        }*/
            int choice = JOptionPane.showOptionDialog(parentContainer, message, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,buttons, buttons[0]);
            if(choice == JOptionPane.CLOSED_OPTION){
                return null;
            } else {
                return buttons[choice];
            }
    }
    
}
