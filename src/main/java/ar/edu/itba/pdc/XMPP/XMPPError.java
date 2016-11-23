package ar.edu.itba.pdc.XMPP;

import static ar.edu.itba.pdc.XML.XMLUtilities.*;

public enum  XMPPError {
    BAD_FORMAT("bad-format"), MALFORMED_REQUEST("malformed_request"), INVALID_NAMESPACE("invalid-namespace"), UNDEFINED_CONDITION("undefined-condition"), NOT_AUTHORIZED("");

    private final String name;

    XMPPError(final String tagName) {
        this.name = tagName;
    }

    private static final StringBuilder stringBuilder = new StringBuilder();

    public String getError(){
        if(this == NOT_AUTHORIZED){
            return "<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><not-authorized/></failure>";
        }
        stringBuilder.setLength(0);
        return stringBuilder.append("<stream:error>")
                .append("<")
                .append(name+" "+getSerializedAttribute("xmlns","urn:ietf:params:xml:ns:xmpp-streams"))
                .append("/>")
                .append("</stream:error></stream:stream>")
                .toString();
    }

}
