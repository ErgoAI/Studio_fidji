package com.xsb.xj;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;

@SuppressWarnings("serial")
public class XJBrowser extends JPanel implements XJComponent{
        static {
            Authenticator.setDefault(new XJAuthenticator()); 
        }
	GUITerm gt;
	PrologEngine engine;
	private boolean dirty;

	private URL startUrl = null;
	private String startingUri = "";
	private JEditorPane jpane;
	private JTextField addr;
	private JLabel statusbar;
	private Stack<URL> bStack;
	private Stack<URL> fStack;
	private URL currentUrl;
	private JButton fwd;
	private JButton back;
	private JButton home;
	private boolean enableWeb;

	public XJBrowser(PrologEngine engine,GUITerm gt){
		this.gt=gt;
		this.engine=engine;
		dirty = false;

		TermModel webEnabled = gt.findProperty("web");
		if(webEnabled != null){
			String enable = (String)webEnabled.node;
			if(enable.equals("true"))
				enableWeb = true;
			else
				enableWeb = false;
		}
		else
			enableWeb = false;

		TermModel file = gt.findProperty("file");
		if(file != null){
			startingUri = (String)file.node;
			try{
				startUrl = new URL(startingUri);
			}catch(MalformedURLException mue)
			{	System.err.println("malformed url exception");
				System.err.println(mue.getMessage());
				mue.printStackTrace();
			}
		}

		jpane = new JEditorPane();
		EditorKit edKit = jpane.getEditorKitForContentType("text/html");
		jpane.setEditorKit(edKit);
		jpane.setEditable(false);

		jpane.addHyperlinkListener(new XJLinkListener());

		JScrollPane jsp = new JScrollPane(jpane);
		Dimension d = new Dimension(1000,500);
		jsp.setPreferredSize(d);

		this.setLayout(new BorderLayout());
		this.add(jsp,BorderLayout.CENTER);


		JToolBar webbar = makeToolBar(startingUri);
		this.add(webbar,BorderLayout.NORTH);

		statusbar = new JLabel("");
		statusbar.setBorder(BorderFactory.createLoweredBevelBorder());

		this.add(statusbar,BorderLayout.SOUTH);

		if(startUrl != null)
			gotoPage(startUrl);

	}

	class XJLinkListener implements HyperlinkListener {
		public XJLinkListener() {
		}

		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				jpane = (JEditorPane) e.getSource();
				if (e instanceof HTMLFrameHyperlinkEvent) {
				  HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
				  HTMLDocument doc = (HTMLDocument)jpane.getDocument();
				  doc.processHTMLFrameHyperlinkEvent(evt);
			  	} else {
				  	try {
						bStack.push(currentUrl);
						back.setEnabled(true);

						fStack.removeAllElements();
						fwd.setEnabled(false);

				  		jpane.setPage(e.getURL());
				  		currentUrl = e.getURL();
				  		addr.setText(e.getURL().toExternalForm());
				  		statusbar.setText(getPageTitle());
				  	} catch (Throwable t) {
				  		t.printStackTrace();
				  	}
			  	}
			}
		}
	}

	public JToolBar makeToolBar(String starturl) {
		JToolBar jb = new JToolBar();
		//jb.setRollover(true);		//only works in java 1.4.0 and higher
		jb.putClientProperty(new String("JToolBar.isRollover"), Boolean.TRUE);
		jb.setFloatable(false);

		ImageIcon backicon = new ImageIcon(getClass().getResource("/com/xsb/xj/images/Back16.gif"));
		ImageIcon fwdicon  = new ImageIcon(getClass().getResource("/com/xsb/xj/images/Forward16.gif"));
		ImageIcon homeicon = new ImageIcon(getClass().getResource("/com/xsb/xj/images/Home16.gif"));

		back = new JButton("Back",backicon);
		back.setActionCommand("back");
		bStack = new Stack<URL>();
		back.setEnabled(false);
		back.addActionListener(new java.awt.event.ActionListener () {
		      public void actionPerformed (java.awt.event.ActionEvent evt) {
		      	goBack();
		      }
		});

		fwd  = new JButton("Forward",fwdicon);
		fwd.setActionCommand("fwd");
		fStack = new Stack<URL>();
		fwd.setEnabled(false);
		fwd.addActionListener(new java.awt.event.ActionListener () {
				      public void actionPerformed (java.awt.event.ActionEvent evt) {
				      	goForward();
				      }
		});

		home = new JButton("Home",homeicon);
		home.setActionCommand("home");
		home.addActionListener(new java.awt.event.ActionListener () {
				      public void actionPerformed (java.awt.event.ActionEvent evt) {
				      	goHome();
				      }
		});

		JSeparator js = new JSeparator(SwingConstants.VERTICAL);
		js.setMaximumSize(new Dimension(4,20));

		JLabel addrLabel = new JLabel("   Address ");
		addr = new JTextField(starturl,40);
		if(enableWeb){
			addr.setEnabled(true);
			addr.addActionListener(new java.awt.event.ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent evt){
					String nextUrl = addr.getText();
					gotoPage(nextUrl);
				}
			});
		}
		else
			addr.setEnabled(false);


		jb.add(back);
		jb.add(fwd);
		jb.add(home);
		jb.add(js);
		jb.add(addrLabel);
		jb.add(addr);

		return jb;

	}

	private String getPageTitle()
	{
		String pageTitle = null;
	    while (pageTitle == null)	{
	    	pageTitle = (String) jpane.getDocument().getProperty( Document.TitleProperty );
	      try {
	        Thread.sleep(100);
	      } catch(InterruptedException e) { }
	    }

	    return pageTitle;
  	}

	public void goBack(){

		if(bStack.empty())
		;
		else{
			URL nextUrl = bStack.pop();
			if(bStack.empty())
				back.setEnabled(false);

			if(currentUrl != nextUrl){
				fStack.push(currentUrl);
				fwd.setEnabled(true);
			}
			gotoPage(nextUrl);

		}
	}

	public void goForward(){
		if(fStack.empty())
		;
		else{
			URL nextUrl = (URL)fStack.pop();
			if(fStack.empty())
				fwd.setEnabled(false);

			if(currentUrl != nextUrl){
				bStack.push(currentUrl);
				back.setEnabled(true);
			}
			gotoPage(nextUrl);
		}
	}

	public void goHome(){
		if(currentUrl != startUrl){
			bStack.push(currentUrl);
			back.setEnabled(true);

			fStack.removeAllElements();
			fwd.setEnabled(false);
			gotoPage(startUrl);
		} else
		;
	}

	public void gotoPage(String nextUrl){
		try{
			URL gotoUrl = new URL(nextUrl);
			if(currentUrl != gotoUrl){
				bStack.push(currentUrl);
				back.setEnabled(true);

				fStack.removeAllElements();
				fwd.setEnabled(false);

				gotoPage(gotoUrl);
			}
			else
				;
		}catch(MalformedURLException me){}
	}

	public void gotoPage(URL nextUrl){
		try{
			jpane.setPage(nextUrl);
			addr.setText(nextUrl.toExternalForm());
			currentUrl = nextUrl;
			statusbar.setText(getPageTitle());
		}catch(IOException io){
			System.err.println("error going to " + nextUrl);
			io.getMessage();
			io.printStackTrace();
		}
	}
	// Methods from XJComponent Interface.
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

	public void selectGUI(Object[] parts){
		GUITerm.typicalContainerSelect(this,parts);
	}

	public void setDefaultValue(TermModel dv){
	}
        
        public void destroy() {
        }
        
}