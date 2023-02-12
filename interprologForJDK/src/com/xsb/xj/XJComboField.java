package com.xsb.xj;
import java.util.ArrayList;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.util.VariableNode;
import com.xsb.xj.util.XJException;

/** An atomic field restricted by a combo box, and keeping as data only its selected value 
gt(_,[class='com.xsb.xj.XJComboField',combobox(b,[a,b,c])],_)
Optional properties: editable,optional

*/
@SuppressWarnings("serial")
public class XJComboField extends XJComboBox {
	GUITerm myGT;
	public XJComboField(PrologEngine engine, GUITerm gt){
		super(engine,prepareGT(engine,gt));
		myGT = gt;
	}
	static GUITerm prepareGT(PrologEngine engine,GUITerm GT){
		TermModel combo = GT.findProperty(GUITerm.COMBOBOX);
		if (combo==null)
			throw new XJException("Missing property "+GUITerm.COMBOBOX);
		TermModel[] props;
		if (GT.findProperty(GUITerm.OPTIONAL)!=null)
			props = new TermModel[]{new TermModel(GUITerm.ATOM),new TermModel(GUITerm.OPTIONAL),new TermModel(GUITerm.BORDERLESS)};
		else
			props = new TermModel[]{new TermModel(GUITerm.ATOM),new TermModel(GUITerm.BORDERLESS)};
		// list(gt(_,[atom,borderless],[])):
		GUITerm template = new GUITerm(
			new TermModel(new VariableNode(0)),
			props,
			new TermModel[0],
			false);
		TermModel list = new TermModel("list",new TermModel[]{template});
				
		ArrayList<TermModel> props2Bag = new ArrayList<TermModel>();
		props2Bag.add(list);props2Bag.add(new TermModel(GUITerm.PLAINCOMBO));
		
		if (GT.findProperty(GUITerm.EDITABLE)!=null)
			props2Bag.add(new TermModel(GUITerm.EDITABLE));
		TermModel tiptext = GT.findProperty(GUITerm.TIP);
		if (tiptext!=null)
			props2Bag.add(new TermModel("=",new TermModel[]{new TermModel(GUITerm.TIP),tiptext}));
		
		if (GT.findProperty(GUITerm.ROOT)!=null)
			props2Bag.add(new TermModel(GUITerm.ROOT));
		// Copy all operations.... although only selectionChanged will be relevant
		for (TermModel op : GT.findProperties(GUITerm.OPERATION))
			props2Bag.add(op);

		TermModel width = GT.findProperty(GUITerm.WIDTH);
		if (width!=null)
			props2Bag.add(new TermModel("=",new TermModel[]{new TermModel(GUITerm.WIDTH),width}));
		
		TermModel[] props2 = props2Bag.toArray(new TermModel[0]);
		// System.out.println("props2:"+java.util.Arrays.toString(props2));
		GUITerm realGT = new GUITerm(".",props2, combo.children,true);
		realGT.checkRoots();
		//System.out.println("realGT:"+realGT);
		return realGT;
	}
    public GUITerm getGT(){
        return myGT;
    }
	public void setGT(GUITerm gt){
		throw new XJException("Inadmissible setGT");
	}
	public boolean loadFromGUI() {
		if (super.loadFromGUI()){
			TermModel X = getSelectedTerms()[0];
			myGT.setNodeValue(X.node);
			return true;
		} else return false;
	}
	public void refreshGUI(){
		setSelectedItem(new TermModel(myGT.node));
	}
	/** Hack to avoid complications in the superclass*/
	public void constructionEnded(){}
}