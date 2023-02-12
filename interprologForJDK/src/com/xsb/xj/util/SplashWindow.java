package com.xsb.xj.util;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
public class SplashWindow extends JWindow {
	JProgressBar progress=null;
	boolean hasProgressBar,mayCancel;

	public SplashWindow(Window owner, ImageIcon image, boolean hasProgressBar) {
		this(owner,  image,  hasProgressBar, false);
	}
	public SplashWindow(Window owner, ImageIcon image, boolean hasProgressBar, boolean cancellable) {
		this(owner,image,hasProgressBar,cancellable,true);
	}
	public SplashWindow(Window owner, ImageIcon image, boolean hasProgressBar, boolean cancellable, boolean visible) {
		this(owner,image,hasProgressBar,cancellable,visible,null,0);
	}
	public SplashWindow(Window owner, ImageIcon image, boolean hasProgressBar, boolean cancellable, boolean visible, String title, float fontSize){
		this(owner,image,hasProgressBar,cancellable,visible,title,fontSize,Color.black);
	}
	public SplashWindow(Window owner, ImageIcon image, boolean hasProgressBar, boolean cancellable, boolean visible, String title, float fontSize, Color textColor) {
		super(owner);
		this.mayCancel=cancellable;
		if (cancellable&&!hasProgressBar)
			throw new XJException("bad configuration of SplashWindow");
		SplashWindow.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		JPanel panel          = new JPanel(new BorderLayout());
		getContentPane().add("Center", panel);

		panel.setBorder(BorderFactory.createRaisedBevelBorder());
		JLabel imageLabel = new JLabel(title,image,SwingConstants.CENTER);
		if (title!=null) {
			imageLabel.setFont(imageLabel.getFont().deriveFont(fontSize));
			imageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
			imageLabel.setForeground(textColor);
		}
		if (image!=null) panel.add("Center", imageLabel);
		this.hasProgressBar = hasProgressBar;

		int imageHeight       = 0;
		int imageWidth = 0;
		if(hasProgressBar) {
			progress = new JProgressBar(0, 100);
			progress.setValue(0);
			progress.setStringPainted(true);
			imageHeight = 20;
		}
		if (mayCancel) {
			progress.setToolTipText("Click to abort startup");
			imageLabel.setToolTipText("Click the progress bar below to abort startup");
			progress.addMouseListener(new MouseAdapter(){
				public void mouseClicked(MouseEvent e){
					System.err.println("User cancelled startup");
					System.exit(1);
				}
			});
		}
		if (hasProgressBar) 
			add("South", progress);

		validate();
		Toolkit toolkit       = getToolkit();
		Dimension screenSize  = toolkit.getScreenSize();
		if (image!=null){
			imageWidth        = image.getIconWidth();
			imageHeight = +image.getIconHeight() + 20;
		}
		// Centers window
		setBounds(((screenSize.width - imageWidth) / 2), ((screenSize.height - imageHeight) / 2), imageWidth, imageHeight);
		addMouseListener(
			new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (mayCancel) return;
					setVisible(false);
					dispose();
				}
			});
		if (visible)
			setVisible(true);
	}

	public void setProgress(int percentage, String status) {
		if(hasProgressBar) {
			progress.setValue(percentage);
			progress.setString(status);
		}
	}

	public void finishSplash() {
		dispose();
	}
}
