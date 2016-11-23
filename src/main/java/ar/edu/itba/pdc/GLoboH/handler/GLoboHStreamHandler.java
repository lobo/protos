package ar.edu.itba.pdc.GLoboH.handler;

import ar.edu.itba.pdc.GLoboH.GLoboHConversation;
import ar.edu.itba.pdc.GLoboH.GLoboHData;
import ar.edu.itba.pdc.GLoboH.GLoboHError;
import ar.edu.itba.pdc.GLoboH.GLoboHState;
import ar.edu.itba.pdc.GLoboH.element.PartialGLoboHElement;
import ar.edu.itba.pdc.GLoboH.element.PartialGLoboHElementType;
import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.proxy.ProxyData;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.util.NoSuchElementException;

import ar.edu.itba.pdc.XML.handler.XMLEventHandler;
import ar.edu.itba.pdc.XML.handler.XMLStreamHandler;
import ar.edu.itba.pdc.XML.element.*;

import static ar.edu.itba.pdc.GLoboH.GLoboHError.*;
import static ar.edu.itba.pdc.GLoboH.GLoboHState.*;
import static ar.edu.itba.pdc.GLoboH.element.PartialGLoboHElementType.*;
import static ar.edu.itba.pdc.XML.XMLUtilities.XML_DOCUMENT_START;
import static ar.edu.itba.pdc.utilities.ValidateUtilities.control;

public class GLoboHStreamHandler extends XMLStreamHandler implements XMLEventHandler {

    private PartialXMLElement xmlElement;
    private final GLoboHConversation conversation;
    private GLoboHState state = STARTED;
    private final TCPStream toClientStream;

    private final ProxyData proxyData = ProxyData.getInstance();
    private final GLoboHData globohData = GLoboHData.getInstance();

    private static final Logger logger = LoggerFactory.getLogger(GLoboHStreamHandler.class);
    private static final StringBuilder stringBuilder = new StringBuilder();

    public GLoboHStreamHandler(GLoboHConversation conversation,TCPStream toClientStream) throws XMLStreamException {
        this.conversation = conversation;
        this.toClientStream = toClientStream;
    }

    @Override
    public void handleElementStart(AsyncXMLStreamReader<?> reader) {
        if (xmlElement == null) {
            xmlElement = new PartialXMLElement();
        } else {
            PartialXMLElement newXMLElement = new PartialXMLElement();
            xmlElement.addChild(newXMLElement);
            xmlElement = newXMLElement;
        }
        xmlElement.readName(reader).readAttributes(reader);

        try {
            handleStart(PartialGLoboHElement.fromXMLELement(xmlElement));
        } catch (IllegalStateException e) {
            sendError(UNEXPECTED_COMMAND);
            exitGLoboH();
        }
    }

    @Override
    public void handleElementEnding(AsyncXMLStreamReader<?> reader) {
        xmlElement.endElement(reader);

        handleEnd(PartialGLoboHElement.fromXMLELement(xmlElement));

        try{
            xmlElement = xmlElement.getParent().get();
        }catch (NoSuchElementException e){
            // We do nothing because we have already sent an error
        }
         
    }

    /**
     * Handle start section of the GLoboH element
     * @param element The element received
     */
    private void handleStart(PartialGLoboHElement element) {
        switch (state) {
            case STARTED:

                switch (element.getType()) {
                    case GLOBOH:
                        state = WAITING_AUTH;
                        sendToClient(XML_DOCUMENT_START+"\n");
                        sendToClient("<hello>\n");
                        break;
                    case OTHER:
                        sendError(UNRECOGNIZED_COMMAND);
                        exitGLoboH();
                        break;
                    default:
                        sendError(UNEXPECTED_COMMAND);
                        break;
                }
                break;
            case WAITING_AUTH:
                switch (element.getType()) {
                    case AUTH:
                        state = READ_AUTH;
                        break;
                    case GOODBYE:
                        sendOK();
                        exitGLoboH();
                        break;
                    case OTHER:
                        sendError(UNRECOGNIZED_COMMAND);
                        break;
                    default:
                        sendError(UNEXPECTED_COMMAND);
                        break;
                }
                break;
            case WAITING_COMMAND:
                switch (element.getType()) {
                    case L33T:
                        state = READ_L33T;
                        break;
                    case MUTE:
                        state = READ_MUTE;
                        break;
                    case SERVER_MULTIPLEX:
                        state = READ_SERVER_MULTIPLEX;
                        break;
                    case DATA:
                        state = READ_DATA;
                        break;
                    case GOODBYE:
                        sendOK();
                        exitGLoboH();
                        break;
                    case OTHER:
                        sendError(UNRECOGNIZED_COMMAND);
                        break;
                    default:
                        sendError(UNEXPECTED_COMMAND);
                        break;
                }
                break;
            default:
                sendError(UNKNOWN_ERROR);
                resetStream();
                break;
        }
    }

