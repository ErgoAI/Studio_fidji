package com.xsb.xj;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.xj.util.XJException;

/**
 * The renderer for both XJ lists and (single node type) trees; polimorphic
 * trees use this too, through PolimorphicRenderer. It keeps prototype
 * XJComponents to later be "rubber-stamped" into the JTree nodes and JTable
 * cells. Each cell/node XJComponent is built with GUITerm.makeGUI as in other
 * XJ contexts. <br>
 * If rendering performance becomes an issue in the future, it may probably be
 * addressed by following the recommendations in
 * http://java.sun.com/j2se/1.3/docs/api/javax/swing/table/DefaultTableCellRenderer.html
 * and http://java.sun.com/j2se/1.3/docs/api/javax/swing/tree/DefaultTreeCellRenderer.html;
 * for this it suffices to define a new "TreeAndListAware extends XJComponent"
 * interface with a void setupForListsAndTrees() method, which performance hog
 * XJComponents should implement. This method should set a inListOrTree flag,
 * and then to define each method validate, revalidate, etc as the single
 * instruction: if (!inListOrTree) super.method
 *
 *@version   $Id: XJCellTupleRenderer.java,v 1.13 2004/08/22 20:44:43 tvidrevich Exp $
 */

class XJCellTupleRenderer implements ListCellRenderer<Object>, TableCellRenderer, TreeCellRenderer, ActionListener {
    GUITerm[] cellGTs;
    TermModel[] specifiedColor;
    //JPanel overTopComponent;
    JButton repeatedArrow;
    XJComponent topComponent;
    XJComponent[] cellComponents;
    Color[] originalBackgrounds;
    Color[] originalForegrounds;
    Font[] originalFonts;

    /**
     * template for the list/tree tuples
     */
    GUITerm template;
    TermModel currentTuple;

    //To get standard colors etc.
    private static final DefaultTreeCellRenderer standardTreeRenderer  = new DefaultTreeCellRenderer();
    protected static Border noFocusBorder                              = new EmptyBorder(1, 1, 1, 1);
    
    // for empty ComboBoxes 
    // for now not an XJComponent - change if necessary
    private JLabel emptyLabel = new JLabel("");

    HashMap<String,TreePath> nodesShown;
    
    /**
     * A cell renderer for a single tuple type, for use in lists (tables) and
     * mono-typed trees
     *
     *@param engine  PrologEngine
     *@param gt      Template guiterm
     */
    XJCellTupleRenderer(PrologEngine engine, GUITerm gt, boolean DRY) {
        if (DRY) 
        	nodesShown = new HashMap<String,TreePath>(2*LazyTreeModel.REFRESH_PAGE);
        else nodesShown = null;
        template = gt;
        topComponent = template.makeGUI(engine);
        // overTopComponent = new JPanel(new BorderLayout());
        //overTopComponent = new JPanel(new BorderLayout());
        //overTopComponent.add((Component)topComponent,BorderLayout.CENTER);
        repeatedArrow = null;
        if (((Container)topComponent).getLayout() instanceof FlowLayout){
        	if (DRY){
				repeatedArrow= new JButton(XJTree.repeatArrow);
				repeatedArrow.setContentAreaFilled(false);
				repeatedArrow.setBorderPainted(false);
				repeatedArrow.setMargin(new Insets(0,0,0,0));
				repeatedArrow.setSize(20,20);
				repeatedArrow.addActionListener(this); // not enough...
        		((Container)topComponent).add(repeatedArrow);
        		repeatedArrow.setVisible(false);
        	}
        } else if (DRY)
        	throw new XJException("'dry' display of repeated tree nodes requires node template to use FlowLayout on its top container");
        	        
        cellComponents = gt.collectSignificantRenderers();
        cellGTs = new GUITerm[cellComponents.length];

        for(int c = 0; c < cellComponents.length; c++) {
            cellGTs[c] = cellComponents[c].getGT();
        }
        ignoreConstantNodes();

        originalBackgrounds = new Color[cellComponents.length];
        originalForegrounds = new Color[cellComponents.length];
        originalFonts = new Font[cellComponents.length];
        specifiedColor = new TermModel[cellComponents.length];

        for(int c = 0; c < cellComponents.length; c++) {
            originalBackgrounds[c] = ((Component) (cellComponents[c])).getBackground();
            originalForegrounds[c] = ((Component) (cellComponents[c])).getForeground();
            originalFonts[c] = ((Component) (cellComponents[c])).getFont();

            //cellgts.length == cellcomponents.legnth   **see ignoreconstantnodes
            specifiedColor[c] = cellGTs[c].findProperty(GUITerm.COLOR);
        }
        currentTuple = null;
    }

