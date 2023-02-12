/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2013
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, see http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.fiji;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.util.logging.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.zip.*;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.jar.*;
import java.net.URLEncoder;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.FloraSubprocessEngine;
import com.xsb.xj.XJDesktop;

/** A single instance will exist, containing preferences including the Flora and XSB locations 
 // Other parts of the application may add hooks persisting keeping interesting props :
 Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
 public void run() {
 store();
 }
 }));

*/
@SuppressWarnings("serial")
public class FijiPreferences extends Properties{
    /** Studio can support Flora/Ergo or Prolog; this flag tells what is 
        supported by the current running instance: the Coherent's
	proprietary or InterProlog Consulting's open source Prolog studio. */
    public static boolean floraSupported = false;
    // These 2 variables will be not null if the corresponding Coherent proprietary classes are present
    public static Class<?> otherEditorClass = null;
    public static Class<?> otherOutputPaneClass = null;
    public static Class<?> otherPreferencesDialogClass = null;
	
    public static URL splashImage = null;
    public static Color splashTextColor = null;
    public static float splashTextSize;

    /** These go into the directory containing the jar file */
    static final String LOG_FILE = ".ergoAI.log";
    static final String PREFS_FILE = ".ergoAI.prefs";
    static final String BROKEN_PREFS_FILE = ".ergoAI.prefs.broken";
    private static final String FACTORY_PREFS_FILE = ".ergoAIFactory.prefs"; 
    private static final String FACTORY_PREFS_RESOURCE = "/ergoAIFactory.prefs";
    static final String PROLOG_STUDIO_HISTORY = ".PrologStudioHistory.txt";
    static final String ERGO_STUDIO_HISTORY = ".ergoAIHistory.txt";
    static final String FIDJI_XREF = ".fidjiXref";
    static final String FIDJI_RC = "fidjiRC.P";
    static final String FIJI_RC = "fijiRC.P";
    static final String FIDJI_RC_INJAR = "fidjiRC.xwam"; // assumes XSB Prolog
    static final String FIDJIFLORA = "ErgoAI";
    static final String FIDJIERGO = "ErgoEngine" + File.separator + "ErgoAI";
    static final String FIDJIERGO2 = "ErgoAI";
    static final String FIDJIXSB = "XSB";
    static final String FIJIXSB = "fijiXSB";
    static final String FIDJINLP = "NLP";
    static final String FIJINLP = "fijiNLP";
    static final String FIJIQUALM = "qualm";
    static final String FIJILPS = "LPS";
    static final String OBJECT_SOURCES_FILENAME = "ObjectSources.P";
    static final String BUGREPORT = "STUDIO_BUG_REPORT.zip";
    static final String FIDJI_MAILBOX = "ergo.support@coherentknowledge.com"; 
    static final String FIJI_MAILBOX = "studio@interprolog.com"; 
    static final String FIDJI_STUDIO = "ErgoAI"; 
    static final String FIJI_STUDIO = "Prolog Studio"; 
    static String MAILBOX, STUDIO_NAME;
	
    public static final String PREF_SEPARATOR = "\t";
    /** The Studio preferences file */
    static File file = new File(PREFS_FILE); // transient location, will be fixed below
    private File factoryFile;
    private static File classesFile;

    static private File brokenFile = new File (BROKEN_PREFS_FILE);
    private static File fidjiDir;
    static File fidjiXrefDir;
    static File fidjiNLPdir;
    static String FIDJIBUILD = "??";
    public static boolean quietLog=false;
    /** How many times slower startup was comparing to a reference value */
    private static double slowness = 1.0;
    private static int PATIENCE_THRESHOLD_MS = 10000;
	
    FijiPreferences(boolean quietLog){
        this(quietLog,null,null,false);
    }
	
