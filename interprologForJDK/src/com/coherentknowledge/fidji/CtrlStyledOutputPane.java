/* File:   CtrlStyleOutputPane.java
**
** Author(s): Miguel Calejo
**
** Contact:   mc@interprolog.com
**
** Copyright (C) Coherent Knowledge Systems, LLC, 2015 - 2016.
** All rights reserved.
**
*/

package com.coherentknowledge.fidji;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import com.declarativa.fiji.FijiSubprocessEngineWindow;
import com.declarativa.interprolog.gui.ListenerWindow;
import com.declarativa.interprolog.gui.SmartScroller;
import com.declarativa.interprolog.gui.StyledOutputPane;
import com.declarativa.interprolog.util.IPException;
import com.xsb.xj.XJDesktop;

/**
** @author mc
**
**/
public class CtrlStyledOutputPane extends StyledOutputPane {
	private static final long serialVersionUID = 1L;
	// coloring for output pane
	Style[] styles;
	String[] styleTips;
	/** Style currently active, to be applied to the next chars to output from the logic engine */
	Style currentStyle = null;


	/** This instance will be initialized to the same styles used by Ergo Studio */
	public CtrlStyledOutputPane() {
		super();
	}

	public void initializeOutputStyles() {
	    Style def = addStyle("mydefault", null);
	    StyleConstants.setForeground(def, Color.BLACK);	
	    
	    Style stdout = addStyle("stdout", null);
	    StyleConstants.setForeground(stdout, Color.BLACK);	
	    
	    Style stdwarn = addStyle("stdwarn", null);
	    StyleConstants.setForeground(stdwarn, new Color(153,0,0));	
	    
	    Style stdmsg = addStyle("stdmsg", null);
	    StyleConstants.setItalic(stdmsg, true);
	    
	    Style stddbg = addStyle("stddbg", stdwarn);
	    
	    Style stfdbk = addStyle("stfdbk", stdout);	
	    
	    Style prompt = addStyle("prompt", null);
	    StyleConstants.setForeground(prompt, new Color(0,153,0));	
	    
	    Style endLoad = addStyle("endLoad", null);
	    StyleConstants.setForeground(endLoad, new Color(148,60,148));	
	    
	    Style stderrStyle = addStyle("stderr", null);
	    StyleConstants.setForeground(stderrStyle, Color.RED);
	    
	    // this is \01 9
	    Style boldtext = addStyle("boldtext", null);
	    StyleConstants.setBold(boldtext, true);
	    
	    // this is \01 :
	    Style bolditaltext = addStyle("bolditaltext", null);
	    StyleConstants.setItalic(bolditaltext, true);
	    StyleConstants.setBold(bolditaltext, true);
	    
	    // this is \01 ;
	    Style bluetext = addStyle("bluetext", null);
	    StyleConstants.setForeground(bluetext, Color.BLUE);
	    
	    // this is \01 <
	    Style magentatext = addStyle("magentatext", null);
	    StyleConstants.setForeground(magentatext, Color.MAGENTA);
	    
	    // this is \01 =
	    Style orangetext = addStyle("orangetext", null);
	    StyleConstants.setForeground(orangetext, new Color(255,110,20));
	    
	    // indices are 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, :, ;, <, =
	    // If more will be needed, continue with the following ASCII codes:
	    //    > (62), ? (63), @ (64), A (65) etc.
	    styles = new Style[]{def,stdout,stderrStyle,stdwarn,stdmsg,stddbg,stfdbk,prompt,endLoad,boldtext,bolditaltext,bluetext,magentatext,orangetext};
	    styleTips = new String[]{null,"Standard output","Errors","Warnings","Messages","Debug","Feedback","Prompt","End of load","","","","",""};
	    if (styles.length!=styleTips.length)
		throw new RuntimeException("Bad styleTips array");
	}
	
	/** Experimental method to change the looks of one of the existing styles.
	 * @param styleIndex
	 * @param red
	 * @param green
	 * @param blue
	 */
	public void setStyle(int styleIndex,int red,int green,int blue){
	    if (red>255||red<0||green>255||green<0||blue>255||blue<0)
		throw new RuntimeException("Color component must be between 0 and 255");
	    if (styleIndex<0||styleIndex>=styles.length)
		throw new RuntimeException("Style index must be between 0 and "+styles.length);
	    StyleConstants.setForeground(styles[styleIndex], new Color(red,green,blue));	
	    // tips buggy: styleTips[styleIndex] = tip;
	}

	public String attributesToTooltip(AttributeSet attributes) {
	    if (attributes == null) return null;
	    for (int i=0; i< styles.length; i++)
		if (attributes.equals(styles[i]))
		    return styleTips[i];
	    return null;
	}
	boolean justGotControlChar = false;
	/** skips \001 \001, and interprets \001 C color commands */

