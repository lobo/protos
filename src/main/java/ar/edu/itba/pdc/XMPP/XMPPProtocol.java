package ar.edu.itba.pdc.XMPP;

import ar.edu.itba.pdc.TCP.handler.protocol.TCPProxiedProtocolHandler;
import ar.edu.itba.pdc.proxy.ProxyControlledTCPConversation;
import ar.edu.itba.pdc.proxy.ProxyData;
import ar.edu.itba.pdc.proxy.TCPProxyManager;

import java.nio.channels.SocketChannel;

public class XMPPProtocol extends TCPProxiedProtocolHandler{

    private static final long TIMEOUT = ProxyData.getInstance().getTimeout(); //ms

    public XMPPProtocol(TCPProxyManager manager) {
        super(manager);
    }

    @Override
    protected ProxyControlledTCPConversation getConversation(SocketChannel channel) {
        return new XMPPConversation(channel, this::closeAfterTimeout);
    }

    private void closeAfterTimeout(SocketChannel channel) {
        closeAfterTimeout(channel, TIMEOUT);
    }

}
