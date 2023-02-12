/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2013
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/

package com.declarativa.fiji;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

import org.fife.rsta.ui.GoToDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.rtext.SyntaxFilters;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.DocumentRange;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.OccurrenceMarker;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaHighlighter;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import org.fife.ui.rtextarea.SmartHighlightPainter;
import org.fife.ui.rtextarea.ToolTipSupplier;
import org.mozilla.universalchardet.UniversalDetector;

import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.SubprocessEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.gui.ListenerWindow;
import com.declarativa.interprolog.gui.TermModelWindow;
import com.declarativa.interprolog.util.IPException;
import com.declarativa.interprolog.util.IPPrologError;
import com.xsb.xj.XJDesktop;

/** A window for editing Prolog or text files */
@SuppressWarnings("serial")
public class LogicProgramEditor extends JFrame implements PropertyChangeListener, TabsWindow.Editable, SearchListener{
    public static final int FULL_STOP = PrologTokenMaker.FULL_STOP;  
    static protected HashMap<File,LogicProgramEditor> editors = new HashMap<File,LogicProgramEditor>(); 
    protected static SubprocessEngine sharedValidatingEngine = null;
    protected SubprocessEngine engine;
    static Theme theme;
    static protected ImageIcon[] icons;
    static {
        try{
            icons = new ImageIcon[3];
            icons[0] = new ImageIcon(LogicProgramEditor.class.getResource("error_obj.gif"));
            icons[1] = new ImageIcon(LogicProgramEditor.class.getResource("warning_obj.gif"));
            icons[2] = new ImageIcon(LogicProgramEditor.class.getResource("info_obj.gif"));
            File themeFile = new File(FijiPreferences.getFidjiDir(),".fidjiTheme.xml"); // "invisible" file present
            if (themeFile.exists()){
                FileInputStream FS = new FileInputStream(themeFile);
                theme = Theme.load(FS);
                FS.close();
            } else theme = Theme.load(LogicProgramEditor.class.getResourceAsStream("/com/declarativa/fiji/fidjiTheme.xml"));
        } catch (IOException e) {
            System.err.println("Could not load syntax coloring theme or icon:"+e);
        }
        System.setProperty("UnicodeWriter.writeUtf8BOM","false"); // XSB does not like BOMs
    }
    private static SyntaxFilters syntaxMapper = new SyntaxFilters();
    protected RTextScrollPane sp;
    protected FijiSubprocessEngineWindow listener;
    public MyTextEditorPane editor;
    private ReplaceDialog findReplaceDialog;
    boolean prologFile = false;
    boolean QUALMfile = false;
    boolean lps_pFile = false;
    boolean lps_wFile = false;
    boolean lps_File = false;

    public SaveAction saveAction = null;
    protected SaveAsAction saveAsAction = null;
    final ShowWindowAction showWindowAction; 
    protected GoToDefinitionAction goToDefinitionAction;
    protected JMenu windowsMenu, fileMenu;
    protected JMenu newSubmenu, editMenu;
    static final String PREF_PREFIX = "com.declarativa.interprolog.gui.LogicProgramEditor.";
    protected static final String WAS_LOADED_PROPERTY = "WAS_LOADED";
    protected static final String WAS_ADDED_PROPERTY = "WAS_ADDED";  
    protected static final String WAS_INCLUDED_PROPERTY = "WAS_INCLUDED";
    protected static final String MODULE_PROPERTY = "PREFERRED_MODULE";
    private static final String MAX_PARSEABLE_SIZE_PREF = "com.declarativa.fiji.LogicProgramEditor.MAX_PARSEABLE_SIZE";

    protected boolean anonymous;
    /** Whether the file was loaded (without errors) */
    protected boolean wasLoadedOnce_;
	
    /** Whether the file was included by another (without known errors) */
    protected boolean wasIncludedOnce_;

    /** Undefined if !wasIncludedOnce_ */
    protected File includer;

    /** Whether the file was added, e.g. consulted in multifile (non redefining) mode */
    protected boolean wasAddedOnce_;

    protected boolean someStuffNotYetLoaded = false;
	
    protected boolean hasWarnings=false, hasErrors=false;
	
    protected ImageIcon currentStatusIcon = null;
	
    /** place holder hack for editor status icon, used on Mac only since its windows do not display their icons*/
    protected JMenu iconMenu;

    /** Significant only for non-XSB Prolog languages */
    protected String preferredModule;
	
    private static HashSet<EditorLifecycleListener> listeners = new HashSet<EditorLifecycleListener>();
	
    protected Action tabItemAction;
	
    public Action getTabItemAction() {
        return tabItemAction;
    }
	
    protected static BackAction backAction = new BackAction();

    static void reparse(){
        for (Iterator<LogicProgramEditor> i = editors.values().iterator(); i.hasNext();){
            LogicProgramEditor lpe = i.next();
            if (lpe.isLogicFile())
                lpe.forceReparsing();
        }
    }
		
    public static void setValidatingEngine(SubprocessEngine engine){
        sharedValidatingEngine=engine;
    }
	
    void forceReparsing(){
        if (editor!=null)
            for (int i=0; i< editor.getParserCount(); i++)
                editor.forceReparsing(i);
    }
	
    protected int maxParseableSize(){
        String preferenceName = parseablePreferenceName();
        String P = listener.getPreference(preferenceName);
        int PM = 0;
        if (P!=null){
            try{
                PM = Integer.parseInt(P);
                if (PM>0) return PM;
                else  System.err.println("Ignoring negative int in preference "+preferenceName+":"+P);
            } catch(NumberFormatException e){
                System.err.println("Ignoring bad preference "+preferenceName+":"+P);
            }
        }
        return (int)(1200000.0/FijiPreferences.getSlowness()); // about 1.1 seconds to parse a 1.2 Mb Prolog file on the reference machine
    }
	
    static Iterator<LogicProgramEditor> getEditors(){
        return editors.values().iterator();
    }
	
    public static LogicProgramEditor makeEditor(String F,FijiSubprocessEngineWindow listener) throws IOException{
        return makeEditor(new File(F), listener);
    }
	
    public static LogicProgramEditor makeEditor(File F,FijiSubprocessEngineWindow listener) throws IOException{
        return makeEditor(F,listener,false);
    }
	
