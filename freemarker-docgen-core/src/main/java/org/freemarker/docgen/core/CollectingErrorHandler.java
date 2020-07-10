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
