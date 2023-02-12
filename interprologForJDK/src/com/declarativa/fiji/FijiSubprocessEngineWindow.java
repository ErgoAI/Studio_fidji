/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2013
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.fiji;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import com.declarativa.fiji.project.AbstractProjectWindow;
import com.declarativa.fiji.project.EditableProjectWindow;
import com.declarativa.fiji.project.ErgoMemoryProjectModel;
import com.declarativa.fiji.project.VirtualProjectWindow;
import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.EngineController;
import com.declarativa.interprolog.FloraSubprocessEngine;
import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.SubprocessEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.XSBPeer;
import com.declarativa.interprolog.XSBSubprocessEngine;
import com.declarativa.interprolog.gui.StyledOutputPane;
import com.declarativa.interprolog.gui.SubprocessEngineWindow;
import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.IPInterruptedException;
import com.declarativa.interprolog.util.IPPrologError;
import com.declarativa.interprolog.util.InvisibleObject;
import com.xsb.xj.LazyListModel;
import com.xsb.xj.LazyTreeModel;
import com.xsb.xj.XJButton;
import com.xsb.xj.XJDesktop;
import com.xsb.xj.XJPrologEngine;
import com.xsb.xj.XJTextArea;
import com.xsb.xj.util.SplashWindow;
import com.xsb.xj.util.XJCommandHistory;
import com.xsb.xj.util.XJEngineController;
import com.xsb.xj.util.XJProgressDialog;

@SuppressWarnings("serial")
public class FijiSubprocessEngineWindow extends SubprocessEngineWindow {
    static SplashWindow splash = null;
    // too eager: static final String TRIVIAL_FLORA_OUTPUT = "(flora2 \\?- \\"+"nElapsed \\(CPU\\) time .* seconds\\"+"n\\"+"nYes\\"+"n\\"+"n)+";
    static final String TRIVIAL_FLORA_OUTPUT = "(flora2 \\?- \\"+"nElapsed \\(CPU\\) time [0-9\\.\\(\\)]+ seconds\\"+"n\\"+"nYes\\"+"n\\"+"n)+";
    // non-escaped:\rElapsed \(CPU\) time \d+\.\d+ \(\d+\.\d+\) seconds\r\rYes\r\rergo>
    //static final String TRIVIAL_ERGO_OUTPUT = "\\rElapsed \\(CPU\\) time \\d+\\.\\d+ \\(\\d+\\.\\d+\\) seconds\\r\\rYes\\r\\rergo>";
    static final String TRIVIAL_ERGO_OUTPUT = "\\nElapsed \\(CPU\\) time \\d+\\.\\d+ \\(\\d+\\.\\d+\\) seconds\\n\\nYes\\n\\rergo>";
    protected Pattern goalSuccessPattern;
    public static final String FLORA_LOAD = "\\load";
    public static final String FLORA_ADD = "\\add";
    public static final String ERGO_STUDIO_MODULE = "\\fidji";
    /** Same, but for injecting as part of Prolog goals */
    Container problemsPanel = null;
    LazyListModel problemsModel = null;
    Container floraQueryPanel = null;
    Container prologQueryPanel = null;
    XJTextArea floraQueryField = null, prologQueryField = null;
    XJButton floraQueryButton = null, prologQueryButton=null; 
    Container tablesPanel = null;
    LazyTreeModel tablesModel = null;
    final static Dimension NEW_WINDOW_SIZE = new Dimension(600,200);
    final static FileFilter logicFilesFilter = new FileNameExtensionFilter("Ergo, Flora-2, and Prolog files", "ergo", "ergotxt", "flr", "P", "p", "PL", "ab", "pl", "lpsp", "lpsw", "lps");
    final static FileFilter floraFilesFilter = new FileNameExtensionFilter("Flora-2 files", "flr");
    final static FileFilter ergoFilesFilter = new FileNameExtensionFilter("Ergo files", "ergo", "ergotxt");
    final static FileFilter prologFilesFilter = new FileNameExtensionFilter("Prolog files", "P", "pl", "p", "PL");
    final static FileFilter qualmFilesFilter = new FileNameExtensionFilter("QUALM files", "ab");
    final static FileFilter lpsFilesFilter = new FileNameExtensionFilter("LPS files", "lps", "lpsw", "lpsp", "P", "pl", "PL");
    final static String FIDJIEXTENSION = "ergoprj";
    final static FileFilter fidjiProjectFilesFilter = new FileNameExtensionFilter("Ergo project files", FIDJIEXTENSION);
    class JFileChooseWithCheck extends JFileChooser{
        public void approveSelection(){
            File f = getSelectedFile();
            if (getDialogType()!=JFileChooser.OPEN_DIALOG)
                super.approveSelection();
            else {
                if (!f.exists()) JOptionPane.showMessageDialog(FijiSubprocessEngineWindow.this,
                                                               "File does not exist",f.getAbsolutePath(),JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    /**
     * Adapter class to specify an old fashioned java.io.FilenameFilter from the union of javax.swing.filechooser.FileFilter(s)
     *
     */
    static class MyFilenameFilter implements FilenameFilter{
        ArrayList<FileFilter> swingFilters;
        MyFilenameFilter(FileFilter swingFilter){
            this(new FileFilter[]{swingFilter});
        }
        MyFilenameFilter(FileFilter[] swingFilters){
            this.swingFilters = new ArrayList<FileFilter>();
            for (FileFilter F:swingFilters)
                this.swingFilters.add(F);
        }
        @Override
        public boolean accept(File dir, String name) {
            File F = new File(dir,name);
            for(FileFilter filter:swingFilters)
                if (filter.accept(F))
                    return true;
            return false;
        }
        
    }
    /**
     * Emulates JFileChooser enough for our needs, on top of a good old fashioned (but native, hence more modern...) FileDialog.
     *
     */
    public static class MyFileDialog extends FileDialog{
        private File lastDirSet = null;
        MyFileDialog(){
            super((Frame)null);
        }
        public File getCurrentDirectory() {
            String D = getDirectory();
            return new File(D==null?"":D);
        }
        public void setCurrentDirectory(File D) {
            setDirectory(D.getAbsolutePath());
            lastDirSet = D;
        }
        public void setDialogTitle(String T) {
            setTitle(T);
        }
        public void setFileFilter(FileFilter swingfilter) {
            setFilenameFilter(new MyFilenameFilter(swingfilter));
        }
        /**
         * @return the selected file, guaranteed to exist in FileDialog.LOAD mode; null if it does not (due to some system file chooser buglet)
         */
        public File getSelectedFile() {
            String F = getFile();
            if (F==null) 
                return null;
            String D = getDirectory();
            File result = new File(D,F);
            if (getMode() == FileDialog.SAVE || result.exists())
                return result;
            else {
                if (getDirectory() == null || getDirectory().equals("")){
                    // There seems to be a buglet on Windows; turnaround with our memory:
                    result = new File(lastDirSet,F);
                    if (result.exists())
                        return result;
                    else return null;
                } else return null;
            }
        }
        /** Opens the modal LOAD dialog.
         * @param parent ignored
         * @return JFileChooser.CANCEL_OPTION or JFileChooser.APPROVE_OPTION
         */
        public int showOpenDialog(Component parent) {
            setMode(FileDialog.LOAD);
            int result = showDialog(parent);
            File f = getSelectedFile();
            if (f==null && result == JFileChooser.APPROVE_OPTION) {
                JOptionPane.showMessageDialog(this,
                                              "That file does not exist",getFile(),JOptionPane.ERROR_MESSAGE);
                return JFileChooser.CANCEL_OPTION;
            }
            return result;
        }
        /** Opens the modal SAVE dialog.
         * @param parent ignored
         * @return JFileChooser.CANCEL_OPTION or JFileChooser.APPROVE_OPTION
         */
        public int showSaveDialog(Component parent) {
            setMode(FileDialog.SAVE);
            return showDialog(parent);
        }
        private int showDialog(Component parent){
            setVisible(true);
            if (getFile()!=null) return JFileChooser.APPROVE_OPTION;
            else return JFileChooser.CANCEL_OPTION;
        }
        /**
         * @param parent ignored
         * @param approveButtonText ignored
         * @return JFileChooser.CANCEL_OPTION or JFileChooser.APPROVE_OPTION
         */
        public int showDialog(Component parent,String approveButtonText) {
            return showSaveDialog(parent);
        }
    }
    final MyFileDialog fileChooser = new MyFileDialog();
    public final MyFileDialog projectChooser = new MyFileDialog();
    /** Used for the Ergo Load/Add operations in the listener, allowing a module to be created or chosen */
    final JFileChooser fileChooser2 = new JFileChooser();
    PreferencesDialog preferencesDialog = null;
    JMenu otherLoadedFiles;
    WindowsMenuManager windowsMenuManager;
    public NewFloraAction newFloraAction; 
    public NewErgoAction newErgoAction;
    public NewErgoTextAction newErgoTextAction;
    NewPrologAction newPrologAction;
    public Action inMemoryAction;
    public FijiPreferences preferences;
    XJEngineController controller;
    static final String LISTENER_WINDOW_PREF = "com.declarativa.fiji.FijiSubprocessEngineWindow";
    static final String QUERYPANEL_PREF = "com.declarativa.fiji.FijiSubprocessEngineWindow.queryPanel";
    static final String COMPILATIONPANEL_PREF = "com.declarativa.fiji.FijiSubprocessEngineWindow.compilationPanel";
    static final String TABLESPANEL_PREF = "com.declarativa.fiji.FijiSubprocessEngineWindow.tablesPanel";
    static final String SECONDLISTENER_PREF = "com.declarativa.fiji.FijiSubprocessEngineWindow.secondEngineListener";
    static final String FIRST_ENGINE_MAX_STARTUP_PREF = "com.declarativa.fiji.FijiSubprocessEngineWindow.maxEngineStartup";
    static final String WINDOWS_SET_PREF = "com.declarativa.fiji.FijiSubprocessEngineWindow.visibleWindows";
    static final String REOPEN_WINDOWS_SET_PREF = "com.declarativa.fiji.FijiSubprocessEngineWindow.reopenWindows";
    private static final String ERGO_ABORT_NOTRACE = "_$ergo:abort_notrace";
    private static final String ERGO_USER_ABORT =  "_$ergo:user_abort";
    public static final Color OUTPUT_BACKGROUND = new Color(240,240,240);
    Color inputBackground = null;
    public static final String FONT_SIZE_PREF = "com.declarativa.fiji.FijiSubprocessEngineWindow.fontSize";
    public static final String VERBOSE_LISTENER = "com.declarativa.fiji.FijiSubprocessEngineWindow.verboseListener";
    public static final String INPUT_BACKGROUND_PREF = "com.declarativa.fiji.FijiSubprocessEngineWindow.inputBackground";
        
    /** Some code that must run on exit to keep preferences and other stuff; may run more than once */
    private Runnable preferencesCloser = new Runnable() {
            public void run() {
                // window bounds preferences are kept updated by events set up by updateBoundsPreferences
                // that and caret events are used in LogicProgramEditor
                preferences.setProperty("com.declarativa.fiji.FijiSubprocessEngineWindow.fileChooser",
                                        fileChooser.getCurrentDirectory().getAbsolutePath());
                preferences.setProperty("com.declarativa.fiji.FijiSubprocessEngineWindow.fileChooser2",
                                        fileChooser2.getCurrentDirectory().getAbsolutePath());
                preferences.setProperty("com.declarativa.fiji.FijiSubprocessEngineWindow.projectChooser",
                                        projectChooser.getCurrentDirectory().getAbsolutePath());                
                preferences.setProperty(WINDOWS_SET_PREF,visibleWindowsPreference());   
                preferences.store();
                commandHistory.save();
            }
        };
    /** Auxiliary validating engine, for the editor */
    static SubprocessEngine ve = null;
    static EngineController vec = null;
    
    /** Shared for all listener windows; if and when we have more than one, the last one will set this...*/
    public static int preferredFontSize;
        
    static Thread awtThread = null;
        
    /** (Prolog-compatible) file paths of all files loaded into usermod, thereby excluding files loaded into specific modules */
    String[] userModFiles = new String[0];
    /** file paths of all library directories currently known */
    String[] libraryDirectories = new String[0];
    /** current working directory of the main Prolog engine, as far as we know */
    String currentPrologDirectory = "";
    /** operator declarations inserted since startup */
    TermModel[] insertedOps = new TermModel[0];
    protected VirtualProjectWindow ergoMemoryProjectWindow;
    protected ErgoMemoryProjectModel ergoMemoryProjectModel;
    /** true if want to activate the timed_call based Pausing machinery. ctrl-C is always available... although it will probably crash it true */
    protected static final boolean useTimedCallPausing = false; 
    private String prologCleanupCommand;
        
    /**
     * @return the prologCleanupCommand
     */
    public String getPrologCleanupCommand() {
        return prologCleanupCommand;
    }

    /**
     * @param prologCleanupCommand a Prolog goal to execute before Studio exits
     */
    public void setPrologCleanupCommand(String prologCleanupCommand) {
        this.prologCleanupCommand = prologCleanupCommand;
    }

    private static void waitAndCleanUntilOutputMarker(String regex, final JTextComponent prologOutput, SubprocessEngine e){             
        Pattern P = Pattern.compile(regex,Pattern.DOTALL);
        Matcher M = P.matcher(prologOutput.getText());
        while( !M.find()){
            Thread.yield();
            M = P.matcher(prologOutput.getText());
        }
        final int endOfMarker = M.end();
        //final int beginOfMarker = (M.start()>0?M.start():1);
        SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    try{ 
                        prologOutput.getDocument().remove(0,endOfMarker);
                    } catch(BadLocationException ex){
                        System.err.println("Problem encountered while cleaning up listener clutter:"+ex);
                    }
                }
            });
    }

