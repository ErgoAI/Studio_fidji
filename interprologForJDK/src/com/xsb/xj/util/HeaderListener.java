package com.xsb.xj.util;

import com.declarativa.interprolog.TermModel;
import com.xsb.xj.GUITerm;
import com.xsb.xj.XJAbstractListModel;
import com.xsb.xj.XJComponent;
import com.xsb.xj.renderers.SortButtonRenderer;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

/**
 * Listens to clicks on table headers through mouse listener. When clicked
 * changed SortButtonRenderer state and resorts the table by calling prolog.
 * Original source code from http://www2.gol.com/users/tame/swing/examples/JTableExamples5.html
 *
 *@author    Harpreet Singh
 *@version   $Id: HeaderListener.java,v 1.8 2004/04/29 20:01:44 tvidrevich Exp $
 */
public class HeaderListener extends MouseAdapter {
    JTable table;
    JTableHeader header;
    SortButtonRenderer renderer;
    Vector<Integer> indexLookup = null;
    Hashtable<String,TermModel> ascHash  = new Hashtable<String,TermModel>();
    Hashtable<String,TermModel> descHash  = new Hashtable<String,TermModel>();

    /**
     *@param header                XJTable header
     *@param renderer              SortButtonRenderer keeps the old state of the
     *      column
     *@param sortingSpecification  Users can provide specifications on what to
     *      do when a paticular column enters a asc or desc state. The
     *      specifications for each column must be of the form f(ColNum, [Asc
     *      Specs], [Desc Specs]). If Desc specs are not provided asc specs are
     *      used with directions reversed. If no specs are provided for a column
     *      asc(ColNum) and desc(ColNum) is used.
     *@param rendererTemplate      Template describing the columns in the table.
     *      If the table is polimorphic the first template is used!!!!!
     */
    public HeaderListener(JTableHeader header, SortButtonRenderer renderer,
                          TermModel sortingSpecification, TermModel rendererTemplate) {

        this.header = header;
        this.renderer = renderer;
        this.table = this.header.getTable();

        constructSortHash(sortingSpecification);
        constructIndexLookup(rendererTemplate);
    }

