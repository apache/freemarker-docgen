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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.freemarker.docgen.core.DocBook5Constants.*;

/**
 * Resolves (removes) <tt>colgroup</tt> and <tt>col</tt> elements, because
 * they are not known by many important browsers. This resolution is possible as
 * <tt>colgroup</tt> and <tt>col</tt> are just convenience elements to avoid
 * typing the aligns for each cells of the same column.
 */
final class TableSimplifier {

    private final Element table;

    /**
     * Alignment defaults (possibly {@code null}) with the column as their
     * index.
     */
    private final List<Alignment> colGroupAligns
            = new ArrayList<Alignment>(); 
    private final List<VAlignment> colGroupVAligns
            = new ArrayList<VAlignment>();
    
    /**
     * Used for tracking the various cell spans. This is a 2D matrix, where
     * each entry symbolizes an imaginary table cell that is either visually
     * overlapped by an actual table cell ({@code true}) or not ({@code false}).
     */
    private boolean[] cellMatrix;
    private int cellMatrixWidth;
    private int cellMatrixHeight;
    private int cellMatrixCurRow;
    private int cellMatrixCurCol;
    
    static void simplify(Element table) throws SAXException, DocgenException {
        new TableSimplifier(table).processTable();
    }
    
    private TableSimplifier(Element table) {
        this.table = table;
    }

    private void processTable() throws SAXException, DocgenException {
        processAndRemoveColGroupsAndCols();
        decorateCells();
    }
    
    /**
     * Fills {@link #colGroupAligns} and {@link #colGroupVAligns}, while
     * removes the related nodes. 
     * @throws SAXException if something violates the DocBook rules
     * @throws DocgenException if something violates the Docgen restrictions
     *     (or the DocBook rules, if there was no prior Relax NG validation).
     */
    private void processAndRemoveColGroupsAndCols()
            throws SAXException, DocgenException {
        LinkedList<Element> toRemove = new LinkedList<Element>();
        
        NodeList children = table.getChildNodes();
        int childCnt = children.getLength();
        fetchChildren: for (int childIdx = 0; childIdx < childCnt; childIdx++) {
            Node child = children.item(childIdx);
            if (child instanceof Element) {
                Element elem = (Element) child;
                if (!elem.getNamespaceURI().equals(XMLNS_DOCBOOK5)) {
                    continue fetchChildren;
                }
                String elemName = elem.getLocalName(); 
                
                if (elemName.equals(E_COLGROUP)) {
                    processColGroup(elem);
                    toRemove.add(elem);
                } else if (elemName.equals(E_COL)) {
                    processCol(elem, null, null);
                    toRemove.add(elem);
                }
                // Ignore other elements, "tbody" and like.
            }
            // Ignore non-elements
        }
        
        for (Element child : toRemove) {
            table.removeChild(child);
        }
    }
    
    private void processColGroup(Element colGroup)
            throws SAXException, DocgenException {
        boolean usesAtts = false;
        
        int span = 1;  // Default from the HTML spec.
        Alignment align = null;
        VAlignment valign = null;
        
        NamedNodeMap atts = colGroup.getAttributes();
        int attCnt = atts.getLength();
        fetchAtts: for (int attIdx = 0; attIdx < attCnt; attIdx++) {
            Attr att = (Attr) atts.item(attIdx);
            String attNS = att.getNamespaceURI();
            if (attNS != null && attNS.length() != 0) {
                continue fetchAtts;
            }
            String attName = att.getLocalName();
            String attValue = att.getValue().trim();
            
            if (attName.equals(A_SPAN)) {
                usesAtts = true;
                span = parseSpanAttribute(attValue, A_SPAN, E_COLGROUP);
            } else if (attName.equals(A_ALIGN)) {
                align = parseAlignAttribute(attValue, E_COLGROUP);
            } else if (attName.equals(A_VALIGN)) {
                valign = parseVAlignAttribute(attValue, E_COLGROUP);
            } else {
                throw new DocgenException("The \"" + attName
                        + "\" attribute of the \"" + E_COLGROUP
                        + "\" element is not supported by Docgen.");
            }
        }  // fetchAtts
        
        if (usesAtts) {
            for (int i = 0; i < span; i++) {
                colGroupAligns.add(align);
                colGroupVAligns.add(valign);
            }
        }
        
        NodeList children = colGroup.getChildNodes();
        int childCnt = children.getLength();
        fetchChildren: for (int childIdx = 0; childIdx < childCnt; childIdx++) {
            Node child = children.item(childIdx);
            if (child instanceof Element) {
                Element elem = (Element) child;
                if (!elem.getNamespaceURI().equals(XMLNS_DOCBOOK5)) {
                    continue fetchChildren;
                }
                String elemName = elem.getLocalName();
                
                if (elemName.equals(E_COL)) {
                    if (usesAtts) {
                        throw new SAXException("The \"" + E_COLGROUP
                                + "\" already used attributes, so it can't "
                                + "have \"" + E_COL + "\" elements in it.");
                    }
                    processCol(elem, align, valign);
                } else {
                    throw new SAXException("The \"" + elemName + "\" element "
                            + "is unexpected inside \"" + E_COLGROUP + "\".");
                }
            }
            // Ignore non-elements
        }
        
    }

