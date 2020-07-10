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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Converts SAX events to a W3C DOM {@link Document} while validates
 * the content with another {@link ContentHandler}. (This was introduced
 * to work around various deficiencies with the iso_relax
 * {@code DocumentBuilderFactory}. For an example see:
 * {@link RelaxNGValidator}.)
 */
abstract class ValidatingDOMBuilder implements ContentHandler {

    private final ContentHandler validator;
    private final Document doc;
    private Node parent;
    
    public ValidatingDOMBuilder(ContentHandler validator)
            throws ParserConfigurationException {
        if (validator == null) {
            throw new IllegalArgumentException(
                    "The \"validator\" parameter can't be null.");
        }
        this.validator = validator;
        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .newDocument();
        parent = doc;
    }
    
    /**
     * Should be called after the parsing was completed.
     */
    public Document getDocument() {
        return doc;
    }
    
    public void startElement(
            String ns, String lname, String qname, Attributes atts)
            throws SAXException {
        validator.startElement(ns, lname, qname, atts);
        
        Element e = doc.createElementNS(ns, qname);
        parent.appendChild(e);
        parent = e;
        processElement(e);
        
        for (int i = 0;  i < atts.getLength(); i++) {
            e.setAttributeNS(
                    atts.getURI(i), atts.getQName(i), atts.getValue(i));
        }
    }
    
    public abstract void processElement(Element e);
    
    public void endElement(String ns, String lname, String qname)
            throws SAXException {
        validator.endElement(ns, lname, qname);
        
        parent = parent.getParentNode();
    }
    
    public void characters(char[] buf, int start, int length)
            throws SAXException {
        validator.characters(buf, start, length);
        parent.appendChild(doc.createTextNode(new String(buf, start, length)));
    }
    
    public void ignorableWhitespace(char[] buf, int start, int len) {
        parent.appendChild(doc.createTextNode(new String(buf, start, len)));
    }

    public void endDocument() throws SAXException {
        validator.endDocument();
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        validator.endPrefixMapping(prefix);
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
        validator.processingInstruction(target, data);
    }

    public void setDocumentLocator(Locator locator) {
        validator.setDocumentLocator(locator);
    }

    public void skippedEntity(String name) throws SAXException {
        validator.skippedEntity(name);
        throw new SAXException("Unknown entity: " + name);
    }

    public void startDocument() throws SAXException {
        validator.startDocument();
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        validator.startPrefixMapping(prefix, uri);
    }
    
}
