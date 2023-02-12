package com.xsb.xj;
import com.declarativa.interprolog.*;
import com.xsb.xj.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;

/** A simple dispatcher of TreeCellRenderer messages to its bunch of XJCellTupleRenderers, 
one for each tree template.
The same approach works with lists */
class PolimorphicRenderer implements TreeCellRenderer, TableCellRenderer{
	Hashtable<String,XJCellTupleRenderer> renderers;
	PolimorphicRenderer(PrologEngine engine,GUITerm[] templates,boolean DRY){
		renderers = new Hashtable<String,XJCellTupleRenderer>(templates.length);
		for(int t=0;t<templates.length;t++){
			String key = templates[t].findProperty(GUITerm.TYPENAME).toString();
			if (key==null) throw new XJException("typename property missing in "+templates[t]);
			renderers.put(key,new XJCellTupleRenderer(engine,templates[t],DRY));
		}
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value, 
		boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus){
		
		LazyTreeModel.LazyTreeNode treenode = (LazyTreeModel.LazyTreeNode)value;
		XJCellTupleRenderer renderer = (XJCellTupleRenderer)renderers.get(treenode.getType());
                if(renderer == null){
                    throw new XJException("GT for typename "+treenode.getType()+" does not exist");
                }
        // System.out.println("Preparing renderer for value "+value);
		Component c = renderer.getTreeCellRendererComponent(tree,value,selected,expanded,leaf,row,hasFocus);
        // System.out.println("Renderer  is "+c);
		// always 0...System.out.println("renderer size:"+c.getSize());
		return c;
	} 
        
    public Component getTableCellRendererComponent(JTable table,Object value /* a TermModel */ ,
		boolean isSelected,boolean hasFocus,int row,int column){
			if(value != null){
				XJCellTupleRenderer renderer = (XJCellTupleRenderer)renderers.get(((TermModel)value).getChild(0).toString());
				if(renderer == null){
					throw new XJException("GT for typename "+((TermModel)value).getChild(0).toString()+" does not exist");
				}
				Component c = renderer.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
				return c;
			} else return null;
	}

}