    FijiPreferences(boolean quietLog,String[] prologStartCommand,String workingDir,boolean prologstudio){
        if (AbstractPrologEngine.isMacOS()){
            //System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name","Studio");  
        }
        try{
            try {
                // tests whether Coherent proprietary classes are present, and remembers their objects for later dynamic instantiation
            	if (! prologstudio){
                    otherEditorClass=Class.forName( "com.coherentknowledge.fidji.FloraProgramEditor" ); 
                    otherPreferencesDialogClass=Class.forName("com.coherentknowledge.fidji.ErgoStudioPreferencesDialog");
                    otherOutputPaneClass=Class.forName("com.coherentknowledge.fidji.CtrlStyledOutputPane");
                    floraSupported = true;
                    splashImage = otherEditorClass.getResource("splash.png");
                    splashTextColor = Color.lightGray; splashTextSize = 24.0f;
            	} else {
                    splashImage = getClass().getResource("bridge.png");
                    splashTextColor = Color.black; splashTextSize = 18.0f;
            	}
            } catch( ClassNotFoundException e ) {
                splashImage = getClass().getResource("bridge.png");
                splashTextColor = Color.black; splashTextSize = 18.0f;
            }
            //System.out.println("Starting with floraSupported == "+floraSupported);
            MAILBOX = (floraSupported?FIDJI_MAILBOX:FIJI_MAILBOX);
            STUDIO_NAME = (floraSupported?FIDJI_STUDIO:FIJI_STUDIO);
            classesFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (workingDir==null)
            	fidjiDir=classesFile.getParentFile();
            else 
            	fidjiDir=new File(workingDir);
            fidjiXrefDir = new File(getFidjiDir(),FIDJI_XREF);
            if (!fidjiXrefDir.exists())
                if (!fidjiXrefDir.mkdir())
                    throw new RuntimeException("Could not create "+fidjiXrefDir);
            if (!getFidjiDir().canWrite())
                throw new RuntimeException("The directory of the Studio jar must be writeable");
            File nlpDir = new File(getFidjiDir(),FIDJINLP);
            if (nlpDir.exists()) fidjiNLPdir = nlpDir;
            else {
                nlpDir = new File(getFidjiDir(),FIJINLP);
                if (nlpDir.exists()) fidjiNLPdir = nlpDir;
                else fidjiNLPdir = null;
            }
            if (quietLog) 
                initHiddenLogging(getFidjiDir());
            if (!classesFile.isDirectory()){
                JarFile JF = new JarFile(classesFile);
                FIDJIBUILD  = JF.getManifest().getMainAttributes().getValue("Implementation-Version");
                JF.close();
            }
            file = new File(getFidjiDir(),PREFS_FILE);
            brokenFile = new File(getFidjiDir(),BROKEN_PREFS_FILE);
            factoryFile = new File(getFidjiDir(), FACTORY_PREFS_FILE);
            boolean newPrefs;
            if (!file.exists()){
                newPrefs = true;
                putAll(getFactory());
                if (!floraAndXSBnearBy()) {
                    JFileChooser chooser = new JFileChooser();
                    if (floraSupported){
                        chooser.setDialogTitle("Where is Ergo installed?");
                        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(null)){
                            String flora = chooser.getSelectedFile().getAbsolutePath();
                            setProperty("FLORADIR",flora);
                            File X = new File(flora,".flora_paths");
                            if (!AbstractPrologEngine.isWindowsOS() && X.exists()){
                                tryToFindProlog(X);
                            } else {
                                X = new File(flora,".flora_paths.bat");
                                if (AbstractPrologEngine.isWindowsOS() && X.exists()){
                                    tryToFindProlog(X);
                                } 
                            }					
                        }
                    } else if (prologStartCommand==null){
                        chooser.setDialogTitle("Where is the XSB Prolog executable?");
                        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(null)){
                            String prolog = chooser.getSelectedFile().getAbsolutePath();
                            setProperty("PROLOG",prolog);
                        }
                    }
                }
                File qualmDir = new File(getFidjiDir(),FIJIQUALM);
                if (qualmDir.exists())
                    setProperty("QUALM",qualmDir.getAbsolutePath());
                File lpsDir = new File(getFidjiDir(),FIJILPS);
                if (lpsDir.exists())
                    setProperty("LPS",lpsDir.getAbsolutePath());
                store();
            } else newPrefs = false;
            FileReader FR = new FileReader(file);
            load(FR); FR.close();
            if (!newPrefs)
                floraAndXSBnearBy(); // overwrite locations if Fidji dir moved
        } catch (Exception e){
            System.err.println(e);
            if (quietLog)
                FijiSubprocessEngineWindow.abortStartup("Failed to start Studio: could not initialize preferences:\n"+e);
            else throw new RuntimeException("Failed to initialize Studio preferences:\n"+e);
        }
    }
    
    public String getQUALMdir(){
    	return getProperty("QUALM");
    }
    
    public String getLPSdir(){
    	return getProperty("LPS");
    }
    
    public static File historyFile(){
    	if (floraSupported)
            return new File(getFidjiDir(),ERGO_STUDIO_HISTORY);
    	else
            return new File(getFidjiDir(),PROLOG_STUDIO_HISTORY);
    }
    /** Returns the jar file or binaries directory where this class is */
    public static File getClassesFile() {
        return classesFile;
    }
    /** Enough logic components present near our jar file */
    boolean floraAndXSBnearBy() {
        File floraFile = new File(getFidjiDir(),FIDJIFLORA);
        File ergoFile = new File(getFidjiDir(),FIDJIERGO);
        if (!ergoFile.exists()) {
            ergoFile = new File(getFidjiDir(),FIDJIERGO2);
        }
        File xsbFile = new File(getFidjiDir(),FIDJIXSB);
        boolean exists = xsbFile.exists();
        if (!exists){
            xsbFile = new File(getFidjiDir(),FIJIXSB);
            exists = xsbFile.exists();
        }
        if (exists){
            // some redundancy here with XSBPeer.executablePath:
            if (AbstractPrologEngine.is64WindowsOS()) {
                setProperty("PROLOG",new File(xsbFile,"bin/xsb64.bat").getAbsolutePath());
                if (!new File(xsbFile,"config/x64-pc-windows").exists())
                    FijiSubprocessEngineWindow.abortStartup("Cannot start Studio.\nApparently you did not install the 64-bit XSB");
            } else if (AbstractPrologEngine.isWindowsOS()) {
                setProperty("PROLOG",new File(xsbFile,"bin/xsb.bat").getAbsolutePath());
                if (!new File(xsbFile,"config/x86-pc-windows").exists())
                    FijiSubprocessEngineWindow.abortStartup("Cannot start Studio.\nApparently you did not install the 32-bit XSB");
            } else setProperty("PROLOG",new File(xsbFile,"bin/xsb").getAbsolutePath());
            if (xsbFile.getAbsolutePath().contains(" "))
                if (AbstractPrologEngine.isWindowsOS())
                    setProperty("PROLOG","\""+getProperty("PROLOG")+"\"");
        }
        if (floraSupported && floraFile.exists())
            setProperty("FLORADIR",floraFile.getAbsolutePath());
        else if (floraSupported && ergoFile.exists())
            setProperty("FLORADIR",ergoFile.getAbsolutePath());
        return ((!floraSupported || (floraFile.exists() || ergoFile.exists())) && exists /* xsbFile.exists() */);
    }
	
    /** if Prolog path found, store it in property PROLOG; strips quotes */
    void tryToFindProlog (File F) throws IOException{
        BufferedReader prologFinder = new BufferedReader(new FileReader(F));
        String line = prologFinder.readLine();
        String prolog = null;
        while (prolog==null && line!=null){
            if (line.startsWith("PROLOG=")) prolog = line.substring(7);
            else if (line.startsWith("@set PROLOG=")) prolog = line.substring("@set PROLOG=".length()).trim();
            else line = prologFinder.readLine();
        }
        prologFinder.close();
        if (prolog!=null){
            if (!AbstractPrologEngine.isWindowsOS() && prolog.startsWith("\"") && prolog.endsWith("\""))
                prolog=prolog.substring(1,prolog.length()-1);
            setProperty("PROLOG",prolog);
        }
    }
	
    boolean store(){
        try{
            FileWriter fw = new FileWriter(file);
            store(fw,STUDIO_NAME+" preferences, in Java Properties format\n#This file must be in the same directory as the jar file\n#Do NOT edit this while Studio is running, as it will overwrite it on exit!");
            fw.close();
        } catch(IOException e){
            System.err.println("Could not save Studio preferences:\n"+e);
            return false;
        }
        return true;
    }
		
    boolean storeFactoryPrefs(Properties P){
        try{
            FileWriter fw = new FileWriter(factoryFile);
            P.store(fw,STUDIO_NAME+" factory preferences, in Java Properties format\n#After setting up your windows, you can generate it with:  ?- ipListenerWindow(LW), java(LW,saveFactoryPreferences).");
            fw.close();
        } catch(IOException e){
            System.err.println("Could not save Studio factory preferences:\n"+e);
            return false;
        }
        return true;
    }

    /** Get the factory preferences
     * @return a new Properties object
     */
    public Properties getFactory() {
        Properties factory = new Properties();
        try{
            if (!factoryFile.exists()){
                InputStream resourceStream = getClass().getResourceAsStream(FACTORY_PREFS_RESOURCE);
                if (resourceStream == null){
                    System.err.println("Could not find factory preferences resource");
                    return factory;
                }
                FileOutputStream fos = new FileOutputStream(factoryFile);
                byte[] buffer = new byte[512]; int len;
                while ((len = resourceStream.read(buffer, 0, buffer.length)) != -1) 
                    fos.write(buffer, 0, len);
                fos.close(); resourceStream.close();
            }
            FileReader FR = new FileReader(factoryFile);
            factory.load(FR); FR.close();
        } catch (IOException e){
            System.err.println("Could not read Studio factory preferences:\n"+e);
        }
        return factory;
    }
	
    public static void main(String[] args) {
        FijiSubprocessEngineWindow.main(args);
    }

    // grabbed from https://blogs.oracle.com/nickstephen/entry/java_redirecting_system_out_and:
    static class StdOutErrLevel extends Level {

        private StdOutErrLevel(String name, int value) {
            super(name, value);
        }
        public static Level STDOUT =
            new StdOutErrLevel("STDOUT", Level.INFO.intValue()+53);
        public static Level STDERR =
            new StdOutErrLevel("STDERR", Level.INFO.intValue()+54);

        protected Object readResolve()
            throws ObjectStreamException {
            if (this.intValue() == STDOUT.intValue())
                return STDOUT;
            if (this.intValue() == STDERR.intValue())
                return STDERR;
            throw new InvalidObjectException("Unknown instance :" + this);
        }        

    }

    static class LoggingOutputStream extends ByteArrayOutputStream { 
 
        private String lineSeparator; 
 
        private Logger logger; 
        private Level level; 
 
        public LoggingOutputStream(Logger logger, Level level) { 
            super(); 
            this.logger = logger; 
            this.level = level; 
            lineSeparator = System.getProperty("line.separator"); 
        } 
 
        public void flush() throws IOException { 
 
            String record; 
            synchronized(this) { 
                super.flush(); 
                record = this.toString(); 
                super.reset(); 
                if (record.length() == 0 || record.equals(lineSeparator)) { 
                    // avoid empty records 
                    return; 
                } 
                logger.logp(level, "", "", record); 
            } 
        } 
    } 
	
    // preserve old stdout/stderr streams in case they might be useful      
    static PrintStream stdout = System.out;                                        
    static PrintStream stderr = System.err;  
    static final int LOG_ROLLOVER = 1;
    
    /** Redirect standard output and error streams to log file */                                
    static void initHiddenLogging(File directory){
        try{
            // System.getProperties().setProperty("java.util.logging.config.file","foobarr");
            // initialize logging to go to rolling log file
            LogManager logManager = LogManager.getLogManager();
            logManager.reset();

            // log file max size 200K, LOG_ROLLOVER rolling files, append-on-open
            Handler fileHandler = new FileHandler(new File(directory,LOG_FILE).toString(), 5000000, LOG_ROLLOVER, false);
            fileHandler.setFormatter(new SimpleFormatter());			
			
            Logger.getLogger("stdout").addHandler(fileHandler);
            Logger.getLogger("stderr").addHandler(fileHandler);
		
            // now rebind stdout/stderr to logger                                   
            Logger logger;                                                          
            LoggingOutputStream los;                                                

            logger = Logger.getLogger("stdout");                                    
            los = new LoggingOutputStream(logger, StdOutErrLevel.STDOUT);           
            System.setOut(new PrintStream(los, true));                              

            logger = Logger.getLogger("stderr");                                    
            los= new LoggingOutputStream(logger, StdOutErrLevel.STDERR);            
            System.setErr(new PrintStream(los, true)); 
            quietLog = true;  
        } catch (IOException e){ throw new RuntimeException("failed to set up Studio logging");}                           
    }
	
    public static String getLogFile(){
        return new File(getFidjiDir(),LOG_FILE).toString();
    }
	
    static void initHiddenLogging(){
        initHiddenLogging(getFidjiDir());
    }
	
    public static String buildRevision(){
        return FIDJIBUILD;
    }

    public static void adjustBounds(Component component, String value) {
        if (component == null || !component.isDisplayable())
            return;
        Rectangle R = pref2Rectangle(value);
        if (R==null) return;
        Component w = (Component)XJDesktop.findWindowOrSimilar(component);
        w.setBounds(R);
    }
	
    /**
     * @param preference the full preference value (may be null)
     * @return whether the window prefers to position itself in a tab (yes by default)
     */
    public static boolean pref2InTab(String preference) {
        if (preference==null) return true;
        Scanner S = new Scanner(preference);
        S.useDelimiter(PREF_SEPARATOR);
        if (!S.hasNext()){
            S.close();
            System.err.println("Bad preference:"+preference);
            copyTo(file,brokenFile);
            return false;
        }
        String it = S.next(); // inTab
        S.close();
        if (it!=null) return it.equals("true");
        else return false;
    }
	
    /** Does return null if the preferred screen is not present, or if the preference is null; if the preference text is ill-formed will throw an Exception */
    public static Rectangle pref2Rectangle(String preference){
        if (preference==null) return null;
        Scanner S = new Scanner(preference);
        S.useDelimiter(PREF_SEPARATOR);
        String screen;
        Rectangle R = null;
        try{
            String inTab = S.next(); // inTab
            if (inTab.equals("true")||inTab.equalsIgnoreCase("false"))
                screen = S.next();
            else screen = inTab; // little hack to keep reading old preferences files
            int height = S.nextInt();
            int width = S.nextInt();
            int x = S.nextInt();
            int y = S.nextInt();
            GraphicsDevice defaultScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            boolean screenPresent = false;
            @SuppressWarnings("unused")
                boolean inMainScreen = false;
            for (GraphicsDevice device: GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
                if (device.getIDstring().equals(screen)){
                    screenPresent = true;
                    int maxX = device.getDisplayMode().getWidth()-100;
                    int maxY = device.getDisplayMode().getHeight()-100;
                    if (x > maxX)
                        x = maxX;
                    if (y > maxY)
                        y = maxY;
                    if (device.equals(defaultScreen))
                        inMainScreen = true;
                }
            S.close();
            //System.out.println(preference);
            if (screenPresent) {
                //if (!inMainScreen)
                // Apparent Java bug on Mac OS 10.10.2: y taken as ordinate in main screen, and negative values throwing window titles offscreen
                // ... but even so this hack still sees windows placed offscreen on second monitor (of 3), no idea why:-(
                //	if (y<0) y=0; 
                // TODO: the above problem seems to relate instead to bounds of GraphicsConfiguration? see isVirtualEnvironment() below
                // Non-Mac testing needed!
                R =  new Rectangle(x,y,width,height);
            }
            else R = null;
        } catch (Exception e){
            System.err.print("Bad preference..:"+preference);
            copyTo(file,brokenFile);
        }
        //System.out.println(R);
        return R;
    }
    /** Returns pair of ints after string and 4 ints; if they're not there returns null */
    static Point pref2PointAfterRectangle(String preference){
        if (preference==null) return null;
        Scanner S = new Scanner(preference);
        S.useDelimiter(PREF_SEPARATOR);
        try{
            String inTab = S.next(); // inTab or...used to be screen...
            // little hack to keep reading old preferences files
            if (inTab.equals("true")||inTab.equalsIgnoreCase("false")) // it's a new one
                S.next(); // screen
            S.nextInt(); // height
            S.nextInt(); // width
            S.nextInt(); // x
            S.nextInt(); // y
            if (!S.hasNext()) {
                S.close();
                return null;
            } else{
                Point P = new Point(S.nextInt(), S.nextInt());
                S.close();
                return P;
            }
        } catch (Exception e){
            System.err.println("Bad point/rect preference:"+preference);
            copyTo(file,brokenFile);
            return null;
        }
    }
    /** inTab(boolean), screen, rect bounds */
    public static String window2pref(Component W, boolean inTab){
        String favoriteScreen = W.getGraphicsConfiguration().getDevice().getIDstring();
        int height = W.getBounds().height;
        int width = W.getBounds().width;
        int x = W.getBounds().x;
        int y = W.getBounds().y;
        //if (isVirtualEnvironment()){
        //	Rectangle screenR = W.getGraphicsConfiguration().getBounds();
        //	x =- screenR.x;
        //	y =- screenR.y;
        //}
        return inTab+PREF_SEPARATOR+favoriteScreen+PREF_SEPARATOR+height+PREF_SEPARATOR+width+PREF_SEPARATOR+x+PREF_SEPARATOR+y;
    }
    static String array2pref(ArrayList<String> strings){
        StringBuilder sb = new StringBuilder();
        boolean first=true;
        for (String S:strings){
            if (first) first = false;
            else sb.append(PREF_SEPARATOR);
            sb.append(S);
        }
        return sb.toString();
    }
    static HashSet<String>  pref2array(String preference){
        HashSet<String> result = new HashSet<String>();
        Scanner S = new Scanner(preference);
        S.useDelimiter(PREF_SEPARATOR);
        while(S.hasNext())
            result.add(S.next());
        S.close();
        return result;
    }
	
    private static Boolean virtualEnvironment = null;
    /** The current monitor configuration uses a multiple monitor virtual coordinate system */
    static boolean isVirtualEnvironment(){
        if (virtualEnvironment == null){
            virtualEnvironment = new Boolean(false);
            for(GraphicsDevice device: GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
                for (GraphicsConfiguration gc:device.getConfigurations())
                    if (gc.getBounds().x != 0 || gc.getBounds().y != 0)
                        virtualEnvironment = new Boolean(true);
        }
        return virtualEnvironment.booleanValue();
    }
	
    private static HashSet<String> situationsVisited = new HashSet<String>();
	
    static long mayBeLonger(long ms,String S){
        if (!situationsVisited.contains(S)){
            ms = ms*3;
            situationsVisited.add(S);
        }
        return ms;
    }
	
    static void setSlowness(double S){
        slowness = S;
        //System.out.println("slowness=="+slowness);
    }
	
    public static double getSlowness(){
        return slowness;
    }

    public static class ImpatientBugReporter{
        boolean ranOK, handlingProblem;
        Thread background, current;
        Exception extraWorry = null;
		
        public ImpatientBugReporter(AbstractPrologEngine suspect){
            this((long)(PATIENCE_THRESHOLD_MS*(slowness<1? 1 : slowness)), Thread.currentThread().getStackTrace()[2].toString(), null, false, true, true, null, null, suspect);
        }
		
        public ImpatientBugReporter(long ms, String situation, FijiSubprocessEngineWindow listener){
            this(ms, situation, null, false, false, true, null, listener,null);
        }
		
        public ImpatientBugReporter(long ms, String situation, boolean shouldEmail, FijiSubprocessEngineWindow listener){
            this(ms, situation, null, false, false, shouldEmail, null, listener,null);
        }
		
        /** about two seconds (times slowness factor) to call dontWorry, otherwise Fiji constructs bug report and exits. */
        public ImpatientBugReporter(FijiSubprocessEngineWindow listener){
            this((long)(PATIENCE_THRESHOLD_MS*(slowness<1? 1 : slowness)), Thread.currentThread().getStackTrace()[2].toString(), null, true, false, true,null, listener,null);
        }
		
        public ImpatientBugReporter(String situation, FijiSubprocessEngineWindow listener){
            this(0, situation, null, false, false, true,null, listener,null);
        }
		
        /** Construct one of these just before a source code segment which should never take more than timeout milliseconds; 
            immediately afterwards (assuming expected execution time...) call dontWorry().
            If the timeout happens, a mailto link will open with a bug report draft, and a message will appear asking the user
            to attach to the email the file fidji_Bug_Report.zip (@see #BUGREPORT). 
            Studio will exit or not depending on shouldExitfidji.
            If secondRun is not null: in secondRun's run method one can duplicate the code segment plus some more detailed debug logging 
            (such as sending setDebug(true) to the PrologEngine).
            In this case this Java's standard output will be redirected (@see FijiPreferences.initHiddenLogging).
            If listener is null, no debugging information from it will be considered. 
            If timeout is 0 (zero), the bug reporting procedure happens immediately.
            The first time in situation S, wait 3x the time. */
		
        public ImpatientBugReporter(long ms,String S, Runnable secondRun, boolean shouldExitfidji,  FijiSubprocessEngineWindow listener){
            this(ms,S,secondRun,shouldExitfidji,false,true,null,listener,null);
        }
		
        public ImpatientBugReporter(long ms,String S,final Runnable secondRun,final boolean shouldExitfidji,final boolean justLogIt, final boolean shouldEmail, final Object someContext, final FijiSubprocessEngineWindow listener, final AbstractPrologEngine suspect){
            final String situation;
            if (S==null) situation="";
            else situation = S;
            final long timeout = mayBeLonger(ms,situation);
            current = Thread.currentThread();
            handlingProblem = false;
            Runnable br = new Runnable(){
                    public void run(){
                        try{
                            Thread.sleep(timeout);
                            if (!ranOK){
                                handlingProblem = true;
                                Toolkit.getDefaultToolkit().beep(); Toolkit.getDefaultToolkit().beep();
                                if (justLogIt){
                                    System.err.println("IBR woke up for "+situation+", context:"+someContext);
                                } else {
                                    if (secondRun!=null){
                                        if (!quietLog)
                                            initHiddenLogging();
                                        new Thread(secondRun,"IBR second run").start();
                                        System.err.println("IBR started second run for "+situation);
                                        Thread.sleep(timeout);
                                        System.err.println("IBR woke up second time for "+situation);
                                    }
                                    File zipFile = new File(getFidjiDir(),BUGREPORT);
                                    ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile));

                                    File prefs = new File (getFidjiDir(),PREFS_FILE);
                                    zip.putNextEntry(new ZipEntry(prefs.getName())); // dates ok???
                                    copyInputStream(new FileInputStream(prefs), zip);
						
                                    if (listener!=null) {
                                        File LO = new File(getFidjiDir(),"ListenerOutput.txt"); 
                                        FileWriter LOF = new FileWriter(LO);
                                        LOF.write(listener.getOutputPane().getText());
                                        LOF.close();
                                        zip.putNextEntry(new ZipEntry(LO.getName()));
                                        copyInputStream(new FileInputStream(LO), zip);
                                        LO.delete();
                                    }
                                    for (int L = 0; L<LOG_ROLLOVER; L++){ //TODO: should not guess file names, should grab them from java.util.logging ...
                                        File logFile = new File(getFidjiDir(),".ergoAI"+L+".log");
                                        if (!logFile.exists()) continue;
                                        zip.putNextEntry(new ZipEntry(logFile.getName()));
                                        copyInputStream(new FileInputStream(logFile), zip);
                                    }
						
                                    File context = new File(getFidjiDir(),"context.txt");
                                    FileWriter COF = new FileWriter(context);
                                    COF.write("System:"+System.getProperty("java.version")+","+System.getProperty("os.name") + "," + System.getProperty("os.version")+"," + System.getProperty("user.home")+"\n");
                                    COF.write("Studio revision:"+buildRevision()+"\n");
                                    COF.write("Studio location:"+getFidjiDir()+"\n");
                                    if (extraWorry!=null)
                                        COF.write("Extra worry exception:"+extraWorry+"\n");
                                    COF.write("SomeContext:"+someContext);
                                    COF.write("Situation:"+situation+"\nThread stacks:\n");
                                    COF.write(AbstractPrologEngine.printAllStackTraces(true));
                                    COF.close();
                                    zip.putNextEntry(new ZipEntry(context.getName())); 
                                    copyInputStream(new FileInputStream(context), zip);
                                    context.delete();
						
						
                                    zip.closeEntry(); zip.close();
                                    if (shouldEmail){
                                        String body = 
                                            "Please attach the zip archive "
                                            +zipFile.getCanonicalPath()
                                            //+ "c:\\foo\\bar.zip" // test
                                            + " if it was created."
                                            + "\n\nYou can also add other relevant information below.\n\nPlease send this mail now, before continuing with "+STUDIO_NAME+". Thank you!\n";
                                        String subject = STUDIO_NAME+" bug report - "+situation;
                                        URI M = URI.create("mailto:"+MAILBOX+ "?subject="+encodeURI(subject)+"&body="
                                                           //+ URLEncoder.encode(body,"UTF-8")
                                                           +encodeURI(body)
                                                           );
                                        Desktop.getDesktop().mail(M); 
                                    }
						
                                    if (shouldExitfidji) 
                                        FijiSubprocessEngineWindow.abortStartup(
                                                                                "System operation took too long.\nPlease send the email that has just been drafted for you, including the zip file archive.");
                                }
                                if (suspect!=null){
                                    System.err.println("Asking the engine to stop it");
                                    suspect.stop();
                                } else {
                                    System.err.println("Interrupting the current thread");
                                    current.interrupt();
                                }
                            }
                        } catch(InterruptedException e){} // normal...
                        catch(Exception e){
                            System.err.println("Exception while reporting bug:"+e);
                        }
                    }
                };
            ranOK = false; 
            background = new Thread(br,situation);
            background.start();
            Thread.yield(); // try to make sure IBR's timer is able to kick in
        }
        public void dontWorry(){
            if (handlingProblem){
                handlingProblem = false;
                System.err.println("IBR seemed to have recovered");
            }
            ranOK=true;
            background.interrupt();
        }
        public void doWorry(Exception e){
            ranOK = false;
            extraWorry = e;
            // background.interrupt();
        }
    }
	
    // following from http://www.dmurph.com/2011/01/java-uri-encoder/
    private static final String mark = "-_.!~*'()\"";
    private static final char[] hex = "0123456789ABCDEF".toCharArray();

    public static String encodeURI(String argString) {
        StringBuilder uri = new StringBuilder();

        char[] chars = argString.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') || mark.indexOf(c) != -1) {
                uri.append(c);
            } else {
                appendEscaped(uri, c);
            }
        }
        return uri.toString();
    }

    private static void appendEscaped(StringBuilder uri, char c) {
        if (c <= (char) 0xF) {
            uri.append("%");
            uri.append('0');
            uri.append(hex[ c]);
        } else if (c==' ') {
            uri.append("%20");
        } else if (c==':') {
            uri.append("%3a");
        } else if (c==',') {
            uri.append("%2c");
        } else if (c=='/') {
            uri.append("%2f");
        } else if (c=='\\') {
            uri.append("%5c");
        } else if (c <= (char) 0xFF) {
            uri.append("%");
            uri.append(hex[ c >> 8]);
            uri.append(hex[ c & 0xF]);
        } else {
            // unicode
            uri.append('\\');
            uri.append('u');
            uri.append(hex[ c >> 24]);
            uri.append(hex[(c >> 16) & 0xF]);
            uri.append(hex[(c >> 8) & 0xF]);
            uri.append(hex[ c & 0xF]);
        }
    }

    /** Closes in afterwards, but not out */
    public static void copyInputStream(InputStream in, OutputStream out)
        throws IOException {

        byte[] buffer  = new byte[1024];
        int len;

        while((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);

        in.close();
    }

    static boolean mayLoadObjectSources(AbstractPrologEngine e){
        File F = new File (getFidjiDir(),OBJECT_SOURCES_FILENAME);
        if (!F.exists()) return true;
        return e.load_dynAbsolute(F);
    }

    /** Grabbed from http://stackoverflow.com/questions/1010919/adding-files-to-java-classpath-at-runtime?answertab=active#tab-top */
    public static void addSoftwareLibrary(File file) throws Exception {
        AbstractPrologEngine.addSoftwareLibrary(file);
    }
	
    static boolean copyTo(File from, File to){
        try {
            FileInputStream in = new FileInputStream(from);
            FileOutputStream out = new FileOutputStream(to);
            byte[] buffer = new byte[512];
            while (in.available()>0)
                out.write(buffer, 0, in.read(buffer));
            in.close(); out.close();
        } catch (IOException e) {
            System.err.println("Could not copy "+from+" to "+to);
            e.printStackTrace();
        }
        return true;
    }
	
    // This should be refactored to some Coherent-specific class (Ergo engine...?)
    public static boolean nlpSupported(){
        return fidjiNLPdir!=null;
    }

    static boolean addNLPJars(){
        File baseDir = fidjiNLPdir;
        try{
            addSoftwareLibrary(new File(baseDir,"ejml-0.23.jar"));
            addSoftwareLibrary(new File(baseDir,"joda-time.jar"));
            addSoftwareLibrary(new File(baseDir,"xom.jar"));
            addSoftwareLibrary(new File(baseDir,"jollyday.jar"));
            addSoftwareLibrary(new File(baseDir,"stanford-corenlp-3.3.0-models.jar"));
            addSoftwareLibrary(new File(baseDir,"stanford-corenlp-3.3.0.jar"));
            return true;
        } catch (Exception e){
            System.err.println("Could not add NLP jars:"+e);
        }
        return false;
    }
	
	
    private static String relativeStudioJar = "ergo_lib"+File.separator+"ergo2java"+File.separator+"java"+File.separator+"ergoStudio.jar";
	
    public static File ergo2javaJar(FloraSubprocessEngine engine){
        return new File(engine.getFloraDirectory(),relativeStudioJar);
    }

    private static String relativeJenaJar = "ergosuite"+File.separator+"jena"+File.separator+"JenaAllInOne.jar";
	
    public static File jenaJar(FloraSubprocessEngine engine){
        return new File(engine.getFloraDirectory(),relativeJenaJar);
    }
	
    private static String relativeOWL =
        "ergo_lib"+File.separator+"ergo2owl"+File.separator+
        "java"+File.separator+"ergoOWL.jar";
	
    public static File ergoOWLJar(FloraSubprocessEngine engine){
        return new File(engine.getFloraDirectory(),relativeOWL);
    }
    public static boolean ergoOWLSupported(FloraSubprocessEngine engine){
        return ergoOWLJar(engine).exists();
    }

    static boolean addErgoOWLJars(FloraSubprocessEngine engine){
        try{
            addSoftwareLibrary(new File(engine.getFloraDirectory(),relativeOWL));
            return true;
        } catch (Exception e){
            System.err.println("Could not add ErgoOWL jars:"+e);
        }
        return false;
    }

    private static String relativeSPARQL =
        "ergo_lib"+File.separator+"ergo2sparql"+File.separator+
        "java"+File.separator+"ergoSPARQL.jar";

    public static File ergoSPARQLjar(FloraSubprocessEngine engine){
        return new File(engine.getFloraDirectory(),relativeSPARQL);
    }
    public static boolean ergoSPARQLSupported(FloraSubprocessEngine engine){
        return ergoSPARQLjar(engine).exists();
    }

    static boolean addErgoSPARQLJars(FloraSubprocessEngine engine){
        try{
            addSoftwareLibrary(new File(engine.getFloraDirectory(),relativeSPARQL));
            return true;
        } catch (Exception e){
            System.err.println("Could not add ErgoSPARQL jars:"+e);
        }
        return false;
    }
	
    public static void launchJavaTool(String toolTitle,String classPath,String mainClass){
        System.out.println("Launching...");
        try {
            String[] commands = new String[]{ "java", "-cp", classPath, mainClass, FijiPreferences.getFidjiDir().getAbsolutePath() };
            // Runtime.getRuntime().exec(commands, null, FijiPreferences.fidjiDir);
            // ...but the following has no better error control than the above ;-)
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.directory(FijiPreferences.getFidjiDir());
            pb.start();
        } catch (IOException e1) {
            e1.printStackTrace();
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(null,"Error launching "+toolTitle,"Could not launch",JOptionPane.ERROR_MESSAGE);	
        }
    }

    public static File getFidjiDir() {
        return fidjiDir;
    }

}
