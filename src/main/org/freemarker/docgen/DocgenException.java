package org.freemarker.docgen;

/**
 * Exception that is docgen-specific. 
 */
public class DocgenException extends Exception {
    
    public DocgenException(String message) {
        super(message);
    }
    
    public DocgenException(
            String message, Throwable cause) {
        super(message, cause);
    }
    
}