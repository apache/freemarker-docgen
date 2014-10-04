package org.freemarker.docgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;

final class FileUtil {
    
    private static final int READ_BUFFER_SIZE = 4096;

    // Can't be instantiated
    private FileUtil() {
        // nop
    }
    
    private static final int COPY_BUFFER_SIZE = 64 * 1024; 

    /**
     * Copies a class-loader resource into a plain file of the same name.
     * The path of the source resource is calculated as
     * {@link srcDirClass}'s package path +
     * {@link srcDirFurtherPath} +
     * {@link srcRelativePath}.
     * The path of the destination file is calculated as
     * {@link destDir} + {@link srcRelativePath}. 
     *    
     * @param srcBaseDir Possibly {@code null}.
     */
    static void copyResourceIntoFile(
            Class<?> srcBaseClass, String srcBaseDir,
            String srcRelativePath, File destDir)
            throws IOException {
        File dstFile = new File(
                destDir,
                srcRelativePath.replace('/', File.separatorChar));

        File curDestDir = dstFile.getParentFile();
        if (!curDestDir.isDirectory() && !curDestDir.mkdirs()) {
            throw new IOException("Failed to create destination directory: "
                    + curDestDir.getAbsolutePath());
        }

        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        String finalResourcePath;
        if (srcBaseDir == null || srcBaseDir.length() == 0) {
            finalResourcePath = srcRelativePath;
        } else {
            finalResourcePath = srcBaseDir + "/" + srcRelativePath;
        }
        InputStream in = srcBaseClass.getResourceAsStream(finalResourcePath);
        if (in == null) {
            throw new IOException("Failed to open class-loader resource: "
                    + finalResourcePath + " relatively to "
                    + Transform.class.getPackage().getName());
        }
        try {
            OutputStream out = new FileOutputStream(dstFile);
            try {
                int ln;
                while ((ln = in.read(buffer)) != -1) {
                    out.write(buffer, 0, ln);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    static int copyDir(
            File srcDir, File destDir) throws IOException {
        return copyDir(srcDir, destDir, false);
    }
    
    
    /**
     * @return the number of files copied.
     */
    static int copyDir(
            File srcDir, File destDir, boolean skipTopLevelXMLs)
            throws IOException {
        int fileCounter = 0;
        
        destDir = destDir.getAbsoluteFile();
        srcDir = srcDir.getAbsoluteFile();
        
        if (!destDir.isDirectory()) {
            if (destDir.exists()) {
                throw new IOException("Can't create directory, because a "
                        + "file with the same name already exists: "
                        + destDir.getAbsolutePath());
            }
            if (!destDir.mkdir()) {
                throw new IOException("Failed to create directory: "
                        + destDir.getAbsolutePath());
            }
        }
        
        File[] ls = srcDir.listFiles();
        if (ls == null) {
            throw new IOException("Failed to list directory: "
                    + srcDir.getAbsolutePath());
        }
        for (File f : srcDir.listFiles()) {
            String fName = f.getName(); 
            if (isUsualIgnorableFileOrDirectory(fName) || isDocgenFile(fName)) {
                continue;
            }
            File dest = new File(destDir, fName);
            if (f.isFile()) {
                if (!(skipTopLevelXMLs
                        && fName.toLowerCase().endsWith(".xml"))) {
                    copyFile(f, dest);
                    fileCounter++;
                }
            } else if (f.isDirectory()) {
                fileCounter += copyDir(f, dest);
            } else {
                throw new IOException(
                        "Failed decide if it's a file or a directory: "
                        + f.getAbsolutePath());
            }
        }
        
        return fileCounter;
    }

    private static void copyFile(File src, File dst) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        InputStream in = new FileInputStream(src);
        try {
            long srcLMD = 0L;
            srcLMD = src.lastModified();
            if (srcLMD == 0) {
                throw new IOException("Failed to get the last modification "
                        + "time of " + src.getAbsolutePath());
            }
            OutputStream out = new FileOutputStream(dst);
            try {
                int ln;
                while ((ln = in.read(buffer)) != -1) {
                    out.write(buffer, 0, ln);
                }
            } finally {
                out.close();
            }
            if (srcLMD != 0L) {
                if (!dst.setLastModified(srcLMD)) {
                    throw new IOException(
                            "Failed to set last-modification-date for: "
                            + dst.getAbsolutePath());
                }
            }
        } finally {
            in.close();
        }
    }
    
    static boolean isDocgenFile(String fName) {
        fName = fName.toLowerCase();
        return fName.startsWith("docgen-") || fName.startsWith("docgen.")
                || fName.equals("docgen");
    }
    
    static boolean isUsualIgnorableFileOrDirectory(String fName) {
        fName = fName.toLowerCase();
        int i = fName.lastIndexOf(".");
        
        String fExt;
        if (i == -1) {
            fExt = "";
        } else {
            fExt = fName.substring(i + 1);
        }
        
        // CVS files:
        if (fName.equals(".cvsignore")  
                || fName.equals("cvs")
                || (fName.length() > 2 && fName.startsWith(".#"))) {
            return true;
        }
        
        // SVN files:
        if (fName.equals(".svn")) {
            return true;
        }

        // Temporary/backup files:
        if (
                (
                    fExt.equals("bak")
                    || fExt.equals("lock")
                    || fExt.startsWith("~"))
                || (fName.length() > 2 && (
                    (fName.startsWith("#") && fName.endsWith("#"))
                    || (fName.startsWith("%") && fName.endsWith("%"))
                    || fName.startsWith("._")))
                || (fName.length() > 1 && (
                    fName.endsWith("~")
                    || fName.startsWith("~")))
                ) {
            return true;
        }
        
        return false;
    }

    public static String loadString(File f, Charset charset) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            return loadString(in, charset);
        } finally {
            in.close();
        }
    }
    
    public static String loadString(InputStream in, Charset charset)
            throws IOException {
        Reader r = new InputStreamReader(in, charset);
        StringBuilder sb = new StringBuilder(256);
        try {
            char[] buf = new char[READ_BUFFER_SIZE];
            int ln;
            while ((ln = r.read(buf)) != -1) {
                sb.append(buf, 0, ln);
            }
        } finally {
            r.close();
        }
        return sb.toString();
    }

}
