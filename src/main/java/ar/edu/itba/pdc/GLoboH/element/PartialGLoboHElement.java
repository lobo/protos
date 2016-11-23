package ar.edu.itba.pdc.GLoboH.element;

import ar.edu.itba.pdc.XML.element.PartialXMLElement;
import ar.edu.itba.pdc.utilities.PartiallySerializable;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

public class PartialGLoboHElement implements PartiallySerializable {

    public static PartialGLoboHElement fromXMLELement(PartialXMLElement element) {
        return new PartialGLoboHElement(element);
    }

    private final PartialXMLElement xmlElement;
    private final PartialGLoboHElementType type;

    private PartialGLoboHElement(final PartialXMLElement xmlElement) {
        this.xmlElement = xmlElement;
        this.type = PartialGLoboHElementType.parseName(xmlElement.getName());
    }

    public PartialXMLElement getXML() {
        return xmlElement;
    }

    public PartialGLoboHElementType getType() {
        return type;
    }

    @Override
    public String serializeContent() {
        return xmlElement.serializeContent();
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
