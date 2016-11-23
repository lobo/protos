package ar.edu.itba.pdc.XMPP;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static ar.edu.itba.pdc.XML.XMLUtilities.*;

public abstract class XMPPUtilities {

    private final static StringBuilder builder = new StringBuilder();
    public static String getOpenStreamTag(){
        return getOpenStreamTag(null,null);
    }

    public static String getOpenStreamTag(String from, String to){
        StringBuilder builder1 = new StringBuilder();
        builder1.append("<stream:stream ")
                .append(getSerializedAttribute("xmlns:stream", "http://etherx.jabber.org/streams")) .append(" ")
                .append(getSerializedAttribute("version", "1.0"))                                   .append(" ")
                .append(getSerializedAttribute("xmlns", "jabber:client"))                           .append(" ")
                .append(getSerializedAttribute("xml:lang", "en"))                                   .append(" ")
                .append(getSerializedAttribute("xmlns:xml","http://www.w3.org/XML/1998/namespace"));
        if(from != null){
            builder1.insert(builder1.length()," ").append(getSerializedAttribute("from",from));
        }
        if(to != null){
            builder1.insert(builder1.length()," ").append(getSerializedAttribute("to",to));
        }
        builder1.append(">");
        return builder1.toString();
    }

    public static String getFeaturesTag(){

        builder.setLength(0);
        return builder
                .append("<stream:features>")
                .append("<register ")
                .append(getSerializedAttribute("xmlns","http://jabber.org/features/iq-register"))
                .append("/>")
                .append("<mechanisms ")
                .append(getSerializedAttribute("xmlns","urn:ietf:params:xml:ns:xmpp-sasl"))
                .append(">")
                .append("<mechanism>")
                .append("PLAIN") //Can append future auth-mechanism
                .append("</mechanism>")
                .append("</mechanisms>")
                .append("</stream:features>")
                .toString();

    }

    public static String[] decodeCredentials(String chars){
        //Credentials data came with \n \t \r, removes them
        String cleanCredentials = chars.replaceAll("\n","").replaceAll("\t","").replaceAll("\r","");
        String decodedCredentials = new String(Base64.getDecoder().decode(cleanCredentials), StandardCharsets.UTF_8);
        //Removes initial '/0'
        if(decodedCredentials.charAt(0)=='\0'){
            decodedCredentials = decodedCredentials.substring(1);
        }
        //Removes separating '\0'
        return decodedCredentials.split("\0");
    }

    public static String getAuthTag(String credentials){

        builder.setLength(0);
        return
                builder
                        .append("<auth ")
                            .append(getSerializedAttribute("xmlns","urn:ietf:params:xml:ns:xmpp-sasl")
                            + " "+ getSerializedAttribute("mechanism","PLAIN"))//Can change to param
                            .append(">")
                        .append(credentials)
                        .append("</auth>")
                .toString();

    }

    public static String encodeCredentials(String username, String password){
        String credentiaString = "\0" + username + "\0" + password;
        return new String(Base64.getEncoder().encode(credentiaString.getBytes()), StandardCharsets.UTF_8);
    }


    public static String getMessageTag(String messageText, String from, String to){

        builder.setLength(0);
        return builder
                .append("<message ")
                .append(getSerializedAttribute("from", from)
                + " " + getSerializedAttribute("to", to)
                + " " + getSerializedAttribute("type", "chat"))
                .append(">")
                    .append("<body>")
                    .append(messageText)
                    .append("</body>")
                .append("</message>")
                .toString();
    }

}
