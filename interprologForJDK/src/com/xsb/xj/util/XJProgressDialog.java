package com.xsb.xj.util;

import com.declarativa.interprolog.AbstractPrologEngine;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Frame;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

/**
 * A nonmodal dialog which will either display an (animated) gif or a progress
 * bar
 *
 */
@SuppressWarnings("serial")
public class XJProgressDialog extends JDialog implements ActionListener, Runnable, SwingConstants {
	AbstractPrologEngine engine;
	// >=0; 0 means indeterminate length
	int taskLength;	
	JPanel barPanel;
	JPanel animPanel;
	JProgressBar bar;
	// only one of these two will be displayed at a time:
	JLabel animation;
	JButton cancel;
	private JComponent currentDisplayer;
	private boolean stopped;

	public XJProgressDialog(AbstractPrologEngine engine) {
		this(engine, null);
	}

	public XJProgressDialog(AbstractPrologEngine engine, Frame owner) {
		super(owner);
		this.engine = engine;
		taskLength = -1;
		stopped = true;

		this.getContentPane().setLayout(new BorderLayout());

		setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		setResizable(false);

		getContentPane().add(new JLabel("Please Wait", CENTER), BorderLayout.NORTH);

		bar = new JProgressBar();
		bar.setPreferredSize(new Dimension(200, 20));

		barPanel = new JPanel();
		barPanel.setPreferredSize(new Dimension(300, 60));
		barPanel.add(bar);
		getContentPane().add(barPanel, BorderLayout.CENTER);

		ImageIcon animatedGIF  = new ImageIcon(getClass().getResource("/com/xsb/xj/images/ani-gear.gif"));
		animation = new JLabel(animatedGIF);
		animPanel = new JPanel();
		animPanel.setPreferredSize(new Dimension(300, 80));
		animPanel.add(animation);

		currentDisplayer = barPanel;

		JPanel jp              = new JPanel();
		cancel = new JButton("Cancel");
                cancel.setMnemonic('C');
		cancel.setPreferredSize(new Dimension(100, 24));

		jp.add(cancel);

		getContentPane().add(jp, BorderLayout.SOUTH);

		cancel.addActionListener(this);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		pack();
		setLocationRelativeTo(owner);

	}

	private void prepareForAnimation() {
		if(currentDisplayer != animPanel) {
			currentDisplayer = animPanel;
			getContentPane().remove(barPanel);
			getContentPane().add(animPanel, BorderLayout.CENTER);
		}
	}

	private void prepareForProgress(boolean hasLength) {
		if(currentDisplayer != barPanel) {
			currentDisplayer = barPanel;
			getContentPane().remove(animPanel);
			getContentPane().add(barPanel, BorderLayout.CENTER);
		}

		if(hasLength) {
			bar.setIndeterminate(false);
			bar.setStringPainted(true);
		} else {
			bar.setIndeterminate(true);
			bar.setStringPainted(false);
		}

	}

	public void showProgress(String title, int length) {
		if(!isStopped()) {
			// throw new XJException("Can not showProgress before terminating previous task");
			endProgress();
		}
		stopped = false;
		taskLength = length;
		final String t    = title;
		final Runnable r  =
			new Runnable() {
				public void run() {
					if(isStopped()) {
						// Progress dialog was invoked through invokeLater()
						// because of thread safety issues
						// (per Java documentation all GUI rendering should
						// be done in AWT thread by using invokeLater() or invokeAndWait())
						// Somehow sometimes invokeLater() caused it to be
						// invoked really later
						System.out.println("Warning: Progress dialog could not start before the process is finished");
					} else {
						setTitle(t);
                                                getAccessibleContext().setAccessibleName("Progress Window: "+t);
						if(taskLength == 0) {
							prepareForAnimation();
						} else if(taskLength == -1) {
							prepareForProgress(false);
							bar.setMaximum(0);
							bar.setValue(0);
						} else {
							prepareForProgress(true);
							bar.setMaximum(taskLength);
							bar.setValue(0);
						}

						pack();
						setVisible(true);
						validate();
					}
				}
			};
		// tv: in version 1.8
		// I changed SwingUtilities.invokeLater(r) to the following block that calls
		// SwingUtilities.invokeAndWait(r) because Progress dialog did not
		// always appear on time (see comments above).
		// If that causes any problems, switch back to invokeLater()
		// SwingUtilities.invokeLater(r);
		if(SwingUtilities.isEventDispatchThread()) {
			// As per documentation for invokeAndWait()
			// "It should not be called from the EventDispatchThread".
			// Starting a new thread.
			Thread appThread  =
				new Thread("XJ progress thread") {
					public void run() {
						try {
							SwingUtilities.invokeAndWait(r);
						} catch(Exception e1) {
							e1.printStackTrace();
						}
					}
				};
			appThread.start();
		} else {
			// Current thread is not Event Dispatch Thread
			try {
				SwingUtilities.invokeAndWait(r);
			} catch(Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public void run() { }

	public void setProgress(int amount) {
		if(isStopped()) {
			throw new XJException("Can not setProgress before showProgress");
		}
		if(taskLength <= 0) {
			throw new XJException("Can not setProgress without defining a task length in showProgress");
		}
		if(taskLength < amount) {
			throw new XJException("Too much progress for this task:" + amount + ", acceptable maximum is " + taskLength);
		}
		bar.setValue(amount);
	}

	public void endProgress() {
		if(isStopped()) {
			throw new XJException("Can not endProgress before showProgress");
		}
		stopped = true;
		setVisible(false);
	}

	public void setCancellableProgress(boolean yes) {
		cancel.setEnabled(yes);
	}

	public void actionPerformed(ActionEvent e) {
        int returnValue = JOptionPane.showConfirmDialog(this, "Stop process ?", "",  JOptionPane.YES_NO_OPTION);
        if(returnValue == JOptionPane.YES_OPTION){
			endProgress();
			engine.stop(); // nicer than interrupt
        }
	}

	public boolean isStopped() {
		return stopped;
	}
}
