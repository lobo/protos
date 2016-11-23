package ar.edu.itba.pdc.XML.element;

import com.fasterxml.aalto.AsyncXMLStreamReader;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml10;

public class BodySection extends Section {
    private final String text;

    BodySection(final AsyncXMLStreamReader<?> from) {
        this.text = from.getText();
    }

    String getText() {
        return text;
    }

    @Override
    String getSerialization() {
        return escapeXml10(text);
    }
}