    boolean isDRY(){
    	return nodesShown!= null;
    }

    // not very pretty; same as method in XJCellTupleEditor
    void ignoreConstantNodes() {
        int newIndex  = 0;
        int i;
        for(i = 0; i < cellGTs.length; i++) {
            if(cellGTs[i].isConstant()) {
                continue;
            }
            cellGTs[newIndex] = cellGTs[i];
            cellComponents[newIndex] = cellComponents[i];
            newIndex++;
        }
        if(newIndex != i) {
            // constant nodes found
            XJComponent[] newCellComponents  = new XJComponent[newIndex];
            GUITerm[] newCellGTs             = new GUITerm[newIndex];
            for(int c = 0; c < newIndex; c++) {
                newCellComponents[c] = cellComponents[c];
                newCellGTs[c] = cellGTs[c];
            }
            cellGTs = newCellGTs;
            cellComponents = newCellComponents;
        }
    }

    void setCurrentTuple(TermModel tuple) {
        //System.out.println("template=="+template+",tuple=="+tuple);

        if(tuple != null && tuple != currentTuple) {
            // very weird conditional to prevent rootless polimorphic XJTrees to crash
            currentTuple = tuple;
            template.assign(currentTuple);
            template.refreshRenderers();
        }
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int viewColumnIndex) {
        // Now some final fiddling, inspired by javax/swing/table/DefaultTableCellRenderer.java
        //JComponent component  = (JComponent) cellComponents[column];
        int modelColumnIndex  = table.convertColumnIndexToModel(viewColumnIndex);

        JComponent component  = (JComponent) cellComponents[modelColumnIndex];
    	XJComboBox.setFireEvents(component,false);
        // a simple GUITerm version-based 'equals' would allow quick caching here
        setCurrentTuple((TermModel) value);

        if(isSelected) {
            if(specifiedColor[modelColumnIndex] == null) {
                component.setForeground(table.getSelectionForeground());
            } else {
                component.setForeground(originalForegrounds[modelColumnIndex]);
            }
            component.setBackground(table.getSelectionBackground());
            if(originalFonts[modelColumnIndex].isBold()){
                component.setFont(table.getFont().deriveFont(Font.ITALIC|Font.BOLD));
            } else {
                component.setFont(table.getFont().deriveFont(Font.BOLD));
            }
        } else {
            component.setForeground(originalForegrounds[modelColumnIndex]);
            component.setBackground(originalBackgrounds[modelColumnIndex]);
            component.setFont(originalFonts[modelColumnIndex]);
        }

        if(hasFocus) {
            component.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            if(table.isCellEditable(row, viewColumnIndex)) {
                component.setForeground(UIManager.getColor("Table.focusCellForeground"));
                component.setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
        } else {
            component.setBorder(noFocusBorder);
        }
        // ---- begin optimization to avoid painting background ----
        Color back            = component.getBackground();
        boolean colorMatch    = (back != null) && (back.equals(table.getBackground())) && table.isOpaque();
        component.setOpaque(!colorMatch);
        // ---- end optimization to avoid painting background ----
        //XJComboBox.setFireEvents(component,true);
        return component;
    }

    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected, boolean hasFocus) {

        // empty comboboxes return null as a selected value
        if(value == null){return emptyLabel;}
        
        JComponent component  = (JComponent) cellComponents[0];

    	XJComboBox.setFireEvents(component,false);
        // a simple GUITerm version-based 'equals' would allow quick caching here
        setCurrentTuple((TermModel) value);

        // Now some final fiddling, inspired by javax/swing/table/DefaultTableCellRenderer.java
        if(isSelected) {
            component.setForeground(list.getSelectionForeground());
            component.setBackground(list.getSelectionBackground());
        } else {
            component.setForeground(originalForegrounds[0]);
            component.setBackground(originalBackgrounds[0]);
        }
        component.setFont(list.getFont());
        /* ---- begin optimization to avoid painting background ----
         *  Color back            = component.getBackground();
         *  boolean colorMatch = (back != null) &&
         *  back.equals(table.getBackground()) ) && table.isOpaque();
         *  component.setOpaque(!colorMatch);
         */
        // ---- end optimization to avoid painting background ----
    	XJComboBox.setFireEvents(component,true);
       	return component;
    }
    
