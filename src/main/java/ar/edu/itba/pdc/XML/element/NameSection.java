package ar.edu.itba.pdc.XML.element;

import com.fasterxml.aalto.AsyncXMLStreamReader;

import static ar.edu.itba.pdc.XML.XMLUtilities.serializeQName;

/*package*/ class NameSection extends Section {
    private String name;

    /*package*/ NameSection(final AsyncXMLStreamReader<?> reader) {
        this.name = serializeQName(reader.getName());
    }

    /*package*/ String getName() {
        return name;
    }

    @Override
    String getSerialization() {
        return "<" + name;
    }
}
