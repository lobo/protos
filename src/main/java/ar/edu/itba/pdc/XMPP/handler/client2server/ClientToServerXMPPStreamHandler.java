package ar.edu.itba.pdc.XMPP.handler.client2server;

import ar.edu.itba.pdc.GLoboH.GLoboHData;
import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.XMPP.XMPPConversation;
import ar.edu.itba.pdc.XMPP.XMPPUtilities;
import ar.edu.itba.pdc.XMPP.element.MessageElement;
import ar.edu.itba.pdc.XMPP.element.PartialXMPPElement;

import static ar.edu.itba.pdc.XML.XMLUtilities.XML_DOCUMENT_START;
import static ar.edu.itba.pdc.XMPP.XMPPUtilities.*;
import static ar.edu.itba.pdc.XMPP.element.PartialXMPPElementType.*;
import static ar.edu.itba.pdc.XMPP.handler.client2server.ClientToServerXMPPStreamHandlerState.*;
import static ar.edu.itba.pdc.XMPP.XMPPError.*;

import ar.edu.itba.pdc.XMPP.handler.XMPPStreamHandler;

import javax.xml.stream.XMLStreamException;
import java.net.InetSocketAddress;

public class ClientToServerXMPPStreamHandler extends XMPPStreamHandler {

    private ClientToServerXMPPStreamHandlerState state;

    private GLoboHData gLoboHData;
    private static final StringBuilder stringBuilder = new StringBuilder();

    public ClientToServerXMPPStreamHandler(final TCPStream clientToServer,final TCPStream serverToClient,final XMPPConversation conversation) throws XMLStreamException{
        super(clientToServer,serverToClient,clientToServer,conversation);
        state = INIT;
        gLoboHData = GLoboHData.getInstance();
        }

    @Override
    public void handleStart(PartialXMPPElement xmppElement) {
        switch (state){
            case INIT:
                //Got here at te beginning, we may multiplex so must simulate be the server in case o errors

                sendToClient(XML_DOCUMENT_START);
                sendToClient(getOpenStreamTag());

                if(xmppElement.getType() != STREAM_STREAM){
                    sendError(INVALID_NAMESPACE);
                }else{
                    sendToClient(getFeaturesTag());
                    //Still not connected to server must get <auth> stream tag to get user and pass (CredentialInfo.class)
                    state = WAITING_CREDENTIALS;
                }
                break;
            case WAITING_CREDENTIALS:
                if(xmppElement.getType() != AUTH){
                    xmppElement.removeContent();
                }
                break;
            case WAITING_SERVER_ACCREDITATION:
                //The stream was reset so must receive STREAM_STREAM
                if(xmppElement.getType() != STREAM_STREAM){
                    sendError(INVALID_NAMESPACE);
                }

                sendToServer(XML_DOCUMENT_START);
                sendToServer(xmppElement.serializeContent());

                state = CONNECTED;
                break;
            case CONNECTED:
                if (xmppElement.getType() == MESSAGE){
                    MessageElement messageElement = (MessageElement) xmppElement;

                    boolean isStillMuted = gLoboHData.isJIDMuted(gLoboHData.getJID(conversation.getUsername()));
                    if(messageElement.getTo() != null) {
                        isStillMuted |= gLoboHData.isJIDMuted(messageElement.getTo());
                    }
                    if(isStillMuted){
                        state = MUTED;
                    }else if(gLoboHData.isL33tActivated()){
                        messageElement.enableL33t();
                    }
                }
                sendToServer(xmppElement.serializeContent());
                break;
            case MUTED:
                switch (xmppElement.getType()){
                    case BODY:
                    case SUBJECT:
                        //Notifying!
                        MessageElement messageElement = (MessageElement) xmppElement.getParent().get();
                        stringBuilder.setLength(0);

                        boolean isOtherMuted = false;
                        boolean mutingGuilty = gLoboHData.isJIDMuted(gLoboHData.getJID(conversation.getUsername()));
                        if(messageElement.getTo() != null) {
                            isOtherMuted = gLoboHData.isJIDMuted(messageElement.getTo());
                        }
                        if(isOtherMuted || mutingGuilty){
                            gLoboHData.newMutedMessagesIn();
                        } else if(gLoboHData.isL33tActivated()){
                            messageElement.enableL33t();
                        }

                        if (mutingGuilty){
                            stringBuilder.append("You are muted! Unless that change you won't be able to talk anymore :(");
                        }else if(isOtherMuted){
                            stringBuilder.append("I'm muted! You can't talk with me for now :'(");
                        }else {
                            state = CONNECTED;
                            sendToClient(getMessageTag("The mute is over, you can talk now :D -_(^.^)_-",messageElement.getTo(),conversation.getUsername()));
                            sendToServer(xmppElement.serializeContent());
                            break;
                        }
                        sendToClient(getMessageTag(stringBuilder.toString(),messageElement.getTo(),conversation.getUsername()));
                        xmppElement.removeContent();
                        break;
                    default:
                        sendToServer(xmppElement.serializeContent());
                        break;
                }
                break;
        }
    }



