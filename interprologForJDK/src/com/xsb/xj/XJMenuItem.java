package com.xsb.xj;

import java.awt.Component;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.HelpManager;
import com.xsb.xj.util.XJException;

@SuppressWarnings("serial")
public class XJMenuItem extends JMenuItem implements XJComponent, XJComponentTree, XJMenuItemComponent {
	GUITerm gt;
	PrologEngine engine;
        static final String ICON = "icon";
        static final String DISABLED = "disabled";

	public XJMenuItem(PrologEngine engine, GUITerm gt){
        super();
		this.gt=gt;
		this.engine=engine;
                
        addMenuProps(gt);
		if (gt.findProperty(DISABLED)!=null) this.setEnabled(false);
                String tip = gt.tipDescription();
                if(!tip.equals("")){
                    setToolTipText(tip);
                }
	}
        
        protected void addMenuProps(GUITerm gt){
            TermModel icon = gt.findProperty("icon");
            if(icon != null){
                if(icon.isAtom()) {
                    File file    = new File(icon.node.toString());
                    URL iconURL  = null;
                    
                    if(file.exists()) {
                        try {
                            iconURL = file.toURI().toURL();
                            if(iconURL != null) {
                                setIcon(new ImageIcon(iconURL));
                            }
                        } catch(MalformedURLException e) {
                            throw new XJException("XJAction: bad url for image");
                        }
                    }
                }
            }
            TermModel mnemonics = gt.findProperty("mnemonics");
            if(mnemonics != null){
                if(mnemonics.isAtom()) {
                    char[] m = mnemonics.node.toString().toCharArray();
                    if(m.length > 0) {
                        setMnemonic(m[0]);
                    }
                }
            }
            
            TermModel helpId = gt.findProperty("helpid");
            if(helpId != null){
                String helpIdString = (String)helpId.node;
                HelpManager.addMenuHelp(this, helpIdString);
            }
        }
        
        public String[] getPath(){
	    Component parent = getParent();
            // all menu items are wrapped into JPopupMenu
            while((parent != null) && (parent instanceof JPopupMenu)){
                parent = ((JPopupMenu)parent).getInvoker();
            }
            if((parent != null) && (parent instanceof XJMenuItemComponent)){
                String[] parentPath = ((XJMenuItemComponent)parent).getPath();
                String [] path = new String [parentPath.length + 1];
                System.arraycopy(parentPath, 0, path, 0, parentPath.length);
                path[parentPath.length] = gt.node.toString();
                return path;
            }
            return new String[]{gt.node.toString()};
        }
        
	public PrologEngine getEngine(){
		return engine;
	}

	public GUITerm getGT(){
		return gt;
	}
	public void setGT(GUITerm gt){
		this.gt=gt;
	}

	public void refreshGUI(){
            setText(gt.node.toString());
        }

	public boolean loadFromGUI(){
	    return true;
	}

	public boolean isDirty(){
		return false;
	}
	
	public void setDefaultValue(TermModel dv){
	}

	public void selectGUI(Object[] parts){
		GUITerm.typicalContainerSelect(this,parts);
	}

	public java.util.Collection<XJMenuItemComponent> getLeafMenuItems() {
		Vector<XJMenuItemComponent> leafMenus = new Vector<XJMenuItemComponent>();
		leafMenus.add(this);
		return leafMenus;
	}
	
	public void destroy() {
	}
        
}
