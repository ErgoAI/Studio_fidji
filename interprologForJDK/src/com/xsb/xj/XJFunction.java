package com.xsb.xj;
import com.declarativa.interprolog.*;
import com.declarativa.interprolog.util.*;
import com.xsb.xj.util.*;

import java.awt.*;
import java.util.*;

@SuppressWarnings("serial")
class XJFunction extends XJAction{
	TermModel R;
	Runnable afterFunctionResults;
	/** An XJAction encapsulating an XJ function. 
	In some circunstances, such as when our real root is somewhere else (e.g a list) and we're running in threaded mode,
	 we may want to do something after assigning the result of the funtion to our root, 
	hence the afterFunctionResults argument */
	public XJFunction(PrologEngine engine, Component parent, GUITerm target, TermModel root, 
		TermModel operation,Runnable afterFunctionResults
		){
		super(engine,parent, target,root,operation);
		this.afterFunctionResults=afterFunctionResults;
		
		R = (TermModel)operation.getChild(3);
		if (!R.node.equals("result") || R.isLeaf())
			throw new XJException("Bad R argument in function:"+R);
		if (forList){
			if (R.getChildCount()!=1) 
				throw new XJException("function result subterms for lists must have one argument only");
		} else if (R.getChildCount()!=2) 
			throw new XJException("function result subterms must have two arguments");
		if (isSelectionChanged()) 
			throw new XJException("functions can't be triggered by selectionChanged");
		if (isDroppedOperation())
			throw new XJException("functions can't be triggered by DnD");
	}
	
	/** Does nothing, to prevent superclass method to restrict list functions */
	
	void mayListenListSelections(){}
	
	public void run(){ // variation of super method
		/*
		System.out.println("Entered XJFunction.run");
		System.out.println("threaded=="+threaded);
		System.out.println("currentThread:"+Thread.currentThread());
		System.out.println("forList=="+forList);
		System.out.println("target:"+target);
		System.out.println("lambda:"+lambda);
		System.out.println("description:"+description);
		System.out.println("goal:"+goal);
		System.out.println("R:"+R); */
		
		String objSpecs; Object[] objects; String dg;
        IPException exception = null;
		goalWasInterrupted=true;
		goalAborted=true;

		if (forList) {
			if (!(context instanceof XJTable)) 
				// this can not be checked earlier, as actions may have their parents set up after construction
				throw new XJException("Bad list component in XJFunction:"+context);
			Integer listRef = new Integer(engine.registerJavaObject(context));
			TermModel[] selectedTerms = ((XJTable)context).getSelectedTerms();
			objSpecs = "[ListIntObj,SelectedTermsObj,Lambda,Goal,RLambda,LambdaHMspec,DescriptionSpec]";
			objects = new Object[]{listRef,selectedTerms,lambda,goal,R,lambdaHM,description};
			dg = "recoverTermModels([Lambda,Goal,RLambda"+(lambdaHM==null?"":",LambdaHMspec")+"],"+
				"[terms(ListRef,SelectedTerms),G,result(R)"+(lambdaHM==null?"":",LambdaHM")+"]), "+
				"ipObjectSpec('java.lang.Integer',ListIntObj,[ListRef],_), recoverTermModelArray(SelectedTermsObj,SelectedTerms), "+
				(lambdaHM==null?"":"stringArraytoList(DescriptionSpec,LambdaHM), ") +
				(callIfQuickMode?
					"xjCallIfQuick(G,buildTermModelArray(R,Rmodel),Result), (Result=deferred->Rmodel=null;true)":
					"G,buildTermModel(R,Rmodel)");
		} else{
			GUITerm.PathSearch path = new GUITerm.PathSearch();
			TermModel rootTerm = ((GUITerm)(originalRoot)).getTermModel(originalTarget,path);
			objSpecs = "[Root,Path,Lambda,Goal,RLambda,LambdaHMspec,DescriptionSpec]";
			objects = new Object[]{rootTerm,path.getPath(),lambda,goal,R,lambdaHM,description};
			dg = "recoverTermModels([Lambda,Goal,RLambda"+(lambdaHM==null?"":",LambdaHMspec")+"],"+
				"[term(TT,PP),G,R"+(lambdaHM==null?"":",LambdaHM")+"]), "+
				"recoverTermModel(Root,TT), recoverTermModel(Path,PP), "+
				(lambdaHM==null?"":"stringArraytoList(DescriptionSpec,LambdaHM), ") +
				(callIfQuickMode?
					"xjCallIfQuick(G,buildTermModel(R,Rmodel),Result), (Result=deferred->Rmodel=null;true)":
					"G,buildTermModel(R,Rmodel)");
		}
		Object[] bindings;
		try{
			bindings = engine.deterministicGoal(
				rememberMyRef+dg, objSpecs, objects,
				(callIfQuickMode?"[string(Result),Rmodel]":"[Rmodel]"));
		} catch (IPInterruptedException e){
			bindings=null;
			goalWasInterrupted=true;
		} catch (IPAbortedException e){ 
			bindings=null;
			goalWasInterrupted=true;
            exception = e;
		} catch (IPPrologError e){ 
            bindings=null;
            if (e.t instanceof TermModel && ((TermModel)e.t).toString().equals(XJPrologEngine.ERGO_USER_ABORT_HACK))
            	goalWasInterrupted = true;
            else goalAborted=true;
            exception = e;
            e.printStackTrace();
        } catch (IPException e){ 
            bindings=null;
            goalAborted=true;
            exception = e;
        }
		goalEnded(bindings, exception); 
	}

	public void goalEnded(Object[] bindings, IPException exception){
		// System.out.println("Entering XJFunction.goalEnded with bindings=="+bindings);
		boolean currentCallIfQuickMode = this.callIfQuickMode;
		super.goalEnded(bindings, exception);
		if (bindings!=null && outcome!=CANCELLED) {
			Object result = null;
			if (currentCallIfQuickMode && bindings[0].equals(DONE)) result = bindings[1];
			else if (!currentCallIfQuickMode) result = bindings[0];
			/*
			System.out.println("currentCallIfQuickMode=="+currentCallIfQuickMode);
			System.out.println("result=="+result);
			System.out.println("bindings[0]=="+bindings[0]);*/
			if (result!=null){
				if (forList){
					TermModel[] listResult = (TermModel[])result;
					
					/*
					System.out.println("Result of list function:");
					for (int i=0;i<listResult.length;i++)
						System.out.println(listResult[i]);*/
					/* not really:
					if (afterFunctionResults!=null)
						throw new XJException("afterFunctionResults should be null in list function");
						*/
					((XJTable)context).addTerms(listResult);
				} else {
					TermModel resultTM = (TermModel)result;
					TermModel PTR = (TermModel)resultTM.getChild(0);
					//System.out.println("PTR=="+PTR);
					TermModel[] path = TermModel.flatList(PTR);
					Vector<Integer> pv = new Vector<Integer>();
					for (int i=0;i<path.length;i++)
						pv.addElement((Integer)path[i].node);
					GUITerm toReplace = ((GUITerm)originalRoot).subTerm(pv);
					// System.out.println("Term to replace:"+toReplace);
					TermModel RT = (TermModel)resultTM.getChild(1);
					// System.out.println("ResultTerm:"+RT);
					toReplace.assign(RT);
					toReplace.refreshRenderers();
					// System.out.println("Root after function replacement:"+originalRoot);
				}
				if (afterFunctionResults!=null) afterFunctionResults.run();
			}
		}
	}
	
}