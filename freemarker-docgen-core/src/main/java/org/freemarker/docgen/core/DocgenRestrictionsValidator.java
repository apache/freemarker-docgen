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

import static org.freemarker.docgen.core.DocBook5Constants.AV_CONFORMANCE_DOCGEN;
import static org.freemarker.docgen.core.DocBook5Constants.A_CONFORMANCE;
import static org.freemarker.docgen.core.DocBook5Constants.A_LANGUAGE;
import static org.freemarker.docgen.core.DocBook5Constants.A_ROLE;
import static org.freemarker.docgen.core.DocBook5Constants.A_XML_ID;
import static org.freemarker.docgen.core.DocBook5Constants.A_XREFLABEL;
import static org.freemarker.docgen.core.DocBook5Constants.E_ANCHOR;
import static org.freemarker.docgen.core.DocBook5Constants.E_APPENDIX;
import static org.freemarker.docgen.core.DocBook5Constants.E_ARTICLE;
import static org.freemarker.docgen.core.DocBook5Constants.E_BOOK;
import static org.freemarker.docgen.core.DocBook5Constants.E_CHAPTER;
import static org.freemarker.docgen.core.DocBook5Constants.E_COL;
import static org.freemarker.docgen.core.DocBook5Constants.E_COLGROUP;
import static org.freemarker.docgen.core.DocBook5Constants.E_FOOTNOTE;
import static org.freemarker.docgen.core.DocBook5Constants.E_GLOSSARY;
import static org.freemarker.docgen.core.DocBook5Constants.E_GLOSSENTRY;
import static org.freemarker.docgen.core.DocBook5Constants.E_INDEX;
import static org.freemarker.docgen.core.DocBook5Constants.E_INDEXTERM;
import static org.freemarker.docgen.core.DocBook5Constants.E_INFO;
import static org.freemarker.docgen.core.DocBook5Constants.E_INFORMALTABLE;
import static org.freemarker.docgen.core.DocBook5Constants.E_ITEMIZEDLIST;
import static org.freemarker.docgen.core.DocBook5Constants.E_LINK;
import static org.freemarker.docgen.core.DocBook5Constants.E_LISTITEM;
import static org.freemarker.docgen.core.DocBook5Constants.E_MEDIAOBJECT;
import static org.freemarker.docgen.core.DocBook5Constants.E_NOTE;
import static org.freemarker.docgen.core.DocBook5Constants.E_OLINK;
import static org.freemarker.docgen.core.DocBook5Constants.E_ORDEREDLIST;
import static org.freemarker.docgen.core.DocBook5Constants.E_PARA;
import static org.freemarker.docgen.core.DocBook5Constants.E_PART;
import static org.freemarker.docgen.core.DocBook5Constants.E_PREFACE;
import static org.freemarker.docgen.core.DocBook5Constants.E_PRIMARY;
import static org.freemarker.docgen.core.DocBook5Constants.E_PRODUCTNAME;
import static org.freemarker.docgen.core.DocBook5Constants.E_PROGRAMLISTING;
import static org.freemarker.docgen.core.DocBook5Constants.E_QUANDAENTRY;
import static org.freemarker.docgen.core.DocBook5Constants.E_SECONDARY;
import static org.freemarker.docgen.core.DocBook5Constants.E_SECTION;
import static org.freemarker.docgen.core.DocBook5Constants.E_SIMPLESECT;
import static org.freemarker.docgen.core.DocBook5Constants.E_SUBTITLE;
import static org.freemarker.docgen.core.DocBook5Constants.E_TABLE;
import static org.freemarker.docgen.core.DocBook5Constants.E_TBODY;
import static org.freemarker.docgen.core.DocBook5Constants.E_TD;
import static org.freemarker.docgen.core.DocBook5Constants.E_TFOOT;
import static org.freemarker.docgen.core.DocBook5Constants.E_TH;
import static org.freemarker.docgen.core.DocBook5Constants.E_THEAD;
import static org.freemarker.docgen.core.DocBook5Constants.E_TITLE;
import static org.freemarker.docgen.core.DocBook5Constants.E_TITLEABBREV;
import static org.freemarker.docgen.core.DocBook5Constants.E_TR;
import static org.freemarker.docgen.core.DocBook5Constants.E_WARNING;
import static org.freemarker.docgen.core.DocBook5Constants.XMLNS_DOCBOOK5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Adds Docgen-specific restrictions to an already existing DocBook 5 validator.
 */