    private void processCol(Element col,
            Alignment colGroupDefaultAlign, VAlignment colGroupDefaultVAlign)
            throws SAXException, DocgenException {
        int span = 1;  // Default from the HTML spec.
        Alignment align = colGroupDefaultAlign;
        VAlignment valign = colGroupDefaultVAlign;
        
        NamedNodeMap atts = col.getAttributes();
        int attCnt = atts.getLength();
        fetchAtts: for (int attIdx = 0; attIdx < attCnt; attIdx++) {
            Attr att = (Attr) atts.item(attIdx);
            String attNS = att.getNamespaceURI();
            if (attNS != null && attNS.length() != 0) {
                continue fetchAtts;
            }
            String attName = att.getLocalName();
            String attValue = att.getValue().trim();
            
            if (attName.equals(A_SPAN)) {
                span = parseSpanAttribute(attValue, A_SPAN, E_COL);
            } else if (attName.equals(A_ALIGN)) {
                align = parseAlignAttribute(attValue, E_COL);
            } else if (attName.equals(A_VALIGN)) {
                valign = parseVAlignAttribute(attValue, E_COL);
            } else {
                throw new DocgenException("The \"" + attName
                        + "\" attribute of the \"" + E_COL
                        + "\" element is not supported by Docgen.");
            }
        }  // fetchAtts
        
        for (int i = 0; i < span; i++) {
            colGroupAligns.add(align);
            colGroupVAligns.add(valign);
        }
    }
    
    private Alignment parseAlignAttribute(
            String attValue, String elemName)
            throws SAXException {
        String lAttValue = attValue.toLowerCase();
        if (lAttValue.equals("left")) {
            return Alignment.LEFT;
        } else if (lAttValue.equals("center")) {
            return Alignment.CENTER;
        } else if (lAttValue.equals("right")) {
            return Alignment.RIGHT;
        } else {
            throw new SAXException("Illegal \"" + A_ALIGN
                    + "\" attribute value for a(n) \"" + elemName
                    + "\" element: " + attValue);
        }
    }

    private VAlignment parseVAlignAttribute(
            String attValue, String elemName)
            throws SAXException, DocgenException {
        String lAttValue = attValue.toLowerCase();
        if (lAttValue.equals("top")) {
            return VAlignment.TOP;
        } else if (lAttValue.equals("bottom")) {
            return VAlignment.BOTTOM;
        } else if (lAttValue.equals("middle")) {
            return VAlignment.MIDDLE;
        } else if (lAttValue.equals("baseline")) {
            throw new DocgenException("The \"baseline\" value for \"" + A_VALIGN
                    + "\" attribute is not implemented by Docgen.");
        } else {
            throw new SAXException("Illegal \"" + A_VALIGN
                    + "\" attribute value for a(n) \"" + elemName
                    + "\" element: " + attValue);
        }
    }
    
    @SuppressWarnings("boxing")
    private int parseSpanAttribute(
            String attValue, String attName, String elemName)
            throws SAXException {
        int span;
        try {
            span = Integer.valueOf(attValue);
        } catch (NumberFormatException e) {
            throw new SAXException("Illegal \"" + attName
                    + "\" attribute value for a(n) \"" + elemName
                    + "\" element: " + attValue);
        }
        if (span < 1) {
            throw new SAXException("Illegal \"" + attName
                    + "\" attribute value for a(n) \"" + elemName
                    + "\" element: " + attValue);
        }
        return span;
    }

