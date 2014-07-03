package org.freemarker.docgen;

/**
 * Stores validation restrictions rule options; they can be different for each
 * book.
 */
class DocgenValidationOptions {

    private boolean programlistingRequiresRole;

    private boolean programlistingRequiresLanguage;
    
    private boolean outputFilesCanUseAutoID;
    
    private int maximumProgramlistingWidth = Integer.MAX_VALUE;

    public boolean getProgramlistingRequiresLanguage() {
        return programlistingRequiresLanguage;
    }

    /**
     * Specifies if a "programlisting" element must have "language" attribute;
     * defaults to {@code false}.
     */
    public void setProgramlistingRequiresLanguage(
            boolean programlistingRequiresLanguage) {
        this.programlistingRequiresLanguage = programlistingRequiresLanguage;
    }
    
    public boolean getProgramlistingRequiresRole() {
        return programlistingRequiresRole;
    }

    /**
     * Specifies if a "programlisting" element must have "role" attribute;
     * defaults to {@code false}.
     */
    public void setProgramlistingRequiresRole(
            boolean programlistingRequiresRole) {
        this.programlistingRequiresRole = programlistingRequiresRole;
    }

    public boolean getOutputFilesCanUseAutoID() {
        return outputFilesCanUseAutoID;
    }

    /**
     * Specifies if files for which a separate output file will be created
     * can have an automatically assigned <tt>xml:id</tt>; defaults to
     * {@code false}. 
     */
    public void setOutputFilesCanUseAutoID(boolean outputFilesCanUseAutoID) {
        this.outputFilesCanUseAutoID = outputFilesCanUseAutoID;
    }
    
    public int getMaximumProgramlistingWidth() {
        return maximumProgramlistingWidth;
    }

    /**
     * Specifies the maximum length of lines in <tt>programlistings</tt>-s;  
     * defaults to {@link Integer#MAX_VALUE} (means no checking). 
     */
    public void setMaximumProgramlistingWidth(int maximumProgramlistingWidth) {
        this.maximumProgramlistingWidth = maximumProgramlistingWidth;
    }
    
}
