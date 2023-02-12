/*
 * OutputFrame.java
 *
 * Created on April 11, 2003, 9:50 AM
 */

package com.xsb.xj.util;

import java.awt.Color;
import java.awt.Window;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;

import com.declarativa.interprolog.gui.ListenerWindow;

/**
 *
 * @author  tanya
 */
@SuppressWarnings("serial")
public class OutputFrame extends javax.swing.JDialog {
    public static final String INFO_MODE = "info";
    public static final String WARN_MODE = "warn";
    public static final String ERROR_MODE = "error";
    
    /** Creates new form OutputFrame */
    public OutputFrame() {
        initComponents();
        
//        PrintStream out = new PrintStream(new DocumentOutputStream(messagesDocument));
//        System.setOut(out);
//        PrintStream err = new PrintStream(new DocumentOutputStream(messagesDocument));
//        System.setErr(err);

        this.setCentered();
    }
    
    public OutputFrame(Window owner) {
        super(owner);
        initComponents();
        /*
         * If output flickers a lot try 
         * jScrollPane1.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
         * However that might take a larger chunk of memory (may be not that large)
         */

//        PrintStream out = new PrintStream(new DocumentOutputStream(messagesDocument));
//        System.setOut(out);
//        PrintStream err = new PrintStream(new DocumentOutputStream(messagesDocument));
//        System.setErr(err);

        this.setCentered();
    }
    
    
    public OutputFrame(JComponent c){
    	this((Window)c.getTopLevelAncestor());
    }
    
    public void setCentered() {
        this.setLocation((int) (getToolkit().getScreenSize().getWidth() - this.getSize().getWidth()) / 2,
        (int) (getToolkit().getScreenSize().getHeight() - this.getSize().getHeight()) / 2);
    }

