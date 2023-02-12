package com.xsb.xj.util;

import javax.swing.Icon;
import java.awt.Graphics;
import java.awt.Component;

/**
 * Class for an empty icon. Useful when want to specify that no icon is
 * displayed in the components that require it, for example, in the title bar
 * for <code>JInternalFrame</code>. (By default <code>JInternalFrame</code> 
 * displays standard Java icon. Setting it to null leads to displaying no icon 
 * in Metal L&F however displaying default Java Cup icon in Windows L&F)
 */
public class EmptyIcon implements Icon{
    public EmptyIcon(){}
    public void paintIcon(Component c,
                          Graphics g,
                          int x,
                          int y){}
    public int getIconWidth(){
        return 0;
    }
    public int getIconHeight(){
        return 0;
    }
}
