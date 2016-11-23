package ar.edu.itba.pdc.XML.element;

import com.fasterxml.aalto.AsyncXMLStreamReader;

import static ar.edu.itba.pdc.XML.XMLUtilities.serializeQName;

/* package */  class EndingSection extends Section {
    private String name;

    /* package */  EndingSection(AsyncXMLStreamReader<?> from) {
        this.name = serializeQName(from.getName());
    }

    @Override
    String getSerialization() {
        return "</" + name + ">";
    }

}
