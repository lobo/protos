package ar.edu.itba.pdc.proxy;

import ar.edu.itba.pdc.TCP.ChannelTimerCloser;
import ar.edu.itba.pdc.TCP.TCPConversation;
import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.TCP.handler.stream.TCPStreamHandler;
import ar.edu.itba.pdc.utilities.ValidateUtilities;

import static ar.edu.itba.pdc.utilities.nio.ChannelUtilities.ignoringClose;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ProxyControlledTCPConversation implements TCPConversation{

    private static final int BUFFER_SIZE = ProxyData.getInstance().getBufferSize(); // in Bytes
    private static final int CLIENT_TO_SERVER_RATIO = 4; // it is UTF-8, and will have to convert 'c' to '<' what suits 4 bytes for "&lt;"

    private final TCPStream clientToServerStream;
    private final TCPStream serverToClientStream;

    private boolean connecting = false;
    private boolean alreadyConnected = false;

    protected ProxyControlledTCPConversation(SocketChannel clientChannel, ChannelTimerCloser channelTimerCloser) {
        //Create 2 streams, making the proxy available to modify the buffer content

        //The destiny in clientToServer is null as the server is not already connected,
        //For the same reason origin in serverToClient is also null
        this.clientToServerStream = new TCPStream(clientChannel, BUFFER_SIZE, null, BUFFER_SIZE * CLIENT_TO_SERVER_RATIO, channelTimerCloser);
        this.serverToClientStream = new TCPStream(null, BUFFER_SIZE, clientChannel, BUFFER_SIZE, channelTimerCloser);
    }

    public void updateSubscription(Selector selector) throws ClosedChannelException {

        //Get flags from both streams, corresponding to client function with that stream
        int clientFlags = clientToServerStream.getOriginSubscriptionFlags() | serverToClientStream.getDestinySubscriptionFlags();
        getClientChannel().register(selector, clientFlags, this);

        if (getServerChannel() != null) {
            final int serverFlags;

            //If has to connect, set the connect flag. If not, get the flags from streams
            if (connecting && !alreadyConnected) {
                serverFlags = SelectionKey.OP_CONNECT;
                connecting = false;
                alreadyConnected = true;
            } else {
                serverFlags = serverToClientStream.getOriginSubscriptionFlags() | clientToServerStream.getDestinySubscriptionFlags();
            }
            getServerChannel().register(selector, serverFlags, this);
        }
    }

    public ByteBuffer getReadBufferForChannel(SocketChannel channel) {
        if (getClientChannel() == channel) {
            return clientToServerStream.getOriginBuffer();
        }
        if (getServerChannel() == channel) {
            return serverToClientStream.getOriginBuffer();
        }
        throw new IllegalArgumentException("Unexpected channel");
    }

    public ByteBuffer getWriteBufferForChannel(SocketChannel channel) {
        if (getClientChannel() == channel) {
            return serverToClientStream.getDestinyBuffer();
        }
        if (getServerChannel() == channel) {
            return clientToServerStream.getDestinyBuffer();
        }
        throw new IllegalArgumentException("Unexpected channel");
    }

    public TCPStreamHandler getHandlerForChannel(SocketChannel channel) {
        if (getClientChannel() == channel) {
            return clientToServerStream.getHandler();
        }
        if (getServerChannel() == channel) {
            return serverToClientStream.getHandler();
        }
        throw new IllegalArgumentException("Unexpected channel");
    }

    public void closeChannels() {
        ignoringClose(getClientChannel());

        SocketChannel serverChannel = getServerChannel();
        if (serverChannel != null) {
            ignoringClose(serverChannel);
        }
    }

    public SocketChannel getClientChannel() {
        return clientToServerStream.getOriginChannel();
    }

    public SocketChannel getServerChannel() {
        return serverToClientStream.getOriginChannel();
    }

    public void setServerConnection(SocketChannel serverChannel) {
        ValidateUtilities.control(!alreadyConnected,"The connection is already connected to server");
        if (!alreadyConnected){
            this.clientToServerStream.setDestinyChannel(serverChannel);
            this.serverToClientStream.setOriginChannel(serverChannel);
            connecting = true;
        }
    }

    protected TCPStream getClientToServerStream() {
        return clientToServerStream;
    }

    protected TCPStream getServerToClientStream() {
        return serverToClientStream;
    }

}
