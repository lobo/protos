package ar.edu.itba.pdc.XML.element;

import ar.edu.itba.pdc.utilities.PartiallySerializable;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import java.util.*;
import java.util.stream.Stream;

import static ar.edu.itba.pdc.utilities.ValidateUtilities.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class PartialXMLElement implements PartiallySerializable {

    private Optional<PartialXMLElement> parent;
    private final List<Section> sections;

    private boolean startTagEnded = false;
    private boolean ended = false;

    private static final StringBuilder stringBuilder = new StringBuilder();

    public PartialXMLElement() {
        this.sections = new LinkedList<>();
        this.parent = Optional.empty();
    }

    public PartialXMLElement readName(AsyncXMLStreamReader<?> xmlStreamReader) {
        controlNotEnded();
        control(!getNameSection().isPresent(), "%s name already is %s", this, getNameSection());
        sections.add(new NameSection(xmlStreamReader));

        return this;
    }

    public PartialXMLElement readAttributes(AsyncXMLStreamReader<?> xmlStreamReader) {
        controlNotEnded();
        control(getNameSection().isPresent(), "%s has no name", this);
        control(!getAttributesPart().isPresent(), "%s attributes already set: %s", this, getAttributesPart());
        sections.add(new AttributesSection(xmlStreamReader));
        startTagEnded = true;
        return this;
    }

    public PartialXMLElement appendToBody(AsyncXMLStreamReader<?> xmlStreamReader) {
        controlNotEnded();
        controlStartTagEnded();
        sections.add(new BodySection(xmlStreamReader));
        return this;
    }

    public PartialXMLElement addChild(PartialXMLElement child) {
        controlNotEnded();
        controlStartTagEnded();
        control(!child.isParentOf(this), "%s is the parent of %s, can't be its child", child, this);
        control(!this.isParentOf(child), "%s is the parent of %s, can't be its child", this, child);
        sections.add(new ChildSection(child));
        child.parent = Optional.of(this);
        return this;
    }

    public PartialXMLElement endElement(AsyncXMLStreamReader<?> xmlStreamReader) {
        controlNotEnded();
        controlStartTagEnded();
        sections.add(new EndingSection(xmlStreamReader));
        ended = true;
        return this;
    }


    @Override
    public String serializeContent() {
        stringBuilder.setLength(0);
        List<Section> toRemove = new LinkedList<>();
        for (Section section : sections) {
            if (section.isNotSerialized()) {
                stringBuilder.append(serialize(section));
                if (section.isNotSerialized()) {
                    //Get here when can't serialize more
                    return stringBuilder.toString();
                }
            }
            toRemove.add(section);
        }
        sections.removeAll(toRemove);
        return stringBuilder.toString();
    }

    protected String serialize(Section section) { // to be overriden by subclasses
        return section.serialize();
    }

    public String getName() {
        Optional<NameSection> namePartOpt = getNameSection();
        control(namePartOpt.isPresent(), "%s has none name",this);
        return namePartOpt.get().getName();
    }

    public Map<String, String> getAttributes() {
        Optional<AttributesSection> attributesPartOpt = getAttributesPart();
        control(attributesPartOpt.isPresent(), "%s has none attributes", this);
        return attributesPartOpt.get().getAttributes();
    }

    public Map<String, String> getNamespaces() {
        Optional<AttributesSection> attributesPartOpt = getAttributesPart();
        control(attributesPartOpt.isPresent(),"%s has none namespace", this);
        return attributesPartOpt.get().getNamespaces();
    }

    public String getBody() {
        Stream<BodySection> bodyParts = getPartsOfClassAsStream(BodySection.class);
        return bodyParts.map(BodySection::getText).collect(joining());
    }

    public Iterable<? extends PartialXMLElement> getChildren() {
        return getPartsOfClassAsStream(ChildSection.class).map(
                ChildSection::getChild).collect(toList());
    }

    public Optional<? extends PartialXMLElement> getParent() {
        return this.parent;
    }

    /* package */ boolean isCurrentContentFullySerialized() {
        return sections.isEmpty() && ended;
    }

    private boolean isParentOf(PartialXMLElement child) {
        return child != this && child.getParent().isPresent()
                && getChildrenAsStream().anyMatch(myChild -> myChild == child || myChild.isParentOf(child));
    }

    private Stream<PartialXMLElement> getChildrenAsStream() {
        return getPartsOfClassAsStream(ChildSection.class).map(ChildSection::getChild);
    }

    private Optional<NameSection> getNameSection() {
        return getPartsOfClassAsStream(NameSection.class).findFirst();
    }

    private Optional<AttributesSection> getAttributesPart() {
        return getPartsOfClassAsStream(AttributesSection.class).findFirst();
    }

    private <S extends Section> Stream<S> getPartsOfClassAsStream(Class<S> partClass) {
        return sections.stream().filter(partClass::isInstance).map(partClass::cast);
    }

    private void controlStartTagEnded() {
        control(startTagEnded, "%s has none start tag", this);
    }

    private void controlNotEnded() {
        control(!ended, "%s already ended", this);
    }

    private Optional<EndingSection> getEndingSection() {
        control(ended, "%s hasn't ended yet", this);
        if (!sections.isEmpty()) {
            Section lastSection = sections.get(sections.size()-1);
            if (lastSection instanceof EndingSection) {
                return Optional.of((EndingSection) lastSection);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        Stream<Section> unserializedParts = sections.stream();
        String toSerialize = unserializedParts.map(
                this::getSerialization).collect(joining());

        return "\nTo serialize: \n" + toSerialize;
    }

    private String getSerialization(Section section) {
        return section.getSerialization();
    }
}
