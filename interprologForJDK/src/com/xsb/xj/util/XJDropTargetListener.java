package com.xsb.xj.util;
import com.xsb.xj.*;
import com.declarativa.interprolog.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.Component;
import javax.swing.*;

/** This class knows a bit too much about XJTree and XJTable */
public class XJDropTargetListener implements DropTargetListener{
    XJComponent xjc;
    XJAction[] dropped;
//    JPopupMenu menu;
    
    public XJDropTargetListener(XJComponent xjc,XJAction[] dropped){
        this.xjc=xjc;
        this.dropped=dropped;
//        if(dropped.isDroppedMenuOperation()){
//            menu = new JPopupMenu("Operations");
//            JMenuItem [] menuItems = dropped.buildDropActionMenus();
//            for(int i = 0; i < menuItems.length; i++){
//                menu.add(menuItems[i]);
//            }
//        } else menu = null;
    }
    public void dragOver(DropTargetDragEvent dtde){}
    public void dropActionChanged(DropTargetDragEvent dtde){}
    public void dragExit(DropTargetEvent dte){}
    
    public void drop(DropTargetDropEvent dtde){
        boolean found = false;
        // System.out.println("drop:"+dtde);
        try{
            Transferable transferable = dtde.getTransferable();
            if (dtde.getDropAction()==DnDConstants.ACTION_COPY &&
                transferable.isDataFlavorSupported(TransferableXJSelection.xjSelectionFlavor)){
                TransferableXJSelection txjs = (TransferableXJSelection)transferable.getTransferData(TransferableXJSelection.xjSelectionFlavor);
		TermModel droppedTerm = txjs.makeDroppedTerm();
                for(int i = 0; (i < dropped.length) && !found ; i++){
		if(dropped[i].isAcceptableDnDTerm(droppedTerm)){
                    found = true;
		    boolean acceptsDrop = xjc.loadFromGUI();
                // System.out.println("acceptsDrop=="+acceptsDrop);
                if (acceptsDrop){
                    if(xjc instanceof XJTable){
                        TermModel hit = ((XJTable)xjc).point2Term(dtde.getLocation());
                        if (hit!=null) dropped[i].setSelectedTerms(new TermModel[]{hit});
                        else dropped[i].setSelectedTerms(new TermModel[0]);
                    } else if (xjc instanceof XJTree){
                        TermModel hit = ((XJTree)xjc).point2Term(dtde.getLocation());
                        if (hit!=null) dropped[i].setSelectedTerms(new TermModel[]{hit});
                        else dropped[i].setSelectedTerms(new TermModel[0]);
                    }
                    dropped[i].setDroppedTerm(droppedTerm);
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    dtde.getDropTargetContext().dropComplete(true);
                    if(dropped[i].isDroppedMenuOperation()){
                        JPopupMenu menu = new JPopupMenu("Operations");
                        JMenuItem [] menuItems = dropped[i].buildDropActionMenus();
                        for(int j = 0; j < menuItems.length; j++){
                            menu.add(menuItems[j]);
                        }
                        menu.show((Component)xjc, (int)dtde.getLocation().getX(),(int)dtde.getLocation().getY());
                    } else {
                        dropped[i].doit(); // Execute the Prolog operation
                    }
                } else dtde.rejectDrop();
		}
                }
	    } else dtde.rejectDrop();
        } catch (Exception e){
            throw new XJException("Problems dropping:"+e);
        }
    }
    
    
    public void dragEnter(DropTargetDragEvent dtde){
        //System.out.println("dragEnter:"+dtde);
        //System.out.println("Component isEnabled:"+((Component)xjc).isEnabled());
    }
}