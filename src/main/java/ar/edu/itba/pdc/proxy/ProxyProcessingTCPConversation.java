package ar.edu.itba.pdc.proxy;

import ar.edu.itba.pdc.TCP.ChannelTimerCloser;
import ar.edu.itba.pdc.TCP.TCPConversation;
import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.TCP.handler.stream.TCPStreamHandler;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static ar.edu.itba.pdc.utilities.nio.ChannelUtilities.ignoringClose;

public class ProxyProcessingTCPConversation implements TCPConversation {

    private static final int BUFFER_SIZE = ProxyData.getInstance().getBufferSize(); // bytes

    private final TCPStream stream;
    private boolean hasExit = false;

    protected ProxyProcessingTCPConversation(SocketChannel channel, ChannelTimerCloser channelCloser) {
        this.stream = new TCPStream(channel, BUFFER_SIZE, channel, BUFFER_SIZE, channelCloser);
    }

    @Override
    public void updateSubscription(Selector selector)
            throws ClosedChannelException {
        int streamFlags = stream.getOriginSubscriptionFlags();
        int streamToFlags = stream.getDestinySubscriptionFlags();
        stream.getOriginChannel().register(selector, streamFlags | streamToFlags, this);
    }

    @Override
    public void closeChannels() {
        ignoringClose(stream.getOriginChannel());
    }

    public void exit() {
        hasExit = true;
    }

    public boolean hasExit() {
        return hasExit;
    }

    protected TCPStream getStream() {
        return stream;
    }

    public ByteBuffer getReadBuffer() {
        return stream.getOriginBuffer();
    }

    public ByteBuffer getWriteBuffer() {
        return stream.getDestinyBuffer();
    }

    public TCPStreamHandler getHandler() {
        return stream.getHandler();
    }

    public SocketChannel getChannel() {
        return stream.getOriginChannel();
    }

}
