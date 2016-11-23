package ar.edu.itba.pdc.XMPP;

import ar.edu.itba.pdc.TCP.ChannelTimerCloser;
import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.XMPP.handler.client2server.ClientToServerXMPPStreamHandler;
import ar.edu.itba.pdc.XMPP.handler.server2client.ServerToClientXMPPStreamHandler;
import ar.edu.itba.pdc.proxy.ProxyControlledTCPConversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.nio.channels.SocketChannel;

import static ar.edu.itba.pdc.XMPP.XMPPUtilities.encodeCredentials;
import static ar.edu.itba.pdc.utilities.LoggerUtilities.logErrorInDetail;

public class XMPPConversation extends ProxyControlledTCPConversation{

    //Credentials
    private String username;
    private String password;

    public XMPPConversation(SocketChannel socketChannel, ChannelTimerCloser timerCloser){
        super(socketChannel,timerCloser);

        Logger logger = LoggerFactory.getLogger(XMPPConversation.class);

        final TCPStream clientToServer = getClientToServerStream();
        final TCPStream serverToClient = getServerToClientStream();

        /* clientTOserver and serverTOclient Handlers, must be different because must be available to control
        *     in a different way
        */
        try{

            clientToServer.setHandler(new ClientToServerXMPPStreamHandler(clientToServer,serverToClient,this/*SET NEW HANDLER*/));
            serverToClient.setHandler(new ServerToClientXMPPStreamHandler(clientToServer,serverToClient,this)/*SET NEW HANDLER*/);
        }catch (XMLStreamException e){
            logger.error("XMPP handler set Error");
            logErrorInDetail(logger,e);
        }

    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getEncodedCredentials(){
        return encodeCredentials(username,password);
    }

    public String getUsername() {
        return username;
    }
}
