/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.freemarker.docgen.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import freemarker.template.utility.StringUtil;

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
        return copyDir(srcDir, destDir, srcDir, Collections.emptySet());
    }

    static int copyDir(
            File srcDir, File destDir, Collection<Pattern> ignoredFilePathPatterns)
            throws IOException {
        return copyDir(srcDir, destDir, srcDir, ignoredFilePathPatterns);
    }
    
    /**
     * @return the number of files copied.
     */
    private static int copyDir(
            File srcDir, File destDir, File srcBaseDir, Collection<Pattern> ignoredFilePathPatterns)
            throws IOException {
        int fileCounter = 0;
        
        destDir = destDir.getAbsoluteFile();
        srcDir = srcDir.getAbsoluteFile();
        String srcBaseDirPath = ensureEndsWithFileSeparator(srcBaseDir.getAbsolutePath());
        
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
                if (!isIgnoredFile(f, srcBaseDirPath, ignoredFilePathPatterns)) {
                    copyFile(f, dest);
                    fileCounter++;
                }
            } else if (f.isDirectory()) {
                fileCounter += copyDir(f, dest, srcBaseDir, ignoredFilePathPatterns);
            } else {
                throw new IOException(
                        "Failed decide if it's a file or a directory: "
                        + f.getAbsolutePath());
            }
        }
        
        return fileCounter;
    }

    private static boolean isIgnoredFile(File f, String srcBaseDirPath, Collection<Pattern> ignoredFilePathPatterns)
            throws IOException {
        if (ignoredFilePathPatterns.isEmpty()) {
            return false;
        }
        
        srcBaseDirPath = ensureEndsWithFileSeparator(srcBaseDirPath);
        
        String filePath = f.getAbsolutePath();
        if (!filePath.startsWith(srcBaseDirPath)) {
            throw new IOException("Unexpected: " + StringUtil.jQuote(filePath) + " doesn't start with "
                    + StringUtil.jQuote(srcBaseDirPath));
        }
        String slashRelFilePath = pathToUnixStyle(filePath.substring(srcBaseDirPath.length() - 1));
        for (Pattern pattern : ignoredFilePathPatterns) {
            if (pattern.matcher(slashRelFilePath).matches()) {
                return true;
            }
        }
        return false;
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

    /**
     * Converts UN*X style path to regular expression. In additional to standard UN*X path meta characters (
     * <code>*</code>, <code>?</code>) it understands <code>**</code>, that is the same as in Ant. It assumes that the
     * matched path always starts with slash (they are absoulte paths to an imaginary base), and uses slash instead of
     * backslash.
     */
    public static Pattern globToRegexp(String text) {
        StringBuilder sb = new StringBuilder();
    
        if (!text.startsWith("/")) {
            text = "/" + text;
        }
        if (text.endsWith("/")) {
            text += "**";
        }
        
        char[] chars = text.toCharArray();
        int ln = chars.length;
        for (int i = 0; i < ln; i++) {
            char c = chars[i];
            if (c == '\\' || c == '^' || c == '.' || c == '$' || c == '|'
                    || c == '(' || c == ')' || c == '[' || c == ']'
                    || c == '+' || c == '{'
                    || c == '}' || c == '@') {
                sb.append('\\');
                sb.append(c);
            } else if (i == 0 && ln > 2
                    && chars[0] == '*' && chars[1] == '*'
                    && chars[2] == '/') {
                sb.append(".*/");
                i += 2;
            } else if (c == '/' && i + 2 < ln
                    && chars[i + 1] == '*' && chars[i + 2] == '*') {
                if (i + 3 == ln) {
                    sb.append("/.*");
                } else {
                    sb.append("(/.*)?");
                }
                i += 2;
            } else if (c == '*') {
                sb.append("[^/]*");
            } else if (c == '?') {
                sb.append("[^/]");
            } else {
                sb.append(c);
            }
        }
    
        return Pattern.compile(sb.toString());
    }
    
    public static String pathToUnixStyle(String path) {
        return path.replace(File.separatorChar, '/');
    }

    public static String ensureEndsWithFileSeparator(String path) {
        return path.length() > 0 && path.charAt(path.length() - 1) == File.separatorChar
                ? path : path + File.separatorChar;
    }

    public static Writer newFileWriter(File outputFile) throws IOException {
        return Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8);
    }
}
