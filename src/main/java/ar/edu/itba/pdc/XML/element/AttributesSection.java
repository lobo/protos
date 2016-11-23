package ar.edu.itba.pdc.XML.element;

import com.fasterxml.aalto.AsyncXMLStreamReader;

import java.util.LinkedHashMap;
import java.util.Map;

import static ar.edu.itba.pdc.XML.XMLUtilities.*;
import static java.util.Collections.unmodifiableMap;

/* package */ class AttributesSection extends Section {
    private final Map<String, String> namespaces = new LinkedHashMap<>();
    private final Map<String, String> attributes = new LinkedHashMap<>();


    /* package */ AttributesSection(AsyncXMLStreamReader<?> reader) {
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            String prefix = reader.getNamespacePrefix(i);
            String uri = reader.getNamespaceURI(i);
            namespaces.put(prefix, uri);
        }

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String name = serializeQName(reader.getAttributeName(i));
            String value = reader.getAttributeValue(i);
            attributes.put(name, value);
        }
    }

    /* package */ Map<String, String> getAttributes() {
        return unmodifiableMap(attributes);
    }

    /* package */ Map<String, String> getNamespaces() {
        return unmodifiableMap(namespaces);
    }

    @Override
    /* package */ String getSerialization() {

        return serializeNamespaces(namespaces)
                + serializeAttributes(attributes) + ">";
    }
}
