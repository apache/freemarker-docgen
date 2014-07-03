package org.freemarker.docgen;

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
