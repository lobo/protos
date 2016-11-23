package ar.edu.itba.pdc.XMPP.element;

import ar.edu.itba.pdc.XML.element.PartialXMLElement;
import ar.edu.itba.pdc.utilities.PartiallySerializable;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import static ar.edu.itba.pdc.XML.XMLUtilities.serializeQName;

public class PartialXMPPElement extends PartialXMLElement implements PartiallySerializable{

    private final PartialXMPPElementType type;

    public static PartialXMPPElement fromReader(AsyncXMLStreamReader<?> reader) {
        String elementName = serializeQName(reader.getName());
        switch (PartialXMPPElementType.parseName(elementName)){
            case MESSAGE:
                return new MessageElement(reader);
            case SUBJECT:
            case BODY:
                return new MessageElement.ModificableMessageSubElement(reader);
        }
        return new PartialXMPPElement(reader);
    }

    /*package*/ PartialXMPPElement(AsyncXMLStreamReader<?> reader){
        readName(reader);
        readAttributes(reader);

        type = PartialXMPPElementType.parseName(getName());
    }

    public PartialXMPPElementType getType() {
        return type;
    }

}
