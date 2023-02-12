/* File:   FloraProgramEditor.java
**
** Author(s): Miguel Calejo
**
** Contact:   mc@interprolog.com
**
** Copyright (C) Coherent Knowledge Systems, LLC, 2014 - 2016.
** All rights reserved.
**
*/

package com.coherentknowledge.fidji;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;

import com.declarativa.fiji.FijiPreferences;
import com.declarativa.fiji.FijiSubprocessEngineWindow;
import com.declarativa.fiji.LogicCompletionProvider;
import com.declarativa.fiji.FijiSubprocessEngineWindow.LoadOrAddResult;
import com.declarativa.fiji.LogicProgramEditor;
import com.declarativa.fiji.TabsWindow;
import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.FloraSubprocessEngine;
import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.SubprocessEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.gui.ListenerWindow;
import com.declarativa.interprolog.util.IPPrologError;

@SuppressWarnings("serial")
public class FloraProgramEditor extends LogicProgramEditor{
    static final String MAX_PARSEABLE_SIZE_ERGO_PREF = "com.coherentknowledge.fidji.FloraProgramEditor.MAX_PARSEABLE_SIZE";
    protected static final String LOCAL_TOP_CALL_TREES_TITLE = "Top Calls for ";
    static Icon /*floraIcon, */ ergoIcon;
    static {
        // floraIcon = new ImageIcon(FloraProgramEditor.class.getResource("flora.gif")); 
        ergoIcon = new ImageIcon(FloraProgramEditor.class.getResource("coherent.png")); 
    }
    AbstractAction showTopAction;
	
    protected void updateTitle(){
        String suffix = "", prefix = "";
        if (wasLoadedOnce())
            suffix = " >> "+preferredModule;
        else if (wasAddedOnce()){
            prefix = "+";
            suffix = " >> "+preferredModule;
        } else if (wasIncludedOnce()){
            prefix = "# ";
            suffix = " >> "+preferredModule;
        }
        setTitle(prefix+(isDirty()?"*":"")+editor.getFileName()+suffix);
    }
	
    /**
     * @param exceptThis An editor whose state will not affect the result; may be null
     * @return whether at least one editor window (except exceptThis) has unloaded changes
     */
    public static boolean someNotFullyLoadedFloraEditor(LogicProgramEditor exceptThis){
        for (LogicProgramEditor LPE : editors.values())
            if (!LPE.isFullyLoaded() && LPE instanceof FloraProgramEditor && (exceptThis==null || exceptThis!=LPE)) 
                return true;
        return false;
    }
    public static boolean someFloraFileWasNeverLoaded(){
        for (LogicProgramEditor LPE : editors.values())
            if (LPE.neverLoadedNorAddedNorIncluded() && LPE instanceof FloraProgramEditor) 
                return true;
        return false;
    }
		
    public static LogicProgramEditor makeEditor(File F,FijiSubprocessEngineWindow listener) throws IOException{
        LogicProgramEditor newEditor;
        if (FloraSubprocessEngine.isFloraSourceFile(F) || FloraSubprocessEngine.isErgoSourceFile(F) || FloraSubprocessEngine.isErgotextSourceFile(F))
            newEditor = new FloraProgramEditor(F,(FloraSubprocessEngine)sharedValidatingEngine,(FijiSubprocessEngineWindow)listener,
                                               getPreferredBounds(F,listener), getPreferredSelection(F,listener));
        else
            newEditor = new LogicProgramEditor(F,sharedValidatingEngine,listener,getPreferredBounds(F,listener), getPreferredSelection(F,listener));
        final LogicProgramEditor newEditor_ = newEditor;
        if (getPreferredInTab(F,listener)) {
            TabsWindow.editors.addWindow(newEditor, newEditor.getTabItemAction(), false, 
                                         new Runnable(){
                                             @Override
                                             public void run() {
                                                 newEditor_.doClose();						
                                             }
                                         });
        } else 
            newEditor.setVisible(true);
        return newEditor;
    }
	
    static boolean isAggregateStart(String T){
        return T.startsWith("bagof") || T.startsWith("setof")|| T.startsWith("min")|| T.startsWith("max")|| T.startsWith("count")|| T.startsWith("sum")|| T.startsWith("avg");
    }

    protected LogicCompletionProvider buildCompletionProvider(final SubprocessEngine engine) {
        return new FloraCompletionProvider(engine);
    }
	