    /**
     * This will add the required {@code align} and {@code valign} attributes.
     */
    private void decorateCells() throws SAXException, DocgenException {
        initCellMatrix();
        
        NodeList children = table.getChildNodes();
        int childCnt = children.getLength();
        fetchChildren: for (int childIdx = 0; childIdx < childCnt; childIdx++) {
            Node child = children.item(childIdx);
            if (child instanceof Element) {
                Element elem = (Element) child;
                if (!elem.getNamespaceURI().equals(XMLNS_DOCBOOK5)) {
                    continue fetchChildren;
                }
                String elemName = elem.getLocalName();
                
                if (elemName.equals(E_THEAD)
                        || elemName.equals(E_TBODY)
                        || elemName.equals(E_TFOOT)) {
                    decorateTDiv(elem);
                } else if (elemName.equals(E_TR)) {
                    decorateRow(elem, null, null);
                }
                // Ignore other elements... "caption", etc.
            }
            // Ignore non-elements
        }
        
        checkFinishedCellMatrix();
    }
    
    private void decorateTDiv(Element tDiv)
            throws SAXException, DocgenException {
        initCellMatrix();
        
        Alignment tDivAlign = null;  // "div" refers to tbody, thead or tfoot
        VAlignment tDivVAlign = null;
        
        NamedNodeMap atts = tDiv.getAttributes();
        int attCnt = atts.getLength();
        fetchAtts: for (int attIdx = 0; attIdx < attCnt; attIdx++) {
            Attr att = (Attr) atts.item(attIdx);
            String attNS = att.getNamespaceURI();
            if (attNS != null && attNS.length() != 0) {
                continue fetchAtts;
            }
            String attName = att.getLocalName();
            String attValue = att.getValue().trim();
            
            if (attName.equals(A_ALIGN)) {
                tDivAlign = parseAlignAttribute(
                        attValue, tDiv.getLocalName());
            } else if (attName.equals(A_VALIGN)) {
                tDivVAlign = parseVAlignAttribute(
                        attValue, tDiv.getLocalName());
            } else {
                throw new DocgenException("The \"" + attName
                        + "\" attribute of the \"" + tDiv.getLocalName()
                        + "\" element is not supported by Docgen.");
            }
        }  // fetchAtts
        
        NodeList children = tDiv.getChildNodes();
        int childCnt = children.getLength();
        fetchChildren: for (int childIdx = 0; childIdx < childCnt; childIdx++) {
            Node child = children.item(childIdx);
            if (child instanceof Element) {
                Element elem = (Element) child;
                if (!elem.getNamespaceURI().equals(XMLNS_DOCBOOK5)) {
                    continue fetchChildren;
                }
                String elemName = elem.getLocalName();
                
                if (elemName.equals(E_TR)) {
                    decorateRow(elem, tDivAlign, tDivVAlign);
                } else {
                    throw new SAXException("The \"" + elemName + "\" should "
                            + "not occur inside \"" + tDiv.getLocalName()
                            + "\".");
                }
            }
            // Ignore non-elements
        }
        
        checkFinishedCellMatrix();
    }
    
    private void decorateRow(
            Element tr, Alignment tDivAlign, VAlignment tDivVAlign)
            throws SAXException, DocgenException {
        addRowToCellMatrix();
        
        Alignment trAlign = null;
        VAlignment trVAlign = null;
        
        NamedNodeMap atts = tr.getAttributes();
        int attCnt = atts.getLength();
        fetchAtts: for (int attIdx = 0; attIdx < attCnt; attIdx++) {
            Attr att = (Attr) atts.item(attIdx);
            String attNS = att.getNamespaceURI();
            if (attNS != null && attNS.length() != 0) {
                continue fetchAtts;
            }
            String attName = att.getLocalName();
            String attValue = att.getValue().trim();
            
            if (attName.equals(A_ALIGN)) {
                trAlign = parseAlignAttribute(attValue, E_TR);
            } else if (attName.equals(A_VALIGN)) {
                trVAlign = parseVAlignAttribute(attValue, E_TR);
            } else {
                throw new DocgenException("The \"" + attName
                        + "\" attribute of the \"" + E_TR
                        + "\" element is not supported by Docgen.");
            }
        }  // fetchAtts
        
        if (trAlign == null) {
            trAlign = tDivAlign;
        }
        if (trVAlign == null) {
            trVAlign = tDivVAlign;
        }
        
        NodeList children = tr.getChildNodes();
        int childCnt = children.getLength();
        fetchChildren: for (int childIdx = 0; childIdx < childCnt; childIdx++) {
            Node child = children.item(childIdx);
            if (child instanceof Element) {
                Element elem = (Element) child;
                if (!elem.getNamespaceURI().equals(XMLNS_DOCBOOK5)) {
                    continue fetchChildren;
                }
                String elemName = elem.getLocalName();
                
                if (elemName.equals(E_TD) || elemName.equals(E_TH)) {
                    decorateCell(elem, trAlign, trVAlign);
                } else {
                    throw new SAXException("The \"" + elemName + "\" should "
                            + "not occur inside \"" + E_TR + "\".");
                }
            }
            // Ignore non-elements
        }
    }
    
