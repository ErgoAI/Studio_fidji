package com.xsb.xj.containers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;
import com.xsb.xj.util.XJException;

/* Displays values of a term in horizontal form
 *
 */
@SuppressWarnings("serial")
public class XJDesktopPane extends JDesktopPane implements XJComponent,XJComponentTree{
    GUITerm gt;
    PrologEngine engine;
    private boolean dirty;
    
    public XJDesktopPane(PrologEngine engine,GUITerm gt){
        setPreferredSize(new java.awt.Dimension(800, 600));
        setAlignmentY(0.0F);
        setAlignmentX(0.0F);
        this.gt=gt;
        this.engine=engine;
        dirty = false;
        for (int c=0;c<gt.getChildCount();c++){
            GUITerm child = (GUITerm)gt.children[c];
            
            if(!child.isInvisible()){
                JInternalFrame frame = new JInternalFrame();
                XJComponent component = child.makeGUI(engine);
                frame.setFrameIcon(getFrameIcon(child));
                frame.setTitle(child.getTitle());
                frame.getAccessibleContext().setAccessibleName(child.getTitle());
                
                TermModel bounds = child.findProperty("bounds");
                if (bounds!=null) {
                    int childcount = bounds.getChildCount();
                    if(childcount == 4){
                        TermModel x1 = (TermModel)bounds.getChild(0);
                        TermModel y1 = (TermModel)bounds.getChild(1);
                        TermModel x2 = (TermModel)bounds.getChild(2);
                        TermModel y2 = (TermModel)bounds.getChild(3);
                        frame.setBounds(x1.intValue(),y1.intValue(),x2.intValue(),y2.intValue());
                        //					component.setSize(new Dimension((x2.intValue()-x1.intValue()),(y2.intValue()-y1.intValue())));
                    }
                }
                TermModel maximizable = child.findProperty("maximizable");
                if (maximizable!=null) {
                    frame.setMaximizable(true);
                }
                TermModel resizable = child.findProperty("resizable");
                if (resizable!=null) {
                    frame.setResizable(true);
                }
                TermModel iconifiable = child.findProperty("iconifiable");
                if (iconifiable!=null) {
                    frame.setIconifiable(true);
                }
                frame.getContentPane().add((JComponent)component);
                frame.setVisible(true);
                
                add(frame, JLayeredPane.DEFAULT_LAYER);
            }
        }
    }
    
    private Icon getFrameIcon(GUITerm frameGt){
        Icon frameIcon;
        TermModel iconLocation = frameGt.findProperty("icon");
        if (iconLocation != null) {
            URL iconURL = getClass().getResource((String)iconLocation.node); //in classpath
            if(iconURL == null){iconURL = getClass().getResource("/"+(String)iconLocation.node);}
            if(iconURL == null){ //file path, not in classpath
                File file = new File((String)iconLocation.node);
                if(file.exists()){
                    try{
                        iconURL = file.toURI().toURL();
                    } catch(MalformedURLException e){
                        throw new XJException("bad file URL???");
                    }
                }
            }
            if(iconURL != null){
                frameIcon = new ImageIcon(iconURL);
            } else {
                frameIcon = new com.xsb.xj.util.EmptyIcon();
                System.err.println("Icon file not found "+(String)iconLocation.node);
            }
        } else {
            frameIcon = new com.xsb.xj.util.EmptyIcon();
        }
        return frameIcon;
    }
        
	public TermModel[] getMyGUIs(){
		return gt.getMyGUIs(); 
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
    }
    
    public boolean loadFromGUI(){
        dirty=false;
        return true;
    }
    
    public boolean isDirty(){
        return dirty;
    }
    
    public void setDefaultValue(TermModel dv){
    }
    
    public void selectGUI(Object[] parts){
        GUITerm.typicalContainerSelect(this,parts);
    }
    
    public void destroy() {
    }
    
}
