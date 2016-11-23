package ar.edu.itba.pdc.XML.handler;

import com.fasterxml.aalto.AsyncXMLStreamReader;

import javax.xml.stream.XMLStreamException;


public interface XMLEventHandler {

    void handleElementStart(AsyncXMLStreamReader<?> reader);

    void handleCharacters(AsyncXMLStreamReader<?> reader);

    void handleElementEnding(AsyncXMLStreamReader<?> reader);

    void handleException(XMLStreamException exc);
}
