package com.xsb.xj.util;
// grabbed from http://www.javaworld.com/javatips/jw-javatip77.html?083099txt
// modified for XJ
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 * ExcelAdapter enables Copy-Paste Clipboard functionality on JTrees.
 * The clipboard data format used by the adapter is compatible with
 * the clipboard format used by Excel. This provides for clipboard
 * interoperability between enabled JTrees and Excel.
 * http://www.javaworld.com/javatips/jw-javatip77.html?083099txt
 */
public class TreeExcelAdapter implements ActionListener {
    private Clipboard system;
    private StringSelection stsel;
    private JTree theJTree ;
    
    /**
     * The Excel Adapter is constructed with a
     * JTree on which it enables Copy-Paste and acts
     * as a Clipboard listener.
     */
    
    
    public TreeExcelAdapter(JTree myJTree) {
        theJTree = myJTree;
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.CTRL_MASK,false);
        
        // Identifying the copy KeyStroke user can modify this
        // to copy on some other Key combination.
        KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.CTRL_MASK,false);
        
        // Identifying the Paste KeyStroke user can modify this
        //to copy on some other Key combination.
        
        theJTree.registerKeyboardAction(this,"Copy",copy,JComponent.WHEN_FOCUSED);
        
        
        theJTree.registerKeyboardAction(this,"Paste",paste,JComponent.WHEN_FOCUSED);
        
        system = Toolkit.getDefaultToolkit().getSystemClipboard();
    }
    
    /**
     * Public Accessor methods for the Tree on which this adapter acts.
     */
    public JTree getJTree() {return theJTree;}
    
    public void setJTree(JTree theJTree) {this.theJTree=theJTree;}
    
    /**
     * This method is activated on the Keystrokes we are listening to
     * in this implementation. Here it listens for Copy and Paste ActionCommands.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().compareTo("Copy")==0) {
            StringBuffer sbf=new StringBuffer();
            
            TreeCellRenderer renderer = theJTree.getCellRenderer();
            TreePath[] paths = theJTree.getSelectionPaths();
            if (paths != null) {
                for (int p=0;p<paths.length;p++) {
                    Object node = paths[p].getLastPathComponent(); // node is of type LazyTreeModel.LazyTreeNode
                    
                    Component cellComponent =
                    renderer.getTreeCellRendererComponent(theJTree, node, true, true, true, 0, true);
                    sbf.append(getStringForComponent((JComponent)cellComponent));
                    sbf.append("\n");
                    
                }
                
                stsel = new StringSelection(sbf.toString());
                system = Toolkit.getDefaultToolkit().getSystemClipboard();
                system.setContents(stsel,stsel);
            }
        }
        
        if (e.getActionCommand().compareTo("Paste")==0) {
            JOptionPane.showMessageDialog(null, "Paste not supported",
            "Paste not supported",
            JOptionPane.ERROR_MESSAGE);
            return;
                          /*
                  System.out.println("Trying to Paste");
                  int startRow=(theJTree.getSelectedRows())[0];
                  int startCol=(theJTree.getSelectedColumns())[0];
                  try
                  {
                     String trstring= (String)(system.getContents(this).getTransferData(DataFlavor.stringFlavor));
                           
                     System.out.println("String is:"+trstring);
                     StringTokenizer st1=new StringTokenizer(trstring,"\n");
                     for(int i=0;st1.hasMoreTokens();i++)
                     {
                        rowstring=st1.nextToken();
                        StringTokenizer st2=new StringTokenizer(rowstring,"\t");
                           
                        for(int j=0;st2.hasMoreTokens();j++)
                        {
                           value=(String)st2.nextToken();
                           if (startRow+i< theJTree.getRowCount() &&
                               startCol+j< theJTree.getColumnCount())
                              theJTree.setValueAt(value,startRow+i,startCol+j);
                           System.out.println("Putting "+ value+"at row="+startRow+i+"column="+startCol+j);
                       }
                    }
                 }
                 catch(Exception ex){ex.printStackTrace();}
                           */
        }
    }
    
    /**
     * Very primitive method, it would be better if each of XJComponents would 
     * be required to overwrite toString method.
     */
    public static StringBuffer getStringForComponent(JComponent comp){
        if(comp instanceof JTextComponent){
            return new StringBuffer(((JTextComponent)comp).getText());
        } else if(comp instanceof JLabel){
            return new StringBuffer(((JLabel)comp).getText());
        } else {
            Component[] subComponents  = comp.getComponents();
            if(subComponents != null){
                StringBuffer buffer = new StringBuffer();
                for (int i = 0; i < subComponents.length; i++){
                    if(subComponents[i] instanceof JComponent){
                       buffer.append(getStringForComponent((JComponent)subComponents[i]));
                    }
                }
                return buffer;
            } else {
                return new StringBuffer();
            }
        }
    }
}