    /** An editor for an Ergo or Flora-2 file */
    protected FloraProgramEditor(final File F, final FloraSubprocessEngine engine, FijiSubprocessEngineWindow l, Rectangle preferredBounds, Point preferredSelection) 
        throws java.io.IOException{
        super(F,engine,l,preferredBounds,preferredSelection);
        if (!FloraSubprocessEngine.isFloraSourceFile(F) && !FloraSubprocessEngine.isErgoSourceFile(F) && !FloraSubprocessEngine.isErgotextSourceFile(F))
            throw new RuntimeException("Ergo or Flora-2 file expected:"+F);
        if (engine==null) 
            throw new RuntimeException("Ergo or Flora-2 editor requires an engine");
        preferredModule = "main";
        setWasLoadedAddedIncluded(true,false,false,false,null,preferredModule);
        ((RSyntaxDocument)editor.getDocument()).setSyntaxStyle(new FloraTokenMaker());
        editor.setMarkOccurrences(true);
        sp.setIconRowHeaderEnabled(true);
        checkIfParseable(new FloraParser(engine,this));
        
        if (engine.isErgo()) {
            newSubmenu.insert(((FijiSubprocessEngineWindow)listener).newErgoTextAction,0);
            newSubmenu.insert(((FijiSubprocessEngineWindow)listener).newErgoAction,0);
        } else
            newSubmenu.insert(((FijiSubprocessEngineWindow)listener).newFloraAction,0);
        
        if (!FloraSubprocessEngine.isErgotextSourceFile(F)){
            fileMenu.addSeparator();
            ListenerWindow.addItemToMenu(fileMenu,"Load...",KeyEvent.VK_L, new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        maySaveSource();
                        if (!listener.checkEngineAvailable()) return;   
                        if (!listener.checkFloraShell()) return;
                        JFrame parent = TabsWindow.editors.currentWindow(FloraProgramEditor.this);
                        
                        String picked = listener.pickModule(parent,preferredModule,"Load into which module?");
                        if (picked==null) return;
                        setWaitCursor();
                        listener.floraLoadCommand(new File(editor.getFileFullPath()),picked,FloraProgramEditor.this);
                        // preferredModule = picked; now gets set as consequence of the above
                        restoreCursor();
                    }
                }).setToolTipText("Save this Ergo file and load it into a module.");		
            /*
              ListenerWindow.addItemToMenu(fileMenu,"Load",new ActionListener(){
              public void actionPerformed(ActionEvent e){
              setWaitCursor();
              if (editor.isDirty()) saveAction.doIt();
              if (!listener.checkEngineAvailable()) return;    
              listener.floraLoadCommand(new File(editor.getFileFullPath()),preferredModule);
              restoreCursor();
              }
              }).setToolTipText("Save and load this Ergo file into the same module");	*/	
            ListenerWindow.addItemToMenuALT(
                  fileMenu, "Add...",
                  KeyEvent.VK_L,
                  new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        maySaveSource();
                        if (!listener.checkEngineAvailable()) return;    
                        if (!listener.checkFloraShell()) return;
                        String M = listener.pickModule(listener,preferredModule,"Add to which module?");
                        /*
                        // This one is wrong: Module should be editable, as it may not exist yet!!!
                        Object M = JOptionPane.showInputDialog(TabsWindow.editors.currentWindow(FloraProgramEditor.this),
                                                               "Add to which module?",
                                                               "Add To Module",
                                                               JOptionPane.PLAIN_MESSAGE,
                                                               null, //floraIcon,
                                                               FijiSubprocessEngineWindow.ModuleChooser.fetchModules(preferredModule,listener.engine),
                                                               preferredModule);
                        */
                        if (M==null) return;
                        setWaitCursor();
                        listener.floraAddCommand(new File(editor.getFileFullPath()),(String)M,FloraProgramEditor.this);
                        preferredModule = (String)M;
                        restoreCursor();
                    }
                  }).setToolTipText("Save this Ergo file and add it to a module.");		
        }
        fileMenu.addSeparator();
        fileMenu.add(listener.inMemoryAction);
        listener.disableWhenBusy(goToDefinitionAction);
        
    }
    
    //TODO: This implementation is redundant with subTermAtCursor
    protected ToolTipSupplier createTooltipSupplier(AbstractPrologEngine e) {
        final FloraSubprocessEngine engine = (FloraSubprocessEngine)e;
        return new ToolTipSupplier(){
            public String getToolTipText(RTextArea textArea,MouseEvent e){
                Token current =  ((RSyntaxTextArea)textArea).viewToToken(e.getPoint());
                // if (current!=null) System.out.println( current.toString());
                if (!e.isControlDown() && current!=null && isFloraFile() &&
                    (current.getType()==Token.VARIABLE || current.getType()==Token.IDENTIFIER || current.getType()==Token.LITERAL_CHAR)) {
                    Token last = lastSubtermToken(current,isFloraFile());
                    TermModel TM  = null;
                    String T = "?";
                    String tip = "";
                    try{
                        int L = last.getEndOffset()-current.getOffset();
                        if (L<0) L=1; // TODO: replace this hack by tuning of lastSubtermToken()
                        T = editor.getText(current.getOffset(),L).trim();
                        if (!engine.isAvailable()) {
                            System.err.println("Auxiliar engine unavailable in gettoolTipText");
                            return null;
                        }
                        if (isAggregateStart(T))
                            return "Aggregate expression, try hovering its query body";
                        if (T.startsWith("%"))
                            T = "${"+T+"}";
                        FijiPreferences.ImpatientBugReporter ibr = new FijiPreferences.ImpatientBugReporter(1000,"in getToolTipText",null,false,true,true,T,listener,engine);
                        TM = engine.floraDeterministicGoal("%buildHilogTree(("+T+"),?fj_HT)@"+FijiSubprocessEngineWindow.ERGO_STUDIO_MODULE+", p2h{?fj_HTP,?fj_HT}.","?fj_HTP");
                        //TM = engine.floraDeterministicGoal("%buildHilogTree((?T),?fj_HT)@fidji, p2h{?fj_HTP,?fj_HT}.","?fj_HTP");
                        /*TM = (TermModel)engine.deterministicGoal(
                          "fj_flora_query('%buildHilogTree(("+T+"),?fj_HT)@fidji, p2h{?fj_HTP,?fj_HT}.',['?fj_HTP'=HT],_Status,_WAMSTATE,Ex), Ex==normal, _WAMSTATE=:=0, buildTermModel(HT,__TM)",
                          "[__TM]"
                          )[0];*/
                        ibr.dontWorry();
                        tip = TM.toIndentedString();
                    } catch (NullPointerException ex){
                        System.err.println("Tooltip failed for " + T);
                    } catch (IPPrologError ex){
                        System.err.println("Tooltip error for " + T);
                        System.err.println("current=="+current+" last=="+last);
                        System.err.println(ex);
                    } catch (Exception ex){
                        System.err.println("current token:"+current);
                        System.err.println("last token:"+last);
                        // throw new RuntimeException("Bad exception" + ex); getting here too often, possibly because of some violent exception in %buildHilogTree, which should be firmed up
                        System.err.println("Bad exception" + ex);
                    } 
                    // return "Text: "+T+"\nSubterm:\n"+tip;
                    return tip;
                } else if (e.isControlDown() && isFloraFile()) {
                    PrologEngine mainEngine = listener.engine;
                    if (!mainEngine.isAvailable())
                        return "You can't inspect clauses while executing a goal";
                    // Show compiled omni rules
                    int pos = editor.viewToModel(e.getPoint());
                    if (pos < 0 ) return null;
                    int line = editor.getDocument().getDefaultRootElement().getElementIndex(pos) + 1;
                    int char_pos = pos - editor.getDocument().getDefaultRootElement().getElement(line-1).getStartOffset() + 1;
                    // find all clauses with compatible textual info:-)
                    // %textualPositionToClause(24,17,'/Users/mc/Dropbox/declarativa/projectos/Fidji/MichaelMeetingExamples.ergo',?RuleID,?Module)@fidji, clause{@{?Tag} @!{?RuleID} ?H,?Body}.
                    Object[] bindings = mainEngine.deterministicGoal(
                                                                     "flora_textualPositionToClausesAtom("+line+","+char_pos+",FilePath,ClausesAtom)", 
                                                                     "[string(FilePath)]", 
                                                                     new Object[]{getFileName()}, 
                                                                     "[string(ClausesAtom)]");
                    if (bindings == null)
                        return "Can't see that loaded clause. \nPlease make sure you have loaded this file and that you're hovering rules, not facts.";
                    return (String)bindings[0];
                    
                }
                else return null;
            }
        };
    }
    /** This implementation does nothing */
    protected void configurePopupMenu(JPopupMenu popupMenu, JMenu calls, JMenu calledBy){}
    
    /* This implementation does nothing
     * @see com.declarativa.fiji.LogicProgramEditor#mayAddFindNext()
     */
    protected void mayAddFindNext(){}	
    
    // Should be refactored with the superclass version
    protected JMenu predicatesMenu(){
        String menuTitle = "Predicates and Objects";
        final JMenu _predicatesMenu = new JMenu(menuTitle); 
        _predicatesMenu.addMenuListener(new MenuListener(){
                public void menuCanceled(MenuEvent e){}
                public void menuDeselected(MenuEvent e){}
                public void menuSelected(MenuEvent e){
                    _predicatesMenu.removeAll();
                    ArrayList<Token> tokens = predicateHeads(100,"neg");
                    for (final Token head : tokens){
                        JMenuItem predicateItem = new JMenuItem(head.getLexeme());
                        Font smaller = predicateItem.getComponent().getFont().deriveFont((float)(listener.preferredFontSize));
                        predicateItem.getComponent().setFont(smaller);
                        predicateItem.addActionListener(new ActionListener(){
                                public void actionPerformed(ActionEvent ae){
                                    backAction.rememberPosition(FloraProgramEditor.this);
                                    editor.setCaretPosition(head.getOffset());
                                    backAction.didJump(FloraProgramEditor.this);
                                }
                            });
                        _predicatesMenu.add(predicateItem);
                    }
                }
            });
        return _predicatesMenu;
    }
    protected void setupNavigationMenus(JMenuBar mb) {
        JMenu navigationMenu = new JMenu("Navigation");
        showTopAction = new AbstractAction("Show "+LOCAL_TOP_CALL_TREES_TITLE+(preferredModule==null?"this module":preferredModule)){
                @Override
                public void actionPerformed(ActionEvent e) {
                    showLocalTopCallTrees();
                }		
            };
        // Not feeling like creating an AbstractAction subclass for this, so.....:
        addPropertyChangeListener(MODULE_PROPERTY,new PropertyChangeListener(){
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (showTopAction!=null)
                        showTopAction.putValue(Action.NAME, "Show "+LOCAL_TOP_CALL_TREES_TITLE+preferredModule);
                }	
            });
        listener.disableWhenBusy(showTopAction);
        navigationMenu.add(showTopAction);
        mb.add(navigationMenu);
        //super.setupNavigationMenus(mb);
        navigationMenu.add(predicatesMenu());
    }
    
    protected TermModel parseTextToTerm(String S){
        return ((FloraSubprocessEngine)engine).floraDeterministicGoal("%buildHilogTree(("+S+"),?fj_HT,\\true,0,?)@"+FijiSubprocessEngineWindow.ERGO_STUDIO_MODULE+", p2h{?fj_HTP,?fj_HT}.","?fj_HTP");
    }
    protected void doQueryString(final String stringWithoutDot){
        if (isFloraFile()) {
            Runnable doer = new Runnable(){
                    @Override
                    public void run() {
                        String S = floraLiteralWithModule(stringWithoutDot);
                        listener.showFloraQuery(S+".",true);
                    }		
                };
            doAfterFloraDeemedFullyLoaded(doer);
        } else super.doQueryString(stringWithoutDot);
    }
    protected String floraLiteralWithModule(String stringWithoutDot){
        String S = stringWithoutDot;
        if (!stringWithoutDot.contains("@") && !preferredModule.equals("main")){  //in some cases (e.g. a:=:b) it would cause an error
            if (!stringWithoutDot.startsWith("(") && !stringWithoutDot.endsWith(")"))
                S = "("+stringWithoutDot+")" + "@" + preferredModule;
            else S = stringWithoutDot + "@" + preferredModule;
        }
        return S;
    }
    /*
      protected int endOfSubTerm(Token last){
      int end = last.getEndOffset();
      Iterator<Token> it = getNextTokens((RSyntaxDocument)editor.getDocument(), last);
      if (it.hasNext() && (it.next()).is(Token.RESERVED_WORD,"@") && it.hasNext()){
      Token first = it.next();
      Token lastOfModule = lastSubtermToken(first,isFloraFile());
      end = lastOfModule.getEndOffset();
      }
      return end;
      }*/
    public void showDefinitionFor(final String stringWithoutDot_){
        Runnable doer = new Runnable(){
                @Override
                public void run() {
                    String stringWithoutDot = "${"+floraLiteralWithModule(stringWithoutDot_)+"}";
                    //FijiPreferences.ImpatientBugReporter ibr = new FijiPreferences.ImpatientBugReporter((FloraSubprocessEngine)listener.getEngine());
                    String G = "?_fj_no_Head_like_this_ = ("+stringWithoutDot+"), %showDefinitionFor(?_fj_no_Head_like_this_)@"+FijiSubprocessEngineWindow.ERGO_STUDIO_MODULE;		
                    System.err.println("G:"+G);
                    if (! ((FloraSubprocessEngine)listener.getEngine()).floraDeterministicGoal (G))
                        Toolkit.getDefaultToolkit().beep();
                    //ibr.dontWorry();
                }
			
            };
        doAfterFloraDeemedFullyLoaded(doer);
    }
	
    LoadOrAddResult loadOrAdd(){
        if (!wasAddedOnce() && !wasLoadedOnce())
            throw new RuntimeException("Bad use of loadOrAdd");
        return listener.loadOrAdd(new File(editor.getFileFullPath()),(wasAddedOnce()?"\\add":"\\load"), preferredModule);
    }
    protected void doAfterFloraDeemedFullyLoaded(Runnable doer) {
        doAfterFloraDeemedFullyLoaded(doer,listener,this);
    }
    public static boolean doAfterFloraDeemedFullyLoaded(FijiSubprocessEngineWindow listener){
        return doAfterFloraDeemedFullyLoaded(null,listener,null);
    }
    public static boolean doAfterFloraDeemedFullyLoaded(FijiSubprocessEngineWindow listener,LogicProgramEditor editor){
        return doAfterFloraDeemedFullyLoaded(null,listener,editor);
    }
    // Synchronous implementation, which will run AWT thread; background implementation can be done with (say) a chain of SwingWorkers
    /** Do something after all Flora files are loaded (or if the user doesn't care that they don't)
     * @param doer null if nothing to do (acts as mere test)
     * @param listener 
     * @param editor null if no particular editor window focused
     * @return Whether Flora files are deemed fully loaded
     */
    public static boolean doAfterFloraDeemedFullyLoaded(Runnable doer, FijiSubprocessEngineWindow listener, LogicProgramEditor editor) {
        // not necessary.... will be done by saving ops below: listener.mayRefreshMemoryProject();
        if (someNotFullyLoadedFloraEditor(null)){
            if (!someFloraFileWasNeverLoaded()){
                // all files were loaded at least once, let's auto-load
                boolean errors = false;
                boolean warnings = false;
                HashSet<File> loaded = new HashSet<File>();
                HashSet<File> includers = new HashSet<File>();
                for (LogicProgramEditor LPE : editors.values()){
                    if (!(LPE instanceof FloraProgramEditor))
                        continue;
                    if (!LPE.isFullyLoaded()){
                        LPE.saveAction.doIt();
                        if (LPE.wasIncludedOnce())
                            // We can't load it directly, write it down for later below:
                            includers.add(LPE.getIncluder());
						
                        // In principle the following condition will be disjuntive... 
                        // but someone may really want to load an included file on its own!
                        if (LPE.wasAddedOnce() || LPE.wasLoadedOnce()) {
                            LoadOrAddResult result = ((FloraProgramEditor)LPE).loadOrAdd();
                            errors = result.getHasErrors();
                            warnings = warnings || result.getHasOnlyWarnings();
                        }
                    } else continue;
                    if (errors)
                        break;
                    else
                        loaded.add(LPE.getFile());					
                }
                if (!errors){
                    // Let's make sure files that include modified files are also reloaded
                    for (File includer:includers)
                        if (!(loaded.contains(includer))){
                            LogicProgramEditor LPEi = getExistingEditor(includer);
                            if (LPEi!=null&&LPEi.isDirty())
                                LPEi.saveAction.doIt();
                            LoadOrAddResult result = listener.reloadErgo(includer);
                            errors = result.getHasErrors();
                            warnings = warnings || result.getHasOnlyWarnings();
                        }
                }
                listener.refreshProblemsPanel(errors || warnings);
                if (errors){
                    JOptionPane.showMessageDialog(null,
                                                  "Please fix your program errors first","Auto load failed",JOptionPane.ERROR_MESSAGE);
                    return false;
                } 
                if (doer!=null)
                    doer.run();
            } else {
                // There's at least one Flora/Ergo file open in the editor which was never loaded, 
                // maybe because the user likes staring at it on a large screen!
                // So we have no clue wrt to its module or loading action (load/add)... 
                // ...and the user MUST confirm that we can ignore such files.
                String subMessage = !(editor!=null && !editor.isFullyLoaded()) ? "At least one file open in the Studio editor":"This editor file";
                if (
                    JOptionPane.showConfirmDialog(
                                                  null,
                                                  subMessage+" was never loaded. Proceed without loading?",
                                                  "Proceed without loading files first?",
                                                  JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION)
                    return false;
                else if (doer!=null)
                    doer.run();
            }
        } else if (doer!=null)
            doer.run();
        return true;
    }
	
    protected void showCallHierarchy(final String stringWithoutDot){
        Runnable doer = new Runnable(){
                @Override
                public void run() {
                    String term = "${"+floraLiteralWithModule(stringWithoutDot)+"}";
                    System.err.println("term:"+term);
                    if (!listener.getEngine().deterministicGoal(
                                                                "fjFloraCallHierarchy('"+CALL_HIERARCHY_TITLE+"',TermWithoutDot,"+listener.getEngine().registerJavaObject(FloraProgramEditor.this)+")", 
                                                                "[string(TermWithoutDot)]", new Object[]{term})){
                        Toolkit.getDefaultToolkit().beep();
                        System.err.println("Failed call hierarchy for "+term);
                    }
                }
            };
        doAfterFloraDeemedFullyLoaded(doer);
    }

    protected void showCallTree(final String stringWithoutDot){		
        Runnable doer = new Runnable(){
                @Override
                public void run() {
                    String term = "${"+floraLiteralWithModule(stringWithoutDot)+"}";
                    System.err.println("term:"+term);
                    if (!listener.getEngine().deterministicGoal(
                                                                "fjFloraCallTree('"+CALL_TREE_TITLE+"',TermWithoutDot,"+listener.getEngine().registerJavaObject(FloraProgramEditor.this)+")", 
                                                                "[string(TermWithoutDot)]", new Object[]{term})){
                        Toolkit.getDefaultToolkit().beep();
                        System.err.println("Failed call tree for "+term);
                    }
                }
            };
        doAfterFloraDeemedFullyLoaded(doer);
    }
	
    protected void showLocalTopCallTrees(){	
        setWaitCursor();
        Runnable doer = new Runnable(){
                @Override
                public void run() {
                    if (!listener.getEngine().deterministicGoal(
                                                                "fjFloraLocalCallTrees('"+LOCAL_TOP_CALL_TREES_TITLE+"','"+
                                                                preferredModule+"',"+
                                                                //listener.getEngine().registerJavaObject(FloraProgramEditor.this)  to center over the editor window...
                                                                null
                                                                +")"
                                                                ))
                        {
                            Toolkit.getDefaultToolkit().beep();
                            System.err.println("Failed "+LOCAL_TOP_CALL_TREES_TITLE+ " for module "+preferredModule);
                        }
                }
            };
        doAfterFloraDeemedFullyLoaded(doer);
        restoreCursor();
    }
	
    protected boolean isNegation(Token T){
        return T.is(Token.RESERVED_WORD, "\\neg") || T.is(Token.RESERVED_WORD, "\\+")|| T.is(Token.RESERVED_WORD, "\\naf");
    }
	
    public boolean isFloraFile(){ return true; }
    public boolean isLogicFile(){return true; }

    protected int maxParseableSize(){
        String P = listener.getPreference(MAX_PARSEABLE_SIZE_ERGO_PREF);
        int PM = 0;
        if (P!=null){
            try{
                PM = Integer.parseInt(P);
                if (PM>0) return PM;
                else  System.err.println("Ignoring negative int in preference "+MAX_PARSEABLE_SIZE_ERGO_PREF+":"+P);
            } catch(NumberFormatException e){
                System.err.println("Ignoring bad preference "+MAX_PARSEABLE_SIZE_ERGO_PREF+":"+P);
            }
        }
        return defaultParseableSize();
    }
	
    protected String parseablePreferenceName(){
        return MAX_PARSEABLE_SIZE_ERGO_PREF;
    }
			
    public static int defaultParseableSize(){
        return (int)(300000.0/FijiPreferences.getSlowness()); // about 1.5 seconds to parse a 300k Ergo file, barely acceptable
    }
	
}
