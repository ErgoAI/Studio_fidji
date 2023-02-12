package com.xsb.xj;

import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Vector;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.xsb.interprolog.NativeEngineWindow;
import com.xsb.xj.util.XJException;

@SuppressWarnings("serial")
public class XJTopLevel extends NativeEngineWindow {

	/**
	 *  Calls xjmain/1 with a list of command line arguments.
	 *
	 *@param  display        Display flag for xjListener
	 *@param  prologDir      Location of xsb
	 *@param  initialFile    Name of main file
	 *@param  debug          Debug flag
	 *@param  remainingArgs  Command line arguments
	 */
	XJTopLevel(boolean display, String prologDir, String[] prologArgs, 
                   String initialFile, boolean debug, String[] remainingArgs) {
		super(new XJPrologEngine(prologDir, prologArgs, debug), display);
		setTitle("XJ XSB listener");
                this.getAccessibleContext().setAccessibleName("XJ XSB listener");
                this.getAccessibleContext().setAccessibleDescription("Window for evaluation of manually inputted prolog goals");
		engine.command("cd('" + new File(new File(initialFile).getAbsolutePath()).getParent().replaceAll("\\\\","/") + "')");
		setIcon();

		engine.command("assert(xjConsole(" + engine.registerJavaObject(this) + "))");

                String goal, argString;
                Object[] args_List;
                if(remainingArgs == null){
                    goal = new String("consult('" + initialFile + "'),xjmain");
                    argString = null;
                    args_List = null;
                } else {
                    goal = new String("consult('" + initialFile + "'),stringArraytoList(ArgsArray,ArgsList),xjmain(ArgsList)");
                    argString = "[ArgsArray]";
                    args_List = new Object[]{remainingArgs};
                }

                try {
                    Object[] bindings = engine.deterministicGoal(goal, argString, args_List, "[]");
                    if (bindings == null) {
                        System.exit(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Press any key to continue . . . ");
                    try{System.in.read();} catch (Exception e1){}
                    System.exit(-1);
                }
                engine.waitUntilIdle();
	}

	public void windowClosing(WindowEvent e) {
		setVisible(false);
	}


	public void minimize() {
		System.out.println("Minimized XSB listener window");
		setState(Frame.ICONIFIED);
	}


	void setIcon() {
		setIconImage(XJDesktop.XSB_ICON.getImage());
	}

        /**
         * Overwrites method in NativeEngineWindow for displaying 
         * results of goal in XJConsole. XJPrologEngine wraps console goals with 
         * catch. This method strips off catch functor for display back to user.
         */
        protected String formatGoalResult(Object[] bindings){
            if (bindings == null) {return("FAILED\n");}
            else if(bindings.length > 0){
                if(bindings[0] instanceof TermModel){
                    TermModel resultModel = (TermModel)bindings[0];
                    if(resultModel.node.equals("catch") && (resultModel.getChildCount() == 3)){
                        if(((TermModel)resultModel.getChild(1)).isVar()){
                            return(resultModel.getChild(0).toString()+"\n");
                        } else {
                            return("EXCEPTION: " + resultModel.getChild(1).toString() + "\n");
                        }
                    } 
                } 
            }  
            return(bindings[0].toString()+"\n");
        }
        
        public static void main(String args[]) {
            System.out.println("Welcome " + System.getProperty("user.name") + " to InterProlog " + PrologEngine.version + " + XJ on Java " +
            System.getProperty("java.version") + " (" +
            System.getProperty("java.vendor") + "), " +
            System.getProperty("os.name") + " " +
            System.getProperty("os.version"));
            
            String initialFile = null;
            boolean debug = false;
            String prologDir = null;
            String[] remainingArgs;
            Vector<String> prologArgVector = new Vector<String>();
            String[] prologArgs = null;
            
            if (args.length >= 3) {
                int i = 0;
                while (true) {
                    String arg = args[i];
                    if (arg.equals("-initfile")) {
                        initialFile = args[i + 1];
                        i = i + 2;
                    } else if (arg.startsWith("-d")) {
                        debug = true;
                        i = i + 1;
                    } else if (arg.startsWith("-")) {
                        prologArgVector.add(arg);
                        i++;
                    } else if (!arg.startsWith("-")) {
                        prologDir = arg;
                        i++;
                        break;
                    }
                }
                
                if((args.length - i) > 0){
                    remainingArgs = new String[args.length - i];
                    System.arraycopy(args, i, remainingArgs, 0, args.length - i);
                } else {
                    remainingArgs = null;
                }
                
            } else {
                throw new XJException("Missing arguments in command line");
            }
            
            if (initialFile == null) {
                throw new XJException("Missing  initfile");
            }
            if (prologDir == null) {
                throw new XJException("Prolog start string missing");
            }
            
            // try to figure out whether the path to base directory
            // or full path is provided
            if((prologDir.indexOf("config") == -1) || 
               !((prologDir.endsWith("bin")) || prologDir.endsWith("bin"+File.separator))){
               // only base path is provided - upgrade to full path
               // (NativeEngine requires it, but only uses base part, 
               //  so it does not really matter what to append as long as it has
               //  "config" in it)
                   prologDir = prologDir.concat(File.separator+"config"+
                                                File.separator+"anything"+
                                                File.separator+"bin");
            }
            
            System.out.println("Starting with file: " + initialFile);
            System.out.println("XSB_DIR: " + prologDir);
            if(!prologArgVector.isEmpty()){
                prologArgs = new String[prologArgVector.size()];
                for(int i=0; i<prologArgs.length; i++){
                    prologArgs[i] = (String)prologArgVector.get(i);
                    System.out.println("XSB_ARG: " + prologArgs[i]);
                }
            }
            
            new XJTopLevel(false, prologDir, prologArgs, initialFile, debug, remainingArgs);
        }
}