class DocgenRestrictionsValidator implements ContentHandler {

    public static final int MAX_SECTION_NESTING_LEVEL = 3;

    private static final Set<String> SUPPORTED_ELEMENTS;
    static {
        Set<String> supportedElements = new TreeSet<String>();

        supportedElements.add(E_ANCHOR);
        supportedElements.add("answer");
        supportedElements.add(E_APPENDIX);
        supportedElements.add(E_ARTICLE);
        supportedElements.add(E_BOOK);
        supportedElements.add(E_CHAPTER);
        supportedElements.add("classname");
        supportedElements.add(E_COL);
        supportedElements.add(E_COLGROUP);
        supportedElements.add("emphasis");
        supportedElements.add("entry");
        supportedElements.add(E_FOOTNOTE);
        supportedElements.add(E_GLOSSARY);
        supportedElements.add("glossdef");
        supportedElements.add(E_GLOSSENTRY);
        supportedElements.add("glosssee");
        supportedElements.add("glossseealso");
        supportedElements.add("glossterm");
        supportedElements.add("imagedata");
        supportedElements.add("imageobject");
        supportedElements.add(E_INDEX);
        supportedElements.add(E_INDEXTERM);
        supportedElements.add(E_INFO);
        supportedElements.add(E_INFORMALTABLE);
        supportedElements.add(E_ITEMIZEDLIST);
        supportedElements.add(E_LINK);
        supportedElements.add(E_LISTITEM);
        supportedElements.add("literal");
        supportedElements.add(E_MEDIAOBJECT);
        supportedElements.add("methodname");
        supportedElements.add(E_NOTE);
        supportedElements.add(E_OLINK);
        supportedElements.add(E_ORDEREDLIST);
        supportedElements.add("package");
        supportedElements.add(E_PARA);
        supportedElements.add(E_PART);
        supportedElements.add("phrase");
        supportedElements.add(E_PREFACE);
        supportedElements.add(E_PRIMARY);
        supportedElements.add(E_PRODUCTNAME);
        supportedElements.add(E_PROGRAMLISTING);
        supportedElements.add(E_QUANDAENTRY);
        supportedElements.add("qandaset");
        supportedElements.add("question");
        supportedElements.add("quote");
        supportedElements.add("remark");
        supportedElements.add("replaceable");
        supportedElements.add(E_SECONDARY);
        supportedElements.add(E_SECTION);
        supportedElements.add(E_SIMPLESECT);
        supportedElements.add(E_SUBTITLE);
        supportedElements.add(E_TBODY);
        supportedElements.add(E_TD);
        supportedElements.add(E_TFOOT);
        supportedElements.add(E_TH);
        supportedElements.add(E_THEAD);
        supportedElements.add(E_TR);
        supportedElements.add(E_TITLE);
        supportedElements.add(E_TITLEABBREV);
        supportedElements.add(E_WARNING);
        supportedElements.add("xref");

        SUPPORTED_ELEMENTS = Collections.unmodifiableSet(supportedElements);
    }

    private static final Set<String> ELEMENTS_ALLOW_ID;
    static {
        // Attention! When adding entries here, be sure that the corresponding
        // element indeed generates HTML anchors (or is an output-file element).

        Set<String> elementsAllowId = new TreeSet<String>();

        elementsAllowId.add(E_PART);
        elementsAllowId.add(E_APPENDIX);
        elementsAllowId.add(E_CHAPTER);
        elementsAllowId.add(E_SECTION);
        elementsAllowId.add(E_SIMPLESECT);
        elementsAllowId.add(E_PREFACE);
        elementsAllowId.add(E_INDEX);
        elementsAllowId.add(E_GLOSSARY);

        elementsAllowId.add(E_PARA);
        elementsAllowId.add(E_MEDIAOBJECT);
        elementsAllowId.add(E_INFORMALTABLE);
        elementsAllowId.add(E_PROGRAMLISTING);
        elementsAllowId.add(E_ITEMIZEDLIST);
        elementsAllowId.add(E_ORDEREDLIST);
        elementsAllowId.add(E_LISTITEM);

        elementsAllowId.add(E_GLOSSENTRY);
        elementsAllowId.add(E_QUANDAENTRY);

        elementsAllowId.add(E_ANCHOR);

        ELEMENTS_ALLOW_ID = Collections.unmodifiableSet(elementsAllowId);
    }

