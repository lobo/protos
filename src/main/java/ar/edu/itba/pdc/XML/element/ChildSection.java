package ar.edu.itba.pdc.XML.element;

/* package */ class ChildSection extends Section {
    private final PartialXMLElement child;

    ChildSection(final PartialXMLElement child) {
        this.child = child;
    }

    /* package */  PartialXMLElement getChild() {
        return child;
    }

    @Override
    /* package */ String serialize() {
        return child.serializeContent();
    }

    @Override
    /* package */ boolean isNotSerialized() {
        return !child.isCurrentContentFullySerialized();
    }
}
