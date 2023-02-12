package com.declarativa.fiji;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.declarativa.interprolog.FloraSubprocessEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.gui.ListenerWindow;

@SuppressWarnings("serial")
public class FinderWindow extends JFrame {
	JPanel myContentPane; // to be able to integrate with TabsWindow
	JTextArea query;
	JCheckBox heads,facts,remaining;
	JButton findButton;
	JLabel noResultsYet;
	FijiSubprocessEngineWindow listener;
	private JComponent lastResults;
	Action findAction;
	static String TITLE_PREFIX = "Find Term";
	public FinderWindow(FijiSubprocessEngineWindow listener){
		super(TITLE_PREFIX);
		this.listener = listener;
		JPanel top = new JPanel(new BorderLayout());
		top.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		myContentPane = new JPanel(new BorderLayout());
		myContentPane.add(top,BorderLayout.NORTH);
		
		Box labelBox = new Box(BoxLayout.Y_AXIS);
		labelBox.add(new JLabel("Term:", JLabel.RIGHT),BorderLayout.WEST);
		labelBox.add(Box.createGlue());
		top.add(labelBox,BorderLayout.WEST);
		
		query = new JTextArea(2, 20);
		query.setBorder(BorderFactory.createLoweredBevelBorder());
		ListenerWindow.popupEditMenuFor(query);
		query.setLineWrap(true);
		query.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				if (e.getKeyCode()==KeyEvent.VK_ENTER) {
					if (!findAction.isEnabled())
						return;
					e.consume();
					doFind();
				} 
			}
		});
		top.add(query,BorderLayout.CENTER);
		
		Box options = new Box(BoxLayout.Y_AXIS);
		heads = new JCheckBox("Heads", true); heads.setToolTipText("Search in rule heads");
		options.add(heads);
		facts = new JCheckBox("Facts", true); facts.setToolTipText("Search in facts");
		options.add(facts);
		remaining = new JCheckBox("Other", false); 
		remaining.setToolTipText("Search in other program components: rule bodies, tags, ids, defeasibility tags");
		
		// While there's a bug on the Ergo side:
		//heads.setEnabled(false); facts.setEnabled(false); remaining.setEnabled(false);

		options.add(remaining);
		
		Box topRight = new Box(BoxLayout.X_AXIS);
		topRight.add(options);
		findAction = new AbstractAction("Find"){
			@Override
			public void actionPerformed(ActionEvent e) {
				doFind();
			}
		};
		listener.disableWhenBusy(findAction);
		findButton = new JButton(findAction); 
		findButton.setToolTipText("Click or press enter to search for the term");
		topRight.add(findButton);
		top.add(topRight,BorderLayout.EAST);
		// our central component will later contain a JComponent (XJTable) made on the Ergo side. For now:
		noResultsYet = new JLabel("Results will appear here",JLabel.CENTER);
		noResultsYet.setPreferredSize(new Dimension(450, 300));
		myContentPane.add(noResultsYet, BorderLayout.CENTER);
		getContentPane().add(myContentPane, BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pack();
		//setVisible(true);
		TabsWindow.finders.addWindow(this, TabsWindow.finders.makeTabActionFor(this));
		query.requestFocusInWindow();
	}
	
	void doFind(){
		String S = query.getText().trim();
		if (S.length()<1){
			badSearch("The search term cannot be empty");
			return;
		}
		// Assuming Flora in use
		if(!listener.checkEngineAvailable())
			return;
		ListenerWindow.setWaitCursor(this);
		FloraSubprocessEngine engine = (FloraSubprocessEngine)listener.getEngine();
		TermModel result = engine.floraDeterministicGoal(
			"%f_forGUI(("+S+"),"+ heads.isSelected() +","+ facts.isSelected() + ","+ remaining.isSelected()+",?ListGUIint)@"+FijiSubprocessEngineWindow.ERGO_STUDIO_MODULE
			, "?ListGUIint");
		ListenerWindow.restoreCursor(this);
		if (result==null){
			badSearch("The search has failed."); // syntax errors here??
			return;
		}
		JComponent list = (JComponent)engine.getRealJavaObject(result.intValue()); // getting an object reference, so...
		if (lastResults!=null)
			myContentPane.remove(lastResults);
		else 
			myContentPane.remove(noResultsYet);
		myContentPane.add(list,BorderLayout.CENTER);
		lastResults = list;
		S = abbreviate(S,30);
		setTitle(TITLE_PREFIX+" ("+S+")");
		TabsWindow.finders.updateTitle(this,S);
		validate();
	}
	
	/** Actually may return String with maxLength+1 length */
	static String abbreviate(String S, int maxLength){
		if (S.length()<=maxLength)
			return S;
		return S.substring(0, maxLength)+"…";
	}
	
	void badSearch(String message){
		Toolkit.getDefaultToolkit().beep();
		JOptionPane.showMessageDialog(this, message, "Cannot find", JOptionPane.ERROR_MESSAGE);
	}

}
