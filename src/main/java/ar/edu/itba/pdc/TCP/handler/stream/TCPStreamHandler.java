package ar.edu.itba.pdc.TCP.handler.stream;

import ar.edu.itba.pdc.TCP.ServerConnector;

import java.nio.ByteBuffer;

public abstract class TCPStreamHandler {

    public abstract void handleEndOfInput();

    public abstract void handleRead(ByteBuffer buf, ServerConnector serverConnector);

    protected abstract void handleException(Exception e);

}
