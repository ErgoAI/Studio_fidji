package com.xsb.xj.util;
import com.declarativa.interprolog.*;
import com.xsb.xj.*;
import java.awt.datatransfer.*;
import java.io.*;
import javax.swing.*;

/** Acts as representation class for xjSelectionFlavor */
public class TransferableXJSelection implements Transferable{
	public static final DataFlavor xjSelectionFlavor = new DataFlavor(TransferableXJSelection.class,"XJ dragged term data");
	// Not your perfect OO design, but it's just two cases, and a reasonably localized... "union" or "variant"
	// These two for drags from XJTemplateComponents:
	public final JComponent source;
	public final TermModel[] selectedTerms;
	// These two for drags from other nodes:
	public final TermModel wholeTerm;
	public final TermModel path;
	
	public TransferableXJSelection(JComponent source,TermModel[] selectedTerms){
		wholeTerm=null; path=null;
		this.source = source; this.selectedTerms=selectedTerms;
		if (!wasDraggedFromTreeOrList()) throw new XJException("Bad construction of TransferableXJSelection 1");
	}
	
	public TransferableXJSelection(TermModel term,TermModel pathToNode){
		wholeTerm=term; path=pathToNode;
		this.source = null; this.selectedTerms=null;
		if (wasDraggedFromTreeOrList()) throw new XJException("Bad construction of TransferableXJSelection 2");
	}
	
	public DataFlavor[] getTransferDataFlavors(){
		return new DataFlavor[]{ xjSelectionFlavor, DataFlavor.stringFlavor };
	}
	
	public boolean isDataFlavorSupported(DataFlavor flavor){
		return (flavor.equals(xjSelectionFlavor) || flavor.equals(DataFlavor.stringFlavor));
	}
	
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException{
		if (flavor.equals(xjSelectionFlavor)) return this;
		else if (flavor.equals(DataFlavor.stringFlavor)) return toString();
		else throw new UnsupportedFlavorException(flavor);
	}
	
	public boolean wasDraggedFromTreeOrList(){
		return source!=null && selectedTerms!=null && wholeTerm==null && path==null;
	}
	
	public String toString(){
		if (wasDraggedFromTreeOrList()) 
			return TermModel.makeList(selectedTerms)+" terms dragged from "+source;
		else 
			return "Node with path "+path+" in term "+wholeTerm;
	}
	
	public TermModel makeDroppedTerm(){
		if (wasDraggedFromTreeOrList()){
			PrologEngine engine = ((XJComponent)source).getEngine();
			int sourceRef = engine.registerJavaObject(source);
			TermModel left = new TermModel(sourceRef);
			TermModel right = TermModel.makeList(selectedTerms);
			return new TermModel("terms",new TermModel[]{left,right});
		} else
			return new TermModel("term",new TermModel[]{wholeTerm,path});
	}
}