package ar.edu.itba.pdc.basicTCPimpl;

import ar.edu.itba.pdc.TCP.ChannelTimerCloser;
import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.basicTCPimpl.handler.BasicTCPStreamHandler;
import ar.edu.itba.pdc.proxy.ProxyControlledTCPConversation;

import java.nio.channels.SocketChannel;

public class ProxyBasicTCPConversation extends ProxyControlledTCPConversation{

    public ProxyBasicTCPConversation(SocketChannel clientChannel, ChannelTimerCloser channelTimerCloser) {
        super(clientChannel, channelTimerCloser);
            final TCPStream clientToServer = getClientToServerStream();
            final TCPStream serverToClient = getServerToClientStream();

            final BasicTCPStreamHandler clientToServerHandler = new BasicTCPStreamHandler(clientToServer);
            clientToServer.setHandler(clientToServerHandler);

            final BasicTCPStreamHandler serverToClientHandler = new BasicTCPStreamHandler(serverToClient);
            serverToClient.setHandler(serverToClientHandler);
    }

}
