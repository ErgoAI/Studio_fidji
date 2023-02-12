/* 
** Author(s): Miguel Calejo
** Contact:   interprolog@declarativa.com, http://www.declarativa.com
** Copyright (C) Declarativa, Portugal, 2013
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.fiji;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.DocumentRange;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level;

import com.declarativa.interprolog.AbstractPrologEngine;
import com.declarativa.interprolog.SubprocessEngine;
import com.declarativa.interprolog.TermModel;

public class LogicParser extends AbstractParser{
	protected String extension;
	protected File tempDirectory=null;
	protected File tempFile=null;
	protected AbstractPrologEngine engine;
	protected LogicProgramEditor lpe;
	
	Level errorLevel = Level.INFO;
	
	protected HashMap<String,Inclusion> includes = new HashMap<String,Inclusion>() ;
	static final char[] INCLUDE = "#include ".toCharArray();
	
	public Set<File> getIncluded(){
		HashSet<File> R = new HashSet<File>();
		for (Inclusion i:includes.values())
			R.add(i.original);
		return R;
	}
	
	/** the file beingEdited should NOT be changed by this object */
	protected LogicParser(AbstractPrologEngine engine,LogicProgramEditor lpe){
		this.engine=engine;
		if (engine instanceof SubprocessEngine)
			// hack to tolerate some uncaught but irrelevant errors which would otherwise
			// anniquilate all warnings
			((SubprocessEngine)engine).setDetectErrorMessages(false);
		this.lpe=lpe;
		extension = LogicProgramEditor.dotExtension(lpe.getFileName());
		prepareTempFile();
	}
	protected void prepareTempFile(){
		try{
			tempDirectory = File.createTempFile("FJtempDir_","");
			if (!tempDirectory.delete() || ! tempDirectory.mkdir())
				throw new IOException("Could not create temp parser directory");
			//tempFile = File.createTempFile("FJ_",extension,tempDirectory);
			tempFile = new File(tempDirectory,new File(lpe.getFileName()).getName());
	        Runtime.getRuntime().addShutdownHook(new Thread() {
	            @Override
	            public void run() {
	              if (!engine.isDebug())
	            	  AbstractPrologEngine.deleteAll(tempDirectory);
	            }
	       });
			
		}
		catch(IOException e){throw new RuntimeException("Could not create temporary parser file:"+e);}
	}
	
	protected TermModel[] getNoticeTerms(String filename){
		// long start = System.currentTimeMillis();
		String G = "cd(CWD), stringArraytoList(UserModFilesArray,UserModFiles), fjVerifyAndLoadUsermodXref(UserModFiles), stringArraytoList(DirsArray,Dirs), fjUpdateLibraryDirs(Dirs),"+
			"recoverTermModelArray(InsertedOpsArray,InsertedOps), fjMayApplyOpDeltas(InsertedOps)";
		//System.out.println(G);
		boolean rc = engine.deterministicGoal(G,
			"[UserModFilesArray,DirsArray,InsertedOpsArray,string(CWD)]",
			new Object[]{lpe.listener.userModFiles,lpe.listener.libraryDirectories,lpe.listener.insertedOps,lpe.listener.currentPrologDirectory}
		);
		if (!rc){ // System.out.println("Loaded usermod xrefs in (mS) "+(System.currentTimeMillis()-start));
			System.err.println("fjVerifyAndLoadUsermodXref failed! userModFilesList:");
			System.err.println(lpe.listener.userModFilesList());
		}
		FijiPreferences.ImpatientBugReporter ibr = new FijiPreferences.ImpatientBugReporter(engine);
		int dot = lpe.getFileName().lastIndexOf('.');
		String userFilePrefix = lpe.getFileName().substring(0,dot).replace("\\\\", "/");
		// G = "checkXSBPrologFile('"+lpe.getFileName()+"','"+userFilePrefix+"','"+ filename +"',Notices), buildTermModelArray(Notices,SM)";
		G = "checkXSBPrologFile(AbsoluteFilename,UserFilePrefix,Filename,Notices), buildTermModelArray(Notices,SM)";
		Object[] bindings = engine.deterministicGoal( G,
				"[string(AbsoluteFilename),string(UserFilePrefix),string(Filename)]",
				new Object[]{lpe.getFileName(),userFilePrefix,filename},"[SM]");
		ibr.dontWorry();
		if (bindings!=null) {
			return (TermModel[])bindings[0];
		} else return null;
	}

	public ParseResult parse(RSyntaxDocument doc, String style){
		long start = System.currentTimeMillis();
		DefaultParseResult result = new DefaultParseResult(this);
		Exception exception = null;
		try{
			FijiSubprocessEngineWindow.setWaitCursor(lpe);
			updateIncludes(doc);	
			//System.out.println("updateIncludes took "+ (System.currentTimeMillis()-start));	
			String text = doc.getText(0, doc.getLength() );
			FileWriter fw = new FileWriter(tempFile);
			fw.write(text,0,text.length());
			fw.close();
			String filename = engine.unescapedFilePath(tempFile.getAbsolutePath());
			String myShortFilename = new File(filename).getName();
			
			if (engine.isAvailable()){
				Level oldErrorLevel = errorLevel;
				errorLevel = ParserNotice.Level.INFO;
				TermModel[] noticeTerms = getNoticeTerms(filename); // Later we may optimize by looking at firstOffsetModded in ParserManager
				if (noticeTerms!=null){
					HashSet<String> warningsElsewhere = new HashSet<String>();
					HashSet<String> errorsElsewhere = new HashSet<String>();
					// each term (Flora/Ergo): notice(warning_or_error,M,p(FirstLine,FirstChar,LastLine,LastChar),null_or_p2,ShortFilename(may be 'null'))...
					// ...Prolog: notice(warning_or_error,MessageTerm,Position)   Position may be end_of_file (if unknown)
					for (int i=0; i<noticeTerms.length; i++){
						TermModel N = noticeTerms[i];
						ParserNotice.Level level = ParserNotice.Level.INFO;
						// System.out.println("N=="+N);
						if (N.getChild(0).toString().equals("warning")) {
							level = ParserNotice.Level.WARNING;
						}
						else if (N.getChild(0).toString().equals("error")) {
							level = ParserNotice.Level.ERROR;
						}
						if (level.isEqualToOrWorseThan(errorLevel))
							errorLevel = level;
					
						String M = LogicProgramEditor.termToMessage(N.children[1]);					
					
						if (N.getChildCount()==5 && !(N.children[4].equals("null"))){
							String shortFilename = N.children[4].toString();
							if (!myShortFilename.equals(shortFilename)){
								if (level == ParserNotice.Level.ERROR)
									errorsElsewhere.add(shortFilename);
								else if (level == ParserNotice.Level.WARNING)
									warningsElsewhere.add(shortFilename);
								continue;
							}
						}
						
						DocumentRange range = LogicProgramEditor.termToRange(N,doc);
						if (range==null){
							System.err.println("Skipping parser notice:"+N);
							continue;
						}
						int tk1beginLine = doc.getDefaultRootElement().getElementIndex(range.getStartOffset());
						
						Token firstInNotice = doc.getTokenListForLine(tk1beginLine);
						if (shouldReport(firstInNotice)) {
							DefaultParserNotice notice = 
								new DefaultParserNotice(this,M, tk1beginLine, range.getStartOffset(), range.getEndOffset()-range.getStartOffset()+1);
							notice.setLevel(level);
							result.addNotice(notice);
							//System.out.println("Added "+notice+ " of level "+level + " and length "+ (offsetEnd-offsetBegin));
						}
					}
					long duration = System.currentTimeMillis()-start;
					// System.out.println(duration+ " mS");
					result.setParseTime(duration);
					result.setParsedLines(0,doc.getDefaultRootElement().getElementCount() );
					
					for (String F:warningsElsewhere){
						Inclusion inc = findInclusionOf(F);
						if (inc!=null){
							int line = doc.getDefaultRootElement().getElementIndex(inc.charPosition);
							Element lineElement = doc.getDefaultRootElement().getElement(line);
							int lineLength = lineElement.getEndOffset()-lineElement.getStartOffset();
							DefaultParserNotice notice = new DefaultParserNotice(this,"Warnings in included file", line, inc.charPosition, lineLength);
							notice.setLevel(ParserNotice.Level.WARNING);
							result.addNotice(notice);
						} else System.err.println("Could not find in included:"+F);
					}
					for (String F:errorsElsewhere){
						Inclusion inc = findInclusionOf(F);
						if (inc!=null){
							int line = doc.getDefaultRootElement().getElementIndex(inc.charPosition);
							Element lineElement = doc.getDefaultRootElement().getElement(line);
							int lineLength = lineElement.getEndOffset()-lineElement.getStartOffset();
							DefaultParserNotice notice = new DefaultParserNotice(this,"Errors in included file", line, inc.charPosition, lineLength);
							notice.setLevel(ParserNotice.Level.ERROR);
							result.addNotice(notice);
						} else System.err.println("Could not find in included:"+F);
					}

					if (errorLevel!=oldErrorLevel)
						lpe.editor.firePropertyChange(LogicProgramEditor.MyTextEditorPane.ERROR_LEVEL_PROPERTY, oldErrorLevel, errorLevel);
				} else exception = new Exception("Failed to check file");
			}
		} catch(Exception ex){exception=ex; System.err.println("ex:"+ex); ex.printStackTrace();}
		finally{FijiSubprocessEngineWindow.restoreCursor(lpe); }

		if (exception!=null) result.setError(exception);
		//System.out.println("Returning "+result);
		return result;
	}
	
	protected boolean shouldReport(Token firstInNotice){
		return firstInNotice.getType()!=Token.PREPROCESSOR;
	}
	
	/** This assumes included files are already saved. A later version under edition is ignored */
	protected void updateIncludes(RSyntaxDocument doc){
		File originalDir = new File(lpe.editor.getFileFullPath()).getParentFile();
		includes.clear();
		Token initial = lpe.editor.getTokenListForLine(0); 
		Iterator<Token> it = LogicProgramEditor.getNextTokens(doc, initial, true, false);
		Token current = null;
		while (it.hasNext()){
			if (current==null) 
				current = it.next();
			current = checkForInclusions(current,it,originalDir);
		}
		//System.out.println("includes:\n"+includes);
		copyIncludedToTemp();
	}

	/**
	 * @param T A Token
	 * @param it Iterator over all remaining tokens, if one must consume more (no lookahead available)
	 * @param originalDir The directory where the main (includer) file currently is
	 * @return The next token to check, if tokens were consumed here besides T; return null otherwise
	 */
	protected Token checkForInclusions(Token T, Iterator<Token> it, File originalDir) {
		if (T.getType()==Token.PREPROCESSOR && T.startsWith(INCLUDE)){
			String included = T.getLexeme().substring(INCLUDE.length).trim();
			if (included.startsWith("\""))
				included = included.substring(1,included.length()-2);
			
			File I = new File(included);
			File original = I.isAbsolute() ? I : new File(originalDir,included);
			if (shouldCopyIncluded() && original.exists())
				includes.put(included,new Inclusion(T.getOffset(),included, I.isAbsolute(), original, new File(tempDirectory,included)));
			
		}
		return null;
	}

	protected void copyIncludedToTemp() {
		for (Inclusion I : includes.values()){
			if (I.absolute)
				continue; // no need to copy
			long lastOriginal = I.original.lastModified();
			long lastTemporary = I.temporary.lastModified();
			if (lastOriginal>0 && lastTemporary<lastOriginal){
				// There is an original include and we need to copy it:
				// System.out.println("copying "+I.original);
				try{
					FileInputStream fis = new FileInputStream(I.original);
					FileOutputStream fos = new FileOutputStream(I.temporary);
					byte[] buf = new byte[1024];
					int len;
					while ((len = fis.read(buf)) > 0) {
						fos.write(buf, 0, len);
					}
					fis.close();
					fos.close();
				} catch (IOException ex){
					System.err.println("Could not prepare inclusion from file:"+lpe.editor.getFileName()+"\n"+I+"\n"+ex);
				}
			}
		}
	}
	
	/** Whether included files will be copied to the auxiliary engine's temp directpry for a full analysis */
	protected boolean shouldCopyIncluded(){
		return true;
	}
	
	Inclusion findInclusionOf(String shortFilename){
		for (Inclusion i:includes.values())
			if (i.isIncluded(shortFilename))
				return i;
		return null;
	}
	
	public static class Inclusion{
		String included;
		File original, temporary;
		int charPosition;
		boolean absolute;
		
		/**
		 * @param charPosition
		 * @param included
		 * @param absolute whether the included file name is an absolute path
		 * @param original
		 * @param temporary
		 */
		public Inclusion(int charPosition, String included,boolean absolute,File original,File temporary){
			this.included=included; this.original=original; this.temporary=temporary;
			this.charPosition = charPosition; this.absolute=absolute;
		}
		public boolean isIncluded(String shortFilename) {
			if (!absolute)
				return shortFilename.equals(included);
			else
				return new File(included).getName().equals(shortFilename);
				// actually should be same as original, but...
		}
		public String toString(){
			return included + "|" + original + "|" + temporary;
		}
	}
}
