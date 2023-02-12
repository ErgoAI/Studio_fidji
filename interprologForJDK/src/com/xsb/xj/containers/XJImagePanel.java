package com.xsb.xj.containers;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJComponent;
import com.xsb.xj.XJComponentTree;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * XJImagePanel extends javax.swing.JPanel and allows the user to place a single
 * image inside it. The properties that can be supplied are as follows:
 * <ul>
 *   <li> <code>PROP_IMAGE</code>: the location of the image to display</li>
 *
 * </ul>
 * <p>
 *
 * Example GT Code:<br>
 * <code>gt(_, [class='com.xsb.xj.containers.XJImagePanel', image='/foo/bar.gif'], [])</code>
 * </p>
 *
 *@author    Harpreet Singh
 *@version   $Id: XJImagePanel.java,v 1.2 2004/07/08 19:43:22 tvidrevich Exp $
 */
@SuppressWarnings("serial")
public class XJImagePanel extends JPanel implements XJComponent, XJComponentTree {
    public static final String PROP_IMAGE  = "image";

    GUITerm gt;
    PrologEngine engine;
    private boolean dirty;

    private boolean drawImage              = false;
    private Image image;

    public XJImagePanel(PrologEngine engine, GUITerm gt) {
        this.gt = gt;
        this.engine = engine;
        dirty = false;

        TermModel image_prop  = gt.findProperty(PROP_IMAGE);
        if(image_prop != null) {
            try {
                setImage((String) image_prop.node);
                setPreferredSize(gt.getPreferredSize(new Dimension(image.getWidth(this), image.getHeight(this))));
            } catch(FileNotFoundException fnfe) {
                System.err.println("ERROR in com.xsb.xj.containers.XJImagePanel:" + fnfe.getMessage());
                fnfe.printStackTrace();
            } catch(MalformedURLException mue) {
                System.err.println("ERROR in com.xsb.xj.containers.XJImagePanel:" + mue.getMessage());
                mue.printStackTrace();
            } catch(IOException ioe) {
                System.err.println("ERROR in com.xsb.xj.containers.XJImagePanel:" + ioe.getMessage());
                ioe.printStackTrace();
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width        = getWidth();
        int height       = getHeight();
        int imageWidth   = image.getWidth(this);
        int imageHeight  = image.getHeight(this);

        int x            = (width - imageWidth) / 2;
        int y            = (height - imageHeight) / 2;
        if (drawImage) g.drawImage(image, x, y, this);
    }

    /**
     * Changes the image being displayed in the panel. DOES NOT resize the panel
     * to the size of the image.
     *
     *@param imageLocation              Location of the image
     *@exception FileNotFoundException  The specified file does not exist
     *@exception IOException            Error reading the file.
     *@exception MalformedURLException  The specified file does not exist
     */
    public void setImage(String imageLocation) throws FileNotFoundException, IOException, MalformedURLException {
        File imageFile  = new File(imageLocation);

        if(imageFile.exists()) {
            image = ImageIO.read(imageFile.toURI().toURL());
            drawImage = true;
        } else {
            throw new FileNotFoundException("Image file not found : " + imageFile.getAbsolutePath());
        }
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

    public boolean loadFromGUI() {
        return true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDefaultValue(TermModel dv) { }

    public void selectGUI(Object[] parts) {
        GUITerm.typicalContainerSelect(this, parts);
    }
    
    public void destroy() {
    }
    
}
