package com.xsb.xj;
import com.declarativa.interprolog.*;
import com.declarativa.interprolog.util.*;
import com.xsb.xj.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.Arrays;


/** A Prolog goal implementing a Swing Action. To report an error from Prolog simply fail the goal
 * with xj_failError(AtomMessage). After invoking the action its method actionConcluded() will return true if
 * the action was neither cancelled by the user nor ended in error.  */
@SuppressWarnings("serial")
public class PrologAction extends AbstractAction implements Runnable {
    protected Component context;
	protected Component contextRoot; 	//root of context
    protected PrologEngine engine;
    /** Prolog goal to store a reference to this object */
    protected String rememberMyRef;
    Object goal;
    Object description;
    Cursor previousCursor, previousRootCursor;
    protected boolean callIfQuickMode;
    /** Prolog variable list, for example "[X,Y]" */
    protected String variables;
    /** Objects to be passed to Prolog, corresponding to variables */
    protected Object[] objects;
    public final int UNDEFINED=-2, STARTED=-1,CONCLUDED=0,ERROR=1,CANCELLED=2;
    /** outcome of last performance of this action */
    protected int outcome;
    private boolean threaded = false; // new policy...
    private boolean inAWTThread = false;
    protected boolean cursorChanges = true;
    protected boolean goalWasInterrupted;
    protected boolean goalAborted;
    boolean ownsProgressDialog;
    /** If true, the action is NOT re-enabled automatically at the end */
    protected boolean disabling;
    
    /** c is a component useful basically to position the error dialogs */
    public PrologAction(PrologEngine e, Component c, Object g, String d, String tip){
        super(d); description=d;
        engine=e; goal=g; context=c;
        callIfQuickMode = true;
        variables=null; objects=null;
        outcome=UNDEFINED;
        goalWasInterrupted=false;
        goalAborted = false;
        rememberMyRef = "xjRememberPrologAction("+engine.registerJavaObject(this)+"),";
        ownsProgressDialog = false;
        if (tip!=null) 
        	putValue(SHORT_DESCRIPTION,tip);
        disabling = false;
    }
    
    public PrologAction(PrologEngine e, Component c, Object g, String d){
    	this(e,c,g,d,null);
    }
    
    public PrologAction(PrologEngine e, Component c, Object g){
        this(e,c,g,"a Prolog Action");
    }
    
    public PrologEngine getEngine(){
        return engine;
    }
    
    /** Determines whether this Action will perform immediately, with control returning to the current thread
     * only after its completion, or in a background thread (threaded==true). */
    public void setThreaded(boolean threaded){
        this.threaded=threaded;
    }
    
    public boolean isThreaded(){
        return this.threaded;
    }
    
    /** Forces this Action to perform in the AWT thread if inAWTThread==true; in this case the execution
     * will be done with either SwingUtilities.invokeAndWait or invokeLater, see setThreaded.
     * If inAWTThread==false then the action may either execute in the AWT event thread or not*/
    public void setInAWTThread(boolean inAWTThread){
        this.inAWTThread=inAWTThread;
    }
    
    public void setCursorChanges(boolean c){
        cursorChanges = c;
    }
    
    public void setArguments(String variables,Object[] objects){
        if ((variables==null || objects==null) && !(variables.equals(objects)))
            throw new XJException("Bad arguments");
        this.variables=variables;
        this.objects=objects;
    }
    
    public String getDescription(){
        return description.toString();
    }
    
    public void showError(String error){
        showError(error, null);
    }
    
