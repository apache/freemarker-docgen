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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.freemarker.docgen.core.ValidatingDOMBuilderWithLocations.Location;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

final class XMLUtil {

    // Can't be instantiated
    private XMLUtil() {
        // Nop
    }
    
    private static Boolean cachedIsJingAvilable;
    
    static boolean isJingAvilable() {
        if (cachedIsJingAvilable == null) {
            ClassLoader cl = Transform.class.getClassLoader();
            cachedIsJingAvilable = Boolean.TRUE;
            try {
                cl.loadClass("com.thaiopensource.validate.Schema");
            } catch (ClassNotFoundException e) {
                cachedIsJingAvilable = Boolean.FALSE;
            }
        }
        return cachedIsJingAvilable.booleanValue();
    }

    static SAXParserFactory newSAXParserFactory()
            throws SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance(); 
        spf.setNamespaceAware(true);
        try {
            spf.setXIncludeAware(true);
        } catch (UnsupportedOperationException e) {
            throw new SAXException(
                    "The default SAX parser (XML) implementation doesn't "
                    + "support XInclude. Updating your Java installation will "
                    + "possibly fix this.");
        }
        spf.setValidating(false);  // since we attach a schema explicitly
        return spf;
    }

    static DocumentBuilderFactory newDocumentBuilderFactory()
            throws SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
        dbf.setNamespaceAware(true);
        try {
            dbf.setXIncludeAware(true);
        } catch (UnsupportedOperationException e) {
            throw new SAXException(
                    "The default Document builder (XML) implementation doesn't "
                    + "support XInclude. Updating your Java installation will "
                    + "possibly fix this.");
        }
        dbf.setValidating(false);  // since we attach a schema explicitly
        return dbf;
    }
    
    static Document loadDocBook5XML(File bookFile, boolean validate,
            DocgenValidationOptions validationOps, DocgenLogger logger)
            throws SAXException, IOException, DocgenException {
        logger.info("Loading " + bookFile.getAbsolutePath() + "...");
        if (validate) {
            if (!isJingAvilable()) {
                throw new DocgenException("Jing classes are reqired for the "
                        + "validation but couldn't be found.");
            }
            
            // Reflection is used to prevent static linking to Jing.
            Method vm;
            try {
                vm = Transform.class.getClassLoader().loadClass(
                        "org.freemarker.docgen.core.RelaxNGValidator")
                            .getMethod("load", new Class[] {
                                    File.class,
                                    DocgenValidationOptions.class});
            } catch (Throwable e) {
                throw new BugException(
                        "Failed to get the "
                        + "org.freemarker.docgen.RelaxNGValidator.validate "
                        + "method (see cause exception).",
                        e);
            }
            try {
                return (Document) vm.invoke(null, bookFile, validationOps);
            } catch (InvocationTargetException e) {
                Throwable te = e.getTargetException();
                if (te instanceof SAXException) {
                    throw (SAXException) te;
                }
                if (te instanceof IOException) {
                    throw (IOException) te;
                }
                throw new BugException(
                        "Failed to setup Relax NG validation "
                        + "(see cause exception).", e);
            } catch (Throwable e) {
                throw new BugException(
                        "Failed to invoke docgen.RelaxNGValidator method "
                        + "(see cause exception).", e);
            }
        } else {
            logger.info("Validation disabled. Be sure the source is "
                    + "valid Docgen-restricted DocBook 5.");
        }
            
        // Here we will use JAXP DocumentBuilderFactory and W3C XML Schema
        ErrorHandler eh = new DraconianErrorHandler(logger);
        
        DocumentBuilderFactory dbf = newDocumentBuilderFactory();

        SchemaFactory schemaFact = SchemaFactory.newInstance(
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFact.setErrorHandler(eh);
        Schema schema = schemaFact.newSchema(
                Transform.class.getResource("schema/docbook.xsd"));
        if (validate) {
            dbf.setSchema(schema);
        }
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new BugException(e);
        }
        db.setErrorHandler(eh);
        
        return db.parse(bookFile);
    }
    
    private static final class DraconianErrorHandler implements ErrorHandler {

        private final DocgenLogger logger;
        
        private DraconianErrorHandler(DocgenLogger logger) {
            this.logger = logger;
        }
        
        public void warning(SAXParseException spe) {
            logger.info("- Warning: " + spe);
        }
        
        public void error(SAXParseException spe) throws SAXParseException {
            throw spe;
        }

        public void fatalError(SAXParseException spe) throws SAXParseException {
            throw spe;
        }
        
    }

    public static String getAttribute(Element elem, String att) {
        String res = elem.getAttribute(att);
        return res.length() == 0 && !elem.hasAttribute(att) ? null : res;
    }

    public static String getAttributeNS(Element elem, String namespace, String att) {
        String res = elem.getAttributeNS(namespace, att);
        return res.length() == 0 && !elem.hasAttributeNS(namespace, att) ? null : res;
    }
    
    public static Iterable<Element> childrenElementsOf(final Node parent) {
        return new Iterable<Element>() {
            
            public Iterator<Element> iterator() {
                return new ElementIterator();
            }
            
            class ElementIterator implements Iterator<Element> {
                private Node continueFrom;
                private Element curNode;
                
                ElementIterator() {
                    continueFrom = parent.getFirstChild();
                    fetch();
                }
            
                public boolean hasNext() {
                    return curNode != null;
                }
            
                public Element next() {
                    Element res = curNode;
                    fetch();
                    return res;
                }
            
                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
                
                private void fetch() {
                        while (!(continueFrom == null
                                || continueFrom instanceof Element)) {
                            continueFrom = continueFrom.getNextSibling();
                        }
                        if (continueFrom != null) {
                            curNode = (Element) continueFrom;
                            continueFrom = curNode.getNextSibling();
                        } else {
                            curNode = null;
                        }
                }
                
            }
            
        };
    }

    public static String theSomethingElement(Element elem) {
        return theSomethingElement(elem, false);
    }
    
    public static String theSomethingElement(Element elem, boolean capFirst) {
        String id = getAttribute(elem, "id");
        if (id == null) {
            id = getAttribute(elem, "xml:id");
        }
        if (id != null && (id.startsWith(Transform.AUTO_ID_PREFIX)
                || id.startsWith(Transform.DOCGEN_ID_PREFIX))) {
            id = null;
        }
        
        StringBuilder sb = new StringBuilder();
        
        if (id != null || elem.getParentNode() instanceof Document) {
            sb.append("the \"");
        } else {
            sb.append("a(n) \"");
        }
        sb.append(elem.getLocalName());
        sb.append("\" element");
        
        if (id != null) {
            sb.append(" with xml:id=\"").append(id).append("\"");
        }
        
        Location loc = (Location) elem.getUserData(
                ValidatingDOMBuilderWithLocations.KEY_LOCATION);
        if (loc != null) {
            sb.append(" (location: ");
            boolean empty = true;
            
            String sysId = loc.getSystemId();
            if (sysId != null) {
                // Since it goes into the middle of other error messages,
                // keep only the file name and the containing directory name: 
                int slashIdx = sysId.lastIndexOf("/");
                if (slashIdx != -1) {
                    slashIdx = sysId.lastIndexOf("/", slashIdx - 1);
                    if (slashIdx > 0) {
                        sysId = "[...]" + sysId.substring(slashIdx);
                    }
                }
                sb.append(sysId);
                empty = false;
            }
            if (loc.getLine() > 0) {
                if (!empty) {
                    sb.append(':');
                }
                sb.append(loc.getLine());
                empty = false;
            }
            if (loc.getColumn() > 0) {
                if (!empty) {
                    sb.append(':');
                }
                sb.append(loc.getColumn());
            }
            sb.append(")");
        }
        
        if (capFirst) {
            sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        }
        
        return sb.toString();
    }

}
