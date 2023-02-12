package com.xsb.xj.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.text.JTextComponent;

/** Adapted from http://stackoverflow.com/questions/1738966/java-jtextfield-with-input-hint 
Use it like this:
JTextArea field = new JTextArea();
field.setUI(new HintTextAreaUI("Search", true));
*/
public class HintTextAreaUI extends BasicTextAreaUI implements FocusListener {

    private String hint;
    private boolean hideOnFocus;
    private Color color;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    private void repaint() {
        if(getComponent() != null) {
            getComponent().repaint();           
        }
    }

    public boolean isHideOnFocus() {
        return hideOnFocus;
    }

    public void setHideOnFocus(boolean hideOnFocus) {
        this.hideOnFocus = hideOnFocus;
        repaint();
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
        repaint();
    }
    public HintTextAreaUI(String hint) {
        this(hint,false);
    }

    public HintTextAreaUI(String hint, boolean hideOnFocus) {
        this(hint,hideOnFocus, null);
    }

    public HintTextAreaUI(String hint, boolean hideOnFocus, Color color) {
        this.hint = hint;
        this.hideOnFocus = hideOnFocus;
        this.color = color;
    }

    @Override
    protected void paintSafely(Graphics g) {
        super.paintSafely(g);
        JTextComponent comp = getComponent();
        if(hint!=null && comp.getText().length() == 0 && (!(hideOnFocus && comp.hasFocus()))){
            if(color != null) {
                g.setColor(color);
            } else {
                g.setColor(comp.getForeground().brighter().brighter().brighter());              
            }
            //int padding = (comp.getHeight() - comp.getFont().getSize())/2;
            g.drawString(hint, comp.getMargin().left + comp.getInsets().left, comp.getFont().getSize());          
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        if(hideOnFocus) repaint();

    }

    @Override
    public void focusLost(FocusEvent e) {
        if(hideOnFocus) repaint();
    }
    @Override
    protected void installListeners() {
        super.installListeners();
        getComponent().addFocusListener(this);
    }
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        getComponent().removeFocusListener(this);
    }
}
