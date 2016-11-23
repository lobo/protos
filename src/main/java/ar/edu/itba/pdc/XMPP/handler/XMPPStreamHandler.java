package ar.edu.itba.pdc.XMPP.handler;

import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.XML.handler.XMLStreamHandler;
import ar.edu.itba.pdc.XMPP.XMPPConversation;
import ar.edu.itba.pdc.XMPP.XMPPError;
import ar.edu.itba.pdc.XMPP.element.PartialXMPPElement;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.ByteBuffer;

import static ar.edu.itba.pdc.XMPP.XMPPError.BAD_FORMAT;

public abstract class XMPPStreamHandler extends XMLStreamHandler implements XMPPEventHandler{

    /* Is different from basicTCPHandler, will have clientToServerHandler and
     * serverToClientHandler. So, will need only one stream
     */
    private final TCPStream representativeStream;
    protected final TCPStream clientToServer;
    protected final TCPStream serverToClient;
    protected final XMPPConversation conversation;

    //See as PartialXMLElement, we only manage one element at specific time
    private PartialXMPPElement element;

    protected XMPPStreamHandler(final TCPStream clientToServer, final TCPStream serverToClient, final TCPStream representativeStream, final XMPPConversation conversation) throws XMLStreamException{
        this.clientToServer = clientToServer;
        this.serverToClient = serverToClient;
        this.conversation = conversation;
        this.representativeStream = representativeStream;
    }

    @Override
    public void handleElementStart(AsyncXMLStreamReader<?> reader) {
        //XML handling
        if (element == null) {
            element = PartialXMPPElement.fromReader(reader);
        } else {
            PartialXMPPElement newXMPPElement = PartialXMPPElement.fromReader(reader);
            element.addChild(newXMPPElement);
            element = newXMPPElement;
        }
        //XMPP handling
        handleStart(element);
    }

    @Override
    public void handleElementEnding(AsyncXMLStreamReader<?> reader) {
        // XML handling
        element.endElement(reader);

        /*if (lastOpenTag.equals(element.getEndSectionName())){
            LoggerFactory.getLogger("QEW").info(element.getEndSectionName());
        }*/


        // XMPP handling
        handleEnd(element);

        if(element.getParent().isPresent()){
            if(element.getParent().get() instanceof PartialXMPPElement)
                element = (PartialXMPPElement) element.getParent().get();
            else
                element = null;
        } else {
            element = null;
        }
    }

    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        element.appendToBody(reader);

        handleCharacters(element);
    }

    @Override
    public void handleException(Exception e) {
        sendError(BAD_FORMAT);
    }


    protected void sendToClient(String message){
        if(message.length() > serverToClient.getDestinyBuffer().capacity() - serverToClient.getDestinyBuffer().position()){
            serverToClient.amplyDestinyBuffer(message.length() + serverToClient.getDestinyBuffer().position());
            //clientToServer.amplyOriginBuffer(message.length() + serverToClient.getDestinyBuffer().position());
            sendToClient(message);
        }else
        writeStringInStream(message,serverToClient);
    }

    protected void sendToServer(String message){

        if(message.length() > clientToServer.getDestinyBuffer().capacity() - clientToServer.getDestinyBuffer().position()){
            //serverToClient.amplyDestinyBuffer(message.length() + serverToClient.getDestinyBuffer().position());
            clientToServer.amplyDestinyBuffer(message.length() + clientToServer.getDestinyBuffer().position());
            sendToServer(message);
        }else {
            writeStringInStream(message, clientToServer);
        }
    }

    public void sendToRepresentative(String message){
        writeStringInStream(message,clientToServer);
    }

    protected void sendError(XMPPError xmppError){
        sendToClient(xmppError.getError());
        if(xmppError != XMPPError.NOT_AUTHORIZED){
            conversation.closeChannels();
        }
    }

    @Override
    protected void toSleep(){
        super.toSleep();
        representativeStream.toSleepStream();
    }

    @Override
    public void wakeUp(){
        ByteBuffer buffer = representativeStream.getDestinyBuffer();
        buffer.flip(); //Wake Up from reading mode
        super.wakeUp();
        handleRead(buffer,null);//Already connected
        representativeStream.wakeUpStream();
    }

    protected void waitForData(){
        this.toSleep();
    }

    public abstract void notifyNewData();

}
