package org.freemarker.docgen;

/**
 * Exception that should not occur unless there is a bug around. 
 */
class BugException extends RuntimeException {
    
    public BugException(String message) {
        super(message);
    }
    
    public BugException(
            String message, Throwable cause) {
        super(message, cause);
    }
    
    public BugException(Throwable cause) {
        super("Unexpected error (see cause exception)", cause);
    }
    
}