    private void decorateCell(
            Element cell, Alignment trAlign, VAlignment trVAlign)
            throws SAXException, DocgenException {
        Alignment cellAlign = null;
        VAlignment cellVAlign = null;
        int rowSpan = 1;
        int colSpan = 1;
        
        String elemName = cell.getLocalName();
        
        NamedNodeMap atts = cell.getAttributes();
        int attCnt = atts.getLength();
        fetchAtts: for (int attIdx = 0; attIdx < attCnt; attIdx++) {
            Attr att = (Attr) atts.item(attIdx);
            String attNS = att.getNamespaceURI();
            if (attNS != null && attNS.length() != 0) {
                continue fetchAtts;
            }
            String attName = att.getLocalName();
            String attValue = att.getValue().trim();
            
            if (attName.equals(A_ALIGN)) {
                cellAlign = parseAlignAttribute(attValue, elemName);
            } else if (attName.equals(A_VALIGN)) {
                cellVAlign = parseVAlignAttribute(attValue, elemName);
            } else if (attName.equals(A_ROWSPAN)) {
                rowSpan = parseSpanAttribute(attValue, A_ROWSPAN, elemName);
            } else if (attName.equals(A_COLSPAN)) {
                colSpan = parseSpanAttribute(attValue, A_COLSPAN, elemName);
            } else {
                throw new DocgenException("The \"" + attName
                        + "\" attribute of the \"" + elemName
                        + "\" element is not supported by Docgen.");
            }
        }  // fetchAtts
        
        int visualCol = addCellToCellMatrix(rowSpan, colSpan);
        
        if (cellAlign == null) {
            Alignment colGroupAlign = colGroupAligns.size() > visualCol
                    ? colGroupAligns.get(visualCol)
                    : null;
            // Column-scope horizontal alignment has precedence over row-scope
            if (colGroupAlign != null && trAlign != colGroupAlign) {
                cell.setAttribute(A_ALIGN, colGroupAlign.toString());
            }
        }

        // Row-scope vertical alignment has precedence over column-scope
        if (cellVAlign == null && trVAlign == null) {
            VAlignment colGroupVAlign = colGroupVAligns.size() > visualCol
                    ? colGroupVAligns.get(visualCol)
                    : null;
            if (colGroupVAlign != null) {
                cell.setAttribute(A_VALIGN, colGroupVAlign.toString());
            }
        }
        
    }

    private void addRowToCellMatrix() {
        cellMatrixCurRow++;
        cellMatrixCurCol = 0;
    }
    