    public FijiSubprocessEngineWindow(SubprocessEngine e,FijiPreferences preferences){
        this(e,true,true,preferences);
    }
    public FijiSubprocessEngineWindow(SubprocessEngine e,boolean autoDisplay,FijiPreferences preferences){
        this(e,autoDisplay,true,preferences);
    }
    public FijiSubprocessEngineWindow(final SubprocessEngine e, boolean autoDisplay,boolean mayExitApp,FijiPreferences prefs){
        super(e,autoDisplay,mayExitApp,FijiPreferences.pref2Rectangle(prefs.getProperty(LISTENER_WINDOW_PREF)));
                
        if (topLevelCount>1)
            //throw new RuntimeException("Currently only one listener can exist");
            System.err.println("Currently only one listener can exist...continuing anyway...");
        if (splash!=null)
            splash.setProgress(90, "Almost done...");
        this.preferences=prefs;
                
                
        String inputBackgroundString = prefs.getProperty(INPUT_BACKGROUND_PREF); // new Color(255,255,180)
        if (inputBackgroundString == null){
            // FFFFED Color(255,255,244)
            inputBackgroundString = "16777204";
            prefs.put(INPUT_BACKGROUND_PREF, inputBackgroundString);
        }
        inputBackground = new Color(Integer.parseInt(inputBackgroundString));
                
        if (FijiPreferences.floraSupported){
            setTitle("Ergo Listener");
            FloraSubprocessEngine floraEngine = (FloraSubprocessEngine)e;
            String verbosity = prefs.getProperty(VERBOSE_LISTENER,"false");
            prefs.put(VERBOSE_LISTENER, verbosity);
            floraEngine.setVerbose(verbosity.equals("true"));
            //prologOutput.append("\nWelcome to "+ floraEngine.getLanguage() +" "+floraEngine.getFloraVersion()+"\n");
            //prologOutput.append("build "+ floraEngine.getFloraBuild() +" ("+floraEngine.getFloraReleaseDate()+")\n");
            floraEngine.getFloraReleaseDate(); // one more goal before the marker cleaning up...
            setTruncateOutput(false); // THAT mechanism still not working properly...
            prologOutput.setBackground(OUTPUT_BACKGROUND);
            prologInput.setBackground(inputBackground);
            controller.hintWhenIdleOrPaused(prologInput); // done after the previous so the background coloris correct
        } else {
            setTitle("Prolog Studio listener ("+e.getPrologVersion()+")");
            // prologOutput.append("Studio rev: "+FijiPreferences.buildRevision()+"\n");
            e.command("writeln('Studio rev: "+FijiPreferences.buildRevision()+"\n\n')");
        }
        if (useTimedCallPausing)
            disableWhenPaused(prologInput);

        System.err.println("Studio revision "+FijiPreferences.buildRevision()+"\n");
                
        // constructToolsMenu();
                
        JMenu windowsMenu = new JMenu("Windows");
        getJMenuBar().add(windowsMenu);
        addItemToMenu(windowsMenu,getTitle(),new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    setVisible(true);
                    toFront();
                }
            }).setToolTipText("Engine listener window");
        windowsMenu.addSeparator();
                
        windowsMenuManager = new WindowsMenuManager(windowsMenu);
                
        JMenu helpMenu = new JMenu("Help");
        getJMenuBar().add(helpMenu);
                
        goalSuccessPattern = null;
                
