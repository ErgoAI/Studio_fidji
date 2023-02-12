/*
 * XJMenuItemComponent.java
 *
 * Created on April 30, 2004, 11:19 AM
 */

package com.xsb.xj;

/**
 *
 * @author  tanya
 */
public interface XJMenuItemComponent {
    
    /**
     * Returns path from top XJMenu to this component
     */
    public String[] getPath();
    
    /**
     * If the component is a leaf itself return
     * singleton collection that includes that component
     * else return all leaf XJMenuItemComponent below it.
     */
    public java.util.Collection<XJMenuItemComponent> getLeafMenuItems();
    
    public void addActionListener(java.awt.event.ActionListener l);
}