    private final ContentHandler docbook5Validator;
    private final ErrorHandler errorHandler;
    private final MessageStreamActivityMonitor errorMessageMonitor;
    private final DocgenValidationOptions options;

    private Locator locator;
    private String documentElementName;
    private int sectionNestingLevel;
    private int paraNestingLevel;
    private LinkedList<Integer> paraNestingLevelsHiddenByFootnote
            = new LinkedList<Integer>();
    private LinkedList<Integer> programlistingNestingLevelsHiddenByFootnote
            = new LinkedList<Integer>();
    private LinkedList<Integer> programlistingLineLengthHiddenByFootnote
            = new LinkedList<Integer>();
    private ArrayList<Boolean> hadClosedPara
            = new ArrayList<Boolean>();
    private ArrayList<String> elemPath
            = new ArrayList<String>();
    private int programlistingNestingLevel;
    private int invisibleElementNestingLevel;
    private int programlistingLineLength;

    /**
     * @param errorMessageMonitor Used for preventing reporting a violation
     *      that was also a DocBook 5 violation.
     */
    DocgenRestrictionsValidator(
            ContentHandler docbook5Validator, ErrorHandler errorHandler,
            MessageStreamActivityMonitor errorMessageMonitor,
            DocgenValidationOptions options) {
        if (docbook5Validator == null) {
            throw new IllegalArgumentException(
                    "\"docbook5Validator\" can't be null");
        }
        this.docbook5Validator = docbook5Validator;

        if (errorHandler == null) {
            throw new IllegalArgumentException(
                    "\"errorHandler\" can't be null");
        }
        this.errorHandler = errorHandler;

        if (errorMessageMonitor == null) {
            throw new IllegalArgumentException(
                    "\"messageMonitor\" can't be null");
        }
        this.errorMessageMonitor = errorMessageMonitor;

        if (options == null) {
            throw new IllegalArgumentException("\"options\" can't be null");
        }
        this.options = options;
    }

    public void startElement(String uri, final String localName, String name,
            Attributes atts) throws SAXException {
        boolean xmlnsOK = uri.equals(XMLNS_DOCBOOK5);
        if (xmlnsOK) {
            hadClosedPara.add(false);
            elemPath.add(localName);
        }

        errorMessageMonitor.reset();
        docbook5Validator.startElement(uri, localName, name, atts);
        if (!errorMessageMonitor.hadNewErrorMessage()) {
            if (!xmlnsOK) {
                errorHandler.error(newSAXException(
                        "Unsupported element namespace: " + uri));
            } else if (!SUPPORTED_ELEMENTS.contains(localName)) {
                if (localName.equals("sect1")
                        || localName.equals("sect2")
                        || localName.equals("sect3")
                        || localName.equals("sect4")
                        || localName.equals("sect5")) {
                    errorHandler.error(newSAXException(
                            "The \"" + localName + "\" element and other such "
                            + "numbered \"sect\"-s are not allowed; "
                            + "use \"" + E_SECTION + "\"-s instead."));
                } else {
                    errorHandler.error(newSAXException(
                            "Unsupported element: " + localName));
                }
            } else {
                startSupportedDocbook5Element(localName, atts);
            }
        }
    }

