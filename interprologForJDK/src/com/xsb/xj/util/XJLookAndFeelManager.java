/*
 * XJLookAndFeelManager.java
 *
 * Created on September 20, 2002, 8:54 AM
 */

package com.xsb.xj.util;

import javax.swing.*;
import java.awt.event.*;

/**
 *
 * @author  tanya
 */
public class XJLookAndFeelManager {
    
    /** Creates a new instance of XJLookAndFeelManager */
    public XJLookAndFeelManager() {
    }
    
    public static void setLookAndFeelMenuItems(JMenu menuToAttachTo, JFrame forComponent){
        final UIManager.LookAndFeelInfo[] availableLooks = UIManager.getInstalledLookAndFeels();
        for (int looki=0; looki<availableLooks.length; looki++){
            JMenuItem temp = new JMenuItem(availableLooks[looki].getName());
            menuToAttachTo.add(temp);
            final int hack = looki;
            final JFrame finalComponent = forComponent;
            temp.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    try{
                        UIManager.setLookAndFeel(availableLooks[hack].getClassName());
                        SwingUtilities.updateComponentTreeUI(finalComponent.getContentPane());
                        // somehow need to do it twice - does not redraw all correctly from the first time
                        SwingUtilities.updateComponentTreeUI(finalComponent.getContentPane());
                        
                        //finalComponent.pack();

                        // should notify everybody...
                    } catch(Exception foo) {
                        System.out.println("You are not allowed to change to that look and feel!");
                    }
                }
            } );
        }
        menuToAttachTo.revalidate();
    }
    
    public static String getCurrentLookAndFeel(){
        LookAndFeel lf = UIManager.getLookAndFeel();
        if(lf != null){
            return lf.getClass().getName().substring(1);
        }
        return null;
    }
    
    public static boolean setCurrentLookAndFeel(String className, JFrame forComponent){
        try{
            UIManager.setLookAndFeel(className);
            SwingUtilities.updateComponentTreeUI(forComponent.getContentPane());
            // somehow need to do it twice - does not redraw all correctly from the first time
            SwingUtilities.updateComponentTreeUI(forComponent.getContentPane());
            //orComponent.pack();
            return true;
        } catch(Exception e){
            System.out.println("Cannot set look&feel "+className);
            return false;
        }
    }

    /* Might need to use following methods
     UIManager.getCrossPlatformLookAndFeelClassName() 
Returns the string for the one look-and-feel guaranteed to work -- the Java Look & Feel. 

     static ComponentUI getUI(JComponent target) 
     */
}
