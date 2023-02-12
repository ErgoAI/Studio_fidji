package com.xsb.xj;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import com.declarativa.interprolog.PrologEngine;
import com.declarativa.interprolog.TermModel;
import com.declarativa.interprolog.gui.ListenerWindow;
import com.xsb.xj.util.GUIError;
import com.xsb.xj.util.HelpManager;
import com.xsb.xj.util.HintTextFieldUI;
import com.xsb.xj.util.TransferableXJSelection;
import com.xsb.xj.util.XJException;


/** A JTextField capable of editing a single GUITerm node */
@SuppressWarnings("serial")
public class XJAtomicField extends JTextField implements XJComponent, DnDCapable, FocusListener, DocumentListener{
    protected GUITerm gt;
    PrologEngine engine;
    private boolean dirty;
    private JPopupMenu popupMenu;
    private boolean badInputInGUI; // if this is true, we know GUI data is incorrect

    private static final String FOCUS_LISTENER = "focuslistener";
    	//need to be removed
	private static final String BOLD = "bold";
    private static final String ITALIC = "italic";
    private boolean focusChangeListener;

    public XJAtomicField(PrologEngine engine,GUITerm gt){
        super("",0);
        this.engine=engine;
        setGT(gt);
        dirty = true;
        badInputInGUI=false;

        addFocusListener(this);
        //addActionListener(this);

        adjustToTerm(this,gt);

        // no longer here: refreshGUI();

        getDocument().addDocumentListener(this);
        if (gt.findProperty(GUITerm.DISABLED)!=null)
        	this.setEnabled(false);

        if (gt.findProperty(FOCUS_LISTENER)!=null) {
            this.focusChangeListener=true;
        } else {
            this.focusChangeListener=false;}
        
        if (gt.findProperty(ITALIC)!=null) {
            if (gt.findProperty(BOLD)!=null) { // bold & italic
				System.err.println("Font properties should be specified using " + 
					"font(Name,Style,Size) : " + gt.toString());
                //setFont(getFont().deriveFont(Font.BOLD | Font.ITALIC));
            } else { // just italic
				System.err.println("Font properties should be specified using " + 
					"font(Name,Style,Size) : " + gt.toString());
                //setFont(getFont().deriveFont(Font.ITALIC));
            }
        } else if (gt.findProperty(BOLD)!=null) { // just bold
			System.err.println("Font properties should be specified using " + 
					"font(Name,Style,Size) : " + gt.toString());
            //setFont(getFont().deriveFont(Font.BOLD));
        }
        
        HelpManager.registerXJComponentForPopupHelp(this);
    }

    public PrologEngine getEngine(){
        return engine;
    }

    public GUITerm getGT(){
        return gt;
    }
	public void setGT(GUITerm gt){
		this.gt=gt;
		popupMenu = operationsPopup();
	}

    public void refreshGUI(){
        if (!gt.nodeIsVar()) {
            if (gt.isOpaque()) setText(gt.toString());
            else setText(gt.node.toString());
        } else {
        	setText("");
        }
        
        // if tip does not exist set it to be the value of the text
        // (might add a condition to set it only if text length is greater
        // then field size)
        if(gt.tipDescription().equals("")){
            if(getText().equals("")){
                setToolTipText(null);
            } else {
                setToolTipText(getText());
            }
        } 
        
        dirty = false;
    }

    public Dimension getPreferredSize(){
        return gt.getPreferredSize(super.getPreferredSize());
    }

    // DocumentListener methods
    public void insertUpdate(DocumentEvent e){dirty=true; badInputInGUI=false;}
    public void changedUpdate(DocumentEvent e){dirty=true; badInputInGUI=false;}
    public void removeUpdate(DocumentEvent e){dirty=true; badInputInGUI=false;}

    // FocusListener methods
    public void focusGained(FocusEvent e){
        // might change aspect
        // System.out.println("Focus gained:"+gt);
    }

    public void focusLost(FocusEvent e){
    	// System.out.println("Focus lost:"+gt);
        Object oldValue = gt.node;
        loadFromGUI();
        if(gt.node != null){
            if(this.focusChangeListener && !gt.node.equals(oldValue)){
                fireActionPerformed();
            }
        }
    }