    public void showError(String error, String details){
        OutputFrame outputFrame = null;
        Toolkit.getDefaultToolkit().beep();
        restoreCursor();
        if(context instanceof Frame){
            outputFrame = new OutputFrame((Frame)context);
        } else if(context instanceof Dialog){
            outputFrame = new OutputFrame((Dialog)context);
            if(((Dialog)context).isModal()){
                outputFrame.setModal(true);
            }
        } else {
            Dialog dialog = (Dialog)SwingUtilities.getAncestorOfClass(java.awt.Dialog.class, 
                                                                      context);
            if(dialog != null){
                outputFrame = new OutputFrame(dialog);
                if(dialog.isModal()){
                    outputFrame.setModal(true);
                }
            } else {
                Frame frame = (Frame)SwingUtilities.getAncestorOfClass(java.awt.Frame.class,
                                                                       context);
                if(frame != null){
                    outputFrame = new OutputFrame(frame);
                }
            }
        }
        if(outputFrame != null){
            outputFrame.addWarning("error", error, details);
            outputFrame.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(context,error,
            "Error while trying to perform "+getDescription(),JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String formatBacktrace(TermModel backwardCont, TermModel forwardCont){
        StringBuffer backtrace = new StringBuffer();
        TermModel[] backwardContList = backwardCont.flatList();
        TermModel[] forwardContList = forwardCont.flatList();
        for(int i=0; i < backwardContList.length ; i++){
            backtrace.append(backwardContList[i].toString());
            backtrace.append("\n");
        }
        backtrace.append("\n");
        for(int i=0; i < forwardContList.length ; i++){
            backtrace.append(forwardContList[i].toString());
            backtrace.append("\n");
        }
        return backtrace.toString();
    }
    
    /** The action has performed without errors */
    public boolean actionSucceeded(){ return outcome==CONCLUDED; }
    
    /** The action was cancelled or interrupted */
    public boolean wasCancelled(){ return outcome==CANCELLED; }
    
    public void ownsProgressDialog(){
        ownsProgressDialog=true;
    }
    
    /** Sleeps until the action ends, be it succeeding or failing */
    public void waitUntilEnd(){
        if (outcome==UNDEFINED)
            throw new XJException("You should perform the action first");
        while(outcome==STARTED){
            try{Thread.sleep(1);}
            catch(InterruptedException e){
                throw new XJException("action interrupted");
            }
        }
    }
    
    void setWaitCursor() {
		if (!cursorChanges) { 
			return;
		}
        
		if (context!=null) {
			SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
		            previousCursor = context.getCursor();
					contextRoot = SwingUtilities.getRoot(context);		
					if(contextRoot != null) {
						//must be done before changing the cursor for the context.
						previousRootCursor = contextRoot.getCursor();
						contextRoot.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					}	
					context.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
				
			});
        }
		
		
    }
    
    void restoreCursor(){
        if (!cursorChanges) {
			return;
		}
        SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				if (context!=null) {
		            context.setCursor(previousCursor);
				}
				
				if(contextRoot != null) {
					contextRoot.setCursor(previousRootCursor);
		        }
			}
        });
    }
    
    public void actionPerformed(ActionEvent e){
        doit();
    }
    
    public void doit(){
        if (callIfQuickMode /* first time */ && !isEnabled()) return;
        outcome = STARTED;
        if (engine.isIdle() && !engine.isAvailable()) {
            showError("Engine is busy; make sure your last top goal has ended.");
            outcome = ERROR;
        } else {
            setWaitCursor();
            setEnabled(false);
            // not enough to disable popup menus, we would need to set some state on the GUITerm itself(?)
            if (!callIfQuickMode) run(); // only one thread per Action!
            else {
                if (!inAWTThread){
                    if (threaded) {
                        try{
                            new Thread(this,"Threaded PrologAction").start();
                        } catch(IllegalThreadStateException  e){
                            throw new XJException("Problem doing "+description,e);
                        }
                    } else run(); // non threaded
                } else {
                    if (threaded) SwingUtilities.invokeLater(this);
                    else {
                        if (SwingUtilities.isEventDispatchThread()) run();
                        else try{SwingUtilities.invokeAndWait(this);} // blocks here
                        catch(Exception e){throw new XJException("Problems in invokeAndWait:"+e);}
                    }
                }
            }
        }
    }
    
    public void run(){
        String realGoal;
        Object[] realObjects;
        String realVariables;
        Object[] bindings;
        goalWasInterrupted=false;
        goalAborted=false;
        IPException exception = null;
        if (goal instanceof TermModel){
            if(!(goal.getClass().equals(TermModel.class)))
                throw new XJException("PrologAction goal objects can not be of TermModel subclasses");
            String extraVar = "MustBeUnique_XJ";
            String extraVar2 = extraVar+"2";
            if(variables!=null&&(variables.indexOf(extraVar)!=-1||variables.indexOf(extraVar2)!=-1))
                throw new XJException("Can not use this name for a goal variable:"+extraVar);
            realGoal = "recoverTermModel("+extraVar+","+extraVar2+"),"+extraVar2;
            if(objects!=null){
                realObjects=new Object[objects.length+1];
                realObjects[0] = goal;
                for(int o=0;o<objects.length;o++)
                    realObjects[o+1]=objects[o];
                int leftSqPosition = variables.indexOf("[");
                if(leftSqPosition==-1) throw new XJException("Bad variables string");
                realVariables = variables.substring(0,leftSqPosition+1) +
                extraVar + ","+ variables.substring(leftSqPosition+1);
            } else {
                realObjects=new Object[]{goal};
                realVariables="["+ extraVar +"]";
            }
        } else{
            realGoal = goal.toString();
            realObjects = objects;
            realVariables = variables;
        }
        try{
            //System.err.println("PrologAction run:");
            //System.err.println("Goal:"+rememberMyRef+"xjCallIfQuick(("+realGoal+"),Result)");
            //System.err.println("realVariables:"+realVariables);
            if (callIfQuickMode) {
                bindings = engine.deterministicGoal(
                    rememberMyRef+"xjCallIfQuick(("+realGoal+"),Result)",
                    realVariables,realObjects, "[string(Result)]"
                );
            } else {
                bindings = engine.deterministicGoal(
                                    rememberMyRef+realGoal,
                                    realVariables,
                                    realObjects,
                                    null);
            }
        } catch (IPInterruptedException e){
            bindings=null;
            goalWasInterrupted=true;
            exception = e;
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
    
    protected static String DEFERRED = "deferred";
    protected static String DONE = "done";
    
    public void goalEnded(Object[] bindings, IPException exception){
        restoreCursor();
        if( ownsProgressDialog){
            XJProgressDialog pd = XJPrologEngine.getProgressDialog();
            if(pd != null){
                if (!pd.isStopped()) pd.endProgress();
            }
        }
        if (bindings!=null) {
            if (bindings.length>0) {
                if (callIfQuickMode && bindings[0].equals(DEFERRED)){
                    int result = JOptionPane.showConfirmDialog(
                    context,"Are you sure you want to do that?",
                    description.toString(),
                    JOptionPane.OK_CANCEL_OPTION
                    );
                    if (result==JOptionPane.OK_OPTION) {
                        callIfQuickMode = false;
                        doit();
                    } else outcome = CANCELLED;
                } else outcome=CONCLUDED;
            } else outcome=CONCLUDED;
        } else {
            if (goalWasInterrupted || goalAborted){
                String exceptionMessage = null;
                String backtrace = null;
                outcome = CANCELLED;
                if(exception != null){
                    if(exception instanceof IPPrologError){
                        Object error = ((IPPrologError)exception).getError();
                        if(error instanceof TermModel){
                            TermModel tmError = (TermModel)error;
                            if(tmError.getChildCount() == 2){
                                if(((TermModel)tmError.getChild(0)).isList() &&
                                    ((TermModel)tmError.getChild(1)).isList() &&
                                    (tmError.getChild(0).toString().startsWith("[Forward Continuation"))){
                                        exceptionMessage = tmError.node.toString();
                                        backtrace = formatBacktrace((TermModel)tmError.getChild(0), 
                                                                    (TermModel)tmError.getChild(1));
                                }
                            }
                        }
                    }
                    if(exceptionMessage == null){
                        exceptionMessage = exception.getMessage();
                    }
                }
                if (goalWasInterrupted){
                	System.err.println("User aborted '"+getDescription()+"'"+exceptionMessage);
                	showError("User aborted");
                } else if(backtrace == null){
                    showError("Failure while trying to perform "+getDescription()+"\n"+exceptionMessage);
                } else {
                    showError("Failure while trying to perform "
                              +getDescription()+"\n"+exceptionMessage, 
                              backtrace);
                }
            } else { // weird action goal failure
            	System.err.println("Unhappy end in PrologAction, goal=="+goal+", objects=="+Arrays.toString(objects));
                outcome = ERROR;
                // try to fetch error with xj_fetchError(Message)
                Object[] errorDG = engine.deterministicGoal("xj_fetchError(Message)","[Message]");
                if (errorDG!=null)
                	showError(errorDG[0].toString());
                else if (exception!=null){
                    showError("Failure while trying to perform "
                              +getDescription()+"\n"+exception.getMessage());
                } else {
                    /*
                      This happens, for example, when trying to see
                      a matching/calling rule for a subgoal.
                    */
                    String notFoundMsg =
                        "The operation '"+getDescription()+"' returned no result";
                    //showError(notFoundMsg);
                    JOptionPane.showMessageDialog(context,notFoundMsg,
                                                  "Nothing to show",
                                                  JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
        if (!disabling) 
            SwingUtilities.invokeLater(new Runnable(){
                    @Override
                    public void run() {
                        setEnabled(true);
                    }
                });
        callIfQuickMode = true;
    }
    
    

	public String toString(){
        StringBuffer b = new StringBuffer(super.toString());
        b.append(getClass().getName());
        b.append("\ngoal:"+goal);
        b.append("\nvariables:"+variables);
        if (objects!=null){
            b.append(" objects("+objects+"):{");
            for (int i=0;i<objects.length;i++)
                if (i>0) b.append(","+objects[i]);
                else b.append(objects[i]);
            b.append("}");
        }
        return b.toString();
    }
    
    /** Testing */
    public static void main(String[] args){
        PrologEngine engine = new XJPrologEngine(args[0],false);
        JFrame w = new JFrame("PrologAction tests");
        JMenuBar mb = new JMenuBar();
        w.setJMenuBar(mb);
        JMenu test = new JMenu("Test");
        mb.add(test);
        w.setVisible(true);
        TermModel a = new TermModel("A");
        TermModel g = new TermModel("writeln",new TermModel[]{a});
        // g used to write a var instead of a "A"
        PrologAction action = new PrologAction(engine, w, g, "Bad action");
        action.setArguments("[Some]",new Object[]{new TermModel("thing")});
        test.add(action);
    }
}
