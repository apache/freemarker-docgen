package org.freemarker.docgen;

/**
 * Used for monitoring the activity of an error stream.
 */
interface MessageStreamActivityMonitor {

    void reset();
    
    boolean hadNewErrorMessage();
    
}