    public void mouseClicked(MouseEvent e) {
        if(header.getCursor().getType() == Cursor.E_RESIZE_CURSOR ||
            header.getCursor().getType() == Cursor.W_RESIZE_CURSOR) {
            return;
        }

        int viewColumnIndex   = header.columnAtPoint(e.getPoint());
        int modelColumnIndex  = table.convertColumnIndexToModel(viewColumnIndex);
        if (modelColumnIndex==-1)
        	return;
        Integer gtColumn      = indexLookup.get(modelColumnIndex);
        TableColumn column    = table.getColumnModel().getColumn(viewColumnIndex);
        TermModel sortModel   = null;
        String direction;

        //System.out.println("view:model:gt:" + viewColumnIndex + ":" + modelColumnIndex + ":" + gtColumn);
        if(SortButtonRenderer.DOWN == renderer.getNextState(column)) {
            direction = "desc";
            sortModel = (TermModel) descHash.get(gtColumn.toString());
        } else {
            direction = "asc";
            sortModel = (TermModel) ascHash.get(gtColumn.toString());
        }

        if(sortModel == null) {
            //System.out.println("sort model not in hash using default:" + modelColumnIndex + ":" + gtColumn);
            sortModel =
                ((XJAbstractListModel) table.getModel()).buildSortTerm(direction, gtColumn);
        }

        if(!sortModel.isListEnd()) {
            //renderer.setPressedColumn(col);
            //System.out.println("setting selected column:" + viewColumnIndex);
            renderer.setSelectedColumn(column);
            header.repaint();

            if(table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            //System.out.println("settings sort term :" + sortModel.toString());
            ((XJAbstractListModel) table.getModel()).setSortTerm(sortModel);
        }
    }

    /**
     * Looks through the rendering template for the list and constructs a lookup
     * to give a mapping between visible columns and the column number in the
     * specifications So in the end indexLookup.get(visibleColumn) returns the
     * index in prolog for that column. E.g. Invisible, Invisible, Visible,
     * Visible; indexLookup looks like [2,3]. If the list is polymorphic takes
     * the first rendering template as a guide!!
     *
     *@param rendererTemplate  Description of the Parameter
     */
    private void constructIndexLookup(TermModel rendererTemplate) {
        GUITerm template  = null;

        if(rendererTemplate.isList()) {
            template = (GUITerm) rendererTemplate.getChild(0);
        } else {
            template = (GUITerm) rendererTemplate;
        }

        indexLookup = new Vector<Integer>();

        for(int i = 0; i < template.getChildCount(); i++) {
            GUITerm child         = (GUITerm) template.getChild(i);
            // goal term might have compound arguments
            // that span several column when visually represented
            // though correspond to a single index in the goal
            XJComponent[] renderers  = child.collectSignificantRenderers();
            for(int j = 0; j < renderers.length; j++){
            	// TODO: this is not dealing properly with templates containing non persistent nodes; clicks 
            	// cause sorting of some adjacent column instead; unfortunately the commented change below does NOT fix this...
                if(renderers[j] != null && !renderers[j].getGT().isConstant() /*&& !renderers[j].getGT().isNonPersistent()*/) {
                    //prolog programmer starts counting from 1
                    indexLookup.add(new Integer(i + 1));
                    //System.out.println("indexLookup:" + column + ":" + (i+1));                    
                }
            }
        }
    }
    
    /**
     * sortingSpecification can be of the form [f(1,[asc(1)],[desc(1)]),
     * f(3,[asc(3),asc(1)])]. Where 1 and 3 and are the visible columns. The
     * first list is the specification when ascending direction is requested and
     * the second is for descending. If no descending specification is provided
     * the ascending specification is reversed. An empty list specification
     * signifies no sorting on the column. If either of the specifications is an
     * empty list both are specifications are saved in the hash as empty. This
     * is because it would not be possible to resort in the allowed direction
     * after hitting a second column! If no list of specifications is provided
     * nothing is added to the hash and simple sorting specifications are used.
     * sortingSpecification is the sortable term in the gt. Sortable term can be
     * either "sortable" or sortable([X|_]).
     *
     *@param sortingSpecification  TermModel containing specifications
     */
    private void constructSortHash(TermModel sortingSpecification) {
        if(sortingSpecification != null && sortingSpecification.getChildCount() == 1) {
            //get only argumnet of this term which is a list.
            TermModel[] sortingSpecs  = TermModel.flatList((TermModel) sortingSpecification.getChild(0));

            for(int i = 0; i < sortingSpecs.length; i++) {
                TermModel element   = sortingSpecs[i];

                String column       = element.getChild(0).toString();
                TermModel ascSpec   = (TermModel) element.getChild(1);
                TermModel descSpec  = null;

                //if one is [] then both are [].
                if(ascSpec.isListEnd()) {//node.equals("[]");
                    descSpec = (TermModel) ascSpec.clone();
                } else {
                    if(element.getChildCount() == 3) {
                        descSpec = (TermModel) element.getChild(2);
                        if(descSpec.isListEnd()) {
                            ascSpec = (TermModel) descSpec.clone();
                        }
                    } else {
                        descSpec = reverseDir(ascSpec);
                    }
                }

                //System.out.println("adding ascHash : " + column + ":" + ascSpec.toString());
                //System.out.println("adding descHash : " + column + ":" + descSpec.toString());
                if(ascSpec.isList()) {
                    ascHash.put(column, ascSpec);
                } else {
                    System.err.println("Sorting specifications must be lists:" + ascSpec.toString());
                }

                if(descSpec.isList()) {
                    descHash.put(column, descSpec);
                } else {
                    System.err.println("Sorting specifications must be lists:" + descSpec.toString());
                }
            }
        }
    }

    /**
     * If desc specification is not provided reverses the direction of the
     * directives in the asc specifications.
     *
     *@param ascSpecList  Specifications for ascending sort is requested.
     *@return             Ascending specifications reversed
     */
    private TermModel reverseDir(TermModel ascSpecList) {
        if(ascSpecList.isList()) {
            TermModel[] descSpecArray  = TermModel.flatList(((TermModel) ascSpecList.clone()));

            for(int i = 0; i < descSpecArray.length; i++) {
                TermModel descSpec  = descSpecArray[i];
                if(descSpec.node.equals("desc")) {
                    descSpec.node = "asc";
                } else {
                    descSpec.node = "desc";
                }
            }

            return TermModel.makeList(descSpecArray);
        } else {
            System.err.println("specification must be in a list");
            return null;
        }
    }
}
