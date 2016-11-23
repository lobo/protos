package ar.edu.itba.pdc.XMPP.handler.server2client;

import ar.edu.itba.pdc.GLoboH.GLoboHData;
import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.XMPP.XMPPConversation;
import ar.edu.itba.pdc.XMPP.element.MessageElement;
import ar.edu.itba.pdc.XMPP.element.PartialXMPPElement;
import ar.edu.itba.pdc.XMPP.handler.XMPPStreamHandler;

import javax.xml.stream.XMLStreamException;

import static ar.edu.itba.pdc.XML.XMLUtilities.*;
import static ar.edu.itba.pdc.XMPP.XMPPError.*;
import static ar.edu.itba.pdc.XMPP.element.PartialXMPPElementType.*;
import static ar.edu.itba.pdc.XMPP.handler.server2client.ServerToClientXMPPStreamHandlerState.*;
import static ar.edu.itba.pdc.XMPP.XMPPUtilities.getAuthTag;

public class ServerToClientXMPPStreamHandler extends XMPPStreamHandler {

    private ServerToClientXMPPStreamHandlerState state;

    private final GLoboHData gLoboHData = GLoboHData.getInstance();

    public ServerToClientXMPPStreamHandler(final TCPStream clientToServer,final TCPStream serverToClient,final XMPPConversation conversation) throws XMLStreamException{
        super(clientToServer,serverToClient,serverToClient,conversation);
        state = INIT;
    }

    @Override
    public void handleStart(PartialXMPPElement xmppElement) {
        switch (state){
            case INIT:
                if(xmppElement.getType() != STREAM_STREAM){
                    sendError(INVALID_NAMESPACE);
                }
                state = WAITING_AUTH_FEATURES;
                break;
            case WAITING_AUTH_FEATURES:
                if(xmppElement.getType() == STREAM_ERROR){
                    sendError(NOT_AUTHORIZED);
                }
                xmppElement.removeContent();
                break;
            case WAITING_SERVER_ACCREDITATION:
                if(xmppElement.getType() != AUTH_SUCCESS){
                    sendError(NOT_AUTHORIZED);
                }
                break;
            case AUTH_OK:
                //The stream was reset so must receive STREAM_STREAM
                if(xmppElement.getType() != STREAM_STREAM){
                    sendError(INVALID_NAMESPACE);
                }
                sendToClient(XML_DOCUMENT_START);
                sendToClient(xmppElement.serializeContent());
                state = CONNECTED;
                break;
            case CONNECTED:
                if (xmppElement.getType() == MESSAGE){
                    MessageElement messageElement = (MessageElement) xmppElement;
                    boolean isMuted = gLoboHData.isJIDMuted(gLoboHData.getJID(conversation.getUsername()));
                    if(messageElement.getFrom() != null) {
                        isMuted |= gLoboHData.isJIDMuted(messageElement.getFrom());
                    }
                    if(isMuted){
                        state = MUTED;
                    }
                }
                sendToClient(xmppElement.serializeContent());
                break;
            case MUTED:
                switch (xmppElement.getType()){
                    case BODY:
                    case SUBJECT:
                        //Ignore content

                        MessageElement messageElement = (MessageElement) xmppElement.getParent().get();
                        boolean isMuted = gLoboHData.isJIDMuted(gLoboHData.getJID(conversation.getUsername()));
                        if(messageElement.getFrom() != null) {
                            isMuted |= gLoboHData.isJIDMuted(messageElement.getFrom());
                        }
                        if(isMuted){
                            xmppElement.removeContent();
                        }else{
                            state = CONNECTED;
                            sendToClient(xmppElement.serializeContent());
                        }
                        break;
                    default:
                        sendToClient(xmppElement.serializeContent());
                }
                break;
        }
    }

    @Override
    public void handleCharacters(PartialXMPPElement xmppElement) {
        switch (state){
            case INIT:
            case WAITING_AUTH_FEATURES:
            case AUTH_OK:
                xmppElement.removeContent();
                break;
            case WAITING_SERVER_ACCREDITATION:
                break;
            case CONNECTED:
                sendToClient(xmppElement.serializeContent());
                break;
            case MUTED:
                //IGNORE CONTENT
                switch (xmppElement.getType()){
                    case BODY:
                    case MESSAGE:
                    case SUBJECT:
                        xmppElement.removeContent();
                        break;
                    default:
                        sendToClient(xmppElement.serializeContent());
                        break;
                }
                break;
        }
    }

    @Override
    public void handleEnd(PartialXMPPElement xmppElement) {
        switch (state){
            case INIT:
            case AUTH_OK:
                sendError(UNDEFINED_CONDITION);
                break;
            case WAITING_AUTH_FEATURES:
                switch (xmppElement.getType()){
                    case STREAM_STREAM:
                        sendError(NOT_AUTHORIZED);
                        break;
                    case AUTH_STREAM_FEATURES:
                        sendToServer(getAuthTag(conversation.getEncodedCredentials()));
                        state = WAITING_SERVER_ACCREDITATION;
                        break;
                    default:
                        xmppElement.removeContent();
                }
                break;
            case WAITING_SERVER_ACCREDITATION:
                switch (xmppElement.getType()){
                    case STREAM_STREAM:
                        sendToClient(xmppElement.serializeContent()); //Here remove content
                        sendError(NOT_AUTHORIZED);
                        break;
                    case AUTH_SUCCESS:
                        sendToClient(xmppElement.serializeContent()); //Here remove content
                        state = AUTH_OK;
                        resetStream();
                        notifyNewData();
                        break;
                    default:
                        sendError(NOT_AUTHORIZED);
                        break;
                }
                break;
            case CONNECTED:
                sendToClient(xmppElement.serializeContent());
                if(xmppElement.getType() == MESSAGE){
                    gLoboHData.newReceivedMessage(gLoboHData.getJID(((MessageElement)xmppElement).getTo()));
                }
                break;
            case MUTED:
                switch (xmppElement.getType()){
                    case BODY:
                    case SUBJECT:
                        xmppElement.removeContent();
                        break;
                    case MESSAGE:
                        gLoboHData.newMutedMessagesOut();
                        sendToClient(xmppElement.serializeContent());
                        break;
                    default:
                        sendToClient(xmppElement.serializeContent());
                        break;
                }
                break;
        }
    }

    @Override
    public void handleException(XMLStreamException e) {
        sendError(MALFORMED_REQUEST);
    }

    @Override
    public void notifyNewData() {
        ((XMPPStreamHandler)clientToServer.getHandler()).wakeUp();
    }
}