    /**
     * Handle the chars receive
     */
    @Override
    public void handleCharacters(AsyncXMLStreamReader<?> reader) {
        xmlElement.appendToBody(reader);
    }

    /**
     * Handle end section of the GLoboH element
     */
    private void handleEnd(PartialGLoboHElement element) {
        switch (state) {
            case READ_L33T:
                controlType(element, L33T);
                String l33tValue = xmlElement.getBody().toLowerCase();
                switch (l33tValue){
                    case "on":
                        globohData.setL33t(true);
                        logger.info("L33t conversion on");
                        sendOK();
                        break;
                    case "off":
                        globohData.setL33t(false);
                        logger.info("L33t conversion on");
                        sendOK();
                        break;
                    default:
                        sendError(BAD_COMMAND_OPTION);
                        break;
                }
                state = WAITING_COMMAND;
                break;
            case READ_SERVER_MULTIPLEX:
                controlType(element, SERVER_MULTIPLEX);
                String multiplexedUser = xmlElement.getAttributes().get("user");
                String toAddress = xmlElement.getBody();
                globohData.addServerMapping(multiplexedUser, toAddress);
                stringBuilder.setLength(0);
                logger.info(stringBuilder.append("User ").append(multiplexedUser).append(" multiplexed to ").append(toAddress).toString());
                sendOK();
                state = WAITING_COMMAND;
                break;
            case READ_MUTE:
                controlType(element, MUTE);
                String mutedValue = xmlElement.getAttributes().get("value");
                if(mutedValue == null){
                    mutedValue = "";
                }else {
                    mutedValue = mutedValue.toLowerCase();
                }
                String mutedUser = xmlElement.getBody();

                stringBuilder.setLength(0);
                switch (mutedValue){
                    case "on":
                        globohData.userIsMuted(mutedUser,true);
                        logger.info(stringBuilder.append("The user < ").append(mutedUser).append(" > is muted").toString());
                        sendOK();
                        break;
                    case "off":
                        globohData.userIsMuted(mutedUser,false);
                        logger.info(stringBuilder.append("The user < ").append(mutedUser).append(" > is not more muted").toString());
                        sendOK();
                        break;
                    default:
                        sendError(BAD_COMMAND_OPTION);
                        break;

                }
                state = WAITING_COMMAND;
                break;
            case READ_DATA:
                controlType(element, DATA);
                getData(xmlElement.getBody(), xmlElement.getAttributes().get("user"));
                break;
            case EXIT:
                state = STARTED;
                break;
            case WAITING_COMMAND:
                break;
            case READ_AUTH:
                String credentials[] = xmlElement.getBody().split(":");
                if(credentials.length != 2){
                    sendError(BAD_COMMAND_FORMAT);
                    state = WAITING_AUTH;
                }else{
                    String toLoginUsername = credentials[0];
                    String toLoginPassword = credentials[1];

                    logger.info("{}, trying to log in", toLoginUsername);

                    controlType(element,AUTH);

                    // Validate user and pass and change state depending on success
                    if (tryLogin(toLoginUsername, toLoginPassword)) {
                        state = WAITING_COMMAND;
                        logger.info("{}, logged in", toLoginUsername);
                        sendOK();
                    } else {
                        state = WAITING_AUTH;
                        sendError(WRONG_CREDENTIALS);
                    }
                }
                break;
        }
    }

    /**
     * Handle any type of exception, must do this to prevent unhandled exceptions
     * @param e the exception fired
     */
    @Override
    protected void handleException(Exception e) {
        sendError(NOT_WELL_FORMED);
        conversation.exit();
        state = EXIT;
    }