    private void initComponents() {

        additionalPanel = new javax.swing.JPanel();
        JScrollPane jScrollPane2 = new javax.swing.JScrollPane();
        additionalMessagePane = new javax.swing.JTextPane();

        JPanel lastMessagePanel = new javax.swing.JPanel();
        JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        output = new javax.swing.JTextPane();
        JPanel buttonPanel = new javax.swing.JPanel();
        JPanel saveButtonPanel = new javax.swing.JPanel();
        JButton saveButton = new JButton("Save");
        previousMessagesButton = new javax.swing.JToggleButton("Previous Messages");
        detailsButton = new javax.swing.JToggleButton("Details");

        setTitle("Messages");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        additionalMessagePane.setEditable(false);
        jScrollPane2.setDoubleBuffered(true);
        jScrollPane2.setPreferredSize(new java.awt.Dimension(400, 200));
        jScrollPane2.setViewportView(additionalMessagePane);
        additionalPanel.setLayout(new java.awt.BorderLayout());
        additionalPanel.setBorder(BorderFactory.createEmptyBorder(0, 11, 0, 11));
        additionalPanel.add(jScrollPane2, java.awt.BorderLayout.CENTER);
        
        saveButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT,5,5));
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        saveButtonPanel.add(saveButton);
        additionalPanel.add(saveButtonPanel, java.awt.BorderLayout.SOUTH);
        
        getContentPane().add(additionalPanel, java.awt.BorderLayout.SOUTH);
        additionalPanel.setVisible(false);

        jScrollPane1.setDoubleBuffered(true);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(400, 100));
        output.setEditable(false);
        ListenerWindow.popupEditMenuFor(output);
        jScrollPane1.setViewportView(output);
        output.getAccessibleContext().setAccessibleName("Text of the Last Error");
        lastMessagePanel.setLayout(new java.awt.BorderLayout());
        lastMessagePanel.setBorder(BorderFactory.createEmptyBorder(12, 11, 0, 11));
        lastMessagePanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT,5,5));

        detailsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                detailsButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(detailsButton);

        previousMessagesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousMessagesButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(previousMessagesButton);
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitForm(null);
            }
        });
        buttonPanel.add(closeButton);

        lastMessagePanel.add(buttonPanel, java.awt.BorderLayout.SOUTH);
        getContentPane().add(lastMessagePanel, java.awt.BorderLayout.CENTER);
        
        setOutputText(currentMode, currentMessage);
        if(currentDetails == null){
            detailsButton.setVisible(false);
        }
        /* pushLastMessage();
        currentMessage = null;
        currentMode = null;
        currentDetails = null;
*/
        pack();
    }

    private void detailsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if(detailsButton.isSelected()){
            previousMessagesButton.setSelected(false);
            //additionalMessagePane.getEditorKit().createDefaultDocument();
            additionalMessagePane.setDocument(new DefaultStyledDocument());
            additionalMessagePane.setForeground(Color.BLACK);
            if(currentDetails != null){
                additionalMessagePane.setText(currentDetails);
            }
            additionalPanel.setVisible(true);
        } else {
            additionalPanel.setVisible(false);
        }
        pack();
    }

    private void previousMessagesButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if(previousMessagesButton.isSelected()){
            detailsButton.setSelected(false);
            if(additionalMessagePane.getDocument() != messagesDocument){
                additionalMessagePane.setDocument(messagesDocument);
            }
            scrollToEndOfDocument();
            additionalPanel.setVisible(true);
        } else {
            additionalPanel.setVisible(false);
        }
        pack();
    }

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser();
        int returnValue = chooser.showSaveDialog(this);
        if(returnValue == JFileChooser.APPROVE_OPTION){
            File file = chooser.getSelectedFile();
            try{
                FileWriter fileWriter = new FileWriter(file);
                output.write(fileWriter);
                additionalMessagePane.write(fileWriter);
                fileWriter.close();
            } catch(IOException e){
                JOptionPane.showMessageDialog(this, "Error writing to file", "", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exitForm(java.awt.event.WindowEvent evt) {
        setVisible(false);
        dispose();
    }
    
    public void addWarning(String mode, String warning){
        addWarning(mode, warning, null);
    }
    
    public void addWarning(String mode, String warning, String details){
        pushLastMessage();

        currentMessage = warning;
        currentMode = mode;
        currentDetails = details;
        setOutputText(mode, warning);
        
        if(details == null){
            detailsButton.setVisible(false);
            detailsButton.setSelected(false);
        } else {
            detailsButton.setVisible(true);
            if(detailsButton.isSelected()){
                additionalMessagePane.setDocument(new DefaultStyledDocument());
                additionalMessagePane.setForeground(Color.BLACK);
//                additionalMessagePane.getEditorKit().createDefaultDocument();
                if(currentDetails != null){
                    additionalMessagePane.setText(currentDetails);
                }
            }
        }
    }
    
    protected void pushLastMessage(){
        if(currentMessage != null){
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            if(currentMode != null){
                if(currentMode.equals(OutputFrame.WARN_MODE) || currentMode.equals(OutputFrame.ERROR_MODE)){
                    attributes.addAttribute(javax.swing.text.StyleConstants.ColorConstants.Foreground, java.awt.Color.RED);
                }
            }
            try {
                messagesDocument.insertString(messagesDocument.getLength(), currentMessage+"\n", attributes);
            }
            catch (BadLocationException ble) {
            }
        }
    }
    
    public class DocumentOutputStream extends OutputStream {
        private Document doc;
        
        public DocumentOutputStream(Document doc) {
            this.doc = doc;
        }
        
        public void write(int b) throws IOException {
            byte[] one = new byte[1];
            one[0] = (byte) b;
            write(one, 0, 1);
        }
        
        public void write(byte b[], int off, int len) throws IOException {
            try {
                doc.insertString(doc.getLength(),
                new String(b, off, len), null);
                scrollToEndOfDocument();
            }
            catch (BadLocationException ble) {
                throw new IOException(ble.getMessage());
            }
        }
    }
    
    private void scrollToEndOfDocument(){
        try{
            Document doc = additionalMessagePane.getDocument();
            additionalMessagePane.setCaretPosition(doc.getLength());
        } catch(IllegalArgumentException e){
        }
    }
    
    protected void setOutputText(String mode, String warning){
        if(warning != null){
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            if(mode.equals(OutputFrame.WARN_MODE) || mode.equals(OutputFrame.ERROR_MODE)){
                attributes.addAttribute(javax.swing.text.StyleConstants.ColorConstants.Foreground, java.awt.Color.RED);
            }
            try {
                output.setText("" /*annoying: systemDescription*/);
                Document doc = output.getDocument();
                doc.insertString(doc.getLength(), warning + "\n", attributes);
            }
            catch (BadLocationException ble) {
            }
        }
    }
    
    static String getSystemDescription(){
        String version = "", osName="", osVersion="", osArchitecture="" ; 
        try{ version = System.getProperty("java.version",""); } catch(Exception e){}
        try{ osName = System.getProperty("os.name",""); } catch(Exception e){}
        try{ osVersion = System.getProperty("os.version",""); } catch(Exception e){}
        try{ osArchitecture = System.getProperty("os.arch",""); } catch(Exception e){}
        return "Java version: "+ version + "\nOS: " + osName + " " + 
                        osVersion + " " + osArchitecture + "\n";
    }

    // Dialog components
    private javax.swing.JTextPane additionalMessagePane;
    private javax.swing.JPanel additionalPanel;
    private javax.swing.JToggleButton detailsButton;
    private javax.swing.JTextPane output;
    private javax.swing.JToggleButton previousMessagesButton;

    
    private static javax.swing.text.Document messagesDocument = new DefaultStyledDocument();
    private static String currentMessage; //="";
    private static String currentMode; //=INFO_MODE;
    private static String currentDetails;
}