    private int addCellToCellMatrix(int rowSpan, int colSpan)
            throws DocgenException {
        // Find the coordinates of the first free cell in the current row:
        int curIdx = cellMatrixCurRow * cellMatrixWidth + cellMatrixCurCol;
        if (cellMatrixCurRow < cellMatrixHeight) {
            int curIdxLim = (cellMatrixCurRow + 1) * cellMatrixWidth;
            while (curIdx < curIdxLim && cellMatrix[curIdx]) {
                curIdx++;
                cellMatrixCurCol++;
            }
        }
        
        // Draw a rectangle of rowSpan*colSpan there:
        
        // - Ensure that the backing array has the required capacity:
        int newCellMatrixHeight = cellMatrixHeight;
        int newCellMatrixWidth = cellMatrixWidth;
        if (cellMatrixHeight <= cellMatrixCurRow + rowSpan - 1) {
            newCellMatrixHeight = (cellMatrixCurRow + rowSpan) * 2;
        }
        if (cellMatrixWidth <= cellMatrixCurCol + colSpan - 1) {
            newCellMatrixWidth = (cellMatrixCurCol + colSpan) * 2;
        }
        if (newCellMatrixHeight != cellMatrixHeight
                || newCellMatrixWidth != cellMatrixWidth) {
            // Resize the backing array...
            boolean[] newCellMatrix = new boolean[
                    newCellMatrixHeight * newCellMatrixWidth];
            for (int row = 0; row < cellMatrixHeight; row++) {
                System.arraycopy(
                        cellMatrix, row * cellMatrixWidth,
                        newCellMatrix, row * newCellMatrixWidth,
                        cellMatrixWidth);
            }
            cellMatrix = newCellMatrix;
            cellMatrixWidth = newCellMatrixWidth;
            cellMatrixHeight = newCellMatrixHeight;
            
            // Re-calculation needed as width maybe changed
            curIdx = cellMatrixCurRow * cellMatrixWidth + cellMatrixCurCol; 
        }
        
        // - "Pain" the rectangle:
        for (int relRow = 0; relRow < rowSpan; relRow++) {
            int brushIdx = curIdx + relRow * cellMatrixWidth;
            for (int relCol = 0; relCol < colSpan; relCol++) {
                if (cellMatrix[brushIdx]) {
                    throw new DocgenException(
                            XMLUtil.theSomethingElement(table, true)
                            + " has overlapping cells; check if \""
                            + A_ROWSPAN + "\"-s and/or \"" + A_COLSPAN
                            + "\"-s are correct.");
                }
                cellMatrix[brushIdx++] = true;
            }
        }
        
        /*
        // For debugging: Prints the current cell layout to the stdout
        System.out.println(
                "(" + cellMatrixWidth + "x" + cellMatrixHeight + ")");
        for (int row = 0; row < cellMatrixHeight; row++) {
            for (int col = 0; col < cellMatrixWidth; col++) {
                System.out.print(
                        cellMatrix[row * cellMatrixWidth + col] ? "X" : ".");
            }
            System.out.println();
        }
        */
    
        return cellMatrixCurCol;
    }
    
    private void initCellMatrix() throws DocgenException {
        cellMatrix = null;
        cellMatrixWidth = 0;
        cellMatrixHeight = 0;
        cellMatrixCurRow = -1;
        cellMatrixCurCol = 0;
    }
    
    private void checkFinishedCellMatrix() throws DocgenException {
        int firstRowUtilizedWidth = -1;
        for (int row = 0; row < cellMatrixHeight; row++) {
            int currentRowUtilizedWidth = 0;
            // We will check that there is no false -> true change, and we
            // expect the first cell will be true, or that all cells will be
            // false. So:
            boolean lastCell = true;
            for (int col = 0; col < cellMatrixWidth; col++) {
                boolean cell = cellMatrix[row * cellMatrixWidth + col];
                if (cell) {
                    // Is this false -> true change?
                    if (!lastCell) {
                        throw newNonRectangularTableException();
                    }
                    currentRowUtilizedWidth++;
                }
                lastCell = cell;
            }
            if (firstRowUtilizedWidth == -1) {
                firstRowUtilizedWidth = currentRowUtilizedWidth;
            } else if (currentRowUtilizedWidth != firstRowUtilizedWidth
                    && currentRowUtilizedWidth != 0) {
                throw newNonRectangularTableException();
            }
        }
    }
    
    private DocgenException newNonRectangularTableException() {
        return  new DocgenException("Wrong cell layout in "
                + XMLUtil.theSomethingElement(table)
                + ". Due to the bad number of cells in"
                + " some rows (or due to wrong \"" + A_COLSPAN
                + "\" and/or \"" + A_ROWSPAN + "\" values) the contour of the "
                + "table, or the contour of some of its row-groups (\""
                + E_THEAD + "\" or \"" + E_TBODY + "\" or \"" + E_TFOOT
                + "\") is not rectangular. That is, some cells protrude "
                + "out at the edge. (Visual editors may hide this error by "
                + "chopping protruding cells or by adding blind cells.)");
    }
    
    private enum Alignment {
        LEFT {
            @Override
            public String toString() {
                return "left";
            }
        },
        
        CENTER {
            @Override
            public String toString() {
                return "center";
            }
        },
        
        RIGHT {
            @Override
            public String toString() {
                return "right";
            }
        }
    }

    private enum VAlignment {
        TOP {
            @Override
            public String toString() {
                return "top";
            }
        },
        
        MIDDLE {
            @Override
            public String toString() {
                return "middle";
            }
        },
        
        BOTTOM {
            @Override
            public String toString() {
                return "bottom";
            }
        }
    }
    
}