    public boolean loadFromGUI(){
        if (badInputInGUI) return false; // already reported error
        if (!dirty) return true;
        Object x = validatedInput();
        //System.out.println("validatedInput=="+x);
        if (x instanceof GUIError) {
            badInputInGUI=true;
            showError(x,this,gt);
            return false;
        }else {
            gt.setNodeValue(x);
            //if (object instanceof AtomicField) ((AtomicField)object).formatGUI();
            dirty = false;
            return true;
        }
    }

    protected Object validatedInput(){
        return gt.coerceNodeText(getText());
    }

    public void setDefaultValue(TermModel dv){
        // use the hacked assignment to avoid the undo treatment
        if (dv!=null) {
            if (!(dv.node instanceof String))
                throw new XJException("Text fields require a String default");
            gt.node = dv.node;
        } else gt.node="";
    }

    public boolean isDirty(){
        return dirty;
    }

    /** Make the field visible and select its text */
    public void selectGUI(Object[] parts){
        GUITerm.typicalAtomicSelect(this,parts);
        selectAll();
    }

    /** This loads data from the GUI into the GUITerm; beware this may get invoked AFTER operations, as ActionListeners
     * are messaged in arbitrary order, so do not rely on this to make operations work with the latest data in the GUI; for
     * that a closer monitoring of return/enter would be needed */
    // comment no longer applies due to a hack: XJAction  invokes loadFromGUI when its action event source is a XJComponent
        /*
        public void actionPerformed(ActionEvent e){
                if (e.getSource()!=this)
                throw new XJException("Inconsistent event source");
                loadFromGUI();
        }*/

    protected void processMouseEvent(MouseEvent e){
    	// System.out.println("processMouseEvent in "+this);
		if (e.isPopupTrigger() && popupMenu.getComponentCount() > 0){
            e.consume();
            if (loadFromGUI()) {
                requestFocus();
                popupMenu.show(this,e.getX(),e.getY());
            }
        } else super.processMouseEvent(e);
    }

    public void activatePopupEditMenu(){
    	ListenerWindow.popupEditMenuFor(this);
    }

	protected JPopupMenu operationsPopup(){
        return operationsPopup(gt,engine,this);
    }

    static JPopupMenu operationsPopup(GUITerm term,PrologEngine engine,Component parent){
        return operationsPopup(term,engine,parent,null);
    }

    static JPopupMenu operationsPopup(GUITerm term,PrologEngine engine,Component parent,Runnable rememberFunctionResults){
        JPopupMenu popup = new JPopupMenu("Operations");
        XJAction[] actions=term.operations(engine,parent,rememberFunctionResults);
        for (int a=0;a<actions.length;a++)
            if (actions[a].isMenuOperation()) popup.add(actions[a].buildMenu());
        return popup;
    }

    protected static void showError(Object x,Component c,GUITerm gt){
        if (!(x instanceof GUIError))
            throw new XJException("bad use of showError:"+x);
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(c,x,
        "Data entry error in "+gt.getTitle(),JOptionPane.ERROR_MESSAGE);
        if (c!=null) {
            c.requestFocus();
            if (c instanceof JTextComponent) ((JTextComponent)c).selectAll();
        }
    }

