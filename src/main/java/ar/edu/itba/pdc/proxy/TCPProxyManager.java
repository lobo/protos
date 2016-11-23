package ar.edu.itba.pdc.proxy;

import ar.edu.itba.pdc.TCP.handler.protocol.TCPProtocolHandler;

import java.nio.channels.SocketChannel;

public interface TCPProxyManager {

    void subscribe(SocketChannel socketChannel, TCPProtocolHandler handler);

    void unsubscribe(SocketChannel socketChannel);

    void closeAfterTimeout(SocketChannel channel, long millis);

    void addProtocolHandler(TCPProtocolHandler handler, int port);

    void start();
}
