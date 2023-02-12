package com.xsb.xj;
import com.declarativa.interprolog.ObjectExamplePair;
import com.declarativa.interprolog.PrologEngine;
import com.xsb.interprolog.NativeEngine;
import com.xsb.xj.util.XJException;
import com.xsb.xj.util.XJProgressDialog;

/** A PrologEngine with some XJ-specific functionality */
public class XJPrologEngine extends NativeEngine {
	// string constant for hacky test, must be in sync with flora_shell_loop_message in flrshell_loop_handler.P
	public static String ERGO_USER_ABORT_HACK = "abort(unhandled exception: _$ergo:exit_break)";
	/** A dialog to report goal execution progress. Single global instance, but may be replaced for window ownership convenience.*/
	private static XJProgressDialog progress = null;
	
    public XJPrologEngine(String prologDir, boolean debug){
            this(prologDir, null, debug);
    }
        
    public XJPrologEngine(String prologDir, String[] xsbArgs, boolean debug){
	super(prologDir, xsbArgs, debug, true);
	initPrologLayer(this);
	deterministicGoal("assert(xpGlobalProgressController("+registerJavaObject(this)+"))");
    }
	
    public static void initPrologLayer(PrologEngine engine){
		ObjectExamplePair[] examples = {
		    GUITerm.example1(),
		    GUITerm.example2(),
		    new ObjectExamplePair("ArrayOfGUITerm",new GUITerm[0],new GUITerm[1]),
		    // LazyTreeModel.example(),
		    PrologCachedTreeModel.labelExample(),
		    PrologCachedTreeModel.notifierExample(),
		    new ObjectExamplePair("ArrayOfNodeNotifier",new PrologCachedTreeModel.NodeNotifier[0],new PrologCachedTreeModel.NodeNotifier[1])
		};
		engine.consultFromPackage("xj2.xwam",XJPrologEngine.class,true);
		engine.consultFromPackage("xjdisplays.xwam",XJPrologEngine.class,true);
		engine.consultFromPackage("prologCache.xwam",XJPrologEngine.class,true);
		
		//		consultRelative("xj2",this);
		//consultRelative("prologCache",this);
		if (!engine.teachMoreObjects(examples)) throw new XJException("Unable to teach XJ objects");
		
		//progress = new XJProgressDialog(this);
		
		
		
		engine.setThreadedCallbacks(false); // mandatory for XJ: so that javaMessages spawning from UI model building stay in the AWT thread
		//crashes InterProlog if this method runs under javaMessage...
    }
    
    // The following simply delegate to our progress dialog:
    // testing: ipPrologEngine(E),ipObjectSpec(int,L,[50],_),javaMessage(E,showProgress(string(blabla),L))
    public void showProgress(java.awt.Frame owner, String title, int lenght) {
	/* if(progress != null && ! progress.isStopped()) {
	   progress.endProgress();
	   }*/
	progress = new XJProgressDialog(this,owner);
	progress.showProgress(title, lenght);
    }
	
    public void showProgress(String title,int length){
	showProgress(null, title, length);
    }
    public void setProgress(int amount){
	progress.setProgress(amount);
    }
    public void endProgress(){
	progress.endProgress();
    }
    public void setCancellableProgress(boolean yes){
	progress.setCancellableProgress(yes);
    }
    /** Returns the current progress dialog */
    public static XJProgressDialog getProgressDialog(){return progress;}
    
    public static void setProgressDialog(XJProgressDialog d){progress=d;}
    
    /* public Object[] deterministicGoal(String G, String OVar, Object[] objectsP, String RVars){
    //     Object[]  result = super.deterministicGoal("catch((" + G + "), _XJException, handle_xj_error(_XJException))", OVar, objectsP, RVars);
    Object[]  result = super.deterministicGoal(G, OVar, objectsP, RVars);
    return result;
    } */
}