    public static LogicProgramEditor makeEditor(File F,FijiSubprocessEngineWindow listener, boolean anonymous) throws IOException{
        LogicProgramEditor newEditor;
        // On a 64-bit machine our RSTA's/Swing's content takes about (60 bytes per line + 2 bytes per char)
        // Assuming 60 chars average size for lines... plus some working ram...
        long estimate = (1+3)*F.length();
        long memAvailable = Runtime.getRuntime().maxMemory();
        if (estimate > memAvailable){
            JOptionPane.showMessageDialog(null,"Your file is too large for the memory currently available for Java\n"+
                                          "Consider launching Studio with at least java -Xmx 2048m , or some value adding about "+(estimate/(1024*1024))+" Mb\n to the current parameter",
                                          "Can not open",JOptionPane.ERROR_MESSAGE);
            return null;

        }
        if (FijiPreferences.floraSupported){
            try{
                Method method = AbstractPrologEngine.findMethod(FijiPreferences.otherEditorClass,"makeEditor",new Class[]{File.class,FijiSubprocessEngineWindow.class});
                newEditor = (LogicProgramEditor)method.invoke(FijiPreferences.otherEditorClass,new Object[]{F,listener});
            } catch (Exception e){
                e.printStackTrace(System.err);
                throw new RuntimeException("Trouble making editor:"+e);
            }
        } else {
            listener.refreshUserModFilesAndLibDirsAndOps(); // made here to avoid sending a reparse request prematurely
            newEditor = new LogicProgramEditor(F,sharedValidatingEngine,listener,getPreferredBounds(F,listener), getPreferredSelection(F,listener));
            final LogicProgramEditor newEditor_ = newEditor;
            if (getPreferredInTab(F,listener)) 
                TabsWindow.editors.addWindow(newEditor, newEditor.tabItemAction, false, 
                                             new Runnable(){
                                                 @Override
                                                 public void run() {
                                                     newEditor_.doClose();
                                                 }
					
                                             });
            else 
                newEditor.setVisible(true);
        }
        newEditor.anonymous = anonymous;
        return newEditor;
    }
	
	
    /** show or make an editor for a KB file; filename must be escaped (as in Prolog side), @see#prologFilename(). 
	Format of notice: notice(warning_or_error,M,p(FirstLine,FirstChar,LastLine,LastChar),null_or_p2). 
	If !beginOnly, selects the whole range */
    public static LogicProgramEditor showEditor(String filename, TermModel notice,FijiSubprocessEngineWindow listener, boolean beginOnly) {
        return showEditor(filename, null, notice, -1, null, -1, listener, beginOnly, false);
    }
    /** -1 for position and arity if unknown */
    protected static LogicProgramEditor showEditor(String filename, String contextFile, TermModel notice, int position, String functor, int arity, FijiSubprocessEngineWindow listener, boolean beginOnly, boolean noBreadcrumb) {
        File F = new File(filename);
        if (contextFile!=null && !F.isAbsolute()){
            File context = new File(contextFile);
            F = new File(context.getParentFile(),filename);
        }
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (!noBreadcrumb && activeWindow!=null && activeWindow instanceof LogicProgramEditor)
            backAction.rememberPosition((LogicProgramEditor)activeWindow);
		
        LogicProgramEditor editor = getExistingEditor(filename);
        if (editor==null){
            try{ editor = makeEditor(F,listener);}
            catch (IOException ex){
                JOptionPane.showMessageDialog(null,"Error opening file:\n"+ex,"Error",JOptionPane.ERROR_MESSAGE);	
                return null;
            }
        }
        Window currentWindow = TabsWindow.editors.bringToFront(editor);
        MyTextEditorPane realEditor = editor.editor;
        if (notice!=null){
            DocumentRange R = termToRange(notice,(RSyntaxDocument)realEditor.getDocument());
            if (R!=null){
                realEditor.setSelectionStart(R.getStartOffset());
                if (!beginOnly) realEditor.setSelectionEnd(R.getEndOffset());
                else realEditor.setSelectionEnd(R.getStartOffset());
            }
        } else if (position>=0) {
            Token T = firstInterestingToken(position,realEditor.getDocument());
            realEditor.mySetCaretPosition(T.getOffset());
        } else if (functor!=null){
            // try to find first clause of functor/arity
            // System.out.println("Should show "+functor+"/"+arity);
            ArrayList<Token> heads = editor.predicateHeads(1, null, functor, arity, false);
            if (heads.size()!=1) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(currentWindow, "Could not find "+functor+"/"+arity + ".\n A :-dynamic perhaps? ") ;
            } else realEditor.mySetCaretPosition(heads.get(0).getOffset());  
        }
        realEditor.requestFocusInWindow(); 
        if (!noBreadcrumb)
            backAction.didJump(editor);
        listener.mayRefreshMemoryProject();
        return editor;
    }
    public static LogicProgramEditor showEditor(String filename, String contextFile, TermModel notice,FijiSubprocessEngineWindow listener) {
        return showEditor(filename, contextFile, notice, -1, null, -1, listener, false, false);
    }
	
    public static LogicProgramEditor showEditor(String filename, TermModel notice,FijiSubprocessEngineWindow listener) {
        return showEditor(filename, notice, listener, false);
    }
	
    public static LogicProgramEditor showEditor(String filename, int position, String functor, int arity, FijiSubprocessEngineWindow listener) {
        return showEditor(filename, null, null,position, functor, arity, listener, false, false);
    }
	
    /** Returns first Token found from offsetBegin that is not a comment or whitespace */
    static Token firstInterestingToken(int offsetBegin,Document doc){
        int tokenLine = doc.getDefaultRootElement().getElementIndex(offsetBegin);
        Token first = ((RSyntaxDocument)doc).getTokenListForLine(tokenLine);
        int lineCount = doc.getDefaultRootElement().getElementCount();
        while ((first==null||first.getType()==Token.NULL) && tokenLine < lineCount-1){ // end of last token may be empty line...
            tokenLine ++;
            first = ((RSyntaxDocument)doc).getTokenListForLine(tokenLine);
        }
		
        // Token current = RSyntaxUtilities.getTokenAtOffset(first, offsetBegin);
        Iterator<Token> linetokens = getNextTokens((RSyntaxDocument) doc, first, true);
        Token current = first;
        while (linetokens.hasNext() && (current=linetokens.next())!=null && 
               (current.getType()==Token.COMMENT_MULTILINE||current.getType()==Token.COMMENT_DOCUMENTATION||current.getType()==Token.COMMENT_EOL||current.getType()==Token.WHITESPACE || current.getOffset()<offsetBegin));
        if (current==null)
            current=first;
        return current;
    }
    /** Returns first Token found from offsetBegin that is not a comment or whitespace, skipping toSkip terms in the file. */
    static Token firstInterestingToken(int offsetBegin,Document doc,int toSkip){
        Token first = firstInterestingToken(offsetBegin,doc);
        while (toSkip>0){
            if (first==null) return null;
            Token last = lastTokenOfClause((RSyntaxDocument)doc, first);
            if (last==null) return null;
            first = firstInterestingToken(last.getEndOffset(),doc);
            toSkip--;
        }
        return first;
    }
	
    /** Will handle terms in slightly different formats:
	t(CharPos,TermIndex)
	notice(warning_or_error,Message,PositionTerm)  PositionTerm is p(CharPos,TermIndex)
        Where Message is an atom or singleton(Var) or .... - in which case the position refers to the whole term, not the var
	notice(warning_or_error,Message,p(FirstLine,FirstChar,LastLine,LastChar),null_or_p2,ShortFilename(may be null))
	p(FirstLine,FirstChar,LastLine,LastChar)
	returns null if the notice should be ignored
	Lines and characters start at 1, not 0
    */
    public static DocumentRange termToRange(TermModel N,final RSyntaxDocument doc){
        TermModel P1=null,P2=null;
        int offsetBegin=-1, offsetEnd;
		
        boolean xref_location = N.node.equals("t") && N.getChildCount()==2; // N is a simple xref position
        boolean floraRuleIDrange = N.node.equals("p");
        boolean floraNotice = N.node.equals("notice") && N.getChildCount()==5;

        if (floraRuleIDrange) 
            P1 = N;
        else if (floraNotice) {
            P1 = (TermModel)N.getChild(2);
            P2 = (TermModel)N.getChild(3);
            if (P1.isLeaf() && P2.isLeaf()){
                return null; 
            }
        }
        if (floraRuleIDrange || floraNotice){
            int tk1beginLine = P1.children[0].intValue();
            int tk1beginChar = P1.children[1].intValue();
            offsetBegin = doc.getDefaultRootElement().getElement(tk1beginLine-1).getStartOffset()+tk1beginChar-1;
            int line2 = P1.children[2].intValue();
            int column2 = P1.children[3].intValue();
		
            if (line2==-1){ // select whole line only
                line2 = tk1beginLine;
                column2 = doc.getDefaultRootElement().getElement(tk1beginLine-1).getEndOffset() - offsetBegin /* + 1 */;
            }
            if (!floraRuleIDrange && !P2.isLeaf()){
                line2 = P2.children[2].intValue();
                column2 = P2.children[3].intValue();
            }
            offsetEnd = doc.getDefaultRootElement().getElement(line2-1).getStartOffset()+column2-1;
        } else /*prologNotice: notice(Type,Message,PositionTerm) or t(CharPos,TermIndex) */ {
            Token current = null;
			
            TermModel position = (xref_location?N:N.children[2]);
			
            if (!position.isLeaf()){ // somehow (from LPS presumably) getting "unknown" as a position, here's hack to deal with that
                if (position.children[0].node.equals("end_of_file")) {
                    // use term index
                    int termIndex = position.children[1].intValue();
                    Token first = firstInterestingToken(0,doc,termIndex-1);
                    if (first!=null) {
                        offsetBegin = first.getOffset();
                        current = first;
                    }
                    else offsetBegin = -1;
                } else {
                    offsetBegin = position.children[0].intValue();
                    current = firstInterestingToken(offsetBegin,doc);
                    offsetBegin = current.getOffset();
                }
            }
			
            if (xref_location){ // We're basically done
                offsetEnd = offsetBegin +(current==null?0:current.length());
            } else { // We'll try to pinpoint a finer location
                if (current!=null && N.children[1].node.equals("singleton") && !N.children[1].isLeaf()) { // singleton var
                    String varName = N.children[1].children[0].toString();
                    if (current!=null && current.is(Token.VARIABLE,varName)) {
                        offsetBegin = current.getOffset();
                    } else {
                        Iterator<Token> tokens = getNextTokens((RSyntaxDocument) doc, current);
                        while (tokens.hasNext()){
                            Token T = tokens.next();
                            if (T.is(Token.VARIABLE,varName)){
                                offsetBegin = T.getOffset();
                                break;
                            }
                        }
                    }
                    offsetEnd = offsetBegin + varName.length();
                } else if (current!=null && N.children[1].node.equals("bad_module") && !N.children[1].isLeaf()) {
                    // should search "predicate" module_name/0
                    String module = N.children[1].children[0].toString();
                    Token T = firstTokenOfTermInClause(doc,current,module,0);
                    if (T==null)
                        offsetEnd = offsetBegin +(current==null?0:current.length());
                    else {
                        offsetBegin = T.getOffset();
                        offsetEnd = T.getEndOffset();
                    }
                } else if (current!=null && N.children[1].node.equals("subterm") && N.children[1].getChildCount()==2){
                    // subterm(BadTerm,Message); we need to try to find BadTerm
                    // see checkXSBPrologFile/4 in fidji.P
                    TermModel bad = N.children[1].children[0];
                    // Quick hack to deal with most LPS internal syntax scenarios
                    // TODO someday: a proper finder of terms...
                    String firstFunctor = bad.node.toString();
                    int firstArity = bad.getChildCount();
                    String secondFunctor = null;
                    int secondArity = -1;
                    if (firstArity>0){
                        secondFunctor = bad.children[0].node.toString();
                        secondArity = bad.children[0].getChildCount();
                    }
                    Token T = firstTokenOfTermInClause(doc,current,firstFunctor,firstArity);
					
                    if (T==null)
                        offsetEnd = offsetBegin +(current==null?0:current.length());
                    else {
                        offsetBegin = T.getOffset();
                        offsetEnd = T.getEndOffset();
                        if (secondFunctor!=null){
                            T = firstTokenOfTermInClause(doc,T,secondFunctor,secondArity);
                            if (T!=null)
                                offsetEnd = T.getEndOffset();
                        }
                    }
                    System.out.println("Should highlight "+bad);
                } else if (current!=null && !N.children[1].isLeaf()){
                    // undefined (and other remaining cases..) predicate in the first child: let's try to pinpoint it
                    // see checkXSBPrologFile/4 in fidji.P
                    String predicate = N.children[1].children[0].children[0].toString();
                    int arity = N.children[1].children[0].children[1].intValue();
                    Token currentHack = new TokenImpl(current);
                    Token T = firstTokenOfTermInClause(doc,current,predicate,arity);
                    if (T == null && arity!=0) {
                        // try to find just the atom, an approximation to dealing with predicate/arity textual occurrences,
                        // e.g. in import lists
                        current = currentHack;
                        T = firstTokenOfTermInClause(doc,current,predicate,0);
                    }
                    if (T==null)
                        offsetEnd = offsetBegin +(current==null?0:current.length());
                    else {
                        offsetBegin = T.getOffset();
                        offsetEnd = T.getEndOffset();
                    }
                }
                else 
                    offsetEnd = offsetBegin +(current==null?0:current.length());	
            }
        }
        if (offsetBegin>-1) return new DocumentRange(offsetBegin,offsetEnd);	
        else return new DocumentRange(0,1);
    }

    // In sync with termToRange
    public static String termToMessage(TermModel C) {
        if (C.node.equals("subterm") && C.getChildCount()==2)
            return C.children[1].toString();
        else return C.toString();
    }
    public void requestFocus(){
        super.requestFocus();
        if (editor!=null) editor.requestFocus();
    }
	
    public void setCursor(Cursor C){
        super.setCursor(C);
        if (editor!=null) editor.setCursor(C);
    }
	
    /** filename must be escaped (as in Prolog side), @see#prologFilename(); returns null if there isn't one */
    public static LogicProgramEditor getExistingEditor(String filename){
        return editors.get(new File(filename));
    }
    public static LogicProgramEditor getExistingEditor(File file){
        return editors.get(file);
    }
    public static LogicProgramEditor getExistingEditor(File file,FijiSubprocessEngineWindow listener){
        return getExistingEditor(prologFilename(file,listener) );
    }
	
    public static boolean someDirtyEditor(){
        for (LogicProgramEditor LPE : editors.values())
            if (LPE.editor.isDirty()) return true;
        return false;
    }
	
    public void propertyChange(PropertyChangeEvent evt){
        if (evt.getPropertyName().equals(TextEditorPane.FULL_PATH_PROPERTY)){
            if (isLogicFile()){
                String old = prologFilename(new File(evt.getOldValue().toString()),listener);
                LogicProgramEditor me = editors.remove(new File(old));
                if (me!=this)
                    throw new RuntimeException("Inconsistency handling file name change");
                editors.put(new File(getFileName()),this);
                getLogicParser().prepareTempFile();
                forceReparsing();
            }
            setTitle(new File(editor.getFileFullPath()).getName());
            TabsWindow.editors.updateTitle(this);
        } else if (evt.getPropertyName().equals(TextEditorPane.DIRTY_PROPERTY)){
            if (isDirty())
                someStuffNotYetLoaded = true;
            String oldtitle = getTitle();
            updateTitle();
            if (!(oldtitle.equals(getTitle())))
                TabsWindow.editors.updateTitle(this);
        } else if (evt.getPropertyName().equals(MyTextEditorPane.ERROR_LEVEL_PROPERTY)) {
            // the text was just parsed and the error level changed...
            updateEditorIcon((Level)evt.getNewValue());
            TabsWindow.editors.updateIcon(this);
        } else if (evt.getPropertyName().equals(WAS_LOADED_PROPERTY) || evt.getPropertyName().equals(WAS_INCLUDED_PROPERTY) ||
                   evt.getPropertyName().equals(WAS_ADDED_PROPERTY) || evt.getPropertyName().equals(MODULE_PROPERTY)){
            // the file was just loaded or added, preferredModule may have changed too
            String oldtitle = getTitle();
            updateTitle();
            if (!(oldtitle.equals(getTitle())))
                TabsWindow.editors.updateTitle(this);
        }
    }
	
    protected void updateEditorIcon(Level level){
        // the level has changed
        if (level.equals(Level.INFO))
            currentStatusIcon = null;
        else if (level.equals(Level.WARNING))
            currentStatusIcon = icons[1];
        else if (level.equals(Level.ERROR))
            currentStatusIcon = icons[0];
		
        if (iconMenu!=null)
            iconMenu.setIcon(currentStatusIcon);
        setIconImage((currentStatusIcon!=null?currentStatusIcon.getImage():null));
    }
	
    protected void updateTitle(){
        setTitle((isDirty()?"*":"")+editor.getFileName());
    }
	
    public boolean wasLoadedOnce(){
        return wasLoadedOnce_;
    }

    public boolean wasAddedOnce() {
        return wasAddedOnce_;
    }

    public boolean wasIncludedOnce(){
        return wasIncludedOnce_;
    }
	
    public File getIncluder(){
        return includer;
    }
	
    public boolean neverLoadedNorAddedNorIncluded(){
        return !(wasLoadedOnce()||wasAddedOnce()||wasIncludedOnce());
    }

    public boolean isFullyLoaded(){
        return (wasLoadedOnce()||wasAddedOnce()||wasIncludedOnce()) && !someStuffNotYetLoaded;
    }
			
    /** This file was consulted sometime through one or several of the 3 processes, into a module. 
     * If it happened just "now" (without any user edits since), now should be true */
    public void setWasLoadedAddedIncluded(boolean now,boolean loaded,boolean added,boolean included,File includer,String module){
        if (now && (loaded||added||included))
            someStuffNotYetLoaded = false; 
        boolean oldLoaded = wasLoadedOnce_;
        this.wasLoadedOnce_ = loaded;
        boolean oldAdded = wasAddedOnce_;
        this.wasAddedOnce_ = added;
        boolean oldIncluded = wasIncludedOnce_;
        this.wasIncludedOnce_ = included;
        this.includer = includer;
        maySetPreferredModule(module);
		
        boolean unloaded = !loaded && !added && !included;
        if (added||unloaded)
            firePropertyChange(WAS_ADDED_PROPERTY, oldAdded, added);
        if (loaded||unloaded)
            firePropertyChange(WAS_LOADED_PROPERTY, oldLoaded, loaded);
        if (included||unloaded)
            firePropertyChange(WAS_INCLUDED_PROPERTY, oldIncluded, included);
    }

    /** Returns the first LogicParser found, or null if none */
    protected LogicParser getLogicParser(){
        for (int p=0; p<editor.getParserCount(); p++)
            if (editor.getParser(p) instanceof LogicParser)
                return (LogicParser)editor.getParser(p);
        return null;
    } 
	
    protected boolean provideTooltips(){
        String T = listener.preferences.getProperty("com.declarativa.fiji.notooltips");
        boolean R = ! (T != null && T.toLowerCase().equals("true"));
        if (!R) System.err.println("No tooltips will be shown for "+getFileName());
        return R;
    }
    protected ToolTipSupplier createTooltipSupplier(final AbstractPrologEngine engine) {
        return new ToolTipSupplier(){
            public String getToolTipText(RTextArea textArea,MouseEvent e){
                Token current =  ((RSyntaxTextArea)textArea).viewToToken(e.getPoint());
                // if (current!=null) System.out.println( current.toString());
                if (!e.isControlDown() && current!=null && isLogicFile() &&
                    ( current.getType()==Token.IDENTIFIER || current.getType()==Token.LITERAL_CHAR)) {
                    Token last = lastSubtermToken(current,false);
                    String T = "?";
                    String tip = "";
                    try{
                        int L = last.getEndOffset()-current.getOffset();
                        if (L<0) L=1; // TODO: replace this hack by tuning of lastSubtermToken()
                        T = editor.getText(current.getOffset(),L).trim();
                        if (!engine.isAvailable()) {
                            System.err.println("Auxiliar engine unavailable in gettoolTipText");
                            return "Something is wrong with the editor; you should save and restart Studio";
                        }
                        Object[] bindings = getDefinitionFor(T);
                        System.err.println(Arrays.toString(bindings));
                        System.err.println("T:"+T);
                        if (bindings==null) return "Error: can not understand this term";
                        String filename = bindings[2].toString();
                        if (filename.equals("system")||filename.equals("???")) // see fjFindDefinition/5 in fidji.P
                            return filename;
                        LogicProgramEditor editor = getExistingEditor(filename);
                        RSyntaxDocument doc;
                        if (editor!=null) doc = (RSyntaxDocument)editor.editor.getDocument();
                        else { // TODO: keep cache of invisible RSyntaxDocuments
                            doc = new RSyntaxDocument(null,"text/plain");
                            doc.setSyntaxStyle(new PrologTokenMaker());
                            File F = new File(filename);
                            long size = F.length();
                            if (size>Runtime.getRuntime().freeMemory()/2)
                                throw new IPException("File too big");
                            FileInputStream fis = new FileInputStream(F); // should use FileReader???
                            byte[] buffer = new byte[(int)size];
                            if (size!=fis.read(buffer)){ 
                                fis.close();
                                throw new IPException("Unable to read all file bytes");
                            }
                            fis.close();
                            doc.insertString(0, new String(buffer), null);
                        }
                        TermModel PT = (TermModel)bindings[3];
                        TermModel PT_charpos = PT.children[0]; int termIndex = PT.children[1].intValue();
                        String functor = bindings[0].toString();
                        String template = bindings[4].toString();
                        Token first;
                        if (PT_charpos.toString().equals("end_of_file"))
                            first = firstInterestingToken(0, doc, termIndex-1);
                        else
                            first = firstInterestingToken(PT_charpos.intValue(),doc);
                        // System.out.println("first token:"+first);
                        String comment = collectCommentsBefore(doc, first);
                        // if the result does not contain "predicate(" add the meta template to it
                        if (comment.contains(functor+"("))
                            tip = comment;
                        else
                            tip = comment+"\n"+template;
                    } catch (NullPointerException ex){
                        System.err.println("Tooltip failed for " + T);
                    } catch (IPPrologError ex){
                        System.err.println("Tooltip error for " + T);
                        System.err.println("current=="+current+" last=="+last);
                        System.err.println(ex);
                    } catch (Exception ex){
                        System.err.println("current token:"+current);
                        System.err.println("last token:"+last);
                        throw new RuntimeException("Bad exception" + ex);
                    } 
                    return tip;
                } 		
				
                else return null;
            }
        };
    }	
    static void rememberPreferences(final FijiPreferences P){
        for (LogicProgramEditor LPE : editors.values())
            LPE.rememberPreference(P);
    }
	
    static String preferenceName(String filename){
        return PREF_PREFIX+filename;
    }
    static String preferenceName2Filename(String prefName){
        return prefName.substring(PREF_PREFIX.length());
    }
    static boolean hasPreferencePrefix(String preference){
        return preference.startsWith(PREF_PREFIX);
    }
    String preferenceName(){
        return LogicProgramEditor.preferenceName(getFileName());
    }
	
    void rememberPreference(FijiPreferences P){
        P.setProperty(preferenceName(getFileName()), window2pref()); 
    }
    String window2pref(){
        String P = FijiPreferences.window2pref(this,TabsWindow.inSomeTab(this));
        return P+FijiPreferences.PREF_SEPARATOR+editor.getSelectionStart()+FijiPreferences.PREF_SEPARATOR+editor.getSelectionEnd();
    }
    /** If it can't find a preferred window size and position for a present screen, returns null */
    static protected Rectangle getPreferredBounds(File F,FijiSubprocessEngineWindow listener){
        String filename = listener.engine.unescapedFilePath(F.getAbsolutePath()); // Prolog friendly
        String preference = listener.preferences.getProperty(preferenceName(filename));
        return FijiPreferences.pref2Rectangle(preference);
    }
    /** Returns a selection range, encapsulated in a Point for the hacker's convenience; x is selection start, y is selection end */
    static protected Point getPreferredSelection(File F,FijiSubprocessEngineWindow listener){
        String filename = listener.engine.unescapedFilePath(F.getAbsolutePath()); // Prolog friendly
        String preference = listener.preferences.getProperty(preferenceName(filename));
        return FijiPreferences.pref2PointAfterRectangle(preference);
    }
	
    static protected boolean getPreferredInTab(File F,FijiSubprocessEngineWindow listener){
        String filename = listener.engine.unescapedFilePath(F.getAbsolutePath()); // Prolog friendly
        String preference = listener.preferences.getProperty(preferenceName(filename));
        return FijiPreferences.pref2InTab(preference);
    }
	
    public String getFileName(){
        // listener.engine.unescapedFilePath(editor.getFileFullPath());
        return prologFilename(new File(editor.getFileFullPath()),listener); 
    }
	
    public File getFile(){
        return new File(editor.getFileFullPath());
    }
	
    /** Prolog friendly */
    public static String prologFilename(File F,FijiSubprocessEngineWindow listener){
        if (listener!=null&&listener.engine!=null) // hack, there's a bug to be fixed here
            return listener.engine.unescapedFilePath(F.getAbsolutePath());
        else return F.getAbsolutePath();
    }
	
    /** Returns "" if F's filename has no extension */
    public static String dotExtension(String S){
        int i = S.lastIndexOf(".");
        if (i==-1) return "";
        else return S.substring(i);
    }
	
    public static String dotExtension(File F){
        return dotExtension(F.getName());
    }
	
    /** Adds Copy/Paste/Etc popup menus and their key commands  */
    static class MyFindReplaceDialog extends ReplaceDialog{
        public MyFindReplaceDialog(Frame owner, SearchListener listener) {
            super(owner, listener);
            setTitle("Find/Replace"); // to avoid editing  /org/fife/rsta/ui/search/Search.properties
            ListenerWindow.popupEditMenuFor((JTextComponent)findTextCombo.getEditor().getEditorComponent());
            ListenerWindow.popupEditMenuFor((JTextComponent)replaceWithCombo.getEditor().getEditorComponent());
        }
    }

    // based on RSTA example:
    public void initSearchDialogs() {
        findReplaceDialog = new MyFindReplaceDialog(this, this);
        findReplaceDialog.getSearchContext().setMarkAll(false);		
    }
	
    // SearchListener methods:

    /**
     * Listens for events from our search dialogs and actually does the dirty
     * work.
     */
    @Override
    public void searchEvent(SearchEvent e) {

        SearchEvent.Type type = e.getType();
        SearchContext context = e.getSearchContext();
        SearchResult result = null;

        switch (type) {
        default: // Prevent FindBugs warning later
        case MARK_ALL:
            result = SearchEngine.markAll(editor, context);
            break;
        case FIND:
            backAction.rememberPosition(this);
            result = SearchEngine.find(editor, context);
            if (!result.wasFound()) {
                UIManager.getLookAndFeel().provideErrorFeedback(editor);
                try {
                    // wrap down-search
                    editor.mySetCaretPosition(editor.getLineStartOffset(0));
                    backAction.didJump(LogicProgramEditor.this);
                    if (!SearchEngine.find(editor, findReplaceDialog.getSearchContext()).wasFound()) {
                        // wrap up-search
                        editor.mySetCaretPosition(editor.getText().length());
                        backAction.didJump(LogicProgramEditor.this);
                        SearchEngine.find(editor, findReplaceDialog.getSearchContext());
                    }
                } catch (BadLocationException ble) { // Never happens
                    UIManager.getLookAndFeel().provideErrorFeedback(editor);
                    ble.printStackTrace();
                }
            } else backAction.didJump(this);
            break;
        case REPLACE:
            result = SearchEngine.replace(editor, context);
            if (!result.wasFound()) {
                UIManager.getLookAndFeel().provideErrorFeedback(editor);
            }
            break;
        case REPLACE_ALL:
            result = SearchEngine.replaceAll(editor, context);
            JOptionPane.showMessageDialog(null, result.getCount() +
                                          " occurrences replaced.");
            break;
        }
        /*
          String text = null;
          if (result.wasFound()) {
          text = "Text found; occurrences marked: " + result.getMarkedCount();
          }
          else if (type==SearchEvent.Type.MARK_ALL) {
          if (result.getMarkedCount()>0) {
          text = "Occurrences marked: " + result.getMarkedCount();
          }
          else {
          text = "";
          }
          }
          else {
          text = "Text not found";
          }
        */

    }
	
    @Override
    public String getSelectedText() {
        return editor.getSelectedText();
    }
	
    public MyTextEditorPane getEditor() {
        return editor;
    }

    private class GoToLineAction extends AbstractAction {

        public GoToLineAction() {
            super("Go To Line...");
            int c = getToolkit().getMenuShortcutKeyMask();
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, c));
        }

        public void actionPerformed(ActionEvent e) {
            if (findReplaceDialog.isVisible()) {
                findReplaceDialog.setVisible(false);
            }
            GoToDialog dialog = new GoToDialog(TabsWindow.editors.currentWindow(LogicProgramEditor.this));
            dialog.setMaxLineNumberAllowed(editor.getLineCount());
            dialog.setVisible(true);
            int line = dialog.getLineNumber();
            if (line>0) {
                try {
                    backAction.rememberPosition(LogicProgramEditor.this);
                    editor.mySetCaretPosition(editor.getLineStartOffset(line-1));
                    backAction.didJump(LogicProgramEditor.this);
                } catch (BadLocationException ble) { // Never happens
                    UIManager.getLookAndFeel().provideErrorFeedback(editor);
                    ble.printStackTrace();
                }
            }
        }

    }

    private class UseSelectionForFindAction extends AbstractAction {
		
        public UseSelectionForFindAction() {
            super("Use Selection for Find");
            int c = getToolkit().getMenuShortcutKeyMask();
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, c));
        }

        public void actionPerformed(ActionEvent e) {
            String S = editor.getSelectedText();
            if (S==null) return;
            for (Iterator<LogicProgramEditor> i = editors.values().iterator(); i.hasNext();){
                LogicProgramEditor lpe = i.next();
                lpe.setSearchString(S);
                showFindReplaceDialog();
            }
        }

    }
	
    public void setSearchString(String S){
        findReplaceDialog.setSearchString(S);
    }

    // MK: useless action, NOT added to the Edit menu
    private class FindNextAction extends AbstractAction {
		
        public FindNextAction() {
            super("Find Next");
            //int c = getToolkit().getMenuShortcutKeyMask();
            //putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_X,c));
            // use Alt-F for find next
            putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_F,
                                                            ActionEvent.ALT_MASK));
        }
            
        public void actionPerformed(ActionEvent e) {
            findReplaceDialog.getSearchContext().setSearchFor(findReplaceDialog.getSearchString());
            backAction.rememberPosition(LogicProgramEditor.this);
            if (!SearchEngine.find(editor, findReplaceDialog.getSearchContext()).wasFound()) {
                UIManager.getLookAndFeel().provideErrorFeedback(editor);
                try {
                    // wrap down-search
                    editor.mySetCaretPosition(editor.getLineStartOffset(0));
                    backAction.didJump(LogicProgramEditor.this);
                    if (!SearchEngine.find(editor, findReplaceDialog.getSearchContext()).wasFound()) {
                        // wrap up-search
                        editor.mySetCaretPosition(editor.getText().length());
                        backAction.didJump(LogicProgramEditor.this);
                        SearchEngine.find(editor, findReplaceDialog.getSearchContext());
                    }
                } catch (BadLocationException ble) { // Never happens
                    UIManager.getLookAndFeel().provideErrorFeedback(editor);
                    ble.printStackTrace();
                }
            } else backAction.didJump(LogicProgramEditor.this);
        }
            
    }


    private class ShowFindReplaceDialogAction extends AbstractAction {
		
        public ShowFindReplaceDialogAction() {
            super("Find/Replace...");
            int c = getToolkit().getMenuShortcutKeyMask();
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, c));
        }

        public void actionPerformed(ActionEvent e) {
            showFindReplaceDialog();
        }

    }
	
    public void showFindReplaceDialog(){
        Dimension D = findReplaceDialog.getSize();
        findReplaceDialog.setSize(new Dimension(460,D.height));
        findReplaceDialog.setVisible(true);
    }

    public void showWideFindReplaceDialog(){
        Dimension D = findReplaceDialog.getSize();
        findReplaceDialog.setSize(new Dimension(600,D.height));
        findReplaceDialog.setVisible(true);
    }
	
    static class EditorSelection{
        int position;
        String filename;
        FijiSubprocessEngineWindow listener;
        private EditorSelection(String filename,int position,FijiSubprocessEngineWindow listener) {
            this.position = position;
            this.filename = filename;
            this.listener = listener;
        }
    }
	
    EditorSelection getEditorSelectionFor(int position){
        if (position<0) position = 0;
        return new EditorSelection(getFileFullPath(),position,listener);
    }
	
    EditorSelection getEditorSelection(){
        return getEditorSelectionFor(editor.getSelectionStart());
    }
	
    protected static class BackAction extends AbstractAction{
        Stack<EditorSelection> breadcrumb;
        EditorSelection last = null;
        private BackAction(){
            super("Back");
            breadcrumb = new Stack<EditorSelection>();
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            putValue(SHORT_DESCRIPTION,"Go back to the last place where you jumped to");
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (breadcrumb.size()>1) {
                breadcrumb.pop();
                EditorSelection previous = breadcrumb.peek();
                try{
                    showEditor(previous.filename, null, null, previous.position, null, -1, previous.listener, true, true);
                } catch(Exception ex){
                    System.err.println("Could not go back to file:\n"+ex);
                }
                if (breadcrumb.size()<2)
                    setEnabled(false);
            }
			
        }
        public void rememberPosition(LogicProgramEditor editor){
            last=editor.getEditorSelection();
        }
        /** To be called after the jump */
        public void didJump(LogicProgramEditor editor){
            if (last!=null && (breadcrumb.size()==0 || !breadcrumb.peek().equals(last)))
                breadcrumb.push(last);
            breadcrumb.push(editor.getEditorSelection());
            last = null;
            setEnabled(true);
        }
		
    }
	
    protected RSyntaxTextAreaEditorKit.InsertBreakAction myLineBreakAction = new RSyntaxTextAreaEditorKit.InsertBreakAction();
	
    protected class ExtractPredicateAction extends AbstractAction{
        private ExtractPredicateAction(){
            super("Extract Predicate");
            putValue(SHORT_DESCRIPTION,"Move the selected clause subgoals to a new predicate and call it");
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            int start = editor.getSelectionStart();
            int end = editor.getSelectionEnd();
            if (end<=start+3)
                JOptionPane.showMessageDialog(LogicProgramEditor.this,"First you need to select some subgoals in a clause",getValue(NAME)+"Error",JOptionPane.ERROR_MESSAGE);
            else {
                RSyntaxDocument doc = (RSyntaxDocument)editor.getDocument();
                Token startToken = editor.modelToToken(start);
                if (startToken==null) 
                    startToken = editor.modelToToken(start+1);
                startToken = new TokenImpl(startToken);
                Token clauseStartT = firstTokenOfClause(doc,startToken,false);
                int clauseStart = clauseStartT.getOffset();
                Token clauseEndT = lastTokenOfClause(doc, clauseStartT);
                int clauseEnd = clauseEndT.getEndOffset();
                try {
                    String toBeExtracted = editor.getText(start,end-start).trim();
                    String head = editor.getText(clauseStart,start-clauseStart);
                    // make sure the cut part has no leading nor trailing , ; similar treatment might be done for ';':
                    boolean headNeedsComma = false;
                    if (toBeExtracted.startsWith(",")){
                        headNeedsComma=true;
                        toBeExtracted = toBeExtracted.substring(1);
                    }
                    String tail = editor.getText(end,clauseEnd-end).trim();
                    if (tail.endsWith("."))
                        tail = tail.substring(0, tail.length()-1);
                    boolean tailNeedsComma = false;
                    if (toBeExtracted.endsWith(",")){
                        tailNeedsComma = true;
                        toBeExtracted = toBeExtracted.substring(0, toBeExtracted.length()-1);
                    }
                    String dummyClause = head + (headNeedsComma?",":"")+"'_$_$_unique_dummy'" + (tailNeedsComma?",":"")+ tail;
                    Object[] bindings = engine.deterministicGoal("fjExtractPredicateSignature(DC,Cut,Functor,Signature)", 
                                                                 "[string(DC),string(Cut)]", new Object[]{dummyClause,toBeExtracted}, "[string(Functor),string(Signature)]");
                    if (bindings==null){
                        JOptionPane.showMessageDialog(LogicProgramEditor.this,"You can only extract a well formed subgoal expression",getValue(NAME)+"Error",JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String newhead = bindings[0].toString() + bindings[1].toString();
					
                    Position clauseEndPosition = doc.createPosition(clauseEnd);
                    doc.insertString(clauseEnd, "\n\n"+newhead + " :-", null);
                    editor.setCaretPosition(clauseEndPosition.getOffset());
                    Position newBodyPosition = doc.createPosition(editor.getCaretPosition());
                    myLineBreakAction.actionPerformed(null);
                    doc.insertString(newBodyPosition.getOffset(), toBeExtracted+".\n\n", null);
					
                    doc.replace(start, end-start, (headNeedsComma?",":"")+newhead+(tailNeedsComma?",":""), null);
					
                } catch (BadLocationException e1) {
                    System.err.println(getValue(NAME)+"Error:\n"+e1);
                    JOptionPane.showMessageDialog(LogicProgramEditor.this,"Could not refactor, please see log for details.",getValue(NAME)+"Error",JOptionPane.ERROR_MESSAGE);
                }
            }
        }
		
    }
	
    private class QuerySelectionAction extends AbstractAction {
        public QuerySelectionAction() {
            super("Use Current Selection as Query");
            int c = getToolkit().getMenuShortcutKeyMask();
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_U, c));
            putValue(SHORT_DESCRIPTION,"Copy the selected text to the query panel and execute it");
        }
        public void actionPerformed(ActionEvent e){
            String S = editor.getSelectedText();
            if (S==null) return;
            S = S.trim();
            if (S.startsWith("?-"))
                S = S.substring(2).trim();
            if (S.endsWith(".")) S = S.substring(0,S.length()-1);
            // if (!S.endsWith(".")) S = S+".";
            setWaitCursor();
            doQueryString(S);
            restoreCursor();
        }
    }
	
    protected void doQueryString(String stringWithoutDot){
        if (isPrologFile()) listener.showPrologQuery(stringWithoutDot+".",true);
        else if (isQUALMfile())	{
            Toolkit.getDefaultToolkit().beep();
            System.err.println("Can not YET execute QUALM query "+stringWithoutDot+" in file "+getFileName());
        } else {
            Toolkit.getDefaultToolkit().beep();
            System.err.println("Can not execute query "+stringWithoutDot+" in file "+getFileName());
        }
    }

    private class BrowseSelectionAction extends AbstractAction {
        public BrowseSelectionAction() {
            super("Inspect Current Selection");
            putValue(SHORT_DESCRIPTION,"See the selected term, or the subterm starting at the cursor, as a tree; make sure it is syntactically valid");
        }
        public void actionPerformed(ActionEvent e){
            String S = editor.getSelectedText();
            if (S==null) S = subTermAtCursor();
            if (S==null) return;
            S = S.trim();
            if (S.endsWith(".")) S = S.substring(0,S.length()-1);
            TermModel TM = parseTextToTerm(S);
            if (TM==null) 
                JOptionPane.showMessageDialog(null,"Inspection not supported for this type of selection:\n"+S,"Inspect Current Selection Error",JOptionPane.ERROR_MESSAGE);	
            else new TermModelWindow(TM);
        }
    }

    // See FindMatchingClauseAction, redundant
    protected void configurePopupMenu(JPopupMenu popupMenu, JMenu calls, JMenu calledBy){
        String term = subTermAtCursor();
        //System.out.println("subTermAtCursor() == "+term);
        calls.removeAll(); 
        calledBy.removeAll(); 
        if (!engine.isAvailable() || term.length()==0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        Object[] bindings = engine.deterministicGoal(
                                                     "fjImmediateCalls(Filename,Term,Calls,CalledBy), buildTermModelArray(Calls,CallsTM), buildTermModelArray(CalledBy,CalledByTM)",
                                                     "[string(Term),string(Filename)]",new Object[]{term,getFileFullPath()},
                                                     "[CallsTM,CalledByTM]");
        if (bindings!=null){
            TermModel[] callsTerms = (TermModel[])bindings[0];
            addNavigatorsToMenu(calls,callsTerms,listener,LogicProgramEditor.this);
            TermModel[] calledByTerms = (TermModel[])bindings[1];
            addNavigatorsToMenu(calledBy,calledByTerms,listener,LogicProgramEditor.this);
        }
    }

    private String getFileFullPath() {
        return editor.getFileFullPath();
    }

    public class MyTextEditorPane extends TextEditorPane{
        /** Popup menu items which will be rebuilt on use */
        JMenu calls, calledBy;
        public static final String ERROR_LEVEL_PROPERTY = "ERROR_LEVEL";
        public MyTextEditorPane(int textMode, boolean wordWrapEnabled,FileLocation loc,String encoding) throws IOException{
            super(textMode,wordWrapEnabled,null,encoding);
            long initialSize = -1;
            initialSize = new File(loc.getFileFullPath()).length();
            setDocument(new RSyntaxDocument(null,SYNTAX_STYLE_NONE, (int)initialSize));
            load(loc, encoding);
            calls = new JMenu("Calls..."); calls.setToolTipText("Show predicates called by this");
            calledBy = new JMenu("Called by..."); calls.setToolTipText("Show predicates that call this");
        }
        public void firePropertyChange(String property,Object oldValue,Object newValue){
            super.firePropertyChange(property, oldValue, newValue);
        }
		
        /** Later to optimize scrolling to put the caret farther away from the viewport borders */
        public void mySetCaretPosition(int position){
            //JViewport V = sp.getViewport() ;
            setCaretPosition(position);
            //Point P = V.getViewPosition();
            //P.y = P.y + 2*listener.preferredFontSize;
            //V.setViewPosition(P);
        }
        public boolean getShouldIndentNextLine(int line) {
            if (isAutoIndentEnabled()) {
                RSyntaxDocument doc = (RSyntaxDocument)getDocument();
                Token t = doc.getTokenListForLine(line);
                Token last = t.getLastNonCommentNonWhitespaceToken(); 
                //System.out.println("First:"+t);
                //System.out.println("Last non comment:"+last);
                if (last==null || last.getType()==FULL_STOP || last.getType() == Token.PREPROCESSOR) return false;
                if (last.is(Token.SEPARATOR,"(") || last.is(Token.SEPARATOR,"[") || last.is(Token.SEPARATOR,"{") ) {
                    return true;
                }
                if (t.getType()!=Token.WHITESPACE) return true;
            }
            return false;
        }
        protected RTAMouseListener createMouseListener() {
            return new MyRTextAreaMutableCaretEvent(this);
        }
		
        protected class MyRTextAreaMutableCaretEvent extends RTextAreaMutableCaretEvent{
            protected MyRTextAreaMutableCaretEvent(RTextArea textArea) {
                super(textArea);
            }
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()){
                    int position = viewToModel(e.getPoint());
                    if (!(position>=getSelectionStart() && position<=getSelectionEnd()))
                        setCaretPosition(position);
                    myshowPopup(e);
                }
            }
            public void mouseReleased(MouseEvent e) {
                int position = viewToModel(e.getPoint());
                if (!(position>=getSelectionStart() && position<=getSelectionEnd()))
                    setCaretPosition(position);
                if ((e.getModifiers()&MouseEvent.BUTTON1_MASK)!=0 && ((e.getModifiers()&InputEvent.CTRL_MASK)!=0 || e.isMetaDown())){
                    if (goToDefinitionAction!=null) 
                        goToDefinitionAction.doIt();
                } else if (e.isPopupTrigger())// Windows oblige
                    myshowPopup(e);
            }
            private void myshowPopup(MouseEvent e) {
                JPopupMenu popupMenu = getPopupMenu();
                if (popupMenu!=null) {
                    LogicProgramEditor.this.configurePopupMenu(popupMenu,calls,calledBy);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }

		
    }
	
    static void addNavigatorsToMenu(JMenu menu,TermModel[] items, FijiSubprocessEngineWindow listener, final LogicProgramEditor lpe){
        addNavigatorsToMenu(menu,items, listener, lpe,true);
        menu.addSeparator();
        addNavigatorsToMenu(menu,items, listener, lpe,false);
    }
    /** each item is a term TheFunctor(TheArity,File,PositionTerm) */
    static void addNavigatorsToMenu(JMenu menu,TermModel[] items, final FijiSubprocessEngineWindow listener, final LogicProgramEditor lpe, boolean inOpenFiles){
        for (int t=0; t<items.length; t++){
            final String functor = items[t].node.toString();
            final int arity = items[t].children[0].intValue();
            String file1 = items[t].children[1].toString();
            boolean alreadyOpen = getExistingEditor(file1)!=null;
            if ((!inOpenFiles && alreadyOpen) || (inOpenFiles && !alreadyOpen))
                continue;
            final String file = (file1.length()==0?null:file1);
            final TermModel position = items[t].children[2];
            JMenuItem MI = new JMenuItem(functor+"/"+arity);
            menu.add(MI);
            MI.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent ae){
                        TermModel lazyPosition = position;
                        String lazyFile = file;
                        if (lazyFile==null && lpe.engine.isAvailable()){
                            String G = "fjMayLoadImportedXref('"+lpe.getFileName()+"',GP,"+arity+",ModuleFile,Position), buildTermModel(Position,PositionTM)";
                            // System.out.println("Calling "+G);
                            Object[] bindings = lpe.engine.deterministicGoal(
                                                                             G, 
                                                                             "[string(GP)]", // to tolerate weird functors
                                                                             new Object[]{functor},
                                                                             "[string(ModuleFile),PositionTM]"); 
                            if (bindings==null) System.err.println("Could not load xref for "+functor+"/"+arity);
                            else {
                                lazyFile = (String)bindings[0];
                                lazyPosition = (TermModel)bindings[1];
                            }
                        }					
                        lpe.displayPredicate(functor, arity, lazyFile, lazyPosition);
                    }
                });
        }
    }
	
    protected void displayPredicate(String functor, int arity, String file, TermModel position){
        if (file!=null) {
            if (file.equals("system")) 
                JOptionPane.showMessageDialog(TabsWindow.editors.currentWindow(this), "That is a system predicate!");
            else if (file.equals("???"))
                JOptionPane.showMessageDialog(TabsWindow.editors.currentWindow(this), "Unknown predicate!");
            else {
                showEditor(file, null, position, -1, functor, arity, listener, false, false);
            }
        }
    }
	
    /** Adapted from http://code.google.com/p/juniversalchardet/ , Mozilla public license*/
    private synchronized String guessEncodingUniversal(File F){
        try{
            byte[] buf = new byte[4096];
            FileInputStream fis = new java.io.FileInputStream(F);
            UniversalDetector detector = new UniversalDetector(null);
            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();
            String encoding = detector.getDetectedCharset();
            detector.reset();
            fis.close();
            if (encoding != null) {
                return encoding;
            } 
        } catch (IOException e){
            throw new RuntimeException(e+"\nCould not detect encoding for "+F);
        }	
        return System.getProperty("file.encoding");
    }	
	
    protected PropertyChangeListener parserNoticesUpdater = new PropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent evt){
                Gutter g = sp.getGutter();
                // assumes noone else puts icons:
                g.removeAllTrackingIcons();
                java.util.List<ParserNotice> notices = editor.getParserNotices();
                for (Iterator<ParserNotice> i=notices.iterator(); i.hasNext(); ) {
                    ParserNotice notice = i.next();
                    //int line = notice.getLine()-1;
                    Icon icon = icons[notice.getLevel().getNumericValue()];
                    try {
                        // g.addLineTrackingIcon(line, icon, notice.getMessage());
                        g.addOffsetTrackingIcon(notice.getOffset(), icon, notice.getMessage());
                    } catch (BadLocationException ble) { // Never happens
                        System.err.println("*** Error adding notice:\n" +notice + ":");
                        ble.printStackTrace();
                    }
                }

            }
	};
    protected void checkIfParseable(LogicParser P){
        if (editor.getDocument().getLength()<=maxParseableSize()) {
            editor.addParser(P);
            editor.addPropertyChangeListener(RSyntaxTextArea.PARSER_NOTICES_PROPERTY ,parserNoticesUpdater);
        } else {
            System.err.println("Parser not activated, file longer than "+maxParseableSize()+" chars");
            JOptionPane.showMessageDialog(TabsWindow.editors.currentWindow(this),"Your file has "+editor.getDocument().getLength()+" chars, therefore in order to keep editing fast\n"+
                                          "the editor will not show warning and error messages\n"+
                                          "Consider increasing the max. characters size in Preferences",
                                          "Warning",JOptionPane.WARNING_MESSAGE);
        }
    }
	
    protected String parseablePreferenceName(){
        return MAX_PARSEABLE_SIZE_PREF;
    }
	
    protected void setDefaultBounds() {
        Rectangle R = getBounds();
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc!=null) screenHeight = gc.getBounds().height;
        int windowHeight = getSize().height;
        if (windowHeight > 0.85*screenHeight) 
            windowHeight = (int)(0.85*screenHeight);
        else if (windowHeight<FijiSubprocessEngineWindow.NEW_WINDOW_SIZE.height) 
            windowHeight = FijiSubprocessEngineWindow.NEW_WINDOW_SIZE.height;
        int x = (R.x>=0 ? R.x : 0);
        int y = (R.y>=0 ? R.y : 0);
        //editor.setPreferredSize(new Dimension(FijiSubprocessEngineWindow.NEW_WINDOW_SIZE.width,windowHeight));
        setBounds(x,y,FijiSubprocessEngineWindow.NEW_WINDOW_SIZE.width,windowHeight);
    }
	
    /** Open a TextEditorPane-based window loading F, and possibly using engine as a parsing aid */
    public LogicProgramEditor(final File F, final SubprocessEngine engine, final FijiSubprocessEngineWindow l, Rectangle preferredBounds, Point preferredSelection) 
        throws java.io.IOException{
        this.listener = l; this.engine=engine;
		
        JPanel cp = new JPanel(new BorderLayout());
        editor = new MyTextEditorPane(TextEditorPane.INSERT_MODE,false,FileLocation.create(F),guessEncodingUniversal(F));
        System.out.println("File encoding:"+editor.getEncoding());
        sp = new RTextScrollPane(editor);

        theme.apply(editor);
        editor.setFont(editor.getFont().deriveFont((float)l.preferredFontSize));
        // final String filename = getFileName();
        // editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);

        if (isProlog(F.getName()) || isQUALM(F.getName()) || isLPS_W(F.getName())|| isLPS_(F.getName())|| isLPS_P(F.getName())) { 
            if (isQUALM(F.getName())) QUALMfile = true;
            else if (isLPS_W(F.getName())) lps_wFile = true;
            else if (isLPS_P(F.getName())) lps_pFile = true;
            else if (isLPS_(F.getName())) lps_File = true;
            else prologFile = true;
            //TODO: use QUALMTokenMaker
            ((RSyntaxDocument)editor.getDocument()).setSyntaxStyle(new PrologTokenMaker());
			
            editor.setMarkOccurrences(true);
            sp.setIconRowHeaderEnabled(true);
            checkIfParseable(new LogicParser(engine,this));
        } else // use Java, XML or whatever:
            ((RSyntaxDocument)editor.getDocument()).setSyntaxStyle(syntaxMapper.getSyntaxStyleForFile(F.getName(),true)); 
        if (isLogicFile()) {
            AutoCompletion ac = new AutoCompletion(buildCompletionProvider(engine));
            ac.setAutoCompleteEnabled(true);
            ac.setAutoCompleteSingleChoices(true);
            ac.install(editor);
            // editors.put(new File(filename),this);
            editors.put(F,this); // the above uses the canonical File, having de-ref'd symlinks; but why was I using it??
            editor.setAutoIndentEnabled(true);
        } 
        if (provideTooltips()) 
            editor.setToolTipSupplier(createTooltipSupplier(engine));
        setWasLoadedAddedIncluded(true,false,false,false,null,preferredModule);
        // So we hack the editors collection when our file path changes
        editor.addPropertyChangeListener(TextEditorPane.FULL_PATH_PROPERTY,this);
        editor.addPropertyChangeListener(TextEditorPane.DIRTY_PROPERTY,this);
        editor.addPropertyChangeListener(MyTextEditorPane.ERROR_LEVEL_PROPERTY,this);
        addPropertyChangeListener(WAS_LOADED_PROPERTY,this);
        addPropertyChangeListener(WAS_ADDED_PROPERTY,this);
        addPropertyChangeListener(WAS_INCLUDED_PROPERTY,this);
        addPropertyChangeListener(MODULE_PROPERTY,this);
		
        initSearchDialogs();
        // editor.setCodeFoldingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        // sp.setFoldIndicatorEnabled(true);
		
        cp.add(sp);
        ErrorStrip es = new ErrorStrip(editor);
        cp.add(es, BorderLayout.LINE_END);
        setContentPane(cp);
					
        setupMenus(F, engine, l);
        showWindowAction = new ShowWindowAction();

        setTitle(F.getName());
        addWindowListener(new WindowAdapter(){
                public void windowClosing(WindowEvent e){
                    doClose();
                }
            });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        pack();
		
        setLocationRelativeTo(null);
        if (preferredBounds!=null)
            setBounds(preferredBounds);
        else {
            setDefaultBounds();
        }
        if (preferredSelection!=null)
            editor.select(preferredSelection.x,preferredSelection.y);
			
        addComponentListener(new ComponentAdapter(){
                public void componentMoved(ComponentEvent e){
                    rememberPreference(listener.preferences);
                }
                public void componentResized(ComponentEvent e){
                    rememberPreference(listener.preferences);
                }
            });
        editor.addCaretListener(new CaretListener(){
                // might this be too heavy? probably not!
                public void caretUpdate(CaretEvent e){
                    rememberPreference(listener.preferences);
                }
            });
				
        // System.out.println("making visible...");
        //createBufferStrategy(1);
        //System.out.println(getBufferStrategy());
        //System.out.println(getGraphicsConfiguration());
        // System.out.println("shown window...");
        // setContentPane(cp);  Placing this here seems to speed up... but does NOT avoid crashing on Mac with large files (e.g larger than 8 Mb)
        // System.out.println("Added content pane ...");
        editor.requestFocus();
        addEditorLifecycleListener(listener.windowsMenuManager);
        fireDidCreate(); // So this window will get into all Windows menus
    }

    protected LogicCompletionProvider buildCompletionProvider(final SubprocessEngine engine) {
        return new LogicCompletionProvider(engine);
    }
	
    private void setupMenus(final File F, final SubprocessEngine engine,
                            final FijiSubprocessEngineWindow l) {
        JMenuBar mb = new JMenuBar();
				
        if (AbstractPrologEngine.isMacOS()){
            iconMenu = new JMenu();
            mb.add(iconMenu);
            iconMenu.addMenuListener(new MenuListener(){
                    @Override
                    public void menuSelected(MenuEvent e) {
                        //System.out.println("CUCU");
                    }
                    @Override
                    public void menuDeselected(MenuEvent e) {}
                    @Override
                    public void menuCanceled(MenuEvent e) {}
				
                });
        } else iconMenu = null;

        fileMenu = new JMenu("File"); mb.add(fileMenu);
		
        newSubmenu = new JMenu("New...");
        fileMenu.add(newSubmenu);
        newSubmenu.add(listener.newPrologAction);

        ListenerWindow.addItemToMenu(fileMenu,"Open",KeyEvent.VK_O,new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    listener.openFile(LogicProgramEditor.this);
                }
            }).setToolTipText("Open");	

        ListenerWindow.addItemToMenu(fileMenu,"Close",KeyEvent.VK_W,new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    doClose();
                }
            }).setToolTipText("Close this window");		
        fileMenu.addSeparator();
        saveAction = new SaveAction(); 
        // ListenerWindow.addItemToMenu(fileMenu,saveAction.getValue(Action.NAME).toString(),KeyEvent.VK_S,saveAction);
        fileMenu.add(saveAction)
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                                                   Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveAsAction = new SaveAsAction();
        fileMenu.add(saveAsAction)
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));

        if (isPrologFile()) {
            fileMenu.addSeparator();
            FijiSubprocessEngineWindow.addItemToMenu(fileMenu,"Load",KeyEvent.VK_L, new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        maySaveSource();
                        if (!listener.checkEngineAvailable()) return;    
                        setWaitCursor();
                        listener.prologLoadCommand(new File(editor.getFileFullPath()));
                        restoreCursor();
                    }
                }).setToolTipText("Save and then load (reconsult) this Prolog file");		
        } else if (QUALMfile) {
            fileMenu.addSeparator();
            FijiSubprocessEngineWindow.addItemToMenu(fileMenu,"Load QUALM",KeyEvent.VK_L, new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        maySaveSource();
                        if (!listener.checkEngineAvailable()) return;    
                        setWaitCursor();
                        listener.QUALMLoadCommand(new File(editor.getFileFullPath()),null);
                        restoreCursor();
                    }
                }).setToolTipText("Save and then load (reconsult) this QUALM file");		
        } else if (lps_wFile) {
            fileMenu.addSeparator();
            fileMenu.add(new LPSConvertToAction("lpsp"));
            fileMenu.add(new LPSConvertToAction("lps"));
            fileMenu.addSeparator();
            FijiSubprocessEngineWindow.addItemToMenu(fileMenu,"Load & Run & Display",KeyEvent.VK_L, new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        maySaveSource();
                        if (!listener.checkEngineAvailable()) return;    
                        setWaitCursor();
                        listener.LPSWLoadShowCommand(new File(editor.getFileFullPath()),null,null);
                        restoreCursor();
                    }
                }).setToolTipText("Save and then load and execute this LPS file");		
            FijiSubprocessEngineWindow.addItemToMenu(fileMenu,"Load & Run",new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        maySaveSource();
                        if (!listener.checkEngineAvailable()) return;    
                        setWaitCursor();
                        listener.LPSWLoadCommand(new File(editor.getFileFullPath()),null,"[verbose]");
                        restoreCursor();
                    }
                }).setToolTipText("Save and then load and execute this LPS file, verbose output");		
        } else if (lps_pFile) {
            fileMenu.addSeparator();
            fileMenu.add(new LPSConvertToAction("lpsw"));
            fileMenu.addSeparator();
            FijiSubprocessEngineWindow.addItemToMenu(fileMenu,"Load & Run & Display",KeyEvent.VK_L, new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        maySaveSource();
                        if (!listener.checkEngineAvailable()) return;    
                        setWaitCursor();
                        listener.LPSPLoadShowCommand(new File(editor.getFileFullPath()),null,null);
                        restoreCursor();
                    }
                }).setToolTipText("Save and then load and execute this LPS file");		
            FijiSubprocessEngineWindow.addItemToMenu(fileMenu,"Load & Run", new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        maySaveSource();
                        if (!listener.checkEngineAvailable()) return;    
                        setWaitCursor();
                        listener.LPSPLoadCommand(new File(editor.getFileFullPath()),null,"[verbose]");
                        restoreCursor();
                    }
                }).setToolTipText("Save and then load and execute this LPS file, verbose output");		
        } else if (lps_File) {
            fileMenu.addSeparator();
            fileMenu.add(new LPSConvertToAction("lpsw"));
            fileMenu.addSeparator();
            FijiSubprocessEngineWindow.addItemToMenu(fileMenu,"Load & Run & Display",KeyEvent.VK_L, new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        maySaveSource();
                        if (!listener.checkEngineAvailable()) return;    
                        setWaitCursor();
                        listener.LPSLoadShowCommand(new File(editor.getFileFullPath()),null,null);
                        restoreCursor();
                    }
                }).setToolTipText("Save and then load and execute this LPS (new syntax) file");		
            FijiSubprocessEngineWindow.addItemToMenu(fileMenu,"Load & Run", new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        maySaveSource();
                        if (!listener.checkEngineAvailable()) return;    
                        setWaitCursor();
                        listener.LPSLoadCommand(new File(editor.getFileFullPath()),null,"[verbose]");
                        restoreCursor();
                    }
                }).setToolTipText("Save and then load and execute this LPS (new syntax) file, verbose output");		
        }
		
		
        tabItemAction = TabsWindow.editors.makeTabActionFor(this);

        editMenu = new JMenu("Edit"); mb.add(editMenu);
        editMenu.add(RTextArea.getAction(RTextArea.UNDO_ACTION));
        editMenu.add(RTextArea.getAction(RTextArea.REDO_ACTION));
        editMenu.addSeparator();
        editMenu.add(RTextArea.getAction(RTextArea.CUT_ACTION));
        editMenu.add(RTextArea.getAction(RTextArea.COPY_ACTION));
        ListenerWindow.addItemToMenu(editMenu,"Copy Selection as RTF",new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    editor.copyAsRtf();
                }
            }).setToolTipText("Copies the currently selected text to the system clipboard, with any necessary style information");		
		
        editMenu.add(RTextArea.getAction(RTextArea.PASTE_ACTION));
        editMenu.add(RTextArea.getAction(RTextArea.DELETE_ACTION));
        editMenu.addSeparator();
        editMenu.add(RTextArea.getAction(RTextArea.SELECT_ALL_ACTION));
        editMenu.addSeparator();
		
        if (isPrologFile()){
            JMenu rm = new JMenu("Refactoring");
            editMenu.add(rm);
            rm.add(new ExtractPredicateAction());
        }
        editMenu.add(new JMenuItem(new ShowFindReplaceDialogAction()));
        //mayAddFindNext();  // doesn't work (why?) - adding explicitly below
        editMenu.add(new JMenuItem(new FindNextAction()));
        editMenu.add(new UseSelectionForFindAction());
        editMenu.add(new JMenuItem(new GoToLineAction()));
        editMenu.add(backAction);
				
        editMenu.addSeparator();

        FijiSubprocessEngineWindow.addItemToMenu(editMenu,"Copy File Address to Clipboard",new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    StringSelection selection = new StringSelection(editor.getFileFullPath());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(selection, selection);
                }
            }).setToolTipText("Copies the path of this file to the system clipboard");		

        Action querySelection = new QuerySelectionAction();
        listener.disableWhenBusyOrPaused(querySelection);
		
        Action browseSelection = new BrowseSelectionAction();
        editMenu.addSeparator();
		
        if (isLogicFile()){
            editor.getPopupMenu().addSeparator();
            editor.getPopupMenu().add(querySelection);
            editor.getPopupMenu().addSeparator();
            if (isPrologFile())
                editor.getPopupMenu().add(browseSelection);
            // See configurePopupMenu
            goToDefinitionAction = new GoToDefinitionAction();
            editor.getPopupMenu().add(goToDefinitionAction);
            if (isPrologFile()||isQUALMfile()) {
                editor.getPopupMenu().add(editor.calls);
                editor.getPopupMenu().add(editor.calledBy);
                editor.getPopupMenu().add(new MyCallGraphAction());
            } else 
                editor.getPopupMenu().add(new MyCallHierarchyAction());
            editor.getPopupMenu().add(new MyCallTreeAction());
        }
        /*
          ListenerWindow.addItemToMenu(editor.getPopupMenu(),"Test",new ActionListener(){
          public void actionPerformed(ActionEvent ae){
          System.out.println("dot:"+editor.getSelectionStart());
          System.out.println("selection:"+editor.getSelectedText());
          System.out.println("subterm:"+subTermAtCursor());
          }
          }).setToolTipText("test!");*/
		

        editMenu.add(querySelection);	
        if (isPrologFile())
            editMenu.add(browseSelection);	

        setupNavigationMenus(mb);
		
        /*
          JMenuItem pt = new JMenuItem("Print all clause functors");
          pt.setToolTipText("bla bla");
          editor.getPopupMenu().add(pt);
          pt.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e){
          System.out.println("Predicates:\n"+predicateHeads());
          Iterator<Token> i = getPreviousTokens(((RSyntaxDocument)editor.getDocument()), editor.modelToToken(editor.getSelectionEnd()));
          while(i.hasNext())
          System.out.print(i.next());*/
		
        windowsMenu = listener.windowsMenuManager.makeWindowsMenu();
        windowsMenu.insertSeparator(0);
        windowsMenu.insert(tabItemAction,0);
        mb.add(windowsMenu); 

        setJMenuBar(mb);
    }

    /**
     * "Find Next" menu 
     * Does not work for some reason, so adding the editMenu.add()
     * cmd explicitly elsewhere
    */
    /*
    protected void mayAddFindNext() {
        //editMenu.add(new FindNextAction()); // this does nothing!
        editMenu.add(new JMenuItem(new FindNextAction()));
    }
    */

    protected void setupNavigationMenus(JMenuBar mb) {
        if (isLogicFile())
            mb.add(predicatesMenu());
    }
	
    protected TermModel parseTextToTerm(String S){
        if (isPrologFile()) {
            try{
                Object[] bindings = engine.deterministicGoal("buildTermModel("+S+",TM)","[TM]");
                if (bindings!=null) return (TermModel)bindings[0];
            } catch (IPException ex){
                System.err.println(ex);
            }
        } 
        return null; 
    }
	
    protected JMenu predicatesMenu(){
        String menuTitle = "Predicates";
        final JMenu _predicatesMenu = new JMenu(menuTitle); 
        _predicatesMenu.addMenuListener(new MenuListener(){
                public void menuCanceled(MenuEvent e){}
                public void menuDeselected(MenuEvent e){}
                public void menuSelected(MenuEvent e){
                    _predicatesMenu.removeAll();
                    ArrayList<Token> tokens = predicateHeads(100,null);
                    for (final Token head : tokens){
                        JMenuItem predicateItem = new JMenuItem(head.getLexeme());
                        Font smaller = predicateItem.getComponent().getFont().deriveFont((float)(listener.preferredFontSize));
                        predicateItem.getComponent().setFont(smaller);
                        predicateItem.addActionListener(new ActionListener(){
                                public void actionPerformed(ActionEvent ae){
                                    backAction.rememberPosition(LogicProgramEditor.this);
                                    editor.mySetCaretPosition(head.getOffset());
                                    backAction.didJump(LogicProgramEditor.this);
                                }
                            });
                        _predicatesMenu.add(predicateItem);
                    }
                }
            });
        return _predicatesMenu;
    }
    public boolean isDirty(){
        return editor.isDirty();
    }
    /**
     * @return whether the window is effectively closed
     */
    public boolean doClose(){
        return doClose(true);
    }
    /** Returns false if the window is not closed (user cancelled). If !destroyWindow, just saves the contents*/
    boolean doClose(boolean destroyWindow){
        Window w = TabsWindow.editors.currentWindow(LogicProgramEditor.this);
        if (editor == null)
            return true; // we've closed before
        if (isDirty()){
            int choice = JOptionPane.showConfirmDialog(w,"Save changes ?","Save before closing?", JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice==JOptionPane.CANCEL_OPTION) return false;
            boolean cancelled = false;
            if (choice==JOptionPane.YES_OPTION)
                if (anonymous) cancelled = !saveAsAction.doIt();
                else saveAction.doIt();
            if (cancelled)
                return false;
        }
        if (destroyWindow)
            destroy();
        return true;
    }
	
    public void destroy(){
        if (editor == null)
            return;
        rememberPreference(listener.preferences);
        fireWillDestroy();
        editors.remove(new File(getFileName()));
        editor=null;
        TabsWindow.editors.destroy(LogicProgramEditor.this);	
        dispose();
    }

    class MyCallGraphAction extends AbstractAction{
        MyCallGraphAction(){
            putValue(Action.NAME,"My Call Graph");
            putValue(Action.SHORT_DESCRIPTION,"Show call graph for the predicate where the cursor is");
        }
        public void actionPerformed(ActionEvent e){
            String term = subTermAtCursor();
            if (!engine.isAvailable() || term.length()==0) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            String G = "fjCallGraphForPredicate('"+getFileName()+"',Term)";
            // System.out.println("Calling "+G);
            setWaitCursor() ;
            if (!engine.deterministicGoal(G, "[string(Term)]", new Object[]{term}))
                System.err.println("Failed "+G+", Term=="+term);
            restoreCursor();
        }
    }
	
    public static final String CALL_TREE_TITLE = "Goals Called By ";
    class MyCallTreeAction extends AbstractAction{
        MyCallTreeAction(){
            putValue(Action.NAME,CALL_TREE_TITLE+"Selected Goal");
            putValue(Action.SHORT_DESCRIPTION,"Show call tree under the predicate where the cursor is");
        }
        public void actionPerformed(ActionEvent e){
            String term = subTermAtCursor();
            if (!engine.isAvailable() || term.length()==0) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            setWaitCursor() ;
            showCallTree(term);
            restoreCursor();
        }
    }
	
    public static final String CALL_HIERARCHY_TITLE = "Goals Calling ";
    class MyCallHierarchyAction extends AbstractAction{
        MyCallHierarchyAction(){
            putValue(Action.NAME,CALL_HIERARCHY_TITLE+"Selected Goal");
            putValue(Action.SHORT_DESCRIPTION,"Show call hierarchy (ancestors) for the sub goal where the cursor is");
        }
        public void actionPerformed(ActionEvent e){
            String term = subTermAtCursor();
            if (!engine.isAvailable() || term.length()==0) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            setWaitCursor();
            showCallHierarchy(term);
            restoreCursor();
        }
    }

    protected void showCallHierarchy(String stringWithoutDot){
        System.err.println("Not implemented for Prolog files!");
    }

    protected void showCallTree(String term){
        String G = "fjCallTreeForPredicate('"+getFileName()+"',Term,"+ engine.registerJavaObject(LogicProgramEditor.this)+")";
        // System.out.println("Calling "+G);
        if (!engine.deterministicGoal(G, "[string(Term)]", new Object[]{term}))
            System.err.println("Failed "+G+", Term=="+term);
    }
	
    class ShowWindowAction extends AbstractAction implements PropertyChangeListener{
        ShowWindowAction(){
            putValue(Action.NAME,filename());
            putValue(Action.SHORT_DESCRIPTION,"Pop this file window to front");
            editor.addPropertyChangeListener(TextEditorPane.FULL_PATH_PROPERTY, ShowWindowAction.this);
        }
        public void actionPerformed(ActionEvent e){
            TabsWindow.editors.bringToFront(LogicProgramEditor.this);
            editor.requestFocus();
        }
        public void propertyChange(PropertyChangeEvent evt){
            if (evt.getPropertyName().equals(TextEditorPane.FULL_PATH_PROPERTY) && evt.getSource().equals(editor))
                putValue(Action.NAME,filename());
        }
        String filename(){
            return new File(editor.getFileFullPath()).getName();
        }
        public String toString(){
            return "ShowAction for editor "+filename();
        }
    }
	
    abstract class GenericSaveAction extends AbstractAction {
        GenericSaveAction(String name){
            super(name);
        }
    }
    protected class SaveAction extends GenericSaveAction implements PropertyChangeListener{
        SaveAction(){
            super("Save");
            setEnabled(false);
            editor.addPropertyChangeListener(TextEditorPane.DIRTY_PROPERTY,this);
        }
        public void actionPerformed(ActionEvent e){
            doIt();
        }
        public void doIt(){
            boolean wasDirty = editor.isDirty();
            try{
                editor.save();
                if (anonymous && wasDirty)
                    editor.setDirty(true);
            }
            catch(IOException ex){
                JOptionPane.showMessageDialog(TabsWindow.editors.currentWindow(LogicProgramEditor.this),"Error saving file:"+ex,"Error",JOptionPane.ERROR_MESSAGE);	
            }
        }
        public void propertyChange(PropertyChangeEvent evt){
            setEnabled(editor.isDirty()&&!anonymous);
        }
    }
    class SaveAsAction extends GenericSaveAction{
        SaveAsAction(){
            super("Save As...");
            setEnabled(true);
        }
        /**
         * @return whether the save was concluded, not cancelled
         */
        public boolean doIt(){
            File previous = new File(editor.getFileFullPath());
            if (isLogicFile()) 
                listener.fileChooser.setFileFilter(FijiSubprocessEngineWindow.logicFilesFilter);
            listener.fileChooser.setDialogTitle("Save as file...");
            int returnVal = listener.fileChooser.showSaveDialog(LogicProgramEditor.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File F = listener.fileChooser.getSelectedFile();
                if (!dotExtension(previous).equals(dotExtension(F))){
                    JOptionPane.showMessageDialog(TabsWindow.editors.currentWindow(LogicProgramEditor.this),"You cannot change or remove this file name extension.\nPlease create a New file with your intended extension\nand copy the text into there",
                                                  "Can not Save with different extension",JOptionPane.ERROR_MESSAGE);	
                    return false;
                }
                LogicProgramEditor LPE = getExistingEditor(F, listener);
                if (!F.equals(previous) && LPE!=null){
                    JOptionPane.showMessageDialog(TabsWindow.editors.currentWindow(LogicProgramEditor.this),"You cannot overwrite a file open in another editor window.\nPlease close it first.",
                                                  "Can not Save over another open file",JOptionPane.ERROR_MESSAGE);	
                    LPE.toFront();
                    return false;
                }
                if (F.exists() && 
                    JOptionPane.showConfirmDialog(TabsWindow.editors.currentWindow(LogicProgramEditor.this),"Replace existing file?",
                                                  "Replace existing file?",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION)
                    return false;
                try{
                    editor.saveAs(FileLocation.create(F));
                    anonymous = false;
                    return true;
                } catch(IOException ex){
                    JOptionPane.showMessageDialog(TabsWindow.editors.currentWindow(LogicProgramEditor.this),"Error saving file:"+ex,"Error",JOptionPane.ERROR_MESSAGE);	
                }
            }
            return false;	
        }
        public void actionPerformed(ActionEvent e){
            doIt();
        }
    }
	
    class LPSConvertToAction extends GenericSaveAction{
        String fromFormat,toFormat;
        LPSConvertToAction(String toFormat){
            super("Convert to "+toFormat);
            setEnabled(true);
            this.toFormat=toFormat;
            this.fromFormat=dotExtension(getFile()).substring(1); // ignore dot
        }
        /**
         * @return whether the save was concluded, not cancelled
         */
        public boolean doIt(){
            if (!listener.checkEngineAvailable()) return false;          

            if (anonymous){
                JOptionPane.showMessageDialog(TabsWindow.editors.currentWindow(LogicProgramEditor.this),"You cannot convert a file which has not been saved yet.\nPlease save it first.",
                                              "Can not convert unsaved file",JOptionPane.ERROR_MESSAGE);	
                return false;
            }
            String base = editor.getFileFullPath().substring(0, editor.getFileFullPath().lastIndexOf('.'));
            File F = new File(base+"."+toFormat);
            LogicProgramEditor LPE = getExistingEditor(F, listener);
            if (LPE!=null){
                JOptionPane.showMessageDialog(TabsWindow.editors.currentWindow(LogicProgramEditor.this),"You cannot overwrite a file open in another editor window.\nPlease close it first.",
                                              "Can not Save over another open file",JOptionPane.ERROR_MESSAGE);	
                LPE.toFront();
                return false;
            }
            if (F.exists() && 
                JOptionPane.showConfirmDialog(TabsWindow.editors.currentWindow(LogicProgramEditor.this),"Replace existing file?",
                                              "Replace existing file?",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION)
                return false;
            maySaveSource();
            try{
                String papersFile, weiFile;
                boolean toWei;
                if (toFormat.equals("lpsw")){
                    papersFile = editor.getFileFullPath();
                    weiFile = F.getAbsolutePath();
                    toWei=true;
                } else if (toFormat.equals("lpsp")){
                    papersFile = F.getAbsolutePath();
                    weiFile = editor.getFileFullPath();
                    toWei=false;
                } else if (toFormat.equals("lps")){
                    papersFile = F.getAbsolutePath();
                    weiFile = editor.getFileFullPath();
                    toWei=false;
                } else throw new RuntimeException("Inconsistency in LPSConvertToAction");
                String translator = (toFormat.equals("lps")||fromFormat.equals("lps"))?"lps2p":"lpsp2p";
                if (listener.engine.deterministicGoal("syntax2p_file(PapersFile,WeiFile,"+translator+","+toWei+")", "[string(PapersFile),string(WeiFile)]", new Object[]{papersFile,weiFile}))
                    LogicProgramEditor.showEditor(F.getAbsolutePath(), null,listener, false);
                else return false;
            } catch(Exception ex){
                JOptionPane.showMessageDialog(TabsWindow.editors.currentWindow(LogicProgramEditor.this),"Error converting file:"+ex,"Error",JOptionPane.ERROR_MESSAGE);	
            }
			
            return false;	
        }
        public void actionPerformed(ActionEvent e){
            doIt();
        }
    }
	
    public boolean isPrologFile(){ return prologFile; }
    public boolean isQUALMfile(){ return QUALMfile; }
    public boolean isLogicFile(){return prologFile || QUALMfile || lps_wFile|| lps_File || lps_pFile; }
	
    protected static class TokenAndArity{
        public TokenAndArity(Token token, int arity) {
            this.token = token;
            this.arity = arity;
        }
        Token token; int arity;
    }
	
    protected static Token lastSubtermToken(RSyntaxDocument doc, Token first, boolean hilog){
        return lastSubtermTokenAndArity(doc,first,hilog).token;
    }

    protected static TokenAndArity lastSubtermTokenAndArity(RSyntaxDocument doc, Token first, boolean hilog){
        Token current = new TokenImpl(first);
        Iterator<Token> it = getNextTokens(doc, current);
        int parCount = 0;
        boolean foundPar = false; // found at least one opening parenthesis
        boolean foundComplete = false; // opened and closed a parenthesis pair
        int topArity = 0;
        while(it.hasNext()){
            Token N = it.next();
            // System.out.println("N:"+N);
            if (N.is(Token.SEPARATOR,"(") & foundComplete){
                if (foundComplete && !hilog)
                    break;
                foundComplete = false;
                parCount++;
            } else if (foundComplete){
                break;
            } else if (N.is(Token.SEPARATOR,"(") || N.is(Token.SEPARATOR,"[") || N.is(Token.SEPARATOR,"{") || N.is(Token.SEPARATOR,"${")){
                parCount ++;
                foundPar = true;
            } else if (N.is(Token.SEPARATOR,")") || N.is(Token.SEPARATOR,"]") || N.is(Token.SEPARATOR,"}")){
                parCount --;
                if (parCount<0)
                    break; // this parenthesis will NOT be part of the subterm
                if (parCount==0&&foundPar){
                    // current = N;
                    foundComplete = true; // stop at next except if a new (... starts
                }
            } else if (parCount==0 && (N.is(Token.SEPARATOR,",") || N.is(Token.SEPARATOR,"|") ))
                break;
            else if (!hilog && parCount==1 && (N.is(Token.SEPARATOR,","))) // Prolog term arity
                topArity++;
            else if (N.getType()==FULL_STOP)
                break;
            else if (parCount==0 && (N.is(Token.RESERVED_WORD,":-") || N.is(Token.RESERVED_WORD,";") || N.is(Token.RESERVED_WORD,"->")))
                break;
            //else if (N.getType()==Token.NULL){
            //	break;
            //}
            if (N.getType()!=Token.NULL && N.getType()!=Token.WHITESPACE) 
                current = new TokenImpl(N); // to avoid weird side effects!
            // System.err.println("parCount:"+parCount+",foundComplete:"+foundComplete+",current...:"+current);
        }
        // System.out.println("lastSubtermToken returns:"+current);
        if (foundComplete && !hilog)
            topArity++;
        return new TokenAndArity(current,topArity);
    }

    /** Last token will NOT be a dot */
    protected Token lastSubtermToken(Token first, boolean hilog){
        return lastSubtermToken((RSyntaxDocument)editor.getDocument(),first,hilog);
    }
	
    protected String textOfHead(Token first){
        int begin = first.getOffset();
        int end = first.getEndOffset();
        Token last = lastSubtermToken(first,false);
        // System.out.println("last:"+last);
        if (last!=null) end = last.getEndOffset();
        try{
            return editor.getText(begin,end-begin);
        } catch(BadLocationException ex){
            System.err.println("Could not textOfHead:"+ex);
            return first.getLexeme();
        }
    }
	
	
    /** Returns the dot, or the last token in the file */
    protected static Token lastTokenOfClause(RSyntaxDocument doc, Token first){
        Iterator<Token> it = getNextTokens(doc, first);
        Token last = first;
        while (it.hasNext()){
            last = it.next();
            if (last.getType()==FULL_STOP)
                break;
        }	
        return last;
    }
	
    protected static Token firstTokenOfClause(final RSyntaxDocument doc, Token initial, boolean upToPreviousDot){
        Token first = new TokenImpl(initial);
        Iterator<Token> it = getPreviousTokens(doc, initial);
        while (it.hasNext()){
            Token current = it.next();
            if (current.getType()==FULL_STOP)
                break;
            if (current.getType()!=Token.NULL)
                first = current;
        }
        if (upToPreviousDot)
            return first;
        // Let's move forward from the previous clause dot ignoring whitespace:
        Iterator<Token> it2 = getNextTokens(doc, first, true, false);
        while(it2.hasNext()){
            first = it2.next();
            if (!first.isCommentOrWhitespace())
                break;
        }
        return first;
    }


    /**
     * @param doc
     * @param first clause/fact first token to search from
     * @param functor
     * @param arity
     * @return first token in clause starting a functor(A1,A2,..,Aarity) sub term, or null if there isn't one
     */
    protected static Token firstTokenOfTermInClause(RSyntaxDocument doc, Token first, String functor, int arity){
        Iterator<Token> it = getNextTokens(doc, first);
        Token current = first;
        if (current.is(Token.IDENTIFIER,functor) && lastSubtermTokenAndArity(doc, current, false).arity==arity)
            return current;
        while (it.hasNext()){
            current = it.next();
            //System.err.println(current);
            if (current.getType()==FULL_STOP)
                return null;
            if (current.is(Token.IDENTIFIER,functor) && lastSubtermTokenAndArity(doc, current, false).arity==arity)
                return current;;
        }	
        return null;
    }
	
    protected boolean isClauseHead(Token first,Pattern pattern){
        String text = textOfHead(first);
        return pattern.matcher(text).matches();
    }
	
    protected ArrayList<Token> predicateHeads(int maxHeads, String toIgnore){
        return predicateHeads(maxHeads, toIgnore, null, -1, true);
    }
    // should return ArrayList<TokenEssence>, e.g. lexeme char[] pointer and position, 
    /** Return (nonrepeated contiguously) heads, no more than maxHeads. 
	So if a predicate P has contiguous clauses, even of different head arities, a single P will be returned.
	For Flora returns both predicates and objects.
	If toIgnore is not null, it is taken to be a predicate/object name without interest; useful e.g. for Flora's neg 
	If arity != -1 and toMatch != null, returns only one token */
    protected ArrayList<Token> predicateHeads(int maxHeads, String toIgnore, String toMatch, int arity, boolean aggregateContiguous){
        char[] toMatchArray;
        if (toMatch==null) 
            toMatchArray = null;
        else{
            if (TermModel.quotesAreNeeded(toMatch)) 
                // hack; this would probably be better fixed calling toString(true) elsewhere
                toMatch = "'"+TermModel.doubleQuotes(toMatch)+"'";
            toMatchArray=toMatch.toCharArray();
        }
        Pattern pattern = null;
        if (arity>=0 && toMatch!=null){
            String regex;
            String toMatchQ = Pattern.quote(toMatch); // beware of _, $,...
            if (arity==0) regex = toMatchQ;
            else {
                regex = toMatchQ+"\\(";
                for(int arg=0;arg<arity;arg++)
                    if (arg>0) regex += ",.+";
                    else regex += ".+";
                regex += "\\)";
                //System.out.println("regex:"+regex);
            }
            pattern = Pattern.compile(regex);
            //System.out.println("regex:"+regex+", arity:"+arity);
        }
        if (toIgnore==null) toIgnore="";
        ArrayList<Token> ph = new ArrayList<Token>();
        if (maxHeads<=0) return ph;
        Token first = editor.getTokenListForLine(0); 
        boolean assumePreviousDot = true; // so we can get the first identifier
        String previous = null; 
        if (first==null) return ph;
        else if ((first.is(Token.RESERVED_WORD,":-") || first.is(Token.RESERVED_WORD,"?-") )) {
            assumePreviousDot = false; 		
        } else if ((first.getType()==Token.IDENTIFIER || first.getType()==Token.LITERAL_CHAR) && (toMatchArray==null || first.is(toMatchArray))) {
            if (arity==-1 || (arity >= 0 && isClauseHead(first,pattern))) {
                Token firstEssence = new TokenImpl(first);
                //firstEssence.setNextToken(null); 
                ph.add(firstEssence);
                previous = first.getLexeme();	
                if (arity>=0) return ph; 
            }
            assumePreviousDot = false; 		
        }
		
		
        Iterator<Token> it = getNextTokens((RSyntaxDocument)editor.getDocument(), first);
        while(ph.size()<maxHeads && it.hasNext()){
            Token next=null;
            // Advance to dot...
            while( !assumePreviousDot && it.hasNext()){
                next = it.next();
                if (next.getType()==FULL_STOP)
                    break;
            }
            assumePreviousDot = false;
            if (it.hasNext()){
                // ...then to first (non-dot!) identifier
                while( it.hasNext() ){
                    next = it.next();
                    //System.out.println("next:"+next);
                    if (next.is(Token.RESERVED_WORD,":-") || next.is(Token.RESERVED_WORD,"?-")  ) 
                        // skip directive
                        break;
                    if (next.getType()==Token.PREPROCESSOR || next.getType()==Token.COMMENT_MULTILINE||next.getType()==Token.COMMENT_DOCUMENTATION||next.getType()==Token.COMMENT_EOL||
                        next.getType()==Token.WHITESPACE||next.getType()==Token.NULL||next.getType()==Token.ANNOTATION||next.is(Token.IDENTIFIER,toIgnore)
                        ||next.getType()==Token.VARIABLE||next.getType()==Token.SEPARATOR )
                        continue;
                    if ((next.getType()==Token.IDENTIFIER || next.getType()==Token.LITERAL_CHAR) && (toMatchArray==null || next.is(toMatchArray))){
                        String S = next.getLexeme();
                        if (previous!=null && S.equals(previous) && aggregateContiguous)
                            break;
                        previous=S;
                        if (arity==-1 || (arity >= 0 && isClauseHead(next,pattern))) {
                            Token nextEssence = new TokenImpl(next);
                            //nextEssence.setNextToken(null); // to ease garbage collection
                            ph.add(nextEssence); // tokens are reused....BEWARE that 
                            if (arity>=0) return ph;
                        }
                        break;
                    } else break; // we caught something weird, let's skip this line
                }
            } // otherwise we're finished
        } // skipping to next dot
        return ph;
    }

	
    /** Get incrementally all tokens after (and excluding) the initial one. Thread unsafe, iterator is valid
	only in the event thread between document changes */
    public static Iterator<Token> getNextTokens(final RSyntaxDocument doc, final Token initial){
        return getNextTokens(doc, initial, false);
    }
	
    static Iterator<Token> getNextTokens(final RSyntaxDocument doc, final Token initial, final boolean includeInitial){
        return getNextTokens(doc, initial, includeInitial, true);
    }
    static Iterator<Token> getNextTokens(final RSyntaxDocument doc, final Token initial, final boolean includeInitial, final boolean returnWHITESPACEs){
        if (initial==null) 
            throw new RuntimeException("getNextTokens requires a non null initial token");
			
        return new Iterator<Token>(){
            Token initialCopy = new TokenImpl(initial);
            /** Last non null we found */
            Token current = refreshToken(doc,initialCopy);
            /** next Token to return */
            Token nextT = (includeInitial?initial:null);
            /** line of current token */
            int L = -1 ;
            int nLines = doc.getDefaultRootElement().getElementCount();	
			
			
            public boolean hasNext(){
                if (nextT==null&&current!=null) fetch();
                return nextT!=null;
            }
            // always called with nextT==null and current!=null
            // leaves nextT!=null if there is another token
            private void fetch(){
                if (L==-1){
                    /*L=editor.getLineOfOffset(pos);*/ L=doc.getDefaultRootElement().getElementIndex(initialCopy.getOffset() );
                }
                if (nextT!=null) throw new RuntimeException("Inconsistent token iterator");
                current = current.getNextToken();
                if (!returnWHITESPACEs)
                    while(current!=null && current.getType()==Token.WHITESPACE)
                        current = current.getNextToken();
                if (current!=null && current.getType()!=Token.NULL)
                    nextT = current;
                else while (L<nLines-1) {
                        L++;
                        current = doc.getTokenListForLine(L);
                        if (!returnWHITESPACEs)
                            while(current!=null && current.getType()==Token.WHITESPACE)
                                current = current.getNextToken();
                        if (current!=null && current.getType()!=Token.NULL){
                            nextT = current;
                            break;
                        }
                    }
                // System.out.println("L=="+L+",current=="+current+",current.getNextToken()=="+current.getNextToken());
            }
            public Token next(){
                if (nextT==null) fetch();
                if (nextT==null) throw new NoSuchElementException("No more tokens");
                Token temp = nextT;
                nextT=null;
                return temp;
            }
            public void remove(){
                throw new UnsupportedOperationException();
            }
        };
    }
    // an attempt to deal with RSTA's tricky Token behavior, as far as I know it..
    static private Token refreshToken( RSyntaxDocument doc, Token T){
        int L = doc.getDefaultRootElement().getElementIndex(T.getOffset() );
        Token first = doc.getTokenListForLine(L);
        while(!first.equals(T))
            first = first.getNextToken();
        return first;
    }
    /** Get incrementally all tokens before (and excluding) the one situated at pos, starting with the last ones. 
	Thread unsafe, iterator is valid only in the event thread between document changes */
    static Iterator<Token> getPreviousTokens(final RSyntaxDocument doc, final Token initial){
        if (initial==null) 
            throw new RuntimeException("getPreviousTokens assumes a nonnull initial token");
        return new Iterator<Token>(){
            /** Last nonnull we found in the current line*/
            Token current = initial;
            /** next Token to return */
            Token nextT=null;
            /** line of current token */
            int L = -1 ;
            /** First token in line L */
            Token firstInLine = null;
			
            public boolean hasNext(){
                if (nextT==null&&current!=null) fetch();
                return nextT!=null;
            }
            // always called with nextT==null and current!=null
            private void fetch(){
                if (L==-1){ // hack to initialize without de-anonymizing this constructorless class
                    /*L=editor.getLineOfOffset(pos);*/ L=doc.getDefaultRootElement().getElementIndex(initial.getOffset() );
                    firstInLine = doc.getTokenListForLine(L);
                }
                // Use containsPosition() because tokens may get reused during getTokenListForLine, so == will not work
                if (firstInLine.containsPosition(current.getOffset() ) && L>0){
                    // System.out.println("L=="+L+",current=="+current);
                    L--;
                    firstInLine = doc.getTokenListForLine(L);
                    // find the last in this line
                    Token rightmost = firstInLine;
                    while (rightmost.getNextToken()!=null && rightmost.getNextToken().getType()!=Token.NULL){
                        rightmost = rightmost.getNextToken();
                    }
                    // System.out.println("rightmost=="+rightmost);
                    current = rightmost;
                } else if (firstInLine.containsPosition(current.getOffset() )  && L==0) {
                    // reached beginning
                    current = null;
                } else if (/* we know that firstInLine!=current */ L>=0) {
                    Token rightmost = firstInLine;
                    while (rightmost!=null && rightmost.getNextToken()!=null && !rightmost.getNextToken().containsPosition(current.getOffset() ) ){
                        rightmost = rightmost.getNextToken();
                    }
                    if (rightmost==null)
                        throw new RuntimeException("Inconsistent token iterator");
                    if (rightmost!=current) 
                        current = rightmost;
                    else current=null;
                }
                if (current!=null)
                    nextT = current;
            }
            public Token next(){
                if (nextT==null&&current!=null) fetch();
                if (nextT==null) throw new NoSuchElementException("No more tokens");
                Token temp = new TokenImpl(nextT);
                nextT=null;
                return temp;
            }
            public void remove(){
                throw new UnsupportedOperationException();
            }
        };
    }
	
    /** Useful for completions */
    public static Token tokenEndingAtCaret(RSyntaxTextArea rsta){
        RSyntaxDocument doc = (RSyntaxDocument)rsta.getDocument();
        int line = rsta.getCaretLineNumber();
        int caret =  rsta.getCaretPosition();;
        Token t = doc.getTokenListForLine(line);
        if (t==null) return null;
        int lineBegin = t.getOffset();
        Token curToken = RSyntaxUtilities.getTokenAtOffset(t, caret);
        if (curToken==null) // At end of the line
            return t.getLastPaintableToken();
        else if (lineBegin!=caret && caret==curToken.getOffset())
            return RSyntaxUtilities.getTokenAtOffset(t, caret-1);
        else return curToken;
    }
	
    /** Marks all similar variables in same term; requires changing a couple of access qualifiers 
	in package org.fife.ui.rsyntaxtextarea */

    public static OccurrenceMarker createOccurrenceMarker() {
        return new OccurrenceMarker(){
            public Token getTokenToMark(RSyntaxTextArea textArea) {
                // This implementatin in HTMLOccurreneMarker seems complicated...: return getTagNameTokenForCaretOffset(textArea, this);
                int start = textArea.getSelectionStart();
                if (start>=0) return textArea.modelToToken(start);
                else return null;
            }
            public boolean isValidType(RSyntaxTextArea textArea, Token t) {
                return textArea.getMarkOccurrencesOfTokenType(t.getType());
            }
            public void markOccurrences(RSyntaxDocument doc, Token t, final RSyntaxTextAreaHighlighter h, final SmartHighlightPainter p) {
                if (t.isPaintable() && t.getType()==Token.VARIABLE) {
                    final char[] varName = t.getLexeme().toCharArray();
                    visitVariablesOfClauseWith(doc,t,new TokenVisitor(){
                            public void doSomething(Token T){
                                if (T.is(Token.VARIABLE,varName))
                                    try{
                                        h.addMarkedOccurrenceHighlight(T.getOffset() , T.getOffset()  + T.length(), p);
                                    } catch (BadLocationException ble) {
                                        throw new RuntimeException("Bad location of token"+ble);
                                    }
                            }
                        });
					
                }

            }
        };
    }
	
    static void visitVariablesOfClauseWith(RSyntaxDocument doc,Token initial,TokenVisitor V){
        if (initial.getType()==Token.VARIABLE)
            V.doSomething(initial);
        Iterator<Token> i;
        boolean foundDot;
		
        Token tt = new TokenImpl(initial);
		
        i = LogicProgramEditor.getNextTokens(doc,initial);
        foundDot = false;
        while (i.hasNext() && !foundDot){
            Token v = i.next();
            if (v.getType()==Token.VARIABLE)
                V.doSomething(v);
            else if (v.getType()==FULL_STOP)
                foundDot=true;
        }
		
        // This iteration after the previous so reused tokens don't screw up things
        i = LogicProgramEditor.getPreviousTokens(doc,tt);
        foundDot=false;
        while (i.hasNext() && !foundDot){
            Token v = i.next();
            //System.out.println("loop v=="+v);
            if (v.getType()==Token.VARIABLE)
                V.doSomething(v);
            else if (v.getType()==FULL_STOP)
                foundDot=true;
        }
    }
	
    public static interface TokenVisitor{
        public void doSomething(Token T);
    }
	
    public static String collectCommentsBefore(RSyntaxDocument doc,Token first){
        Iterator<Token> it = getPreviousTokens(doc, first);
        ArrayList<String> comments = new ArrayList<String>();
        boolean acceptingWS = true;
        while (it.hasNext()){
            Token previous = it.next();
            if (previous.isComment()){
                comments.add(previous.getLexeme()); // Can be optimized to use access char[] instead
                acceptingWS = false;
            } else if (!acceptingWS || !previous.isWhitespace()) 
                // Only one whitespace is acceptable; 
                break;
            else
                acceptingWS=false;
        }
        StringBuffer sb = new StringBuffer();
        for (int i=comments.size()-1; i>=0; i--)
            sb.append(comments.get(i)+"\n");
        return sb.toString();
    }

    public static boolean getMarkOccurrencesOfTokenType(int type) {
        return true; // so that clicking on non-vars clears the marking
        //return type==Token.VARIABLE;
    }
	
	
    public void setWaitCursor() {
        XJDesktop.setWaitCursor(editor);
        XJDesktop.setWaitCursor(sp);
        XJDesktop.setWaitCursor(this);
    }

    public void restoreCursor() {
        XJDesktop.restoreCursor(editor);
        XJDesktop.restoreCursor(sp);
        XJDesktop.restoreCursor(this);
    }
	
    protected void fireDidCreate(){
        for (EditorLifecycleListener L : listeners)
            L.didCreate(this);
    }
    protected void fireWillDestroy(){
        for (EditorLifecycleListener L : listeners)
            L.willDestroy(this);
    }
    public void addEditorLifecycleListener(EditorLifecycleListener L){
        listeners.add(L);
    }
    public void removeEditorLifecycleListener(EditorLifecycleListener L){
        listeners.remove(L);
    }
    /** Extension or filename; only the ending is checked */
    public static String extensionToLanguage(String extensionOrFileName){
        if (extensionOrFileName.endsWith(".flr")) return "Flora-2";
        else if (isProlog(extensionOrFileName)) return "Prolog";
        else if (extensionOrFileName.endsWith(".ab")) return "QUALM";
        else if (extensionOrFileName.endsWith(".lps")) return "LPS New Syntax";
        else if (extensionOrFileName.endsWith(".lpsp")) return "LPS Experimental Syntax";
        else if (extensionOrFileName.endsWith(".lpsw")) return "LPS Internal Syntax";
        else if (extensionOrFileName.endsWith(".ergo")) return "Ergo";
        else if (extensionOrFileName.endsWith(".ergotxt")) return "Ergotext";
        else return "???";
    }
    public static boolean isQUALM(String S){
        return S.endsWith(".ab");
    }
    public static boolean isLPS_(String S){
        return S.endsWith(".lps");
    }
    public static boolean isLPS_W(String S){
        return S.endsWith(".lpsw");
    }
    public static boolean isLPS_P(String S){
        return S.endsWith(".lpsp");
    }
    public static boolean isProlog(String S){
        S = S.toLowerCase();
        return S.endsWith(".p") || S.endsWith(".pl") || S.endsWith(".plt") || S.endsWith(".lpsp") /* TODO: clean this up*/;
    }
    public static String extensionToLanguage(File F){
        return extensionToLanguage(F.getName());
    }
    /** Won't change if M null */
    public void maySetPreferredModule(String M){
        String oldModule = preferredModule;
        if (M!=null && M.trim().length()>0){
            preferredModule=M;
            if (oldModule==null || !(oldModule.equals(preferredModule)))
                firePropertyChange(MODULE_PROPERTY, oldModule, preferredModule);
        }
    }
    /** Returns the current selection, or the subterm whose first token is at the current caret position */
    String subTermAtCursor(){
        if (editor.getSelectedText()!=null) {
            String T = editor.getSelectedText().trim();
            if (T.endsWith("."))
                T = T.substring(0, T.length()-1);
            return T;
        }
        Token current =  editor.modelToToken(editor.getSelectionStart());
        if (current==null) return "";
        // System.out.println("current=="+current);
        String T = "";
        int begin = current.getOffset();
        int end = current.getEndOffset();
        Token last = current;
        if (current.getType()==Token.VARIABLE /* Hilog...*/ || current.getType()==Token.IDENTIFIER || 
            current.getType()==Token.LITERAL_CHAR || isNegation(current)) {
            last = lastSubtermToken(current,isFloraFile());
            end = last.getEndOffset();
        }
        try{
            T = editor.getText(begin,end-begin);
        } catch (BadLocationException ex){
            throw new RuntimeException("Bad exception" + ex+"\ncurrent: "+current+", last:"+last);
        }
        return T;
    }
	
    protected boolean isNegation(Token T){
        return T.is(Token.RESERVED_WORD, "\\+")|| T.is(Token.RESERVED_WORD, "not");
    }

    /**
     * @param term string without dot
     */
    public void showDefinitionFor(String term){
        Object[] bindings = getDefinitionFor(term); 
        if (bindings!=null){
            displayPredicate(bindings[0].toString(), ((Integer)bindings[1]).intValue(), bindings[2].toString(), (TermModel)bindings[3]);
        }
    }
	
    /** finds file and position information for a predicate
     * @param string with a well formed term, e.g. "foo(bar,X,fff(e))"
     * @return [Functor,Arity,Filename,PositionTermModel,TemplateAtom]
     */
    private Object[] getDefinitionFor(String term){
        String G = "fjFindDefinition('"+getFileName()+"',Term,F,Arity,File,Position,Template), buildTermModel(Position,PositionTM), ipObjectSpec('java.lang.Integer',ArityInt,[Arity],_)";
        Object[] r = engine.deterministicGoal(
                                              G, 
                                              "[string(Term)]", // to tolerate weird functors
                                              new Object[]{term},
                                              "[string(F),ArityInt,string(File),PositionTM,string(Template)]"); 
        if (r==null)
            System.err.println("Failed "+G+", Term=="+term);
        return r;
    }
	
    protected class GoToDefinitionAction extends AbstractAction {
        GoToDefinitionAction(){
            putValue(Action.NAME,"Go To Definition");
            putValue(Action.SHORT_DESCRIPTION,"Show beginning of this predicate definition in its file");
        }
        public void actionPerformed(ActionEvent e){
            doIt();
        }
        public void doIt(){
            String term = subTermAtCursor();
            term = term.trim();
            if (term.endsWith(".")) term = term.substring(0,term.length()-1);
            //System.out.println("subTermAtCursor() == "+term);
            if (!engine.isAvailable() || term.length()==0) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            setWaitCursor() ;
            showDefinitionFor(term);
            restoreCursor();
        }
    }
		
    public boolean isFloraFile(){ return false; }

    public void maySaveSource() {
        if (editor.isDirty()) 
            saveAction.doIt();
        if (getLogicParser()!=null)
            for(File f:getLogicParser().getIncluded()){
                LogicProgramEditor lpe = getExistingEditor(f);
                if (lpe!=null)
                    lpe.maySaveSource();
            }
			
    }

}