        if (FijiPreferences.floraSupported){
            if (((FloraSubprocessEngine)engine).isErgo())
                goalSuccessPattern = Pattern.compile(TRIVIAL_ERGO_OUTPUT,Pattern.DOTALL);
            else
                goalSuccessPattern = Pattern.compile(TRIVIAL_FLORA_OUTPUT,Pattern.DOTALL);
            final String language = ((FloraSubprocessEngine)engine).getLanguage();
            addItemToMenu(helpMenu,language+" Documentation",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        try{
                            Desktop.getDesktop().browse(new URI("http://coherentknowledge.com/ergo-documentation/"));
                            
                        }
                        catch(Exception ex){
                            System.err.println(ex);
                            errorMessage("Cannot open the Ergo documentation site");
                        }
                    }
                }).setToolTipText("Read Ergo documentation");

            /*
              addItemToMenu(helpMenu,language+"AI Tutorial",new ActionListener(){
              public void actionPerformed(ActionEvent e){
              try{
              Desktop.getDesktop().browse(new URI("https://sites.google.com/a/coherentknowledge.com/ergo-suite-tutorial/"));
                                                                             
              }
              catch(Exception ex){
              System.err.println(ex);
              errorMessage("Can't open the ErgoAI Tutorial site");
              }
              }
              }).setToolTipText("Read the ErgoAI Tutorial");
              addItemToMenu(helpMenu,language+" Studio Manual",new ActionListener(){
              public void actionPerformed(ActionEvent e){
              try{
              Desktop.getDesktop().browse(new URI("https://docs.google.com/document/u/2/d/1k_zNnvnEeWWuhvM4cn93i3nFi0VxKCdrNEypYtzhKEY/pub"));
                                                                             
              }
              catch(Exception ex){
              System.err.println(ex);
              errorMessage("Can't open the Studio Manual site");
              }
              }
              }).setToolTipText("Read the Ergo Studio Manual");
              helpMenu.addSeparator();
                        
              addItemToMenu(helpMenu,language+" Reasoner User's Manual",new ActionListener(){
              public void actionPerformed(ActionEvent e){
              File F = new File(((FloraSubprocessEngine)engine).getFloraDirectory(),"docs"+File.separatorChar+language.toLowerCase()+"-manual.pdf");
              if (F.exists()){
              try{
              Desktop.getDesktop().open(F);
              } catch (IOException ex){
              errorMessage("Error opening the manual:\n"+ex);
              }
              } else errorMessage("The "+language+" manual file " + F.getAbsolutePath() + " is missing");
              }
              }).setToolTipText("Open the "+language+" manual with a PDF reader");
                
              addItemToMenu(helpMenu,language+" Packages Manual",new ActionListener(){
              public void actionPerformed(ActionEvent e){
              File F = new File(((FloraSubprocessEngine)engine).getFloraDirectory(),"docs"+File.separatorChar+language.toLowerCase()+"-packages.pdf");
              if (F.exists()){
              try{
              Desktop.getDesktop().open(F);
              } catch (IOException ex){
              errorMessage("Error opening the packages manual:\n"+ex);
              }
              } else errorMessage("The "+language+" packages manual file " + F.getAbsolutePath() + " is missing");
              }
              }).setToolTipText("Open the "+language+" packages manual with a PDF reader");
            */
                        
            if (!((FloraSubprocessEngine)engine).isErgo()) {
                addItemToMenu(helpMenu,"Open "+language+" Example...",new ActionListener(){
                        public void actionPerformed(ActionEvent e){
                            File current = fileChooser.getCurrentDirectory();
                            File demos = new File(((FloraSubprocessEngine)engine).getFloraDirectory(),"demos/");
                            if (demos.exists()){
                                fileChooser.setCurrentDirectory(demos);
                                openFile(FijiSubprocessEngineWindow.this, floraFilesFilter, "Open "+language+" Example");
                            } else errorMessage("The demos directory is missing");
                            fileChooser.setCurrentDirectory(current);
                        }
                    }).setToolTipText("Open one of the distributed examples");
                helpMenu.addSeparator();
            }
        }

        if (!FijiPreferences.floraSupported){
            addItemToMenu(helpMenu,"XSB Manual, Vol. 1 (Programmer's Manual)",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        File F = new File(engine.getPrologBaseDirectory(),"docs"+File.separatorChar+"userman"+File.separatorChar+"manual1.pdf");
                        if (F.exists()){
                            try{
                                Desktop.getDesktop().open(F);
                            } catch (IOException ex){
                                errorMessage("Error opening the manual:\n"+ex);
                            }
                        } else errorMessage("The XSB Manual Vol. 1 file " + F.getAbsolutePath() + " is missing");
                    }
                }).setToolTipText("Open XSB Manual Vol. 1 with a PDF reader");
            
            addItemToMenu(helpMenu,"XSB Manual, Vol. 2 (Interfaces & Packages)",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        File F = new File(engine.getPrologBaseDirectory(),"docs"+File.separatorChar+"userman"+File.separatorChar+"manual2.pdf");
                        if (F.exists()){
                            try{
                                Desktop.getDesktop().open(F);
                            } catch (IOException ex){
                                errorMessage("Error opening the manual:\n"+ex);
                            }
                        } else errorMessage("The XSB Manual Vol. 2 file " + F.getAbsolutePath() + " is missing");
                    }
                }).setToolTipText("Open XSB Manual Vol. 2 with a PDF reader");
        }
                
        if (!FijiPreferences.floraSupported) addItemToMenu(helpMenu,"Open XSB Example...",new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    File current = fileChooser.getCurrentDirectory();
                    File examples = new File(engine.getPrologBaseDirectory(),"examples/");
                    if (examples.exists()){
                        fileChooser.setCurrentDirectory(examples);
                        openFile(FijiSubprocessEngineWindow.this, prologFilesFilter, "Open XSB Example");
                    } else errorMessage("The XSB examples directory is missing");
                    fileChooser.setCurrentDirectory(current);
                }
            }).setToolTipText("Open one of the examples in XSB Prolog's distribution");
                
        if (!FijiPreferences.floraSupported && preferences.getQUALMdir()!=null) { 
            helpMenu.addSeparator();
            addItemToMenu(helpMenu,"Open QUALM Example...",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        File current = fileChooser.getCurrentDirectory();
                        File examples = new File(preferences.getQUALMdir(),"examples/");
                        if (examples.exists()){
                            fileChooser.setCurrentDirectory(examples);
                            openFile(FijiSubprocessEngineWindow.this, qualmFilesFilter, "Open QUALM Example");
                        } else errorMessage("The QUALM examples directory is missing");
                        fileChooser.setCurrentDirectory(current);
                    }
                }).setToolTipText("Open one of the examples in QUALM's distribution");
        }

        if (!FijiPreferences.floraSupported && preferences.getLPSdir()!=null) { 
            helpMenu.addSeparator();
            addItemToMenu(helpMenu,"Open LPS Example...",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        File current = fileChooser.getCurrentDirectory();
                        File examples = new File(preferences.getLPSdir(),"examples/");
                        if (examples.exists()){
                            fileChooser.setCurrentDirectory(examples);
                            openFile(FijiSubprocessEngineWindow.this, lpsFilesFilter, "Open LPS Example");
                        } else errorMessage("The LPS examples directory is missing");
                        fileChooser.setCurrentDirectory(current);
                    }
                }).setToolTipText("Open one of the examples in LPS's distribution");
        }

        if (!FijiPreferences.floraSupported) {
            helpMenu.addSeparator();
            addItemToMenu(helpMenu,"Studio Wiki",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        try{
                            Desktop.getDesktop().browse(new URI("http://interprolog.com/wiki/index.php?title=Prolog_Studio"));
                        }
                        catch(Exception ex){
                            System.err.println(ex);
                            errorMessage("Can't open the wiki web site");
                        }
                    }
                }).setToolTipText("Open the InterProlog Studio web site on your browser");
        }
                
        helpMenu.addSeparator();
        addItemToMenu(helpMenu,"Send Bug Report",new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    new FijiPreferences.ImpatientBugReporter("Help menu",FijiSubprocessEngineWindow.this);
                }
            }).setToolTipText("Automatically gather debugging information and draft an email to technical support");

        Runtime.getRuntime().addShutdownHook(new Thread(preferencesCloser, "Fidji prefs saver"));
        String preferredDir = getPreference("com.declarativa.fiji.FijiSubprocessEngineWindow.fileChooser");
        File preferredDirF;
        if (preferredDir!=null && (preferredDirF=new File(preferredDir)).exists() ) 
            fileChooser.setCurrentDirectory(preferredDirF);
        String preferredDir2 = getPreference("com.declarativa.fiji.FijiSubprocessEngineWindow.fileChooser2");
        File preferredDirF2;
        if (preferredDir2!=null && (preferredDirF2=new File(preferredDir2)).exists() ) 
            fileChooser2.setCurrentDirectory(preferredDirF2);
        preferredDir = getPreference("com.declarativa.fiji.FijiSubprocessEngineWindow.projectChooser");
        if (preferredDir!=null && (preferredDirF=new File(preferredDir)).exists() ) 
            projectChooser.setCurrentDirectory(preferredDirF);
        String PFS = getPreference(FONT_SIZE_PREF);
        if (PFS!=null) preferredFontSize = Integer.parseInt(PFS);
        else {
            preferredFontSize = 13;
            preferences.put(FONT_SIZE_PREF, preferredFontSize+"");
        }
                
        setPreferredFontSize();
        engine.waitUntilAvailable();

        if (!e.deterministicGoal("retractall(ipFontSize(_)), asserta(ipFontSize("+preferredFontSize+"))"))
            throw new IPException("could not assert ipFontSize"); 

        if (!e.deterministicGoal("retractall(ipEngineController(_)), asserta(ipEngineController("+e.registerJavaObject(controller)+"))"))
            throw new IPException("could not assert ipEngineController"); 
        engine.addPrologEngineListener(controller); // see comment in toolsMenu() ;-)
                
        String tcis = getPreference("com.declarativa.fiji.FijiSubprocessEngineWindow.timedCallInterval");
                
        int timeCallInterval = useTimedCallPausing?250:0;
        try{
            if (tcis!=null) timeCallInterval = Integer.parseInt(tcis);
        } catch (NumberFormatException nfe){}
        if (!FijiPreferences.floraSupported) // Ergo Studio prefers not to use timed call
            preferences.setProperty("com.declarativa.fiji.FijiSubprocessEngineWindow.timedCallInterval",timeCallInterval+"");
                                
        if (useTimedCallPausing && !e.deterministicGoal("assert(ipStopBall(error(misc_error,'"+EngineController.STOP_MESSAGE+"',Rest)))"))
            throw new IPException("could not assert fjStopBall"); 

        if (FijiPreferences.floraSupported){
            if (tcis!=null)
                System.err.println("Ignoring preference "+"com.declarativa.fiji.FijiSubprocessEngineWindow.timedCallInterval");
            // Let listener-originated computations be Stoppable in Flora and Ergo; this requires interprolog loaded:
            if ( useTimedCallPausing && !((FloraSubprocessEngine)engine).floraCommand("setruntime{timeout(repeating(1,ipReportWork(?)))}") ||
                 !engine.deterministicGoal("import ipReportWork/1 from interprolog") )
                System.err.println("Failed to activate Stop/Pause for listener window");
            if (!useTimedCallPausing)
                breakAction.setConfirm(false); // allow violent ctrl-C...
            ergoMemoryProjectModel = new ErgoMemoryProjectModel(this);
        } else engine.setTimedCallIntervall(timeCallInterval);
                
        e.deterministicGoal("assert(xpGlobalProgressController("+e.registerJavaObject(this)+"))");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        updateBoundsPreference(LISTENER_WINDOW_PREF,this);
                
        Box topPanel = new Box(BoxLayout.X_AXIS);
        /* uncomment this if you want an engine state label on the top level corner of the listener
           JButton stateButton = new JButton(controller.engineStateAction){
           // This is really a "label", we use a JButton to tap the Actions machinery
           public void processMouseEvent(MouseEvent e) {}
           };
           stateButton.setBackground(getContentPane().getBackground());
           stateButton.setOpaque(true); stateButton.setBorderPainted(false); 
           topPanel.add(stateButton); */
        topPanel.add(Box.createGlue());
        topPanel.add(new JButton(controller.pauseContinueAction));
        topPanel.add(new JButton(controller.stopAction));
        getContentPane().add(BorderLayout.NORTH,topPanel);
    }

    /** Redefining to remove InterProlog greeting */
    protected void listenerGreeting(PrologEngine e){
    }

    static class WindowsMenuManager implements EditorLifecycleListener{
        JMenu prototype;
        ArrayList<JMenu> menus;
                
        WindowsMenuManager(JMenu first){
            menus = new ArrayList<JMenu>();
            prototype = first;
            menus.add(prototype);
        }

        JMenu makeWindowsMenu(){
            JMenu R = copyMenu(prototype);
            menus.add(R);
            return R;
        }
                
        /** Copies a menu into a new menu, ignoring submenus and action listeners beyond the first */
        static JMenu copyMenu(JMenu menu){
            JMenu R = new JMenu(menu.getText());
            for (int i = 0; i< menu.getItemCount() ; i++){
                JMenuItem item = menu.getItem(i);
                if (item==null) 
                    R.addSeparator();
                else{
                    ActionListener[] listeners = item.getActionListeners();
                    if (listeners.length==0) R.addSeparator();
                    else if (listeners[0] instanceof Action) R.add((Action)listeners[0]);
                    else {
                        JMenuItem newItem = new JMenuItem(item.getText());
                        newItem.addActionListener(listeners[0]);
                        R.add(newItem);
                    }
                }
            }
            return R;
        }
        // EditorLifecycleListener methods:
        public void didCreate(LogicProgramEditor editor){
            for (int i=0; i<menus.size(); i++){
                JMenu menu = menus.get(i);
                if ((editor.windowsMenu==menu))
                    continue;
                else
                    menu.add(editor.showWindowAction);
            }
        }
        public void willDestroy(LogicProgramEditor editor){
            for (int i=0; i<menus.size(); i++){
                JMenu windowsMenu = menus.get(i);
                JMenuItem item = findItemWithListener(windowsMenu,editor.showWindowAction);
                if (item!=null) 
                    windowsMenu.remove(item);
            }
        }
                
    }
        
    /** Return the first menu item with L as listener (by using equals(), thereby allowing other tests besides object identity),
        or null if it doesn't find any */
    public static JMenuItem findItemWithListener(JMenu menu, ActionListener L){
        for (int i=0; i<menu.getItemCount(); i++){
            if (menu.getItem(i)==null)
                continue;
            ActionListener[] listeners = menu.getItem(i).getActionListeners();
            for (int a=0;a<listeners.length;a++)
                if (listeners[a].equals(L)){
                    return menu.getItem(i);
                }
        }
        return null;
    }
        
    void setPreferredFontSize(){
        prologInput.setFont(prologInput.getFont().deriveFont((float)preferredFontSize));
        prologOutput.setFont(prologOutput.getFont().deriveFont((float)preferredFontSize));
    }
        
    void showTablesPanel(){
        boolean doRefresh = true;
        if (tablesPanel==null) {
            Object[] b = engine.deterministicGoal("xjTableTree(GUI,TM)","[GUI,TM]");
            if (checkFailed(b==null))
                return;
            tablesPanel = (Container)engine.getRealJavaObject((InvisibleObject)b[0]);
            tablesModel = (LazyTreeModel)engine.getRealJavaObject((InvisibleObject)b[1]);
            // now done on the Prolog side: disableWhenBusy(tablesPanel);
            doRefresh = false; // no need to refresh twice
            Object W = XJDesktop.findWindowOrSimilar(tablesPanel);
            if (W instanceof Window){
                Rectangle R = FijiPreferences.pref2Rectangle(preferences.getProperty(TABLESPANEL_PREF));
                if (R!=null) ((Window)W).setBounds(R);
                updateBoundsPreference(TABLESPANEL_PREF,(Window)W);
                // now done on the Prolog side: addWindowsMenuTo((Window)W);
            }
        }
        if (doRefresh) tablesModel.invalidateAndRefresh();
        XJDesktop.bringToFront(tablesPanel);
    }
        
    public void refreshProblemsPanel(final boolean popup){
        boolean doRefresh = true;
        if (problemsPanel==null && popup) {
            Object[] b = engine.deterministicGoal("fjProblemsPanel(GUI,LM)","[GUI,LM]");
            if (checkFailed(b==null))
                return;
            problemsPanel = (Container)engine.getRealJavaObject((InvisibleObject)b[0]);
            problemsModel = (LazyListModel)engine.getRealJavaObject((InvisibleObject)b[1]);
            doRefresh = false; // no need to refresh twice
            Object W = XJDesktop.findWindowOrSimilar(problemsPanel);
            if (W instanceof Window){
                Rectangle R = FijiPreferences.pref2Rectangle(preferences.getProperty(COMPILATIONPANEL_PREF));
                if (R!=null) ((Window)W).setBounds(R);
                updateBoundsPreference(COMPILATIONPANEL_PREF,(Window)W);
                addWindowsMenuTo((Window)W);
            }
        }
        final boolean doRefresh_ = doRefresh;
        SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run() {
                    if (doRefresh_ && problemsPanel!=null) 
                        problemsModel.invalidateAndRefresh();
                    if (popup && problemsPanel!=null)
                        XJDesktop.bringToFront(problemsPanel);
                }
                        
            });
    }
                
    /** if query is null does not set it */
    // some redundancy in this and following method, somehow price for lack of Java-side structuring...
    // TODO: this could be cleaned up on either side of the border
    public void showFloraQuery(String query){
        showFloraQuery(query, false);
    }
    public void showFloraQuery(){
        showFloraQuery(null);
    }
    public void showFloraQuery(String query, boolean autoRun){
        if (floraQueryPanel==null) {
            int pauseContinue = engine.registerJavaObject(new XJButton(controller.pauseContinueAction,engine));
            int stop = engine.registerJavaObject(new XJButton(controller.stopAction,engine));
            Object[] b = engine.deterministicGoal("xjLogicQueryPanel(flora,"+pauseContinue+","+stop+",GUI,Query,Executor)", "[GUI,Query,Executor]");
            if (checkFailed(b==null))
                return;
            floraQueryPanel = (Container)engine.getRealJavaObject((InvisibleObject)b[0]);
            floraQueryField = (XJTextArea)engine.getRealJavaObject((InvisibleObject)b[1]);
            floraQueryButton = (XJButton)engine.getRealJavaObject((InvisibleObject)b[2]);
            //floraQueryField.setOpaque(true);
            floraQueryField.getTextArea().setBackground(inputBackground);
            controller.hintWhenIdleOrPaused(floraQueryField.getTextArea());
            disableWhenBusyOrPaused(floraQueryButton);
            disableWhenBusyOrPaused(floraQueryField);
            Object W = XJDesktop.findWindowOrSimilar(floraQueryPanel);
            if (W instanceof Window){
                String P = preferences.getProperty(QUERYPANEL_PREF);
                if (P!=null){
                    Rectangle R = FijiPreferences.pref2Rectangle(P);
                    if (R!=null) ((Window)W).setBounds(R);
                }
                updateBoundsPreference(QUERYPANEL_PREF,(Window)W);
                addWindowsMenuTo((Window)W);
            }
        }
        XJDesktop.bringToFront(floraQueryPanel);
        floraQueryField.selectGUI(null);
        if (query!=null){
            floraQueryField.setText(query);
            if (autoRun)
                new Thread(new Runnable(){
                        public void run(){ floraQueryButton.doClick(100);}
                    }, "Query runner").start();
        }
    }
    public void showPrologQuery(){
        showPrologQuery(null);
    }
    public void showPrologQuery(String query){
        showPrologQuery(query,false);
    }
    public void showPrologQuery(String query,boolean autoRun){
        if (prologQueryPanel==null) {
            int pauseContinue = engine.registerJavaObject(new XJButton(controller.pauseContinueAction,engine));
            int stop = engine.registerJavaObject(new XJButton(controller.stopAction,engine));
            Object[] b = engine.deterministicGoal("xjLogicQueryPanel(prolog,"+pauseContinue+","+stop+",GUI,Query,Executor)",
                                                  "[GUI,Query,Executor]");
            if (checkFailed(b==null))
                return;
            prologQueryPanel = (Container)engine.getRealJavaObject((InvisibleObject)b[0]);
            prologQueryField = (XJTextArea)engine.getRealJavaObject((InvisibleObject)b[1]);
            prologQueryButton = (XJButton)engine.getRealJavaObject((InvisibleObject)b[2]);
            disableWhenBusyOrPaused(prologQueryButton);
            disableWhenBusyOrPaused(prologQueryField);
            Object W = XJDesktop.findWindowOrSimilar(prologQueryPanel);
            if (W instanceof Window)
                addWindowsMenuTo((Window)W);
        }
        XJDesktop.bringToFront(prologQueryPanel);
        if (query!=null){
            prologQueryField.setText(query);
            if (autoRun)
                new Thread(new Runnable(){
                        public void run(){ prologQueryButton.doClick(100);}
                    },"Prolog Query runner").start();
        }
    }
    public void addWindowsMenuTo(Container W){
        addWindowsMenuTo(W,null);
    }
    /**
     * @param W window 
     * @param tabAction if not null, the action to embed the window in a tabbed pane
     */
    public void addWindowsMenuTo(final Container W, final Action tabAction){
        addWindowsMenuTo(W,tabAction,false);
    }
    public void addWindowsMenuTo(final Container W, final Action tabAction, boolean immediate){
        Runnable doer = new Runnable(){
                @Override
                public void run() {
                    if (W instanceof JFrame){
                        JFrame J = (JFrame)W;
                        if (J.getJMenuBar()==null){
                            J.setJMenuBar(new JMenuBar());
                            J.pack();
                        }
                        JMenu qpWindowsMenu = windowsMenuManager.makeWindowsMenu();
                        if (tabAction!=null){
                            qpWindowsMenu.insertSeparator(0);
                            qpWindowsMenu.insert(tabAction,0);
                        }
                        J.getJMenuBar().add(qpWindowsMenu);
                    }
                }
            };
        if (immediate) doer.run();
        else SwingUtilities.invokeLater(doer);
    }
                
    protected JMenu constructToolsMenu(){
        String language = "??";
        if (FijiPreferences.floraSupported)
            language = ((FloraSubprocessEngine)engine).getLanguage();
        //here because this code runs before our own constructor... kludgy:
        if (useTimedCallPausing) {
            //controller = new XJEngineController(null, !FijiPreferences.floraSupported);
            controller = new XJEngineController(null, true);
        } else {
            //controller = new XJEngineController((SubprocessEngine)engine, !FijiPreferences.floraSupported); 
            controller = new XJEngineController((SubprocessEngine)engine, true);
        }
        
        if (FijiPreferences.floraSupported){
            Color busyColor, needsMoreInputColor, idleOrPausedColor;
            controller.setHintsForFields("Running...", language.toLowerCase()+">", "Waiting for more input...");
            controller.setLabelsForState("Running...", "Idle", "Waiting...");
            busyColor = new Color(249,249,202);
            needsMoreInputColor = new Color(232,197,116);
            idleOrPausedColor = prologInput.getBackground();
            controller.setColorsForFields(busyColor, idleOrPausedColor, needsMoreInputColor);
        } else {
            controller.setHintsForFields("Running...", "?-", "Waiting for more input...");
            controller.setLabelsForState("Running...", "...?", "Waiting...");
        }
        JMenu toolMenu = new JMenu("Tools"); 
        toolMenu.setMnemonic('T');

        if (FijiPreferences.floraSupported){
            disableWhenBusyOrPaused(addItemToMenu(toolMenu,"Query",KeyEvent.VK_U,new ActionListener(){ 
                    public void actionPerformed(ActionEvent e){
                        if (!checkEngineAvailable()) return;
                        showFloraQuery(null);
                    }
                }));
                
            addItemToMenu(toolMenu,"Compilation Messages",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        if (!checkEngineAvailable()) return;
                        refreshProblemsPanel(true);
                    }
                });
            /**/
            addItemToMenu(toolMenu,"Term Finder (experimental)",KeyEvent.VK_F,new ActionListener(){ 
                    public void actionPerformed(ActionEvent e){
                        new FinderWindow(FijiSubprocessEngineWindow.this);
                    }
                });
            addItemToMenu(toolMenu,"Rule Call Graph (experimental)",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        if (!checkEngineAvailable()) return;
                        boolean ok = engine.deterministicGoal("fjCallsGraph");
                        checkFailed(!ok);
                    }
                });
            /**/
        }
                
        boolean owlSupported = false, sparqlSupported=false;
        if (FijiPreferences.floraSupported){
            owlSupported = FijiPreferences.ergoOWLSupported((FloraSubprocessEngine)engine);
            sparqlSupported = FijiPreferences.ergoSPARQLSupported((FloraSubprocessEngine)engine);
        }
                        
        if (FijiPreferences.floraSupported && (FijiPreferences.nlpSupported()) || owlSupported || sparqlSupported)
            toolMenu.addSeparator();

        if (FijiPreferences.floraSupported){
            final FloraSubprocessEngine fe = (FloraSubprocessEngine)engine;
            if (owlSupported)
                addItemToMenu(toolMenu,"Ergo-to-OWL",new ActionListener(){
                        public void actionPerformed(ActionEvent e){
                            FijiPreferences.launchJavaTool(
                                                           "ErgoOWL", 
                                                           FijiPreferences.ergoOWLJar(fe).getAbsolutePath()+File.pathSeparator
                                                           +FijiPreferences.ergo2javaJar(fe).getAbsolutePath()+File.pathSeparator
                                                           +FijiPreferences.jenaJar(fe).getAbsolutePath(), 
                                                           "com.coherentknowledge.ergo.owl.gui.ErgoOWL_GUI");                                              
                        }
                    });
            if (sparqlSupported)
                addItemToMenu(toolMenu,"Ergo-to-SPARQL",new ActionListener(){
                        public void actionPerformed(ActionEvent e){
                            FijiPreferences.launchJavaTool(
                                                           "ErgoSPARQL", 
                                                           FijiPreferences.ergoSPARQLjar(fe).getAbsolutePath()+File.pathSeparator
                                                           +FijiPreferences.ergo2javaJar(fe).getAbsolutePath()+File.pathSeparator
                                                           +FijiPreferences.jenaJar(fe).getAbsolutePath(), 
                                                           "com.coherentknowledge.ergo.sparql.gui.ErgoSPARQL_GUI");                                                                                           
                        }
                    });                         
        }
        // DISABLE NLP MENU UNTIL IMPLEMENTED
        if (false && FijiPreferences.floraSupported && FijiPreferences.nlpSupported()){
            JMenu NLPmenu = new JMenu("NLP"); 
            addItemToMenu(NLPmenu,"Convert English to basic theory",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        System.out.println("TBD!");
                        beep();
                        JOptionPane.showMessageDialog(FijiSubprocessEngineWindow.this,"In development","Not Yet",JOptionPane.ERROR_MESSAGE);    
                    }
                });
            toolMenu.add(NLPmenu);
        }

        JMenu prologMenu = toolMenu;
        if (FijiPreferences.floraSupported) {
            // no need for Pause/Abort, as we have buttons
            //toolMenu.addSeparator();
            //toolMenu.add(controller.pauseContinueAction);
            //toolMenu.add(controller.stopAction);
            // Still buggy: toolMenu.add(breakAction);
            toolMenu.addSeparator();
            prologMenu = new JMenu("Prolog");
            toolMenu.add(prologMenu);
        }
        disableWhenBusyOrPaused(addItemToMenu(prologMenu,"Prolog Query",new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    if (!checkEngineAvailable()) return;
                    showPrologQuery(null);
                }
            }));
                
        if (FijiPreferences.floraSupported) {
            final JMenuItem prologShell = new JMenuItem("Switch to Prolog Shell");
            disableWhenBusyOrPaused(prologShell);
            final JMenuItem floraShell = new JMenuItem("Return to "+language+" Shell");
            disableWhenBusyOrPaused(floraShell);
            prologMenu.add(floraShell); prologMenu.add(prologShell);
            floraShell.setEnabled(false);
            ActionListener changeToProlog = new ActionListener(){
                    public void actionPerformed(ActionEvent ev){
                        prologShell.setEnabled(false); floraShell.setEnabled(true);
                        ((FloraSubprocessEngine)engine).setPrologShell();
                    }
                };
            ActionListener changeToFlora = new ActionListener(){
                    public void actionPerformed(ActionEvent ev){
                        floraShell.setEnabled(false);prologShell.setEnabled(true);
                        ((FloraSubprocessEngine)engine).setFloraShell();
                    }
                };
            prologShell.addActionListener(changeToProlog);
            floraShell.addActionListener(changeToFlora);
        }
        // MK: This has nothing to do with the end user
        //prologMenu.addSeparator();
        //addInterPrologItems(prologMenu);        
                
        if (!FijiPreferences.floraSupported) {
            // avoid redundant item
            prologMenu.add(controller.stopAction);
            prologMenu.add(interruptAction); // Coherent doesn't want this, users can currently use ctrl-C in listener
        }
                
                 
        return toolMenu;
    }
        
    protected JMenu constructFileMenu(JMenuBar mb){
        JMenu fileMenu;
        fileMenu = new JMenu("File"); 
        fileMenu.setMnemonic('F');
        mb.add(fileMenu);
        
        // These must be created here because this method executes prior to the class constructor:
        newFloraAction = new NewFloraAction();
        newErgoAction = new NewErgoAction();
        newErgoTextAction = new NewErgoTextAction();
        newPrologAction = new NewPrologAction();
        
        JMenu newSubmenu = new JMenu("New...");
        fileMenu.add(newSubmenu);
        if (FijiPreferences.floraSupported) {
            if (((FloraSubprocessEngine)engine).isErgo()) {
                newSubmenu.add(newErgoAction);
                newSubmenu.add(newErgoTextAction);
            } else
                newSubmenu.add(newFloraAction);
        }
        newSubmenu.add(newPrologAction);

        /* addItemToMenu(fileMenu,"Test...",KeyEvent.VK_O, new ActionListener(){
           public void actionPerformed(ActionEvent e){
           FileDialog D = new FileDialog(FijiSubprocessEngineWindow.this,"Hello!",FileDialog.SAVE);
           D.setVisible(true);
           System.out.println(D.getDirectory()+"   "+D.getFile());
           }
           }).setToolTipText("Open a knowledge base file for editing");*/

        addItemToMenu(fileMenu,"Open...",KeyEvent.VK_O, new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    openFile();
                }
            }).setToolTipText("Open a knowledge base file for editing");
        fileMenu.addSeparator();
        addItemToMenu(fileMenu,"Load...",KeyEvent.VK_L, new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    reconsultFile();
                }
            }).setToolTipText("Load (consult) knowledge base file");
                
        if (FijiPreferences.floraSupported)  {
            addItemToMenuALT(fileMenu,"Add...",
                          KeyEvent.VK_L,
                          new ActionListener(){
                              public void actionPerformed(ActionEvent e){
                                  addFile();
                              }
                          }
                          ).setToolTipText("Add Ergo file to a module");
        }
                                
                
        if (!FijiPreferences.floraSupported && engine.getImplementationPeer() instanceof XSBPeer)
            addItemToMenu(fileMenu,"Load dynamically...",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        setWaitCursor();
                        load_dynFile();
                        System.err.println("load_dynFile concluded");
                        if (checkEngineAvailable()) 
                            refreshUserModFilesAndLibDirsAndOps();
                        restoreCursor();
                    }
                }).setToolTipText("load_dyn a Prolog file");
                
                
        if(FijiPreferences.floraSupported){ // Project stuff not really working  Flora-less:
            fileMenu.addSeparator();
            fileMenu.add(new NewProjectAction());
            fileMenu.add(new OpenProjectAction());
                        
            inMemoryAction = new AbstractAction("Loaded Ergo files"){
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        refreshErgoMemoryProjectWindow();
                    }
                };
            inMemoryAction.putValue(Action.SHORT_DESCRIPTION, "Display all files loaded into Ergo; to refresh choose the menu again");
            fileMenu.add(inMemoryAction);
        }
        // Made obsolete by ErgoMemoryProject
        //fileMenu.addSeparator();
        //otherLoadedFiles = new JMenu("Open Loaded");
        //fileMenu.add(otherLoadedFiles);
        //otherLoadedFiles.setToolTipText("Edit a file already loaded with this menu, but not yet open in a window");
                
        fileMenu.addSeparator();
        /*
          fileMenu.add(new AbstractAction("Restore Factory Windows"){
          @Override
          public void actionPerformed(ActionEvent e) {
          applyFactoryWindowPreferences();
          }                     
          });*/
        fileMenu.add(new AbstractAction("Preferences"){
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (preferencesDialog == null)
                        preferencesDialog = PreferencesDialog.makePreferencesDialog(FijiSubprocessEngineWindow.this);
                    preferencesDialog.setVisible(true);
                    String verbosity = preferences.getProperty(VERBOSE_LISTENER);
                    if (engine.isAvailable())
                        ((FloraSubprocessEngine)engine).setVerbose(verbosity.equals("true"));

                }                       
            });
       
        fileMenu.addSeparator();
        addItemToMenu(fileMenu,"Quit", KeyEvent.VK_Q, new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    mayQuit();
                }
            }).setToolTipText("Quit violently and without warning... PLEASE SAVE ANY EDITED WINDOWS FIRST!");
        return fileMenu;
    }
        
    void doQuit(){
        preferencesCloser.run();
        if (engine!=null) {
            try{
                if (getPrologCleanupCommand() != null){
                    if (!engine.command(getPrologCleanupCommand()))
                        System.err.println("Could not execute exit cleanup goal: "+getPrologCleanupCommand());
                }
                engine.shutdown();
            } catch(Exception e){
                System.err.println("Unexpected problem exiting:\n"+e);
            }
        }
        engine=null;
        if (ve!=null) ve.shutdown();
        ve=null;
        System.exit(0);
    }
        
    void mayQuit(){
        if (LogicProgramEditor.someDirtyEditor()){
            if (JOptionPane.showConfirmDialog(FijiSubprocessEngineWindow.this,"There is at least one unsaved editor window. Throw away the changes?",
                                              "Exit without saving?",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION)
                return;
        }
        if (AbstractProjectWindow.someDirtyProject()){
            if (JOptionPane.showConfirmDialog(FijiSubprocessEngineWindow.this,"There is at least one unsaved project window. Throw away the changes?",
                                              "Exit without saving?",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION)
                return;
        }
        if (!isMayExitApp())
            return;
        dispose();
        doQuit();
    }
        
    public void windowClosing(WindowEvent e){
        mayQuit();
    }
    public void finalize(){
        doQuit();
    }
        
    class NewAction extends AbstractAction {
        String extension;
        String template;
        NewAction(String name,String extension,String template){
            super(name);
            this.extension=extension;
            this.template = template;
        }
        NewAction(String name,String extension){
            this(name,extension,"\n\n\n\n\n");
        }
        public void actionPerformed(ActionEvent e){
            try{
                //File F = new File(engine.createTempDirectory(),"new"+extension);
                File F = File.createTempFile("New", extension, engine.createTempDirectory());
                FileOutputStream fos = new FileOutputStream(F);
                PrintWriter pw = new PrintWriter(fos);
                pw.write(template); pw.close();
                fos.close();
                LogicProgramEditor LPE = LogicProgramEditor.makeEditor(F,FijiSubprocessEngineWindow.this,true);
                LPE.toFront(); 
                LPE.requestFocus(); 
            } catch (IOException ex){
                errorMessage("Can't create file:\n"+ex.getMessage());
            }
                        
        }
                
    }
    class NewErgoAction extends NewAction{
        NewErgoAction(){
            super("Ergo File",".ergo");
            putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            putValue(SHORT_DESCRIPTION,"Create a new Ergo file");
        }
    }
    class NewErgoTextAction extends NewAction{
        NewErgoTextAction(){
            super("ErgoText Template File",".ergotxt");
            putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_N,  ActionEvent.ALT_MASK));
            putValue(SHORT_DESCRIPTION,"Create a new ErgoText template file");
        }
    }
    class NewFloraAction extends NewAction{
        NewFloraAction(){
            super("Flora-2 File",".flr");
            // putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            putValue(SHORT_DESCRIPTION,"Create a new Flora file");
        }
    }
    class NewPrologAction extends NewAction{
        NewPrologAction(){
            super("Prolog File",".P");
            putValue(SHORT_DESCRIPTION,"Create a new Prolog file");
        }
    }
    class NewProjectAction extends AbstractAction{
        NewProjectAction(){
            super("New Project...");
            putValue(SHORT_DESCRIPTION,"Create a new Studio project file");
        }
        public void actionPerformed(ActionEvent e){
            projectChooser.setFileFilter(fidjiProjectFilesFilter);
            projectChooser.setDialogTitle("Create Studio project file...");
            int returnVal = projectChooser.showDialog(FijiSubprocessEngineWindow.this,"Create");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File F = projectChooser.getSelectedFile();
                if (F.getName().indexOf(".")==-1){
                    F = new File(F.getParent(),F.getName()+"."+FIDJIEXTENSION);
                }
                if (!F.getName().endsWith(FIDJIEXTENSION)){
                    errorMessage("The new Studio project file must have extension ."+FIDJIEXTENSION);
                } else try{ 
                        FileWriter fw = new FileWriter(F);
                        new Properties().store(fw,AbstractProjectWindow.FILE_COMMENT);
                        fw.close();
                        EditableProjectWindow.showProject(F,FijiSubprocessEngineWindow.this);
                    } catch(IOException ex){
                        errorMessage("Can't create project file:\n"+ex.getMessage());
                    }
            }
        }
    }
    class OpenProjectAction extends AbstractAction{
        OpenProjectAction(){
            super("Open Project...");
            putValue(SHORT_DESCRIPTION,"Open an existing Studio project file");
        }
        public void actionPerformed(ActionEvent e){
            projectChooser.setFileFilter(fidjiProjectFilesFilter);
            projectChooser.setDialogTitle("Open a Studio project file...");
            int returnVal = projectChooser.showOpenDialog(FijiSubprocessEngineWindow.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File F = projectChooser.getSelectedFile();
                if (!F.getName().endsWith("."+FIDJIEXTENSION)){
                    errorMessage("The Studio project file must have extension ."+FIDJIEXTENSION);
                } else { 
                    EditableProjectWindow.showProject(F,FijiSubprocessEngineWindow.this);
                }
            }
        }
    }
        
    protected void openFile(){
        openFile(this);
    }
    void openFile(Component parent){
        openFile(parent,logicFilesFilter,"Open existing file...");
    }
        
    void openFile(Component parent, FileFilter FF, String title){
        fileChooser.setFileFilter(FF);
        fileChooser.setDialogTitle(title);
        // Runtime R = Runtime.getRuntime();
        
        //R.gc();
        //System.out.println(R.freeMemory() + "," + R.totalMemory() + ","+ R.maxMemory());
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File F = fileChooser.getSelectedFile();
            if (LogicProgramEditor.getExistingEditor(F, this)!=null)
                errorMessage("This file is already open");
            LogicProgramEditor.showEditor(F.getAbsolutePath(), null,this, false);
        }
    }
        
    protected void reconsultFile(){
        if (!checkEngineAvailable()) return;            
        FileAndModule FM = null;
        if ((FM = pickFileAndModule((FijiPreferences.floraSupported ? "Load knowledge base file..." : "Load logic file...")))!=null){
            if (FloraSubprocessEngine.isFloraSourceFile(FM.file) || FloraSubprocessEngine.isErgoSourceFile(FM.file)) {
                floraLoadCommand(FM.file,FM.module);
            } else if (FloraSubprocessEngine.isErgotextSourceFile(FM.file))
                JOptionPane.showMessageDialog(this,"ErgoText template files cannot be loaded directly. They should be referred to via the :- ergotext directive","Error loading",JOptionPane.ERROR_MESSAGE);
            else {
                prologLoadCommand(FM.file);
                // in this branch we have no control to display the errors
            }
        }       
    }
        
    public FileAndModule pickFileAndModule(String dialogTitle){
        JComponent previous = fileChooser2.getAccessory();
        ModuleChooser MC = null;
        if (FijiPreferences.floraSupported)
            MC= new ModuleChooser(engine);
        fileChooser2.setAccessory(MC);
        if (FijiPreferences.floraSupported)
            fileChooser2.setFileFilter(logicFilesFilter);
        else
            fileChooser2.setFileFilter(prologFilesFilter);
        fileChooser2.setDialogTitle(dialogTitle);
        int returnVal = fileChooser2.showOpenDialog(this);
        fileChooser2.setAccessory(previous);
        String M = (FijiPreferences.floraSupported ? (String)MC.chooser.getSelectedItem() : "main");
        if (returnVal == JFileChooser.APPROVE_OPTION)
            return new FileAndModule(fileChooser2.getSelectedFile(),M);
        else return null;
    }
        
    public static class FileAndModule{
        public final File file;
        public final String module;
        FileAndModule(File file,String module){
            this.file=file; this.module=module;
        }
    }
        
    public static class ModuleChooser extends JPanel{
        JComboBox<String> chooser;
        ModuleChooser(PrologEngine engine){
            this("main",engine);
        }
        public ModuleChooser(String preferredModule,PrologEngine engine){
            //setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            setLayout(new FlowLayout());
            JLabel label = new JLabel("Module:");
            label.setToolTipText("For Ergo and Flora-2 files only");
            add(label);
            chooser = new JComboBox<String>(fetchModules(preferredModule,engine));
            chooser.setEditable(true);
            chooser.setSelectedItem(preferredModule);
            add(chooser);
        }
        public JComboBox<String> getChooser() {
            return chooser;
        }
        public String getSelectedItem(){
            String M = (String)chooser.getSelectedItem();
            if (M == null || M.equals(""))
                return "main";
            else return M;
        }
        public static String[] fetchModules(String preferredModule,PrologEngine engine){
            String[] modules, modules2;

            Object[] bindings = null;
            if (engine.isAvailable())
                bindings = engine.deterministicGoal("findall(M,flrregistry:flora_user_module_registry(M),L), stringArraytoList(Array,L)","[Array]");
            if (bindings!=null) modules = (String[])bindings[0];
            else {
                System.err.println("Failed to grab Ergo module names");
                if (!preferredModule.equals("main")) modules = new String[]{preferredModule,"main"};
                else modules = new String[]{preferredModule};
            }
            // make sure preferredModule is in array
            int m;
            for (m=0;m<modules.length;m++)
                if (modules[m].equals(preferredModule)) break;
            if (m>=modules.length){
                modules2 = new String[modules.length+1];
                for (m=0;m<modules.length;m++)
                    modules2[m] = modules[m];
                modules2[m] = preferredModule;
            } else modules2 = modules;
            return modules2;
        }
                
    }
        
    public String pickModule(Window parent, String preferredModule,String message){
        return new ModulePicker(parent).pickModule(preferredModule,message);
    }
        
    protected class ModulePicker extends JDialog{
        public ModulePicker(Window parent) {
            super(parent,"Load or Add to Module",ModalityType.APPLICATION_MODAL);
        }
        public String pickModule(String preferredModule,String message) {
            JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER));
            top.add(Box.createVerticalStrut(20));
            top.add(new JLabel(message,JLabel.CENTER));
            add(top, BorderLayout.NORTH);
            ModuleChooser MC = new ModuleChooser(preferredModule,engine);
            add(MC,BorderLayout.CENTER);
            Box bottom = new Box(BoxLayout.X_AXIS);
            add(bottom,BorderLayout.SOUTH);
            bottom.add(Box.createHorizontalGlue());
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setVisible(false);
                    }
                });
            bottom.add(cancelButton);
            final JButton okButton = new JButton("OK");
            final String HACK ="OK!";
            final ActionListener closer = new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setVisible(false);
                        okButton.setText(HACK);
                    }
                };
            MC.getChooser().getEditor().addActionListener(closer);
            okButton.addActionListener(closer);
            okButton.setDefaultCapable(true);
            getRootPane().setDefaultButton(okButton);
            bottom.add(okButton);
            double CX = getParent().getBounds().getCenterX();
            double CY = getParent().getBounds().getCenterY();
            int width = 220; int height = 150;
            setBounds((int)CX-width/2, (int)CY-height/2, width, height);
            //dialog.pack();
            setVisible(true);
            if (!okButton.getText().equals(HACK)) // cancelled
                return null;
            else return MC.getSelectedItem();
        }
                
    }

    void prologLoadCommand(File F){
        prologLoadCommand(F,null);
    }
        
        
    /**
     * @param F file to load
     * @param LPE An optional editor that will be notified only if the file has loaded without errors
     */
    void prologLoadCommand(final File F,final LogicProgramEditor LPE){
        setWaitCursor();
                
        (new SwingWorker<Boolean,Object>(){
                public Boolean doInBackground() {
                    boolean consulted = engine.consultAbsolute(F);
                    if (!consulted){
                        System.err.println("Failed consultAbsolute of "+F);
                        return false;
                    }
                    return refreshUserModFilesAndLibDirsAndOps();
                }
                protected void done() {
                    scrollToBottom();
                    restoreCursor();
                    try{
                        if (!get()) 
                            JOptionPane.showMessageDialog(FijiSubprocessEngineWindow.this,
                                                          "Errors occurred","Loading error",JOptionPane.ERROR_MESSAGE); 
                        else if (LPE!=null)
                            LPE.setWasLoadedAddedIncluded(true, true, false, false, null, null);
                    } catch (Exception ex){
                        JOptionPane.showMessageDialog(FijiSubprocessEngineWindow.this,
                                                      "Bad exception:"+ex,"Loading error",JOptionPane.ERROR_MESSAGE);   
                    }
                }
            }).execute();

    }

    void QUALMLoadCommand(File F, LogicProgramEditor LPE){
        genericLoadCommand(F,LPE,"consultQ",null);
    }
    void LPSWLoadCommand(File F, LogicProgramEditor LPE, String options){
        genericLoadCommand(F,LPE,"go",options);
    }
    void LPSPLoadCommand(File F, LogicProgramEditor LPE, String options){
        genericLoadCommand(F,LPE,"gol",options);
    }
    void LPSLoadCommand(File F, LogicProgramEditor LPE, String options){
        genericLoadCommand(F,LPE,"golps",options);
    }
    void LPSWLoadShowCommand(File F, LogicProgramEditor LPE, String options){
        genericLoadCommand(F,LPE,"gov",options);
    }
    void LPSPLoadShowCommand(File F, LogicProgramEditor LPE, String options){
        genericLoadCommand(F,LPE,"golv",options);
    }
    void LPSLoadShowCommand(File F, LogicProgramEditor LPE, String options){
        genericLoadCommand(F,LPE,"golpsv",options);
    }
    /**
     * @param F file to load
     * @param LPE An optional editor that will be notified only if the file has loaded without errors
     * @param loader Prolog predicate with one argument (file path F); so the goal will be loader(F)
     * @param options Prolog list of execute options; if non null, the Prolog goal will be loader(F,options)
     */
    void genericLoadCommand(final File F,final LogicProgramEditor LPE, final String loader, String options){
        setWaitCursor();
        final String optional = (options==null?"":","+options);
        (new SwingWorker<Boolean,Object>(){
                public Boolean doInBackground() {
                    boolean consulted = engine.deterministicGoal(
                                                                 loader+"('"+engine.unescapedFilePath(F.getAbsolutePath())+"'"+optional+")"
                                                                 );
                    if (!consulted){
                        System.err.println("Failed genericLoadCommand with "+loader+" of "+F);
                        return false;
                    }
                    return refreshUserModFilesAndLibDirsAndOps();
                }
                protected void done() {
                    scrollToBottom();
                    restoreCursor();
                    try{
                        if (!get()) 
                            JOptionPane.showMessageDialog(FijiSubprocessEngineWindow.this,
                                                          "Errors occurred","Loading error",JOptionPane.ERROR_MESSAGE); 
                        else if (LPE!=null)
                            LPE.setWasLoadedAddedIncluded(true, true, false, false, null, null);
                    } catch (Exception ex){
                        JOptionPane.showMessageDialog(FijiSubprocessEngineWindow.this,
                                                      "Bad exception:"+ex,"Loading error",JOptionPane.ERROR_MESSAGE);   
                    }
                }
            }).execute();

    }

    public void floraLoadCommand(final File F,final String module){
        floraLoadCommand(F,module,null);
    }
    /**
     * @param F file to load
     * @param module
     * @param floraProgramEditor Optional editor that will be notified (only) if F is loaded without errors
     */
    public void floraLoadCommand(final File F,final String module, final LogicProgramEditor floraProgramEditor){
        if (!checkFloraShell()) return;
        setWaitCursor();
        (new SwingWorker<LoadOrAddResult,Object>(){
                public LoadOrAddResult doInBackground() {
                    return loadOrAdd(F,FLORA_LOAD,module);
                }
                protected void done() {
                    try{
                        if (get().getHasErrors()) {
                            beep();
                            JOptionPane.showMessageDialog(FijiSubprocessEngineWindow.this,
                                                          /* I don't like this: "Please fix your program errors\n"+ */get().error,"Loading failed",JOptionPane.ERROR_MESSAGE);  
                        } else if (floraProgramEditor!=null)
                            floraProgramEditor.setWasLoadedAddedIncluded(true, true, false, false, null, module);
                        refreshProblemsPanel(!get().isUserAborted() && (get().getHasErrors()||get().getHasOnlyWarnings()));
                    } catch (Exception e){
                        throw new RuntimeException("Bad exception after load:"+e);
                    }
                    scrollToBottom();
                    restoreCursor();
                }
            }).execute();
    }
        
    protected void addFile(){
        if (!checkEngineAvailable()) return;
        FileAndModule FM = null;
        String language = ((((FloraSubprocessEngine)engine).isErgo()) ? "Ergo":"Flora-2");
        if ((FM = pickFileAndModule("Add "+language+" file..."))!=null){
            if (FloraSubprocessEngine.isFloraSourceFile(FM.file) || FloraSubprocessEngine.isErgoSourceFile(FM.file)) {
                floraAddCommand(FM.file,FM.module);
            } else {
                JOptionPane.showMessageDialog(this,"Only Ergo and Flora-2 files can be added","Error adding",JOptionPane.ERROR_MESSAGE);        
            }
        }                       
    }

    public void floraAddCommand(final File F,final String module){
        floraAddCommand(F,module,null);
    }
    public void floraAddCommand(final File F,final String module, final LogicProgramEditor editor){
        if (!checkFloraShell()) return;
        setWaitCursor();
        (new SwingWorker<LoadOrAddResult,Object>(){
                public LoadOrAddResult doInBackground() {
                    return loadOrAdd(F,FLORA_ADD,module);
                }
                protected void done() {
                    try{
                        if (get().getHasErrors()) {
                            JOptionPane.showMessageDialog(FijiSubprocessEngineWindow.this,
                                                          /* I don't like this: "Please fix your program errors\n"+ */get().error,"Add failed",JOptionPane.ERROR_MESSAGE);      
                            System.err.println("Error adding:"+get().error);
                        } else if (editor!=null)
                            editor.setWasLoadedAddedIncluded(true, false, true, false, null, module);;
                        refreshProblemsPanel(!get().isUserAborted() && (get().getHasErrors()||get().getHasOnlyWarnings()));
                    } catch (Exception e){
                        throw new RuntimeException("Bad exception after load:"+e);
                    }
                    restoreCursor();
                }
            }).execute();
    }
        
    public void mayRefreshMemoryProject(){
        if (ergoMemoryProjectModel!=null)
            ergoMemoryProjectModel.refresh();
    }
        
    /** Assumes the file has been already loaded, and gets module and action from Ergo's registry */
    public LoadOrAddResult reloadErgo(File F){
        Object[] bindings = engine.deterministicGoal(
                                                     "fjFileInMemory('"+engine.unescapedFilePath(F.getAbsolutePath())+"', Module, LoadAdd)",
                                                     "[string(Module),string(LoadAdd)]");
        String module = (String)bindings[0];
        String loadAdd = (String)bindings[1];
        if (loadAdd.equals("load"))
            loadAdd = "\\load";
        else if (loadAdd.equals("\\add"))
            loadAdd = "\\add";
        else 
            throw new RuntimeException("Bad registry result in reloadErgo");
        return loadOrAdd(F,loadAdd,module);
    }

    /** loads (\\load) or adds (\\add) a Flora-2 file, storing error notices on the Prolog side for later display. 
        Returns false if the compilation or loading fails*/
    public LoadOrAddResult loadOrAdd(File F,String load_or_add, String module){
        String status = null;
        setWaitCursor();
        String error="";
        try{
            String G = "fjLoadOrAddFile('"+engine.unescapedFilePath(F.getAbsolutePath())+"','"+module+"','"+load_or_add+"',FileStatus)";
            Object[] bindings = engine.deterministicGoal(G,"[string(FileStatus)]");
            if (bindings==null){
                System.err.println("Bad failure loading with goal "+G);
                status = WEIRD_FAILURE;
            } else
                status = (String) bindings[0];
        } catch (IPPrologError e){
            if (e.t instanceof TermModel){
                TermModel t = (TermModel)e.t;
                if (t.toString().equals(XJPrologEngine.ERGO_USER_ABORT_HACK)){
                    // we're calling the Prolog predicate fjLoadOrAddFile above, which somehow propagates
                    // back to us this "dummy" exception
                    status = USER_ABORTED;
                    error = "User aborted.";
                    System.err.println("User aborted "+load_or_add+"ing "+F+":\n"+e);
                } else {
                    if ( !t.isLeaf() && (t.node.equals(ERGO_ABORT_NOTRACE) || t.node.equals(ERGO_USER_ABORT) || t.node.equals("error"))) 
                        error = t.getChild(0).toString();
                    else {
                        error = t.toString();
                        System.err.println("Error encountered while "+load_or_add+"ing "+F+":\n"+e.t);
                    }
                    status = WEIRD_FAILURE;
                }
            }
            else status = WEIRD_FAILURE;
        } catch (IPInterruptedException e){
            System.err.println("User aborted "+load_or_add+"ing "+F+":\n"+e);
            status = USER_ABORTED;
            error = "User aborted.";
        } catch (Exception e){
            System.err.println("Exception encountered while "+load_or_add+"ing "+F+":\n"+e);
            status = WEIRD_FAILURE;
        }
        if (!(status.equals(WEIRD_FAILURE)) && !(status.equals(USER_ABORTED)))
            mayRefreshMemoryProject();
        restoreCursor();
        // ??? seems obsolete: mayAddLoadedFile(F,load_or_add,module);
        LoadOrAddResult R = new LoadOrAddResult(status, error);
        // Now redundant with mayRefreshMemoryProject:
        // if (ergoMemoryProjectModel == null && !R.getHasErrors())
        //      includedFilesWereLoadedOrAdded(F,load_or_add,module);
        return R;
    }
    static final String FAILURE = "failure"; // Only the logic side returns this value
    static final String WEIRD_FAILURE = "weird_failure";
    static final String USER_ABORTED = "user_aborted";
    public static class LoadOrAddResult{
        private final String error;
        private final boolean hasErrors, hasOnlyWarnings;
        private final boolean userAborted;

        LoadOrAddResult(String status, String error){
            hasErrors = status.equals(FAILURE)||status.equals(WEIRD_FAILURE)||status.equals(USER_ABORTED);
            hasOnlyWarnings = status.equals("warnings");
            userAborted = status.equals(USER_ABORTED);
            if (error == null || error.length()==0)
                this.error = status.equals(FAILURE)? "Loading failed. Please see the Warnings and Errors window": "Loading failed. Please see the Ergo Listener console for more details.";
            else 
                this.error=error;
        }

        public boolean getHasErrors() {
            return hasErrors;
        }
        public boolean getHasOnlyWarnings() {
            return hasOnlyWarnings;
        }
        public boolean isUserAborted() {
            return userAborted;
        }
                
    }
    LoadOrAddResult loadOrAdd(File F,String load_or_add){
        return loadOrAdd(F, load_or_add, "main");
    }
        
    void includedFilesWereLoadedOrAdded(File F, String load_or_add, String module){
        String G = "fjIncludedFiles('"+engine.unescapedFilePath(F.getAbsolutePath())+"',StringArray)";
        Object[] bindings = engine.deterministicGoal(
                                                     G, 
                                                     "[StringArray]");
        if (bindings!=null){
            String[] files = (String[])bindings[0];
            for(String s: files){
                LogicProgramEditor LPE = LogicProgramEditor.getExistingEditor(new File(s));
                if (LPE!=null)
                    LPE.setWasLoadedAddedIncluded(true, false, false, true, F, module);
            }
        } else System.err.println("Failed fjIncludedFiles for "+F);
    }
        
    /** Adds file F to the loaded submenu unless it already has an open editor */
    void mayAddLoadedFile(File F,String command,String module){
        if (LogicProgramEditor.getExistingEditor(LogicProgramEditor.prologFilename(F,this))!=null) 
            return;
        Action action = new LoadedFileAction(F,command,module);
        if (findItemWithListener(otherLoadedFiles, action)!=null)
            return; // similar action already in menu
        otherLoadedFiles.add(action);
    }
        
    class LoadedFileAction extends AbstractAction{
        File file; String command; String module;
        LoadedFileAction(File F,String command,String module){
            super(F.getName()+" ("+module+")");
            putValue(SHORT_DESCRIPTION,"Open this file for editing; it will then appear also in the Windows menu");
            file = F; this.command = command; this.module=module;
        }
        public void actionPerformed(ActionEvent e){
            LogicProgramEditor.showEditor(LogicProgramEditor.prologFilename(file,FijiSubprocessEngineWindow.this), 
                                          null,FijiSubprocessEngineWindow.this).maySetPreferredModule(module);
        }
        // to ease search:
        public boolean equals(Object x){
            return x!=null && x.getClass()==getClass() && ((LoadedFileAction)x).file.equals(file);
        }
    }
        
    public boolean processDraggedFile(File f){
        if (!checkEngineAvailable()) return false;
        /* if (engine.consultAbsolute(f)) {
           addToReloaders(f,"'\\load'");
           return true;
           }            else {
           errorMessage("Problems loading "+f.getName());
           return false;
           } */
        return null != LogicProgramEditor.showEditor(LogicProgramEditor.prologFilename(f,this), null,this);
    }
        
    /** Effectively disables the "reloader" menu items, since Studio has other related functionality */
    protected void addToReloaders(File file,String method){}
        
    protected void constructWindowContents(){
        super.constructWindowContents();
        String language;
        if (FijiPreferences.floraSupported)
            language = ((FloraSubprocessEngine)engine).getLanguage();
        else 
            language = "Prolog";
        prologOutput.setToolTipText("This is "+language+" console output");
        prologOutput.getAccessibleContext().setAccessibleName(language+" Console Output");
        prologInput.setToolTipText(language+" input: your-query <Enter>. Drop here an .ergo/.ergotxt/.flr/.P/.pl file to open it. Up/Down arrows walk the command history.");
        prologInput.getAccessibleContext().setAccessibleName(language+" Input");
    }

    protected void setupCommandHistory() {
        commandHistory = new XJCommandHistory(FijiPreferences.historyFile());
        commandHistory.addField(prologInput);
    }
        
    public boolean droppableFile(File f){
        return super.droppableFile(f) || f.getName().endsWith(".H") 
            || f.getName().endsWith(".pl") || f.getName().endsWith(".plt") // SWI Prolog
            || f.getName().endsWith("ab") // QUALM
            || f.getName().endsWith(".lps") // LPS "new syntax"
            || f.getName().endsWith(".lpsp") // LPS "papers syntax"
            || f.getName().endsWith(".lpsw") // LPS internal syntax
            || (FijiPreferences.floraSupported && (FloraSubprocessEngine.isFloraSourceFile(f)||FloraSubprocessEngine.isErgoSourceFile(f)||FloraSubprocessEngine.isErgotextSourceFile(f)) );
    }
    public String badFilesDroppedMessage(){
        if (FijiPreferences.floraSupported)
            return "All dragged files must be Ergo, Flora-2, or Prolog source files (extensions: .ergo, .ergotxt, .flr, .pl or .P)";
        else
            return "All dragged files must be Prolog source files (extension: .P or .pl)";
    }
    /** Usually the system will start through FijiPreferences.main(). This method is useful for launching the system by bypassing preferences
        , passing the full Prolog executable path and  optionally extra arguments */
    public static void main(String[] args){
        //if (!(args.length>1 && args[1].toLowerCase().startsWith("-fidji"))
        String initialFile = commonMain(args);
        initFidji(null,prologStartCommands, null, debug, loadFromJar,quietLog,workingDir,forcePrologStudio,initialFile);
    }
    /** Initialize Studio over an existing engine, say for integrating into an existing Java app with itw own GUI. 
     * A listener window will be created; beware that it includes a "Quit" command (both in the main menu and as a result of closing the window).
     * There MUST be an existing prefs file close to the jar containing this class.
     * @param debug
     * @param loadFromJar
     * @param quietLog
     * @param workingDir
     * @param mainEngine 
     * @param editorEngine if null, it will be created based on the preferences file
     */
    public static void initFidji(boolean debug, boolean loadFromJar, boolean quietLog,String workingDir, boolean prologstudio, SubprocessEngine mainEngine, SubprocessEngine editorEngine){
        initFidji(null, null, null, debug, loadFromJar, quietLog, workingDir, prologstudio, null, mainEngine, editorEngine);
    }
    
    static void initFidji(FijiPreferences preferences,String[] prologStartCommands,String floraDir,boolean debug, boolean loadFromJar, boolean quietLog, 
                          String workingDir, boolean prologstudio, String initialFile){
        initFidji(preferences, prologStartCommands, floraDir, debug, loadFromJar, quietLog, workingDir, prologstudio, initialFile, null, null);
    }
    /**
     * @param preferences a preferences object; if null, one will be created
     * @param prologStartCommands
     * @param floraDir
     * @param debug
     * @param loadFromJar whether Interprolog Prolog files are to be consulted from jar
     * @param workingDir if not null, this will be the directory where preferences, logs etc will be stored; otherwise the system will decide it
     * @param initialFile
     * @param mainEngine  If not null, it will be used; no startup splash window will be shown
     * @param editorEngine If not null, it will be used
     */
    static void initFidji(FijiPreferences preferences,String[] prologStartCommands,String floraDir,
                          boolean debug, boolean loadFromJar, boolean quietLog, String workingDir, boolean prologstudio, String initialFile,
                          SubprocessEngine mainEngine, SubprocessEngine editorEngine){
        // long startTime = System.currentTimeMillis();
        if (System.getProperty("java.version").compareTo("1.6")<0)
            abortStartup("Fidji requires Java 1.6 (aka Java 6) or later, please install it");

        if (preferences == null){
            preferences = new FijiPreferences(quietLog,prologStartCommands,workingDir,prologstudio); 
        }
        String language = "Prolog";
        if (FijiPreferences.floraSupported) {
            if (floraDir==null)
                floraDir = preferences.getProperty("FLORADIR");
            if (mainEngine==null&&floraDir==null)
                abortStartup("Can't find engine");
            language = (FloraSubprocessEngine.isErgo(floraDir)?"Ergo":"Flora-2");
        }
        if (mainEngine==null)
            splash = new SplashWindow(
                                      null,new ImageIcon(FijiPreferences.splashImage), true,true,initialFile==null, 
                                      (FijiPreferences.floraSupported?"":language+" Studio"), FijiPreferences.splashTextSize, FijiPreferences.splashTextColor);
        if (prologStartCommands==null){
            String prologStartCommand = preferences.getProperty("PROLOG");
            if (prologStartCommand!=null)
                prologStartCommands = new String[]{prologStartCommand};
        } // ...else it will NOT be stored in preferences - it was set explicitly by some shell script, or calling app
            
        if (splash!=null)
            splash.setProgress(20, "Creating "+ language +" Engine...");
                
        if (prologStartCommands==null && mainEngine==null)
            abortStartup("Can't find Prolog engine");
                                        
        if (!AbstractPrologEngine.isWindowsOS())
            if (mainEngine==null && prologStartCommands!=null&&prologStartCommands[0].contains(" ") )
                abortStartup("On non-Windows systems XSB must be in a directory without any spaces in its path name.\nPlease reinstall Ergo and XSB and DELETE the .ergoAI.prefs file...\n"+
                             "...or simply download a self-contained Studio (containing Ergo and XSB) into a directory without spaces");
        /*
          if (prologStartCommand!=null&&prologStartCommand.contains(" ") || floraDir!=null&&floraDir.contains(" ") )
          abortStartup("Both Ergo and XSB must be in a directory without any spaces in its path name.\nPlease move them elsewhere and DELETE the .ergoAI.prefs file.");
        */
                
        if (!debug && preferences.getProperty("DEBUG")!=null && preferences.getProperty("DEBUG").toLowerCase().equals("true"))
            debug = true;
        boolean outAndErrMerged = true; // some day we might want to init this from a preference...

        final String[] IBRprologStartCommands = prologStartCommands;
        final String IBRfloraDir = floraDir;
        final boolean IBRloadFromJar = loadFromJar;
                
        String fems = preferences.getProperty(FIRST_ENGINE_MAX_STARTUP_PREF);
        int fems_ = (fems==null?40000:Integer.parseInt(fems));
        SubprocessEngine e = null;
        if (mainEngine==null){
            FijiPreferences.ImpatientBugReporter ibr = new FijiPreferences.ImpatientBugReporter(fems_,"First engine startup",new Runnable(){
                    public void run(){
                        if (FijiPreferences.floraSupported)
                            new FloraSubprocessEngine(IBRprologStartCommands,IBRfloraDir,true,true,IBRloadFromJar);
                        else 
                            new XSBSubprocessEngine(IBRprologStartCommands,true,true,IBRloadFromJar);
                    }
                }, true, null);
            try{
                if (FijiPreferences.floraSupported)
                    e = new FloraSubprocessEngine(IBRprologStartCommands,IBRfloraDir,outAndErrMerged,debug,IBRloadFromJar);
                else 
                    e = new XSBSubprocessEngine(IBRprologStartCommands,outAndErrMerged,debug,IBRloadFromJar);
                e.setSlowWindowsShutdown();
                ibr.dontWorry();
            } catch (Exception ex){
                ibr.doWorry(ex);
            }
        } else e=mainEngine;
                
        if (e==null){
            abortStartup("An error occurred while starting the inference engine.\nCheck the log file "+FijiPreferences.getLogFile());
        }
                
        FloraSubprocessEngine fe = null;
        if (FijiPreferences.floraSupported){
            fe = (FloraSubprocessEngine)e;
            fe.setQuietDeterministicGoals(true);
            //System.out.println(fe.getFloraReleaseDate());
            String MINIMAL_DATE = "2014-03-07";
            if ((fe.getFloraReleaseDate().compareTo(MINIMAL_DATE))<0) 
                abortStartup(language+" version needs to be of "+MINIMAL_DATE+" or later; found "+fe.getFloraReleaseDate());
            /* If we ever wish to execute these tools within our VM...:
             * if (FijiPreferences.ergoOWLSupported(fe))
             if (!FijiPreferences.addErgoOWLJars(fe))
             abortStartup("Can't load ErgoOWL jar");
             if (FijiPreferences.ergoSPARQLSupported(fe))
             if (!FijiPreferences.addErgoSPARQLJars(fe))
             abortStartup("Can't load ErgoSPARQL jar");
            */
        }
        if (splash!=null)
            splash.setProgress(50, "Preparing UI...");
                
        long initStart = System.nanoTime();
        XJPrologEngine.initPrologLayer(e); // Beware of Java thread usage by javaMessage
        long initDuration =  System.nanoTime()-initStart;
        // System.out.println(initDuration);
        // Linux nanoTime() seems an order of magnitude off
        if (AbstractPrologEngine.isLinuxOS())
            initDuration = initDuration / 15; 
        FijiPreferences.setSlowness(initDuration/21557642.0); // ...current reference for slowness 1.0: Miguel's hot MacBook Pro Feb2015, SSD drive  
        System.out.println("Your machine's speed vs Dad's:"+NumberFormat.getPercentInstance().format(1.0/FijiPreferences.getSlowness()));
        // System.out.println("parseable size:"+FloraProgramEditor.defaultParseableSize());
                
        if (splash!=null)
            splash.setProgress(60, "Loading system predicates...");
        e.consultFromPackage("fidji.xwam",FijiSubprocessEngineWindow.class);
        
        String xrefDir = e.unescapedFilePath(FijiPreferences.fidjiXrefDir.getAbsolutePath());
        if (FijiPreferences.floraSupported){
            //fe.consultFromPackage("fidji-cks",FijiPreferences.otherEditorClass);
            //fe.consultFloraFromPackage("fidjiUtils.flr",ERGO_STUDIO_MODULE,FijiPreferences.otherEditorClass);
            fe.deterministicGoal("consult('fidji-cks')");
            fe.deterministicGoal("fj_set_studio_mode");
            // fidjiUtils.ergo now loaded automatically as needed, being a system module
            if (splash!=null)
                splash.setProgress(70, "Creating editor assistant engine...");
            if (ve==null){
                if (editorEngine==null) 
                    ve = new FloraSubprocessEngine(prologStartCommands,floraDir,true,debug,loadFromJar);
                else ve = editorEngine;
                                
                XJPrologEngine.initPrologLayer(ve);
                ve.consultFromPackage("fidji.xwam",FijiSubprocessEngineWindow.class);
                ve.consultFromPackage("myxxref",FijiSubprocessEngineWindow.class);
                ve.command("assert(fjXrefCacheDir('" + xrefDir + "'))");
                //((FloraSubprocessEngine)ve).consultFloraFromPackage("fidjiUtils.flr",ERGO_STUDIO_MODULE,FijiPreferences.otherEditorClass); 
                // fidjiUtils.ergo now loaded automatically as needed, being a system module
                //fidji-cks.P now in Ergo's system libraries  ve.consultFromPackage("fidji-cks.xwam",FijiPreferences.otherEditorClass);
                ve.deterministicGoal("consult('fidji-cks')");
                ve.deterministicGoal("fj_set_studio_mode");
            }
        } else {
            if (splash!=null)
                splash.setProgress(70, "Creating editor assistant engine...");
            if (ve==null){
                if (editorEngine==null)
                    ve = new XSBSubprocessEngine(prologStartCommands,true,debug,loadFromJar);
                else ve=editorEngine;
                XJPrologEngine.initPrologLayer(ve);
                ve.consultFromPackage("fidji.xwam",FijiSubprocessEngineWindow.class);
                ve.consultFromPackage("myxxref",FijiSubprocessEngineWindow.class);
                ve.command("assert(fjXrefCacheDir('" + xrefDir + "'))");
                // display editor engine listener, for debugging:
            }
        }
        ve.setSlowWindowsShutdown();
        LogicProgramEditor.setValidatingEngine(ve);
        if (vec==null){
            if (useTimedCallPausing){
                vec = new EngineController(null,false);
                ve.setTimedCallIntervall(500);
            } else
                vec = new EngineController(ve,false);
            ve.addPrologEngineListener(vec); 
        }

        if (splash!=null)
            splash.setProgress(80, "Preparing listener window...");
        UIStarter starter = new UIStarter(preferences, e);
        try {
            SwingUtilities.invokeAndWait(starter);
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
            abortStartup("Problems building the UI");
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            abortStartup("Problems building the UI");
        }
                
        final FijiSubprocessEngineWindow fsw = starter.fsw;

        if (!ve.deterministicGoal("retractall(fjMainListenerWindow(_)), asserta(fjMainListenerWindow("+ve.registerJavaObject(fsw)+"))"))
            throw new IPException("could not assert fjMainListenerWindow in auxiliar engine"); 

        if (FijiPreferences.nlpSupported())
            if (!FijiPreferences.addNLPJars())
                abortStartup("Can't load NLP jars");
        fsw.refreshUserModFilesAndLibDirsAndOps();

        String QUALMdir = preferences.getProperty("QUALM");
        boolean qualmLoaded = false;
        if (QUALMdir!=null && !FijiPreferences.floraSupported){
            File QD = new File(QUALMdir,"qualm.P");
            if (QD.exists()){
                if (splash!=null)
                    splash.setProgress(65, "Loading QUALM...");
                if (!e.add_lib_dir(new File(QUALMdir)) || !e.consultAbsolute(QD))
                    abortStartup("Failed to load QUALM.\nPlease move the 'qualm' directory elsewhere until it is fixed.");
                else qualmLoaded = true;
            }
        }

        boolean lpsLoaded = false;
        /* LPS support broken after Aug 2017 LPS version:
           String LPSdir = preferences.getProperty("LPS");
           if (LPSdir!=null && !FijiPreferences.floraSupported){
           File LD = new File(LPSdir,"engine"+File.separator+"interpreter.P");
           if (LD.exists()){
           if (splash!=null)
           splash.setProgress(68, "Loading LPS...");
           File LV = new File(LPSdir,"utils"+File.separator+"visualizer.P");
           File LP = new File(LPSdir,"utils"+File.separator+"psyntax.P");
           if (!e.consultAbsolute(LD) || !e.consultAbsolute(LV) || !e.consultAbsolute(LP)
           || !ve.add_lib_dir(new File(LPSdir)) ||!ve.consultAbsolute(LD)|| !ve.consultAbsolute(LP))
           abortStartup("Failed to load LPS.\nPlease move the 'LPS' directory elsewhere until it is fixed.");
           else lpsLoaded = true;
           }
           } */

        if (splash!=null)
            splash.setProgress(95, "Loading initialization file...");
        File rc = new File(FijiPreferences.getFidjiDir(),FijiPreferences.FIDJI_RC);
        if (!rc.exists()){
            rc = new File(FijiPreferences.getFidjiDir(),FijiPreferences.FIJI_RC);
        }
        boolean rcOK = true;
        if (rc.exists()){
            e.command("cd('" + e.unescapedFilePath(FijiPreferences.getFidjiDir().getAbsolutePath()) + "')");
            rcOK = e.consultAbsolute(rc);
        } else
            try{ e.consultFromPackage(FijiPreferences.FIDJI_RC_INJAR,fsw); }
            catch(Exception exx){rcOK=false;}
                
        if (!rcOK)
            abortStartup("Can't load initialization file "+rc);
        if (splash!=null)
            splash.finishSplash();
                
                
        FijiPreferences.mayLoadObjectSources(e); // Extra developer meta info to fully obtain usermod files
        FijiPreferences.mayLoadObjectSources(ve); // ...namely those unjared

        if (FijiPreferences.floraSupported){
            e.command("flrprint:flora_stdmsg_string(magic), flora_welcome_msg, flrprint:flora_stdmsg_string('Studio rev: "+FijiPreferences.buildRevision()+"\n\n')");
            waitAndCleanUntilOutputMarker("m\\s*a\\s*g\\s*i\\s*c",fsw.prologOutput,e);
        }
        
        if (qualmLoaded)
            fsw.printStdout("\nQUALM logic engine loaded\n");
                                
        if (lpsLoaded)
            fsw.printStdout("\nLPS engine loaded\n");
                                
        // fsw.setVisible(true); fsw.focusInput(); now done in Prolog:
        //try{
        //      SwingUtilities.invokeAndWait(new Runnable(){
        //              public void run(){
        if (!fsw.engine.deterministicGoal("fjMain"))
            abortStartup("Can't execute initialization goal fjMain in fidjiRC.P");
        //              }
        //      });
        //} catch(Exception ex){
        //      abortStartup("Can't execute fi[d]jiRC.P file:\n"+ex);
        //}
        if (initialFile!=null){ // implement simplified variant of XJ's startup protocol, cf. XJTopLevel constructor
            if (!e.deterministicGoal(
                                     "cd('" + new File(new File(initialFile).getAbsolutePath()).getParent().replaceAll("\\\\","/") + "'), consult('"+initialFile+"'), xjmain"))
                JOptionPane.showMessageDialog(null,"Can't execute initialization file "+initialFile,"Studio failed to initialize",JOptionPane.WARNING_MESSAGE);     

        }
        try{
            SwingUtilities.invokeAndWait(new Runnable(){
                    public void run(){
                        awtThread = Thread.currentThread();
                    }
                });
        }catch(Exception exc){
            throw new RuntimeException("Bad exception initializing:"+exc);
        }
        // long startupTime = System.currentTimeMillis()-startTime;
        // System.out.println("Started up in "+(startupTime)+" ms");
                
        String reopenWindows = preferences.getProperty(REOPEN_WINDOWS_SET_PREF);
        if (reopenWindows == null){
            reopenWindows = "false";
            preferences.put(REOPEN_WINDOWS_SET_PREF, reopenWindows);
        }
        if (reopenWindows.equals("true"))
            SwingUtilities.invokeLater(new Runnable(){
                    @Override
                        public void run() {
                        fsw.reopenWindowSet();
                    }   
                });
        String second = fsw.getPreference(SECONDLISTENER_PREF);
        if (second!=null && second.equals("true")){
            SwingUtilities.invokeLater(new Runnable(){
                    @Override
                    public void run() {
                        SubprocessEngineWindow EL = new SubprocessEngineWindow(ve,true,false,new Rectangle(100,100,300,200));
                        EL.setExtendedState(Frame.ICONIFIED); EL.setTitle("Editor engine listener, beware!");
                    }   
                });
        } 
        /* MK: This Thread.sleep is needed for synchronization.
           Otherwise, if the query window is opened by
           fsw.reopenWindowSet() (due to saved window configuration),
           the input field will show Running...
           Don't know why this synchronization issue arises.
        */
        try { Thread.sleep(50); } catch(InterruptedException ex) { }
        e.deterministicGoal("assert(fjStudioIsReady)");
    }
        
    static class UIStarter implements Runnable{
        final FijiPreferences preferences;
        final SubprocessEngine e;
        FijiSubprocessEngineWindow fsw = null;
        UIStarter(FijiPreferences preferences, SubprocessEngine e){
            this.preferences = preferences;
            this.e = e;
        }
        @Override
        public void run() {
            fsw = buildListener(e,true,preferences);
            fsw.setVisible(true);
            e.command("assert(xjConsole(" + e.registerJavaObject(fsw) + "))");
            TabsWindow.initialize(fsw);                                                         
        }
    }
    static FijiSubprocessEngineWindow buildListener(SubprocessEngine e,boolean autoDisplay,FijiPreferences preferences){
        return new FijiSubprocessEngineWindow(e,autoDisplay,preferences);
    }
        
    static void abortStartup(String message){
        beep();
        if (awtThread==null || Thread.State.RUNNABLE == awtThread.getState())
            JOptionPane.showMessageDialog(null,message,"Studio cannot continue",JOptionPane.ERROR_MESSAGE);     
        else try{
                beep();
                Thread.sleep(1000);
                beep();
            } catch (Exception e){}
        System.exit(1);
    }
        
    public boolean refreshUserModFilesAndLibDirsAndOps(){
        if (!engine.isAvailable()){
            // mostly failed hack to let the Java-side of the engine catch up under Flora
            try{Thread.sleep(300);} catch(InterruptedException e){}
            if (!engine.isAvailable()){
                System.err.println("engine not available for refreshUserModFilesAndLibDirsAndOps");
                return true;
            }
        }
        Object[] bindings = engine.deterministicGoal(
                                                     "fjUsermodPFiles(L), stringArraytoList(FilesArray,L), findall(D,(current_predicate(has_lib_dir/1)->has_lib_dir(D);library_directory(D)),Dirs), stringArraytoList(DirsArray,Dirs), " +
                                                     "fjFindOperatorsDelta(Inserted), buildTermModelArray(Inserted,InsertedArray), cwd(Dir)",
                                                     "[FilesArray,DirsArray,InsertedArray,string(Dir)]");
        if (bindings==null) {
            System.err.println("failed refreshUserModFilesAndLibDirsAndOps()");
            userModFiles = new String[0];
            libraryDirectories = new String[0];
            insertedOps = new TermModel[0];
            currentPrologDirectory = "";
        } else {
            userModFiles = (String[])bindings[0];
            libraryDirectories = (String[])bindings[1];
            insertedOps = (TermModel[])bindings[2];
            currentPrologDirectory = (String)bindings[3];
        }
        LogicProgramEditor.reparse();
        return true;
    }
        
    /* Returns a String adequate for reading as a Prolog list directly */
    public String userModFilesList(){
        StringBuilder S = new StringBuilder("[");
        for (int i = 0; i<userModFiles.length; i++){
            if (i>0) S.append(",");
            S.append("'"); S.append(userModFiles[i]); S.append("'"); 
        }
        S.append("]");
        return S.toString();
    }
        
    protected void mayTruncateEnd(){
        if (goalSuccessPattern!=null && FijiPreferences.floraSupported && (engine!=null) && ((FloraSubprocessEngine)engine).inFloraShell()) 
            mayTruncateEnd(goalSuccessPattern,goalSuccessPattern.pattern().length()*2);
        else super.mayTruncateEnd();
    }
        
    // PrologOutputListener method:
    public void print(String s){
        super.print(s);
    }
        
    public boolean checkFloraShell(){
        if (FijiPreferences.floraSupported){
            if (((FloraSubprocessEngine)engine).inFloraShell()) return true;
            String language = ((FloraSubprocessEngine)engine).getLanguage();
            JOptionPane.showMessageDialog(this,"To load a "+language+" file you need to activate the "+language+" shell; please use the Tools menu","Warning",
                                          JOptionPane.WARNING_MESSAGE); 
        } else
            JOptionPane.showMessageDialog(this,"Ergo and Flora-2 files not supported in this version","Error",
                                          JOptionPane.ERROR_MESSAGE);   
        return false;   
    }

    public void setWaitCursor() {
        XJDesktop.setWaitCursor(this);
    }

    public void restoreCursor() {
        XJDesktop.restoreCursor(this);
    }
        
    /** Declare XJ lazy (Prolog-dependent) components using this - or in general any UI elements you want locked when the engine is not available */
    public void disableWhenBusy(Component item){
        controller.disableWhenBusy(item);
    }
    public void disableWhenBusyOrPaused(Component item){
        controller.disableWhenBusyOrPaused(item);
    }
    public void disableWhenBusyOrPaused(Action item){
        controller.disableWhenBusyOrPaused(item);
    }
    public void disableWhenPaused(Component item){
        controller.disableWhenPaused(item);
    }
        
    /** Declare Prolog-dependent actions using this */
    public void disableWhenBusy(Action item){
        controller.disableWhenBusy(item);
    }
    
    public void setUItoBusy(){
        XJEngineController ec = (XJEngineController)engine.getThePrologListener();
        if (ec!=null) {
            ec.setUItoBusy();
        }
    }
    
    public void setUItoPausedOrIdle(){
        XJEngineController ec = (XJEngineController)engine.getThePrologListener();
        if (ec!=null) ec.setUItoPausedOrIdle();
    }
    
    public void setUItoNeedsMoreInput(){
        XJEngineController ec = (XJEngineController)engine.getThePrologListener();
        if (ec!=null) ec.setUItoNeedsMoreInput();
    }
    public boolean checkEngineAvailable(){
        if (controller.isInPause()) return true;
        else return super.checkEngineAvailable();
    }

    // Hacky code, redundant with XJPrologEngine
    // Depending on xpGlobalProgressController/1, the listener object or the (XJ-specific) engine will handle these

    public void showProgress(java.awt.Frame owner, String title, int lenght) {
        XJProgressDialog progress = new XJProgressDialog(engine,owner);
        XJPrologEngine.setProgressDialog(progress);
        progress.showProgress(title, lenght);
    }
        
    public void showProgress(String title,int length){
        showProgress(null, title, length);
    }
    public void setProgress(int amount){
        XJPrologEngine.getProgressDialog().setProgress(amount);
    }
    public void endProgress(){
        XJPrologEngine.getProgressDialog().endProgress();
    }
    public void setCancellableProgress(boolean yes){
        XJPrologEngine.getProgressDialog().setCancellableProgress(yes);
    }
        
    // ...end of hacky progress dialog code
        
    /** For logic applications desiring to persist a directory location in the .prefs file */
    public void setPreferredDirectory(String preferenceName, String dirName){
        File dir = new File(dirName);
        if (!dir.isDirectory())
            throw new RuntimeException(preferenceName+" requires a directory and not: "+dir);
        preferences.setProperty(preferenceName,dirName);
        preferences.store();
    }
    /** Returns the preferred directory, asking it to the user if necessary */
    public String getPreferredDirectory(String preferenceName, String userQuestion){
        String dir = getPreference(preferenceName);
        if (dir==null){
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(userQuestion);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(null)){
                dir = chooser.getSelectedFile().getAbsolutePath();
                setPreferredDirectory(preferenceName, dir);
            }
        }
        return dir;
    }
        
    public String getPreference(String propertyName){
        return preferences.getProperty(propertyName);
    }
        
    /** Attaches a ComponentListener to the component so that its preference is stored if new, and also kept current in memory*/
    public void updateBoundsPreference(final String preference,final Component C){
        boolean isNew = (preferences.get(preference)==null?true:false);
        rememberBoundsPreference(preference, C);
        if (isNew) 
            preferences.store();
        // Will not store preferences file below, for better performance
        C.addComponentListener(new ComponentAdapter(){
                public void componentMoved(ComponentEvent e){
                    rememberBoundsPreference(preference, C);
                }
                public void componentResized(ComponentEvent e){
                    rememberBoundsPreference(preference, C);
                }
            });
    }
        
    private void rememberBoundsPreference(final String preference,final Component C){
        preferences.setProperty(preference,FijiPreferences.window2pref(C,TabsWindow.inSomeTab(C)));
    }
        
    public FijiPreferences.ImpatientBugReporter makeIBR(int timeout,String situation,boolean sendEmail){
        return new FijiPreferences.ImpatientBugReporter(timeout, situation, sendEmail,this);
    }
        
    ArrayList<String> visibleWindowsPreferenceNames(){
        ArrayList<String> windowPrefNames = new ArrayList<String>();
        if(floraQueryPanel!=null && ((Window)XJDesktop.findWindowOrSimilar(floraQueryPanel)).isVisible())
            windowPrefNames.add(QUERYPANEL_PREF);
        if(problemsPanel!=null && ((Window)XJDesktop.findWindowOrSimilar(problemsPanel)).isVisible())
            windowPrefNames.add(COMPILATIONPANEL_PREF);
        if(tablesPanel!=null && ((Window)XJDesktop.findWindowOrSimilar(tablesPanel)).isVisible())
            windowPrefNames.add(TABLESPANEL_PREF);
        Iterator<LogicProgramEditor> eit = LogicProgramEditor.getEditors();
        while( eit.hasNext() ) {
            LogicProgramEditor e = eit.next();
            if (!e.anonymous && (e.isVisible() || TabsWindow.inSomeTab(e))) 
                windowPrefNames.add(e.preferenceName());
        }
        Iterator<AbstractProjectWindow> pit = AbstractProjectWindow.getProjects();
        while( pit.hasNext() ) {
            AbstractProjectWindow pw = pit.next();
            if (pw.isVisible()) // TODO: use TabsWindow here too
                windowPrefNames.add(pw.preferenceName());
        }
        if (TabsWindow.editors!=null && TabsWindow.editors.isVisible())
            windowPrefNames.add(TabsWindow.EDITORS_TABWINDOW_PREF);
        if (TabsWindow.justifiers!=null && TabsWindow.justifiers.isVisible())
            windowPrefNames.add(TabsWindow.JUSTIFIERS_TABWINDOW_PREF);
        if (TabsWindow.finders!=null && TabsWindow.finders.isVisible())
            windowPrefNames.add(TabsWindow.FINDERS_TABWINDOW_PREF);
        return windowPrefNames;
    }
        
    public String visibleWindowsPreference(){
        return FijiPreferences.array2pref(visibleWindowsPreferenceNames());
    }
        
    /**
     * Overwrite current factory preferences file with the currently open window set state; 
     * considers only system windows (neither editors nor project windows etc)
     * Use ?- ipListenerWindow(LW), java(LW,saveFactoryPreferences).
     * or
     * ergo> ipListenerWindow(?LW)@\prologall, java(?LW,saveFactoryPreferences)@\prologall(interprolog).
     * @return Whether the save succeeded
     */
    public boolean saveFactoryPreferences(){
        Properties factory = new Properties();
        factory.put(WINDOWS_SET_PREF, visibleWindowsPreference());
        for (String prefName:visibleWindowsPreferenceNames())
            if (!AbstractProjectWindow.hasPreferencePrefix(prefName) && !LogicProgramEditor.hasPreferencePrefix(prefName) )
                factory.put(prefName, preferences.get(prefName));
        factory.put(LISTENER_WINDOW_PREF, preferences.get(LISTENER_WINDOW_PREF));
        factory.put(FONT_SIZE_PREF, preferences.get(FONT_SIZE_PREF));
        factory.put(REOPEN_WINDOWS_SET_PREF, preferences.get(REOPEN_WINDOWS_SET_PREF));
        return preferences.storeFactoryPrefs(factory);
    }
        
    public void applyFactoryWindowPreferences(){
        Properties factory = preferences.getFactory();
        for(Entry<Object, Object> prefEntry:factory.entrySet()){
            String pref = (String)prefEntry.getKey();
            String value = (String)prefEntry.getValue();
            Component component = null;
            preferences.put(pref, value); // override current preference
            // update bounds, ignoring screen:
            if (FijiPreferences.floraSupported && pref.equals(QUERYPANEL_PREF)) component = floraQueryPanel;
            else if (FijiPreferences.floraSupported && pref.equals(COMPILATIONPANEL_PREF)) component = problemsPanel;
            else if (pref.equals(TABLESPANEL_PREF)) component = tablesPanel;
            else if (pref.equals(TabsWindow.EDITORS_TABWINDOW_PREF)) component = TabsWindow.editors;
            // MK: Don't reopen the justifier and term search windows:
            //     makes no sense! They are empty and ugly.
            //else if (FijiPreferences.floraSupported && pref.equals(TabsWindow.JUSTIFIERS_TABWINDOW_PREF)) component = TabsWindow.justifiers;
            //else if (FijiPreferences.floraSupported && pref.equals(TabsWindow.FINDERS_TABWINDOW_PREF)) component = TabsWindow.finders;
            else if (pref.equals(LISTENER_WINDOW_PREF)) component = this;
            FijiPreferences.adjustBounds(component,value); // tolerates null component
        }
        // Make sure new windows popup with factory prefs
        String value = (String)preferences.get(WINDOWS_SET_PREF);
        if (value!=null)
            reopenWindowSet(value,true);
    }
        
    void reopenWindowSet(){
        reopenWindowSet(getPreference(WINDOWS_SET_PREF),false);
    }
        
    /**
     * @param preference name of preference
     * @param factoryOnly Whether to reopen only the subset of system windows, exclusing user windows such as editors and projects
     */
    void reopenWindowSet(String preference, boolean factoryOnly){
        if (preference == null) return;
        for (String WS: FijiPreferences.pref2array(preference)){
            if (FijiPreferences.floraSupported && WS.equals(QUERYPANEL_PREF)) showFloraQuery();
            else if (FijiPreferences.floraSupported && WS.equals(COMPILATIONPANEL_PREF)) refreshProblemsPanel(true);
            else if (WS.equals(TABLESPANEL_PREF)) showTablesPanel();
            else if (WS.equals(TabsWindow.EDITORS_TABWINDOW_PREF)) TabsWindow.editors.setVisible(true);
            // MK: Don't reopen the justifier and term search windows:
            //     makes no sense! They are empty and ugly.
            //else if (FijiPreferences.floraSupported && WS.equals(TabsWindow.JUSTIFIERS_TABWINDOW_PREF)) TabsWindow.justifiers.setVisible(true);
            //else if (FijiPreferences.floraSupported && WS.equals(TabsWindow.FINDERS_TABWINDOW_PREF)) TabsWindow.finders.setVisible(true);
            else if (!factoryOnly){
                File F = null;
                if(AbstractProjectWindow.hasPreferencePrefix(WS)){
                    if (ergoMemoryProjectModel!=null && AbstractProjectWindow.preferenceName2Filename(WS).equals(ergoMemoryProjectModel.getName())){
                        if (ergoMemoryProjectWindow!=null){
                            System.err.println("??? Forgetting previous ergoMemoryProjectWindow");
                            ergoMemoryProjectWindow = null;
                        }
                        refreshErgoMemoryProjectWindow();
                    } else {
                        F = new File(AbstractProjectWindow.preferenceName2Filename(WS));
                        //System.out.println("reopening "+F);
                        if (F.exists())
                            EditableProjectWindow.showProject(F,this);
                    }
                } else if (LogicProgramEditor.hasPreferencePrefix(WS)){
                    F = new File(LogicProgramEditor.preferenceName2Filename(WS));
                    //System.out.println("reopening "+F);
                    if (F.exists())
                        try{ LogicProgramEditor.makeEditor(F,this);}
                        catch(IOException e){System.err.println("Can't reopen editor:"+e);}
                }
            }
        }
    }
        
    protected StyledOutputPane makeOutputPane(){
        StyledOutputPane newPane = null;
        if (FijiPreferences.floraSupported){
            // try to find proprietary subclass
            try{
                Constructor<?> method = AbstractPrologEngine.findConstructor(FijiPreferences.otherOutputPaneClass,new Class[]{});
                newPane = (StyledOutputPane)method.newInstance(new Object[]{});
                return newPane;
            } catch (Exception e){
                e.printStackTrace(System.err);
                throw new RuntimeException("Trouble making editor:"+e);
            }
        } 
        return new StyledOutputPane();
    }
    protected void initializeOutputStyles() {
        super.initializeOutputStyles();
        prologOutput.initializeOutputStyles();
    }
    protected void appendOutputTextRuns(String s){
        prologOutput.appendOutputTextRuns(s);
    }
        
    @Override
    public void printStdout(String s) {
        if (debug) System.out.println("print("+s+")");
        appendOutputTextRuns(s);
        // prologOutput.append(s,null);
        mayTruncateEnd(); 
        // SmartScroller handles this case; scrollToBottom();
    }

    protected void refreshErgoMemoryProjectWindow() {
        if (ergoMemoryProjectWindow == null)
            ergoMemoryProjectWindow = new VirtualProjectWindow(FijiSubprocessEngineWindow.this, ergoMemoryProjectModel);
        ergoMemoryProjectWindow.refresh();
        ergoMemoryProjectWindow.setVisible(true);
        ergoMemoryProjectWindow.toFront();
    }

    /* (non-Javadoc)
     * @see com.declarativa.interprolog.gui.ListenerWindow#constructDebugMenu()
     */
    @Override
    protected JMenu constructDebugMenu() {
        if (!FijiPreferences.floraSupported)
            return null;
        JMenu debugMenu = new JMenu("Debug"); 

        disableWhenBusy(debugMenu);
        addItemToMenu(debugMenu,
                      "Set a runtime monitor",
                      new ActionListener(){
                          public void actionPerformed(ActionEvent e){
                              ((FloraSubprocessEngine)engine).floraCommand("setmonitor{}");
                          }
                      }).setToolTipText("See statistics about the ongoing computations in real time");

        addItemToMenu(debugMenu,
                      "Handling of undefined calls",
                      new ActionListener(){
                          public void actionPerformed(ActionEvent e){
                              ((FloraSubprocessEngine)engine).floraCommand("mustDefine{}");
                          }
                      }).setToolTipText("Define what to do if a call has no supporting rules or facts");

        addItemToMenu(debugMenu,
                      "Set tripwires",
                      new ActionListener(){
                          public void actionPerformed(ActionEvent e){
                              ((FloraSubprocessEngine)engine).floraCommand("setruntime{}");
                          }
                      }).setToolTipText("Define timers, actions to perform when various limits are reached");

        addItemToMenu(debugMenu,
                      "Show active subgoals",
                      new ActionListener(){
                          public void actionPerformed(ActionEvent e){
                              ((FloraSubprocessEngine)engine).floraCommand("showgoals{}");
                          }
                      }).setToolTipText("Show significant subgoals currently being computed (useful during pauses)");

        addItemToMenu(debugMenu,
                      "Show complete subgoals",
                      new ActionListener(){
                          public void actionPerformed(ActionEvent e){
                              ((FloraSubprocessEngine)engine).floraCommand("showtables{}");
                          }
                      }).setToolTipText("Show significant subgoals that have being computed (useful after queries complete)");

        addItemToMenu(debugMenu,"Show all cached inferences",KeyEvent.VK_T,new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    if (!checkEngineAvailable()) return;
                    showTablesPanel();
                }
            }).setToolTipText("Browse through all cached inferences (can be slow, if the set is large)");

        addItemToMenu(debugMenu,
                      "Use Terminyzer",
                      new ActionListener(){
                          public void actionPerformed(ActionEvent e){
                              ((FloraSubprocessEngine)engine).floraCommand("terminyzer{}");
                          }
                      }).setToolTipText("Analyze symptoms of non-terminating behavior");

        debugMenu.addSeparator();

        addItemToMenu(debugMenu,
                      "Stop all monitoring",
                      new ActionListener(){
                          public void actionPerformed(ActionEvent e){
                              ((FloraSubprocessEngine)engine).floraCommand("flora_stop_monitoring(not_silent)@\\prolog(flrerrhandler)");
                          }
                      }).setToolTipText("Stop monitoring and termination analysis");

        return debugMenu;
    }   
    
}
