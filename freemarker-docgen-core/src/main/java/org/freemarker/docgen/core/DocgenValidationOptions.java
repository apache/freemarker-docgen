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
