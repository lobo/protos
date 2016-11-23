package ar.edu.itba.pdc.XML;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Map.Entry;

public abstract class XMLUtilities {
    public static final String XML_DOCUMENT_START = "<?xml version=\"1.0\"?>";

    private static final StringBuilder stringBuilder = new StringBuilder();

    public static String serializeQName(QName qname) {
        String prefix = qname.getPrefix();
        stringBuilder.setLength(0);
        return prefix.isEmpty()?qname.getLocalPart(): stringBuilder.append(prefix).append(":").append(qname.getLocalPart()).toString();
    }

    public static String serializeNamespaces(Map<String, String> namespaces) {
        String ans ="";
        for (Entry<String, String> entry : namespaces.entrySet()) {
            String localPart = entry.getKey();
            String namespace = localPart.isEmpty() ? "xmlns" : ("xmlns:" + localPart);

            ans +=" "+getSerializedAttribute(namespace,entry.getValue());
        }
        return ans;
    }

    public static String serializeAttributes(Map<String, String> attributes) {
        String ans ="";
        for (Entry<String, String> entry : attributes.entrySet()) {

            ans += " " + getSerializedAttribute(entry.getKey(),entry.getValue());
        }
        return ans;
    }

    public static String getSerializedAttribute(String key, String value) {

        stringBuilder.setLength(0);
        return stringBuilder
                .append(key)
                .append("=\"")
                .append(value)
                .append("\"")
                .toString();
    }

}
