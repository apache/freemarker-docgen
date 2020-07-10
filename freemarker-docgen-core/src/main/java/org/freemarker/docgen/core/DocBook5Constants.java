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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class DocBook5Constants {

    // Can't be instantiated
    private DocBook5Constants() {
        // Nop
    }

    public static final String XMLNS_DOCBOOK5 = "http://docbook.org/ns/docbook";
    public static final String XMLNS_XLINK = "http://www.w3.org/1999/xlink";

    public static final String E_BOOK = "book";
    public static final String E_ARTICLE = "article";
    public static final String A_CONFORMANCE = "conformance";
    public static final String AV_CONFORMANCE_DOCGEN = "docgen";
    public static final String E_INFO = "info";
    public static final String E_TITLE = "title";
    public static final String E_TITLEABBREV = "titleabbrev";
    public static final String E_SUBTITLE = "subtitle";
    public static final String E_PRODUCTNAME = "productname";
    public static final String E_SECTION = "section";
    public static final String E_SIMPLESECT = "simplesect";
    public static final String E_PARA = "para";
    public static final String E_ITEMIZEDLIST = "itemizedlist";
    public static final String E_IMAGEDATA = "imagedata";
    public static final String E_MEDIAOBJECT = "mediaobject";
    public static final String E_FIGURE = "figure";
    public static final String E_INFORMALFIGURE = "informalfigure";
    public static final String A_FILEREF = "fileref";
    public static final String E_ORDEREDLIST = "orderedlist";
    public static final String E_LISTITEM = "listitem";
    public static final String E_PROGRAMLISTING = "programlisting";
    public static final String A_LANGUAGE = "language";
    public static final String A_ROLE = "role";
    public static final String E_FOOTNOTE = "footnote";
    public static final String E_CHAPTER = "chapter";
    public static final String E_PART = "part";
    public static final String E_APPENDIX = "appendix";
    public static final String E_GLOSSARY = "glossary";
    public static final String E_GLOSSENTRY = "glossentry";
    public static final String E_INDEX = "index";
    public static final String E_INFORMALTABLE = "informaltable";
    public static final String E_TABLE = "table";
    public static final String E_PREFACE = "preface";
    public static final String E_QUANDAENTRY = "qandaentry";
    public static final String E_ANCHOR = "anchor";

    public static final String A_XML_ID = "xml:id";
    public static final String A_XREFLABEL = "xreflabel";

    public static final String E_INDEXTERM = "indexterm";
    public static final String E_PRIMARY = "primary";
    public static final String E_SECONDARY = "secondary";

    public static final String E_COLGROUP = "colgroup";
    public static final String E_COL = "col";
    public static final String A_ALIGN = "align";
    public static final String A_VALIGN = "valign";
    public static final String A_SPAN = "span";
    public static final String A_ROWSPAN = "rowspan";
    public static final String A_COLSPAN = "colspan";
    public static final String E_TR = "tr";
    public static final String E_TBODY = "tbody";
    public static final String E_THEAD = "thead";
    public static final String E_TFOOT = "tfoot";
    public static final String E_TD = "td";
    public static final String E_TH = "th";

    public static final String E_OLINK = "olink";
    public static final String A_XLINK_HREF = "href";
    public static final String E_LINK = "link";
    public static final String A_TARGETDOC = "targetdoc";

    public static final String E_NOTE = "note";
    public static final String E_WARNING = "warning";

    /**
     * Elements that are like parts, chapters, sections and like.
     */
    public static final Set<String> DOCUMENT_STRUCTURE_ELEMENTS;
    static {
        HashSet<String> docStructElems = new HashSet<String>();

        docStructElems.add(E_APPENDIX);
        docStructElems.add(E_BOOK);
        docStructElems.add(E_ARTICLE);
        docStructElems.add(E_CHAPTER);
        docStructElems.add(E_GLOSSARY);
        docStructElems.add(E_INDEX);
        docStructElems.add(E_PART);
        docStructElems.add(E_PREFACE);
        docStructElems.add(E_SECTION);
        docStructElems.add(E_SIMPLESECT);

        DOCUMENT_STRUCTURE_ELEMENTS = Collections.unmodifiableSet(
                docStructElems);
    }

    /**
     * Elements that directly generates visible artifacts on the page and
     * occur directly under document-structural elements.
     */
    public static final Set<String> VISIBLE_TOP_LEVEL_ELEMENTS;
    static {
        HashSet<String> visibleTopLevelElems = new HashSet<String>();

        visibleTopLevelElems.addAll(DOCUMENT_STRUCTURE_ELEMENTS);
        visibleTopLevelElems.add(E_INFO);
        visibleTopLevelElems.add(E_TITLE);
        visibleTopLevelElems.add(E_SUBTITLE);
        visibleTopLevelElems.add(E_PARA);
        visibleTopLevelElems.add(E_TABLE);
        visibleTopLevelElems.add(E_INFORMALTABLE);
        visibleTopLevelElems.add(E_ORDEREDLIST);
        visibleTopLevelElems.add(E_ITEMIZEDLIST);
        visibleTopLevelElems.add(E_MEDIAOBJECT);
        visibleTopLevelElems.add(E_FIGURE);
        visibleTopLevelElems.add(E_INFORMALFIGURE);
        visibleTopLevelElems.add(E_PROGRAMLISTING);
        visibleTopLevelElems.add(E_NOTE);
        visibleTopLevelElems.add(E_WARNING);

        VISIBLE_TOP_LEVEL_ELEMENTS = Collections.unmodifiableSet(
                visibleTopLevelElems);
    }

}