    /**
     * Process the statistics request
     */
    private void getData(String option, String username) {
        if(username != null){
            username = username.toLowerCase();
            switch (option){
                case "sent_messages":
                    stringBuilder.setLength(0);
                    sendToClient(getDataResponse(option, stringBuilder.append("Number of messages sent by ").append(username).append(" through proxy").toString(), globohData.getSentMessagesCount(username)));
                    break;
                case "received_messages":
                    stringBuilder.setLength(0);
                    sendToClient(getDataResponse(option, stringBuilder.append("Number of messages received by ").append(username).append(" through proxy").toString(), globohData.getReceivedMessagesCount(username)));
                    break;
                case "read_bytes":
                case "written_bytes":
                case "connections":
                case "muted_users":
                case "multiplex":
                case "average_sent_message":
                case "average_received_message":
                case "median_sent_message":
                case "median_received_message":
                case "outside_messages_muted":
                case "inside_messages_muted":
                    sendError(BAD_COMMAND_FORMAT);
                    break;
                default:
                    sendError(BAD_COMMAND_OPTION);
                    break;
            }
        }else {
            switch (option) {
                case "read_bytes":
                    sendToClient(getDataResponse(option, "Number of read bytes", proxyData.getReadBytes()));
                    break;
                case "written_bytes":
                    sendToClient(getDataResponse(option, "Number of written bytes", proxyData.getWrittenBytes()));
                    break;
                case "connections":
                    sendToClient(getDataResponse(option, "Number of connections", proxyData.getAccesses()));
                    break;
                case "sent_messages":
                    sendToClient(getDataResponse(option, "Number of messages sent through proxy", globohData.getSentMessagesCount()));
                    break;
                case "received_messages":
                    sendToClient(getDataResponse(option, "Number of messages received through proxy", globohData.getReceivedMessagesCount()));
                    break;
                case "muted_users":
                    sendToClient(getDataResponse(option, "List of muted users", globohData.getMutedUsersString()));
                    break;
                case "multiplex":
                    sendToClient(getDataResponse(option, "List of multiplexed users (Format: user->host)", globohData.getMultiplexedUsersString()));
                    break;
                case "average_sent_message":
                    sendToClient(getDataResponse(option, "Average messages sent through proxy", globohData.getAverageMessageSentByUser()));
                    break;
                case "average_received_message":
                    sendToClient(getDataResponse(option, "Average messages received through proxy", globohData.getAverageMessageReceivedByUser()));
                    break;
                case "median_sent_message":
                    sendToClient(getDataResponse(option, "Median of messages sent through proxy", globohData.getMedianMessageSentByUser()));
                    break;
                case "median_received_message":
                    sendToClient(getDataResponse(option, "Median of messages received through proxy", globohData.getMedianMessageReceivedByUser()));
                    break;
                case "outside_messages_muted":
                    sendToClient(getDataResponse(option, "Outside messages muted", globohData.getMutedMessagesOut()));
                    break;
                case "inside_messages_muted":
                    sendToClient(getDataResponse(option, "Outside messages muted", globohData.getMutedMessagesIn()));
                    break;
                default:
                    sendError(BAD_COMMAND_OPTION);
                    break;
            }
        }
        state = WAITING_COMMAND;
    }

    /**
     * Closes conversation
     */
    private void exitGLoboH() {
        logger.info("GLoboH out");
        conversation.exit();
        state = EXIT;
    }

    /**
     * Validates if the GLoboH element is corresponds to the type given
     * @param element the GLoboH element to validate
     * @param type the GLoboH element type to control the GLoboH element
     */
    private void controlType(PartialGLoboHElement element, PartialGLoboHElementType type) {
        control(element.getType() == type,"Event type mismatch, got: %s when %s was expected", element,type);
    }

    private void sendOK() {
        sendToClient("<ok/>\n");
    }

    private void sendError(GLoboHError error) {
        sendToClient(error.getError());
    }

    private void sendToClient(String message) {
        writeStringInStream(message,toClientStream);
    }

    @Override
    public void handleException(XMLStreamException exc) {handleException(new Exception());}

    /**
     * Checks if the credentials given corresponds with the credentials set
     * @return true if corresponds, false if not
     */
    private boolean tryLogin(String username, String password) {
        String correctUser = globohData.getGlobohUsername();
        String correctPassword = globohData.getGlobohPassword();

        return username.equals(correctUser) && password.equals(correctPassword);
    }

    private String getDataResponse(String option, String description, Object value){
        return stringBuilder.append("<ok> \n\t<option>")
                .append(option)
                .append("</option> \n")
                .append("\t<description>")
                .append(description)
                .append("</description> \n")
                .append("\t<value>")
                .append(value)
                .append("</value>\n")
                .append("</ok>\n")
                .toString();
    }

}
