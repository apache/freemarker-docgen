package org.freemarker.docgen;

import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * Command-line "interface" to {@link Transform}.
 */
public final class TransformCommandLine {
    
    // Can't be instantiated
    private TransformCommandLine() {
        // Nop
    }

    public static void main(String[] args)
            throws DocgenException, IOException, SAXException {
        if (args.length != 2) {
            p("Usage: java -jar docgen.jar <srcDir> <dstDir> [options]");
            p("Where options are:");
            p("  None is supported yet.");
            System.exit(-1);
        }
        
        Transform tr = new Transform();
        tr.setSourceDirectory(new File(args[0]));
        tr.setDestinationDirectory(new File(args[1]));
        tr.setPrintProgress(true);
        tr.execute();
    }

    static void p(Object o) {
        System.out.println(o);
    }

}
