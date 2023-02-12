/*
 * HelpManager.java
 *
 * Created on December 2, 2003, 3:01 PM
 */

package com.xsb.xj.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.help.CSH;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;

import com.declarativa.interprolog.TermModel;
import com.xsb.xj.XJComponent;

/**
 *
 * @author  tanya
 */
public class HelpManager {
    
    public final static String DEFAULT_HELPSET_NAME = "/docs/help.hs";
    protected static Map<String,HelpSet> helpSetMap;

    /** Creates a new instance of HelpManager */
    public HelpManager() {
    }
    
    public static synchronized HelpSet getHelpSet(String helpSetLocation) throws HelpSetException{
        if (helpSetMap == null) {
            helpSetMap = Collections.synchronizedMap(new TreeMap<String,HelpSet>());
        }
        HelpSet helpSet = helpSetMap.get(helpSetLocation);
        if(helpSet == null){
            URL helpSetURL = HelpManager.class.getResource(helpSetLocation);
            
            if(helpSetURL == null) {
                helpSetURL = HelpManager.class.getResource("/" + helpSetLocation);
            }
            if(helpSetURL == null) {//file path, not in classpath
                File file  = new File(helpSetLocation);
                if(file.exists()) {
                    try {
                        helpSetURL = file.toURI().toURL();
                    } catch(java.net.MalformedURLException e) {
                        throw new XJException("bad file URL???");
                    }
                }
            }
           
            if (helpSetURL != null) {
                helpSet = new HelpSet(null, helpSetURL);
                helpSetMap.put(helpSetLocation, helpSet);
            } else {
                throw new HelpSetException("Error finding HelpSet "+helpSetLocation);
            }
        }
        return helpSet;
    }
    
