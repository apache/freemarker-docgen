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

import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

final class ValidatingDOMBuilderWithLocations extends ValidatingDOMBuilder {
    
    public static final String KEY_LOCATION = "docgen_location";

    private final Set<String> elements;
    private final String namespaceURI;
    
    private Locator locator;
    
    public ValidatingDOMBuilderWithLocations(
            ContentHandler validator,
            String namespaceURI, Set<String> elements)
            throws ParserConfigurationException {
        super(validator);
        this.namespaceURI = namespaceURI != null ? namespaceURI : "";
        this.elements = elements;
    }
    
    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        super.setDocumentLocator(locator);
    }

    @Override
    public void processElement(Element elem) {
        String nsURI = elem.getNamespaceURI();
        if (elements.contains(elem.getLocalName())
                && (nsURI != null ? nsURI : "").equals(namespaceURI)) {
            Location loc = new Location(
                    locator.getSystemId(),
                    locator.getLineNumber(), locator.getColumnNumber());
            elem.setUserData(KEY_LOCATION, loc, null);
        }
    }

    public static class Location {

        private final String systemId;
        private final int line;
        private final int column;
        
        public Location(String file, int row, int column) {
            this.systemId = file;
            this.line = row;
            this.column = column;
        }
        
        public String getSystemId() {
            return systemId;
        }
        
        public int getLine() {
            return line;
        }
        
        public int getColumn() {
            return column;
        }
        
    }
    
}
