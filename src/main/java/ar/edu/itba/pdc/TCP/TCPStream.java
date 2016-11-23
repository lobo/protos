package ar.edu.itba.pdc.TCP;

import ar.edu.itba.pdc.TCP.handler.stream.TCPStreamHandler;
import ar.edu.itba.pdc.proxy.ProxyData;
import ar.edu.itba.pdc.utilities.nio.ByteBufferOutputStream;
import org.apache.commons.lang3.ArrayUtils;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static ar.edu.itba.pdc.utilities.ValidateUtilities.*;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

public class TCPStream{

    private final Terminal origin;
    private final Terminal destiny;
    private final ChannelTimerCloser channelTimerCloser;
    private TCPStreamHandler handler;
    private boolean streamSlept = false;

    public TCPStream(final SocketChannel originChannel, int inputBufferSize,
                     final SocketChannel destinyChannel, int outputBufferSize,
                     final ChannelTimerCloser channelCloser) {
        this.origin = new Terminal(originChannel, inputBufferSize);
        this.destiny = new Terminal(destinyChannel, outputBufferSize);
        this.channelTimerCloser = channelCloser;
    }

    public void setHandler(final TCPStreamHandler handler) {
        control(this.handler == null, "Handler already set: %s");
        this.handler = handler;
    }

    public TCPStreamHandler getHandler() {
        return handler;
    }

    public SocketChannel getDestinyChannel() {
        return destiny.channel;
    }

    public SocketChannel getOriginChannel() {
        return origin.channel;
    }

    public ByteBuffer getDestinyBuffer() {
        return destiny.buffer;
    }

    public ByteBuffer getOriginBuffer() {
        return origin.buffer;
    }


    private int getMinMultiple(int num, int multiple){
        return ((num/multiple) + (num%multiple>0?1:0))*multiple;
    }

    public void amplyDestinyBuffer(int size){

        final ByteBuffer tmpBuffer = ByteBuffer.allocate(getMinMultiple(size, ProxyData.getInstance().getBufferSize()));
        byte[] array = destiny.buffer.array();
        while (ArrayUtils.contains(array,(byte)0))
            array = ArrayUtils.removeElements(array,(byte)0);
        int pos = destiny.buffer.position();
        tmpBuffer.put(array);
        tmpBuffer.position(pos);
        destiny.buffer = tmpBuffer;
        if(byteBufferOutputStream == null){
            byteBufferOutputStream = new ByteBufferOutputStream(destiny.buffer);
        }else {
            byteBufferOutputStream.setBuffer(destiny.buffer);
        }
    }
    public void amplyOriginBuffer(int size){

        final ByteBuffer tmpBuffer = ByteBuffer.allocate(getMinMultiple(size, ProxyData.getInstance().getBufferSize()));
        byte[] array = origin.buffer.array();
        while (ArrayUtils.contains(array,(byte)0))
            array = ArrayUtils.removeElements(array,(byte)0);
        int pos = origin.buffer.position();
        tmpBuffer.put(array);
        tmpBuffer.position(pos);

        origin.buffer = tmpBuffer;

        if(byteBufferOutputStream == null){
            byteBufferOutputStream = new ByteBufferOutputStream(origin.buffer);
        }else {
            byteBufferOutputStream.setBuffer(origin.buffer);
        }
    }


    public int getOriginSubscriptionFlags() {
        if (!streamSlept && getOriginBuffer().hasRemaining()) {
            return SelectionKey.OP_READ;
        }
        return 0;
    }

    public int getDestinySubscriptionFlags() {
        return getDestinyBuffer().position() > 0 ? SelectionKey.OP_WRITE : 0;
    }

    public void setDestinyChannel(final SocketChannel channel) {
        control(getDestinyChannel() == null, "Channel already set: %s");
        destiny.channel = channel;
    }

    public void setOriginChannel(final SocketChannel channel) {
        control(getOriginChannel() == null, "Channel already set: %s");
        origin.channel = channel;
    }


    public void toSleepStream() {
        this.streamSlept = true;
    }

    public void wakeUpStream() {
        this.streamSlept = false;
    }

    public void endStreamAfterTimeout() {
        this.streamSlept = true;
        channelTimerCloser.closeAfterTimeout(getOriginChannel());
    }

    private static class Terminal {
        SocketChannel channel;
        ByteBuffer buffer;

        Terminal(final SocketChannel channel, int bufferSize) {
            this.channel = channel;
            this.buffer = ByteBuffer.allocate(bufferSize);
        }

        @Override
        public String toString() {
            return reflectionToString(this);
        }
    }


    /*
     * That is, where data would be written *into* the Stream by someone outside
     * the normal flow (i.e.: a proxy).
     *
     * IMPORTANT: note that the data written here WILL BE THE ONLY DATA THAT
     * WILL FLOW OUTSIDE THE STREAM.
     */
    private ByteBufferOutputStream byteBufferOutputStream;
    public OutputStream getOutputStream() {
        if(byteBufferOutputStream == null)
            byteBufferOutputStream = new ByteBufferOutputStream(getDestinyBuffer());
        return byteBufferOutputStream;
    }
}