    public static void registerForPopupHelp(JComponent component, HelpSet helpSet, String helpIdString){
        CSH.setHelpSet(component, helpSet);
        CSH.setHelpIDString(component, helpIdString);
        final ActionListener al = new CSH.DisplayHelpFromSource(helpSet,"javax.help.Popup", "popupWindow");
                   
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0), "helpRequest");
        component.getActionMap().put("helpRequest", new AbstractAction(){
			private static final long serialVersionUID = 4473334227338449258L;

			public void actionPerformed(ActionEvent e){
                al.actionPerformed(e);
            }
        });
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "helpRequestF1");
        component.getActionMap().put("helpRequestF1", new AbstractAction(){
			private static final long serialVersionUID = -4366144657751524829L;

			public void actionPerformed(ActionEvent e){
                al.actionPerformed(e);
            }
        });
    }
    
    /**
     * Registers XJComponent for popup help to be triggered by F1 key.
     * The component being registered is searched for helpid property, and is 
     * registered only if the property is present.
     */
    public static void registerXJComponentForPopupHelp(com.xsb.xj.XJComponent component){
        TermModel helpId = component.getGT().findProperty("helpid");
        if (helpId != null){
            String helpIdString = (String)helpId.node;
            try{
                HelpSet helpSet = HelpManager.getHelpSet(HelpManager.DEFAULT_HELPSET_NAME);
                registerForPopupHelp((JComponent)component, helpSet, helpIdString);
            } catch(HelpSetException e){
                System.out.println("Unable to attach help: "+e);
                // e.printStackTrace();
            }
        }
    }
    
    /**
     * Display Help Window
     */
    public static void showHelp(){
         try{
            HelpSet helpSet = HelpManager.getHelpSet(HelpManager.DEFAULT_HELPSET_NAME);
            HelpBroker broker = helpSet.createHelpBroker();
            broker.setHelpSetPresentation(helpSet.getPresentation("mainWindow"));
            broker.setDisplayed(true);
        } catch(Exception e){
            System.out.println("Unable to display help: "+e);
        }
    }
   
    /**
     * Adds help to a menu
     */
    public static void addMenuHelp(final JMenuItem menu, final String helpIdString){
        try{
            HelpSet helpSet = HelpManager.getHelpSet(HelpManager.DEFAULT_HELPSET_NAME);
            if(isValidHelpId(helpSet, helpIdString)){
                CSH.setHelpSet(menu, helpSet);
                CSH.setHelpIDString(menu, helpIdString);
                HelpBroker broker = helpSet.createHelpBroker();
                broker.setHelpSetPresentation(helpSet.getPresentation("mainWindow"));
                final ActionListener al = new CSH.DisplayHelpFromFocus(broker);
                
                menu.addMenuKeyListener(new MenuKeyListener(){
                    public void menuKeyTyped(MenuKeyEvent e){
                    }
                    public void menuKeyPressed(MenuKeyEvent e){
                        if((e.getKeyCode() == KeyEvent.VK_F1) 
                            || (e.getKeyCode() == KeyEvent.VK_HELP)){
                            MenuElement[] selectedMenuPath = e.getMenuSelectionManager().getSelectedPath();
                            MenuElement[] menuPath = e.getPath();
                            if((selectedMenuPath != null) && (menuPath != null)){
                                if((selectedMenuPath.length > 0) && (menuPath.length > 0)){
                                    // when an internal menu is selected 
                                    // the last element in the selected path 
                                    // is a popup menu for submenu
                                    // for a leaf menu it is the menu itself
                                    int index = selectedMenuPath.length - 1;
                                    MenuElement lastMenu;
                                    do{
                                        lastMenu = selectedMenuPath[index];
                                        index--;
                                    } while((lastMenu instanceof JPopupMenu) && (index >= 0));
                                    if(lastMenu.equals(menuPath[menuPath.length - 1])){
                                        al.actionPerformed(new ActionEvent(menu, 0, "helpRequestF1"));
                                    }
                                }
                            }
                        }
                    }
                    public void menuKeyReleased(MenuKeyEvent e){
                    }
                });
            }
        } catch(Exception e){
            System.out.println("Unable to attach help: "+e);
        }
        
    }
    
    /**
     * Checks whether help with helpIdString id exists in helpSet
     */
    public static boolean isValidHelpId(HelpSet helpSet, String helpIdString){
        return helpSet.getCombinedMap().isValidID(helpIdString, helpSet);
    }
    
    public static JButton createHelpButton(XJComponent gui){
        JButton helpButton = null;
        TermModel helpId = gui.getGT().findProperty("helpid");
        if (helpId != null){
            String helpIdString = (String)helpId.node;
            helpButton = new JButton("Help");
            helpButton.setMnemonic('H');
            addWindowHelpButton((JComponent)gui, helpButton, helpIdString);
        }
        return helpButton;
    }
                
    public static JMenu createHelpMenu(XJComponent gui){
        JMenu helpMenu = null;
        TermModel helpId = gui.getGT().findProperty("helpid");
        if (helpId != null){
            String helpIdString = (String)helpId.node;
            helpMenu = new JMenu("Help");
            helpMenu.setMnemonic('H');
            JMenuItem helpSubMenu = new JMenuItem(gui.getGT().getTitle() + " Help");
            helpMenu.add(helpSubMenu);
            addWindowHelpButton((JComponent)gui, helpSubMenu, helpIdString);
        }
        return helpMenu;
    }
    
    protected static void addWindowHelpButton(JComponent gui, AbstractButton helpButton, String helpIdString){
        try{
            HelpSet helpSet = HelpManager.getHelpSet(HelpManager.DEFAULT_HELPSET_NAME);
            HelpBroker hBroker = helpSet.createHelpBroker();
            
            hBroker.enableHelpOnButton(helpButton, helpIdString, helpSet);
            // setup F1 help support
            hBroker.enableHelpKey(gui, helpIdString, helpSet);
        } catch(HelpSetException e){
            System.out.println("Unable to attach help: "+e);
            // e.printStackTrace();
        }
    }
}
