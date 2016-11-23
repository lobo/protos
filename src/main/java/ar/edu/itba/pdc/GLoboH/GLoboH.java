package ar.edu.itba.pdc.GLoboH;

import ar.edu.itba.pdc.TCP.handler.protocol.TCPProcessingProtocolHandler;
import ar.edu.itba.pdc.proxy.ProxyData;
import ar.edu.itba.pdc.proxy.ProxyProcessingTCPConversation;
import ar.edu.itba.pdc.proxy.TCPProxyManager;

import java.nio.channels.SocketChannel;

public class GLoboH extends TCPProcessingProtocolHandler {

    private static final long TIMEOUT = ProxyData.getInstance().getTimeout(); // ms

    public GLoboH(TCPProxyManager manager) {
        super(manager);
    }

    @Override
    protected ProxyProcessingTCPConversation getConversation(SocketChannel channel) {
        return new GLoboHConversation(channel, this::closeAfterTimeout);
    }

    private void closeAfterTimeout(SocketChannel channel) {
        closeAfterTimeout(channel, TIMEOUT);
    }
}
