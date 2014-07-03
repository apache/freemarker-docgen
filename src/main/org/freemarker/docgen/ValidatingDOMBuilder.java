package org.freemarker.docgen;

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
