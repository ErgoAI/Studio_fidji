/* 
** Author(s): Miguel Calejo
** Contact:   info@interprolog.com, http://www.interprolog.com
** Copyright (C) InterProlog Consulting / Renting Point, Portugal, 2016
** Use and distribution, without any warranties, under the terms of the 
** GNU Library General Public License, readable in http://www.fsf.org/copyleft/lgpl.html
*/
package com.declarativa.fiji.reporting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import com.declarativa.fiji.FijiPreferences;

/** Mostly class methods to generate HTML reports based on the visjs.org library */
public class VisjsUtils {
	static final String VISJS_DIR=".fidjiVisjs";
	static final String VISJS = "vis.min.js";
	static final String VISCSS = "vis.min.css";
	static final String VIS_TIMELINE_TEMPLATE = "timelineTemplate.html";
	/**
	 * Obtain the vis.js directory, unpacking it from our Studio jar if necessary (namely if Studio's file is newer, e.g. was rebuilt)
	 * @return path of directory where vis.min.js and dependencies are
	 * @throws IOException 
	 */
	public static File getVisjsBase() throws IOException{
		File Vbase = new File(FijiPreferences.getFidjiDir(),VISJS_DIR);
		if (!Vbase.exists())
			Vbase.mkdirs();
		long lastmodified = FijiPreferences.getClassesFile().lastModified();
		File visjs = new File(Vbase,VISJS);
		if (!visjs.exists() || visjs.lastModified()<lastmodified){
			dumpTo(VisjsUtils.class.getResourceAsStream(VISJS),visjs);
			dumpTo(VisjsUtils.class.getResourceAsStream(VISCSS),new File(Vbase,VISCSS));
			dumpTo(VisjsUtils.class.getResourceAsStream(VIS_TIMELINE_TEMPLATE),new File(Vbase,VIS_TIMELINE_TEMPLATE));
		}
		return Vbase;
	}
	private static void dumpTo(InputStream is,File output) throws IOException{
	    byte[] buffer = new byte[512]; int len;
	    FileOutputStream fos = new FileOutputStream(output);
	    while ((len = is.read(buffer, 0, buffer.length)) != -1) 
			fos.write(buffer, 0, len);
	    fos.close(); is.close();
	}
	public static File instantiateTemplate(File template,HashMap<String,String> bindings,File instantiated) throws IOException{
		StringBuilder sb = readWholeFile(template);
	    for(String var:bindings.keySet())
	    	replaceall(sb,var,bindings.get(var));
	    FileWriter fw = new FileWriter(instantiated);
	    fw.write(sb.toString());
	    fw.close();
	    return instantiated;
	}
	protected static StringBuilder readWholeFile(File F)
			throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(F);
	    StringBuilder sb = new StringBuilder((int)F.length());
	    char[] buffer = new char[512]; int len;
	    while ((len = fr.read(buffer, 0, buffer.length)) != -1) 
			sb.append(buffer, 0, len);
	    fr.close();
		return sb;
	}
	public static void replaceall(StringBuilder sb,String found,String replacement){
		if (replacement==null)
			throw new RuntimeException("Null replacement for:"+found);
		int pos = sb.indexOf(found);
		while(pos!=-1){
			sb.replace(pos, pos+found.length(), replacement);
			pos = sb.indexOf(found);
		}
	}
	/** Unpacks template if necessary */
	static File getVisTimeLineTemplate() throws IOException{
		return new File(getVisjsBase(),VIS_TIMELINE_TEMPLATE);
	}
	/** Generates a report temporary file into the same directory of JSONfilename. Template variable names are hard coded here. */
	public static String generateTimeline(String title,String subtitle,String JSONfilename) throws IOException{
		return generateTimeline(title,subtitle,JSONfilename,File.createTempFile("temp", ".html", new File(JSONfilename).getParentFile()).getAbsolutePath());
	}
	/** Generates a report file. Template variable names are hard coded here. */
	/**
	 * @param title
	 * @param subtitle
	 * @param JSONfilename Existing file with JSON objects
	 * @param outputFile Desired output filename
	 * @return returns outputFile
	 * @throws IOException
	 */
	public static String generateTimeline(String title,String subtitle,String JSONfilename,String outputFile) throws IOException{
		File JF = new File(JSONfilename);
	    File instantiated = new File(outputFile);
	    HashMap<String,String> bindings = new HashMap<String,String>();
	    bindings.put("$VISJSBASE$", getVisjsBase().toString()+File.separator);
	    bindings.put("$TITLE$", title);
	    bindings.put("$SUBTITLE$", subtitle);
	    bindings.put("$JSON_PAYLOAD$", readWholeFile(JF).toString());
	    return instantiateTemplate(getVisTimeLineTemplate(),bindings,instantiated).getAbsolutePath();
	}

}