    private void startSupportedDocbook5Element(
            String localName, Attributes atts) throws SAXException {
        boolean isDocumentElem;
        if (documentElementName == null) {
            documentElementName = localName;
            isDocumentElem = true;
        } else {
            isDocumentElem = false;
        }

        if (localName.equals(E_SECTION)) {
            sectionNestingLevel++;
            if (sectionNestingLevel > MAX_SECTION_NESTING_LEVEL) {
                errorHandler.error(newSAXException(
                        "\"" + localName + "\" element nesting too deep. "
                        + "The maximum supported is "
                        + MAX_SECTION_NESTING_LEVEL
                        + " levels. Hint: Use \"" + E_SIMPLESECT
                        + "\" instead."));
            }
        } else if (localName.equals(E_PARA)) {
            paraNestingLevel++;
        } else if (localName.equals(E_ITEMIZEDLIST)
                        || localName.equals(E_ORDEREDLIST)
                        || localName.equals(E_PROGRAMLISTING)
                        || localName.equals(E_MEDIAOBJECT)) {
            checkNotInAPara(localName);
            if (localName.equals(E_PROGRAMLISTING)) {
                if (options.getProgramlistingRequiresLanguage()
                        && atts.getValue("", A_LANGUAGE) == null) {
                    errorHandler.error(newSAXException(
                            "In this book, \"" + localName
                            + "\" elements must have a \"" + A_LANGUAGE
                            + "\" attribute. Hint: If the language is so "
                            + "marginal that will not ever have syntax "
                            + "highlighter anyway, use \"unknown\" as the "
                            + "attribute value."));
                }
                if (options.getProgramlistingRequiresRole()
                        && atts.getValue("", A_ROLE) == null) {
                    errorHandler.error(newSAXException("In this book, "
                            + "\"" + localName + "\" elements "
                            + "must have a \"" + A_ROLE + "\" attribute. "
                            + "Hint: If none of the avialble roles fit, "
                            + "use \"unspecified\" as the attribute value."
                            ));
                }
                checkHasPrecedingParaInListitem(localName);

                programlistingLineLength = 0;
                programlistingNestingLevel++;
            }
        } else if (localName.equals(E_INFORMALTABLE)
                || localName.equals(E_TABLE)) {
            checkNotInAPara(localName);
            checkHasPrecedingParaInListitem(localName);
        } else if (localName.equals(E_FOOTNOTE)) {
            if (!paraNestingLevelsHiddenByFootnote.isEmpty()) {
                errorHandler.error(newSAXException("\"" + localName
                        + "\" inside another \"" + localName
                        + "\" is not allowed."));
            }
            if (programlistingNestingLevel != 0) {
                errorHandler.error(newSAXException("\"" + localName
                        + "\" inside a \"" + E_PROGRAMLISTING
                        + "\" is not allowed."));
            }
            paraNestingLevelsHiddenByFootnote.add(paraNestingLevel);
            paraNestingLevel = 0;

            programlistingNestingLevelsHiddenByFootnote.add(
                    programlistingNestingLevel);
            programlistingNestingLevel = 0;
            programlistingLineLengthHiddenByFootnote.add(
                    programlistingLineLength);
            programlistingLineLength = 0;
        } else if (localName.equals(E_ANCHOR)
                || localName.equals(E_INDEXTERM)) {
            invisibleElementNestingLevel++;
        } else if (isDocumentElem) {
            String conformance = atts.getValue("", A_CONFORMANCE);
            if (conformance == null) {
                errorHandler.error(newSAXException("The \""
                        + localName + "\" element must have a \""
                        + A_CONFORMANCE + "\" attribute. Hint: "
                        + "Add the attribute with value \""
                        + AV_CONFORMANCE_DOCGEN + "\"."));
            } else if (!conformance.equals(AV_CONFORMANCE_DOCGEN)) {
                errorHandler.error(newSAXException("The value of the \""
                        + A_CONFORMANCE + "\" attribute must be \""
                        + AV_CONFORMANCE_DOCGEN + "\"."));
            }
        }

        if (atts.getIndex(A_XML_ID) != -1
                && !ELEMENTS_ALLOW_ID.contains(localName)) {
            errorHandler.error(newSAXException("The \"" + localName
                    + "\" element can't have an \"" + A_XML_ID + "\" "
                    + "attribute (" + A_XML_ID + "=\""
                    + atts.getValue("xml:id") + "\"). (Hint: "
                    + (localName.equals(E_TITLE)
                            ? "Move the " + A_XML_ID + " over into the "
                              + "element whose \"" + E_TITLE
                              + "\" the element is."
                            : "Try moving the " + A_XML_ID
                              + " higher in the element hierarchy.")
                    + ")"));
        }

        if (atts.getIndex(A_XREFLABEL) != -1
                && !ELEMENTS_ALLOW_ID.contains(localName)) {
            errorHandler.error(newSAXException("The \"" + localName
                    + "\" element can't have an \"" + A_XREFLABEL
                    + "\" attribute, because it couldn't have a \""
                    + A_XML_ID + "\" attribute either, and hence it "
                    + "couldn't be the target of a link. (Hint: "
                    + (localName.equals(E_TITLE)
                            ? "Move the \"" + A_XREFLABEL + "\" attribute "
                              + "over into the element whose \"" + E_TITLE
                              + "\" the element is."
                            : "Try moving the " + A_XREFLABEL
                              + " higher in the element hierarchy.")
                    + ")"));
        }
    }