    public static void adjustToTerm(JTextComponent gui,GUITerm term){
    	boolean shouldJustifyRight = false;
        if(term.findProperty(GUITerm.ATOM) != null){
			FormattedDocument doc = new FormattedDocument();
			gui.setDocument(doc);
		}
		else if(term.findProperty(GUITerm.ATOMUPPER) != null){
			FormattedDocument doc = new FormattedDocument();
			doc.upperOnly = true;
			gui.setDocument(doc);
		}
		else if(term.findProperty(GUITerm.INTEGER) != null){
			WholeNumDocument doc = new WholeNumDocument();
			gui.setDocument(doc);
			shouldJustifyRight= true;
		}
		else if(term.findProperty(GUITerm.FLOAT) != null ||
			term.findProperty(GUITerm.NUMBER) != null){
			FloatDocument doc = new FloatDocument();
			gui.setDocument(doc);
			shouldJustifyRight = true;
		}

        if (term.isOpaque())
        	gui.setEditable(false);

        // if tips exist - set them
        String tip = term.tipDescription();
        if(!tip.equals("")){
            gui.setToolTipText(tip);
        }
        String emptyTip = term.emptyTipDescription();
        if(!emptyTip.equals("")){
            HintTextFieldUI H = new HintTextFieldUI(emptyTip, true);
            H.setColor(Color.GRAY);
            gui.setUI(H);
        }

        if (gui instanceof JTextField)
            ((JTextField)gui).setColumns(term.getCharWidth());

        if (term.findProperty(GUITerm.BORDERLESS)!=null) gui.setBorder(BorderFactory.createEmptyBorder());
        if (term.findProperty(GUITerm.READONLY)!=null) gui.setEditable(false);
		TermModel justify = term.findProperty("justify");
		
		if (gui instanceof JTextField){
			if(justify !=null) {
				String justification = (String)justify.node;
				if(justification.equals("left")){
					((JTextField)gui).setHorizontalAlignment(SwingConstants.LEFT);
				}
				if(justification.equals("right")){
					((JTextField)gui).setHorizontalAlignment(SwingConstants.RIGHT);
				}
				if(justification.equals("center")) {
					((JTextField)gui).setHorizontalAlignment(SwingConstants.CENTER);
				}
			} else if (shouldJustifyRight)
				((JTextField)gui).setHorizontalAlignment(SwingConstants.RIGHT);
		}
   }
    // based on Sun's API spec example
    public static class FormattedDocument extends PlainDocument {
        public boolean upperOnly=false; // too public..

        public void insertString(int offs, String str, AttributeSet a)
        throws BadLocationException {
            if (str == null) return;
            if (upperOnly) {
                char[] upper = str.toCharArray();
                for (int i = 0; i < upper.length; i++) {
                    upper[i] = Character.toUpperCase(upper[i]);
                }
                super.insertString(offs, new String(upper), a);
            } else super.insertString(offs,str,a);
        }
    }

    public static class WholeNumDocument extends PlainDocument {
		public void insertString(int offs, String str, AttributeSet a)
		                					throws BadLocationException {
			char[] source = str.toCharArray();

		    for (int i = 0; i < source.length; i++) {
		    	if (Character.isDigit(source[i]))
		       		;
		       	else if( (source[i]=='-' || source[i]=='+') && (offs == 0))
		       		;
		        else
		            return;
		    } super.insertString(offs, new String(source), a);
	    }
	}


	public static class FloatDocument extends PlainDocument {

	    public void insertString(int offs, String str, AttributeSet a)
	   										throws BadLocationException {
			boolean decimalExists = false;

	    	if (str == null)
	    		return;

			String currentText = this.getText(0,this.getLength());

			if(currentText.indexOf('.')>=0)
				decimalExists = true;

	        char[] number = str.toCharArray();
	        for (int i = 0; i < number.length; i++) {

				if(number[i]=='.' && !decimalExists){
					decimalExists = true;
				}
				else if(Character.isDigit(number[i]))
					;
				else if( (number[i]=='-' || number[i]=='+') && (offs == 0))
					;
				else
					return;
			} super.insertString(offs, new String(number), a);
		}
	}

    // DnDCapable methods:

    public DragGestureListener createDragGestureListener(){
        DragGestureListener dgl = new DragGestureListener(){
            public void dragGestureRecognized( DragGestureEvent event) {
                if (event.getDragAction()!=DnDConstants.ACTION_COPY) return;
                if (!loadFromGUI()) return;
                DragSourceListener dsl = new com.xsb.xj.util.DragSourceAdapter(){
                    public void dragDropEnd(DragSourceDropEvent dsde){
                        //System.out.println("Drop successful ? : "+dsde.getDropSuccess());
                    }
                };
                GUITerm.PathSearch path = new GUITerm.PathSearch();
                TermModel rootTerm = gt.oproot.getTermModel(gt,path);
                TransferableXJSelection txjs = new TransferableXJSelection(rootTerm,path.getPath());
                event.getDragSource().startDrag(event, DragSource.DefaultCopyDrop, txjs, dsl);
                // System.out.println("Started drag");
            }
        };
        return dgl;
    }

    public JComponent getRealJComponent(){
        return this;
    }

    public void destroy() {
    }
    
}