    @Override
    public void handleCharacters(PartialXMPPElement xmppElement) {
        switch (state){
            case INIT:
            case WAITING_SERVER_ACCREDITATION:
                xmppElement.removeContent();
                break;
            case CONNECTED:
                if (xmppElement.getType() == MESSAGE){
                    MessageElement messageElement = (MessageElement) xmppElement;
                    if(gLoboHData.isL33tActivated()){
                        messageElement.enableL33t();
                    }
                }
                sendToServer(xmppElement.serializeContent());
                break;
            case MUTED:
                switch (xmppElement.getType()){
                    //Ignore content
                    case SUBJECT:
                    case BODY:
                    case MESSAGE:
                        xmppElement.removeContent();
                        break;
                    default:
                        sendToServer(xmppElement.serializeContent());
                }
                break;
        }
    }


    @Override
    public void handleEnd(PartialXMPPElement xmppElement) {
        switch (state){
            case INIT:
                //Must never happen
                break;
            case WAITING_CREDENTIALS:
                if(xmppElement.getType() != AUTH) {
                    sendError(INVALID_NAMESPACE);
                }else{
                    String[] credentials = XMPPUtilities.decodeCredentials(xmppElement.getBody());
                    if(credentials.length != 2){
                        sendError(NOT_AUTHORIZED);
                    }else{
                        String username = credentials[0];
                        String password = credentials[1];

                        conversation.setCredentials(username,password);

                        InetSocketAddress socketAddress = gLoboHData.getServerAddress(username);
                        getFutureServerConnector().connectToServer(socketAddress);

                        //send open stream to server
                        sendToServer(XML_DOCUMENT_START);
                        sendToServer(getOpenStreamTag(gLoboHData.getJID(username),socketAddress.getHostName()));
                        resetStream();

                        gLoboHData.newUser(username);

                        state = WAITING_SERVER_ACCREDITATION;
                        waitForData(); //Now the proxy must negotiate with the server, and notify after that. Meanwhile, I will sleep Zzzz...
                    }
                }
                break;
            case WAITING_SERVER_ACCREDITATION:
                //SHOULD NEVER GET HERE
                sendError(NOT_AUTHORIZED);
                break;
            case CONNECTED:
                sendToServer(xmppElement.serializeContent());
                if(xmppElement.getType() == MESSAGE){
                    gLoboHData.newSentMessage(gLoboHData.getJID(((MessageElement)xmppElement).getFrom()));
                }
                break;
            case MUTED:
                switch (xmppElement.getType()){
                    case SUBJECT:
                    case BODY:
                        xmppElement.removeContent();
                        break;
                    case MESSAGE:
                        sendToServer(xmppElement.serializeContent());
                        break;
                    default:
                        sendToServer(xmppElement.serializeContent());
                        break;
                }
                break;
        }
    }

    @Override
    public void handleException(XMLStreamException e) {
        sendError(MALFORMED_REQUEST);//SHOULD NEVER HAPPEN.
    }

    public void notifyNewData(){
        ((XMPPStreamHandler)serverToClient.getHandler()).wakeUp();
    }

}
