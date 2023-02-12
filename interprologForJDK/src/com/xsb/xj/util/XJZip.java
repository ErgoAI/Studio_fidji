package com.xsb.xj.util;

import com.declarativa.interprolog.TermModel;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Uses the java zip library to zip and unzip files.
 *
 *@author    Harpreet Singh
 *@version   $Id: XJZip.java,v 1.1 2004/04/06 21:04:45 hsingh Exp $
 */
public class XJZip {

    public static boolean zip(XJZipEntry[] zipEntries, String outFileName) {
        // Create a buffer for reading the files
        byte[] buf       = new byte[1024];
        boolean success  = false;

        try {
            ZipOutputStream out  = new ZipOutputStream(new FileOutputStream(outFileName));

            for(int i = 0; zipEntries != null && i < zipEntries.length; i++) {
                String baseDir  = zipEntries[i].getParent();
                String[] files  = zipEntries[i].getChildren();

                for(int j = 0; files != null && j < files.length; j++) {
                    File file  = new File(baseDir + File.separator + files[j]);

                    if(!file.isDirectory()) {
                        FileInputStream in  = new FileInputStream(file.getAbsolutePath());
                        ZipEntry ze1        = new ZipEntry(files[j]);

                        out.putNextEntry(ze1);
                        // Transfer bytes from the file to the ZIP file
                        int len;
                        while((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }

                        in.close();
                    } else {
                        ZipEntry ze  = new ZipEntry(files[j]);
                        out.putNextEntry(ze);
                    }
                    out.closeEntry();
                }
            }
            out.close();
            success = true;

        } catch(IOException ioe) {
            System.err.println("ERROR:com.xsb.xj.util.XJZip.zip:" + outFileName);
            ioe.printStackTrace();
            success = false;
        }

        return success;
    }

    public static boolean unzip(String inFileName, String outDir) {
        boolean success      = false;
        Enumeration<? extends ZipEntry> entries;
        ZipFile zipFile;

        try {
            //create output directory
            (new File(outDir)).mkdir();

            zipFile = new ZipFile(inFileName);

            entries = zipFile.entries();
            while(entries.hasMoreElements()) {
                ZipEntry entry  = entries.nextElement();
                File fout       = new File(outDir + File.separator + entry.getName());

                if(entry.isDirectory()) {
                    // Assume directories are stored parents first then children.
                    // This is not robust

                    if(!fout.mkdirs()) {
                        System.err.println("failed to make dir:" + fout);
                    }
                } else {
                    if(fout.getAbsoluteFile().createNewFile()) {
                        copyInputStream(zipFile.getInputStream(entry),
                            new BufferedOutputStream(new FileOutputStream(fout)));
                    }
                }
            }

            zipFile.close();
            success = true;

        } catch(IOException ioe) {
            System.err.println("ERROR:com.xsb.xj.util.XJZip.unzip:" + inFileName);
            ioe.printStackTrace();
            success = false;
        }

        return success;
    }

    public static final void copyInputStream(InputStream in, OutputStream out)
         throws IOException {

        byte[] buffer  = new byte[1024];
        int len;

        while((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }

        in.close();
        out.close();
    }

    public static boolean xjZip(TermModel[] filenames, String outFileName) {
        XJZipEntry[] zipEntries  = tmToXJZe(filenames);
        return zip(zipEntries, outFileName);
    }

    public static boolean xjUnzip(String zipFileName, String outDirName) {
        return unzip(zipFileName, outDirName);
    }

    private static XJZipEntry[] tmToXJZe(TermModel[] tmMap) {
        XJZipEntry[] zipEntries  = null;

        if(tmMap != null) {
            zipEntries = new XJZipEntry[tmMap.length];
        }

        for(int i = 0; tmMap != null && i < tmMap.length; i++) {
            TermModel term     = tmMap[i];
            String[] children  = null;
            if(term.getChildCount() == 2) {
                children = tmToString(TermModel.flatList((TermModel) term.getChild(1)));
                zipEntries[i] = new XJZipEntry((String) term.getChild(0).toString(), children);
            }
        }
        return zipEntries;
    }

    private static String[] tmToString(TermModel[] tmStrings) {
        String[] strings  = null;

        if(tmStrings != null) {
            strings = new String[tmStrings.length];
        }

        for(int i = 0; tmStrings != null && i < tmStrings.length; i++) {
            strings[i] = tmStrings[i].toString();
        }

        return strings;
    }

    public static void main(String[] args) {
        XJZipEntry[] zipEntries  = new XJZipEntry[1];
        zipEntries[0] = new XJZipEntry("C:\\", new String[]{"foo/", "foo/bar1/", "foo/bar2/", "foo/bar1/foo.txt", "foo/bar2/foo.txt"});

        zip(zipEntries, "C:\\foo_test.zip");
        unzip("C:\\foo_test.zip", "C:\\foo_unzipped");
    }
}

/**
 * Description of the Class
 *
 *@author    Harpreet Singh
 *@version   $Id: XJZip.java,v 1.1 2004/04/06 21:04:45 hsingh Exp $
 */
class XJZipEntry {

    String[] _children;
    String _parent;

    public XJZipEntry(String parent, String[] children) {
        _parent = parent;
        _children = children;
    }

    public String getParent() {
        return _parent;
    }

    public String[] getChildren() {
        return _children;
    }

}
