package org.freemarker.docgen;

/**
 * Used in command line tools to signal the need for a {@link System#exit(int)} call (after the given message was
 * printed).
 */
class CommandLineExitException extends Exception {

    private final int exitCode;
    private final String message;

    public CommandLineExitException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
        this.message = message;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getMessage() {
        return message;
    }

}