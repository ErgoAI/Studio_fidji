package com.xsb.xj;

import java.awt.Component;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.HelpManager;
import com.xsb.xj.util.XJException;

@SuppressWarnings("serial")
public class XJMenu extends JMenu implements XJComponent, XJComponentTree, XJMenuItemComponent {
	GUITerm gt;
	PrologEngine engine;
        // Vector path;
	public XJMenu(PrologEngine engine, GUITerm gt){
        super();
    	this.gt=gt;
	    this.engine=engine;
 		//path.push(label);

        for(int c = 0; c < gt.getChildCount(); c++) {
            GUITerm child  = (GUITerm) gt.children[c];
            addSubMenu(engine, child);
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
		String tip = gt.tipDescription();
		if(!tip.equals("")){
			setToolTipText(tip);
		}
		TermModel helpId = gt.findProperty("helpid");
		if(helpId != null){
			String helpIdString = (String)helpId.node;
			HelpManager.addMenuHelp(this, helpIdString);
		}
	}
            
	void addSubMenu(PrologEngine engine,GUITerm child){
            JComponent submenu = (JComponent)child.makeGUI(engine);
            add(submenu);
	}

	public Collection<XJMenuItemComponent> getLeafMenuItems(){
		Vector<XJMenuItemComponent> leafMenus = new Vector<XJMenuItemComponent>();
		Component[] subcomponents = getMenuComponents();
		for(int i=0; i<subcomponents.length; i++){
			if(subcomponents[i] instanceof XJMenuItemComponent){
				leafMenus.addAll(((XJMenuItemComponent)subcomponents[i]).getLeafMenuItems());
			}
		}
		return leafMenus;
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
                
	public GUITerm getGT(){
		return gt;
	}
	public void setGT(GUITerm gt){
		this.gt=gt;
	}
	public PrologEngine getEngine(){
		return engine;
	}
	/** Subcomponents will receive their own refreshGUI() messages */
	public void refreshGUI(){
            setText(gt.node.toString());
            addMenuProps(gt);
        }
	/** This implementation does nothing */
	public boolean loadFromGUI(){return true;}
	/** This implementation returns false*/
	public boolean isDirty(){
		return false;
	}
	/** This implementation does nothing */
	public void setDefaultValue(TermModel dv){}

	public void selectGUI(Object[] parts){
		GUITerm.typicalContainerSelect(this,parts);
	}
        
    public void destroy() {
    }
        
}