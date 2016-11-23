package ar.edu.itba.pdc.TCP.handler.protocol;

import java.nio.channels.SelectionKey;

public interface TCPProtocolHandler {

    void handleAccept(SelectionKey key);
    void handleRead(SelectionKey key);
    void handleWrite(SelectionKey key);
    void handleConnect(SelectionKey key);
}