    private void checkNotInAPara(String localName) throws SAXException {
        if (paraNestingLevel > 0) {
            errorHandler.error(newSAXException("It's not allowed to "
                    + "put a(n) \"" + localName + "\" inside a \""
                    + E_PARA + "\". Hint: Simply split the containing "
                    + "\"" + E_PARA + "\" into two parts, or move the "
                    + "element after the \"" + E_PARA + "\"."));
        }
    }

    private void checkHasPrecedingParaInListitem(String elemName)
            throws SAXException {
        if (elemPath.get(elemPath.size() - 2).equals(E_LISTITEM)
                && !hadClosedPara.get(hadClosedPara.size() - 2)) {
            // This restriction exists as otherwise there are problems
            // with rendering it under most browsers if the element is
            // implemented as a HTML table.
            errorHandler.error(newSAXException("A(n) \""
                    + elemName + "\" in a " + "\"" + E_LISTITEM
                    + "\" must be preceded by a \"" + E_PARA + "\"."));
        }
    }

    public void endElement(String uri, String localName, String name)
            throws SAXException {
        boolean xmlnsOK = uri.equals(XMLNS_DOCBOOK5);
        try {
            docbook5Validator.endElement(uri, localName, name);
            if (xmlnsOK) {
                if (localName.equals(E_SECTION)) {
                    sectionNestingLevel--;
                } else if (localName.equals(E_PARA)) {
                    paraNestingLevel--;
                    hadClosedPara.set(hadClosedPara.size() - 2, true);
                } else if (localName.equals(E_FOOTNOTE)) {
                    paraNestingLevel
                            = paraNestingLevelsHiddenByFootnote.remove();
                    programlistingNestingLevel
                            = programlistingNestingLevelsHiddenByFootnote
                                    .remove();
                    programlistingLineLength
                            = programlistingLineLengthHiddenByFootnote.remove();
                } else if (localName.equals(E_PROGRAMLISTING)) {
                    programlistingNestingLevel--;
                } else if (localName.equals(E_ANCHOR)
                        || localName.equals(E_INDEXTERM)) {
                    invisibleElementNestingLevel--;
                }
            }
        } finally {
            if (xmlnsOK) {
                elemPath.remove(elemPath.size() - 1);
                hadClosedPara.remove(hadClosedPara.size() - 1);
            }
        }
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (invisibleElementNestingLevel == 0
                && programlistingNestingLevel > 0) {
            int end = start + length;
            for (int i = start; i < end; i++) {
                char c = ch[i];
                if (c == 0x0A || c == 0x0D) {
                    programlistingLineLength = 0;
                } else {
                    if (c == 0x09) {
                        // Assuming tab-width 8:
                        programlistingLineLength
                                = ((programlistingLineLength / 8) + 1) * 8;
                        errorHandler.error(newSAXException(
                                "Tab character is not allowed in "
                                + "programlistings. (Hint: Use spaces instead.)"
                                ));
                    } else {
                        programlistingLineLength++;
                    }
                }
                if (programlistingLineLength
                        == options.getMaximumProgramlistingWidth() + 1) {
                    errorHandler.error(newSAXException(
                            "Line length in the programlisting exceeded "
                            + options.getMaximumProgramlistingWidth()
                            + ", which was set as the maximum in the "
                            + "(Related Docgen setting: \""
                            + Transform.SETTING_VALIDATION + "\" per \""
                            + Transform
                                .SETTING_VALIDATION_MAXIMUM_PROGRAMLISTING_WIDTH
                            + "\")"));
                }
            }
        }
        docbook5Validator.characters(ch, start, length);
    }

    public void endDocument() throws SAXException {
        docbook5Validator.endDocument();
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        docbook5Validator.endPrefixMapping(prefix);
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        docbook5Validator.ignorableWhitespace(ch, start, length);

    }

    public void processingInstruction(String target, String data)
            throws SAXException {
        docbook5Validator.processingInstruction(target, data);
    }

    public void setDocumentLocator(Locator locator) {
        docbook5Validator.setDocumentLocator(locator);
        this.locator = locator;
    }

    public void skippedEntity(String name) throws SAXException {
        docbook5Validator.skippedEntity(name);
    }

    public void startDocument() throws SAXException {
        docbook5Validator.startDocument();
    }

    private SAXParseException newSAXException(String message) {
        return new SAXParseException(
                "Docgen-specific DocBook restriction violated: " + message,
                locator);
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        docbook5Validator.startPrefixMapping(prefix, uri);
    }

}
