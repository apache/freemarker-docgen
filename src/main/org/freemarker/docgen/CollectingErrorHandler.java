package org.freemarker.docgen;

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

class CollectingErrorHandler implements ErrorHandler,
        MessageStreamActivityMonitor {
    
    private final int maxErrorMessages;

    private boolean hadNewErrorMessage;
    
    CollectingErrorHandler(int maxErrors) {
        maxErrorMessages = maxErrors;
    }
    
    private final List<String> errors = new LinkedList<String>();
    
    public void warning(SAXParseException spe) {
        // Nop
    }
    
    public void error(SAXParseException spe) throws SAXParseException {
        addError("ERROR", spe);
    }

    public void fatalError(SAXParseException spe) throws SAXParseException {
        addError("FATAL ERROR", spe);
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    private void addError(String type, SAXParseException spe) {
        int ln = errors.size();
        if (ln < maxErrorMessages) {
            hadNewErrorMessage = true;
            errors.add(type + ": " + spe.getMessage() + "\n"
                    + "(at line " + spe.getLineNumber()
                    + ", column " + spe.getColumnNumber()
                    + " of " + spe.getSystemId() + ")");
        } else if (ln == maxErrorMessages) {
            hadNewErrorMessage = true;
            errors.add("TOO MANY ERRORS: Some error messages were discarded.");
        }
    }

    public boolean hadNewErrorMessage() {
        return hadNewErrorMessage;
    }

    public void reset() {
        hadNewErrorMessage = false;
    }
    
}
