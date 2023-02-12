package com.xsb.xj.util;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.awt.Desktop;

/**
 * A simple, static class to display a URL in the system, using either the web browser or the appropriate application.
BrowserControl.displayURL("http://www.javaworld.com")
 * 
BrowserControl.displayURL("file://c:\\docs\\index.html")
 * 
BrowserContorl.displayURL("file:///user/joe/index.html");
 * 
 * Note - you must include the url type -- either "http://" or
 * "file://".
 */
public class BrowserControl
{
    /**
     * Display a file in the system browser. If you want to display a
     * file, you must include the absolute path name.
     * TODO: This should probably clean up spaces and backslashes
     *
     * @param url the file's url (the url must start with either "http://" or
     * "file://").
     */
    public static void displayURL(String url){
        Desktop D = Desktop.getDesktop();
		try{
			if (url.startsWith("file://")) D.open(new File(new URI(url)));
			else D.browse(new URI(url));
		} 
		catch (URISyntaxException e){
			throw new XJException("Bad URL:"+url);
		} 
		catch (IOException e){
			throw new XJException("Could not browse:"+url);
		}
    }

    /**
     * Try to determine whether this application is running under Windows
     * or some other platform by examing the "os.name" property.
     *
     * @return true if this application is running under a Windows OS
     */
    public static boolean isWindowsPlatform()
    {
        String os = System.getProperty("os.name");

        if ( os != null && os.startsWith("Windows"))
            return true;
        else
            return false;
    }
    
    public static boolean isNTPlatform(){
        String os = System.getProperty("os.name");

        if ( os != null && (os.startsWith("Windows NT")||os.startsWith("Windows 2000")||os.startsWith("Windows XP")))
            return true;
        else
            return false;
    }

    /**
     * Simple example.
     */
    public static void main(String[] args)
    {
        displayURL("http://www.javaworld.com");
    }

}
