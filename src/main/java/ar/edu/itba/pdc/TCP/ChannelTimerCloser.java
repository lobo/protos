package ar.edu.itba.pdc.TCP;

import java.nio.channels.SocketChannel;

@FunctionalInterface
public interface ChannelTimerCloser {
    void closeAfterTimeout(SocketChannel socketChannel);
}
