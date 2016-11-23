package ar.edu.itba.pdc.basicTCPimpl;

import ar.edu.itba.pdc.TCP.handler.protocol.TCPProxiedProtocolHandler;
import ar.edu.itba.pdc.proxy.ProxyData;
import ar.edu.itba.pdc.proxy.TCPProxyManager;

import java.nio.channels.SocketChannel;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

public class BasicTCPProxiedProtocol extends TCPProxiedProtocolHandler {
    private static final long TIMEOUT = ProxyData.getInstance().getTimeout(); //ms

    public BasicTCPProxiedProtocol(TCPProxyManager manager) {
        super(manager);
    }

    @Override
    protected ProxyBasicTCPConversation getConversation(SocketChannel clientChannel) {
        return new ProxyBasicTCPConversation(clientChannel, this::closeAfterTimeout);
    }

    private void closeAfterTimeout(SocketChannel channel) {
        closeAfterTimeout(channel, TIMEOUT);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }

}