	public void appendOutputTextRunsFromFile(String filename) throws IOException{
		File F = new File(filename);
		long size = F.length();
		if (size>Runtime.getRuntime().freeMemory()/2)
			throw new IPException("File too big");
		FileInputStream fis = new FileInputStream(F);
		byte[] buffer = new byte[(int)size];
		if (size!=fis.read(buffer)){ 
			fis.close();
			throw new IPException("Unable to read all file bytes");
		}
		fis.close();
		appendOutputTextRuns(new String(buffer)); 
		
	}
	public void appendOutputTextRuns(String s){
	    /* System.out.println("***output:"+s);
	       for (int i=0; i<s.length(); i++)
	       System.out.print(s.charAt(i)+"|"); */
	    int c = 0;
	    StringBuffer chunk = new StringBuffer();
	    while (c < s.length()){
		if (c==0 && justGotControlChar){ // previous call ended with the ctrl char
		    // unnecessary: chunk.delete(0, chunk.length());
		    if (s.charAt(c) == '\01'){
			// end of a "prompt" char pair, invisible text that we skip
			justGotControlChar = false;
			c++;
			continue;
		    }
		    int i = s.charAt(c)-'0';
		    if (i<0 || i>=styles.length)
			System.err.println("Bad color request (on separate stream chunk):"+s.charAt(c)+" in "+s);
		    else currentStyle = styles[i];
		    c++;
		    justGotControlChar = false;
		} else if (s.charAt(c) == '\01'){ // new control sequence
		    append(chunk.toString(),currentStyle);
		    chunk.delete(0, chunk.length());
		    c++;
		    if (c>=s.length()){
			justGotControlChar = true;
			break;
		    }
		    if (s.charAt(c) == '\01'){
			// announcing a "prompt" char pair, invisible text that we skip
			justGotControlChar = false;
			c++;
			continue;
		    }
		    int i = s.charAt(c)-'0';
		    if (i<0 || i>=styles.length)
			System.err.println("Bad color request:"+s.charAt(c)+" in "+s);
		    else currentStyle = styles[i];
		    justGotControlChar = false;
		    c++;
		} else  {
		    justGotControlChar = false;
		    chunk.append(s.charAt(c++));
		}
	    }
	    append(chunk.toString(),currentStyle);
	}
	
	public void print(String s) {
	    appendOutputTextRuns(s);
	    // SmartScroller handles this case; scrollToBottom();
	}

	public static class StyledOutputFrame extends JFrame{
	    private static final long serialVersionUID = 1L;
	    CtrlStyledOutputPane output;
	    
	    StyledOutputFrame(String title,String tip){
			super(title);
			getContentPane().setLayout(new BorderLayout());
			output = new CtrlStyledOutputPane();
			output.setFont(new Font("Courier",Font.PLAIN,FijiSubprocessEngineWindow.preferredFontSize)); 
			output.setEditable(false); 
			output.setToolTipText(tip);
			//prologOutput.setLineWrap(true);  // Swing used to crash with large amounts of text...
			output.setDoubleBuffered(true); // Use Swing double screen buffer
		        output.getAccessibleContext().setAccessibleName(tip);
		        ListenerWindow.popupEditMenuFor(output);
			output.initializeOutputStyles();
			JScrollPane scroller = new JScrollPane();
			scroller.getViewport().add(output);
	        new SmartScroller(scroller);
			getContentPane().add(scroller, BorderLayout.CENTER);
			setSize(600,600);
			setAutoRequestFocus(false);
			setVisible(true);
			XJDesktop.waitForSwing(); // not really necessary, but just in case someday Prolog callbacks are planted on the frame
	    }
	    public void clear(){
	    	SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
					output.setText("");					
				}
	    	});
	    }
	    /** Adds text to output, styling according to \001... sequences. Styles coded in initializeOutputStyles()  */
	    public void print(String s){
	    	output.print(s);
	    }
	    public void setStyle(int styleIndex,int red,int green,int blue){
	    	output.setStyle(styleIndex, red, green, blue);
	    }
	    public void appendOutputTextRunsFromFile(String filename) throws IOException{
	    	output.appendOutputTextRunsFromFile(filename);
	    }
	}
    /** Example:
	java('com.coherentknowledge.fidji.CtrlStyledOutputPane',W,makeOutputFrame(string('My Output'),string('Some tool tip here'))),
	java(W,print(string('\n\01\2What\01\3 a\01\7 colorful\01\8 text'))),
	java(W,setStyle(int(2),int(0),int(0),int(255))), java(W,setStyle(int(8),int(255),int(0),int(0))), 
	java(W,print(string('\n\01\2What\01\3 a\01\7 colorful\01\8 text'))).
    */
    public static StyledOutputFrame makeOutputFrame(String title, String tip){
    	return new StyledOutputFrame(title,tip);
    }
    
}