    static String toString(XJComponent C){
    	if (C instanceof JTextComponent)
    		return ((JTextComponent)C).getText();
    	else if (C instanceof JLabel)
    		return ((JLabel)C).getText();
    	else if (C instanceof ValueRow) return "";
    	else return C.toString();
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        JComponent component                 = (JComponent) topComponent;
        LazyTreeModel.LazyTreeNode node = (LazyTreeModel.LazyTreeNode)value;
        
        setCurrentTuple(node.getNodeTerm());
        
        if (((XJTree.XJTreeView)tree).isDRY()){
        	if (!node.isRepeated()){
				StringBuilder sb = new StringBuilder();
				for(int c = 0; c < cellComponents.length; c++) {
					sb.append(toString(cellComponents[c]));
				} 
				String key = sb.toString();
				TreePath path = nodesShown.get(key);
				if (path==null){
					nodesShown.put(key,LazyTreeModel.findPath(node)); // if memory or speed become a problem, compact this into ints...
					System.out.println("Recorded new node:"+node);
				} else {
					node.youAreArepetition(path);
					System.out.println("Found repeated node:"+node);
				}
			} else System.out.println("Redrawing repeated node:"+node);
			if (node.isRepeated())
				repeatedArrow.setVisible(true);
			else repeatedArrow.setVisible(false);
				
        	
        }
        // Now some final fiddling, inspired by javax.swing.tree/DefaultTreeCellRenderer.java
        String firstTooltip = null;

        for(int c = 0; c < cellComponents.length; c++) {
            JComponent comp  = (JComponent) cellComponents[c];
            if(selected) {
                if(specifiedColor[c] == null) {
                    //if user has specified color for tree node don't paint over it.
                    comp.setForeground(standardTreeRenderer.getTextSelectionColor());
                }
                comp.setBackground(standardTreeRenderer.getBackgroundSelectionColor());
            } else {
                comp.setForeground(originalForegrounds[c]);
                //comp.setBackground(originalBackgrounds[c]);
                comp.setBackground(tree.getBackground());
            }
            comp.setOpaque(true);
            String tip = comp.getToolTipText();
            if (firstTooltip==null && tip!=null && !tip.equals(""))
            	firstTooltip = tip;
        }
        component.setToolTipText(firstTooltip);
        
		// System.out.println("Prepared renderer components for row "+row + ":"+component+", value=="+value);

        /*
         *  for displaying tooltips
         *  called by getToolTipText(MouseEvent event) in JTree
         *  if finer control over tooltips is needed or performance is to be improved
         *  override tree's public String getToolTipText(MouseEvent event) method
         */
         /*
        String toolTipText                   = TreeExcelAdapter.getStringForComponent(component).toString();
        component.setToolTipText(toolTipText);
        AccessibleContext accessibleContext  = component.getAccessibleContext();
        accessibleContext.setAccessibleName("Tree node: " + toolTipText);
*/
        return component;
    }
    
    public void actionPerformed(ActionEvent e){
    	if (e.getSource()==repeatedArrow){
    		System.out.println("Click:-) missing path...");
    	}
    }
}
