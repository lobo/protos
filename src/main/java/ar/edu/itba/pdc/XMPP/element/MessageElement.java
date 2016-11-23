package ar.edu.itba.pdc.XMPP.element;

import ar.edu.itba.pdc.XML.element.BodySection;
import ar.edu.itba.pdc.XML.element.PartialXMLElement;
import ar.edu.itba.pdc.XML.element.Section;
import com.fasterxml.aalto.AsyncXMLStreamReader;

public class MessageElement extends PartialXMPPElement{
    private static final String FROM_ATTRIBUTE = "from";
    private static final String TO_ATTRIBUTE = "to";

    private String from;
    private String to;
    private boolean toL33t = false;

    public MessageElement(AsyncXMLStreamReader<?> reader) {
        super(reader);
    }

    @Override
    public PartialXMLElement readAttributes(AsyncXMLStreamReader<?> reader) {
        super.readAttributes(reader);
        this.from = getAttributes().get(FROM_ATTRIBUTE);
        this.to = getAttributes().get(TO_ATTRIBUTE);
        return this;
    }

    @Override
    public PartialXMLElement addChild(PartialXMLElement child) {
        super.addChild(child);
        if (child instanceof ModificableMessageSubElement && toL33t) {
            ((ModificableMessageSubElement) child).enableL33t();
        }
        return this;
    }

    public void enableL33t() {
        this.toL33t = true;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public static class ModificableMessageSubElement extends PartialXMPPElement{

        private static final StringBuilder transformed = new StringBuilder();

        private boolean toL33t = false;

        ModificableMessageSubElement(AsyncXMLStreamReader<?> reader){ super(reader);}

        void enableL33t(){
            toL33t = true;
        }

        @Override
        public String serialize(Section section){
            String serialized = super.serialize(section);
            if(section instanceof BodySection && toL33t){
                transformed.setLength(0);
                for (int i = 0; i < serialized.length(); i++) {
                    switch (serialized.charAt(i)) {
                        case 'a':
                            transformed.append("4");
                            break;
                        case 'e':
                            transformed.append("3");
                            break;
                        case 'i':
                            transformed.append("1");
                            break;
                        case 'o':
                            transformed.append("0");
                            break;
                        case 'c':
                            transformed.append("&lt;");
                            break;
                        default:
                            transformed.append(serialized.charAt(i));
                            break;
                    }
                }
                serialized = transformed.toString();
            }
            return serialized;
        }
    }
}
