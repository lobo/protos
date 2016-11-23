package ar.edu.itba.pdc.TCP.handler.protocol;

import ar.edu.itba.pdc.TCP.handler.stream.TCPStreamHandler;
import ar.edu.itba.pdc.proxy.ProxyProcessingTCPConversation;
import ar.edu.itba.pdc.proxy.TCPProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static ar.edu.itba.pdc.utilities.LoggerUtilities.logErrorInDetail;

public abstract class TCPProcessingProtocolHandler implements TCPProtocolHandler {

    private final Logger logger = LoggerFactory.getLogger(TCPProcessingProtocolHandler.class);

    private final TCPProxyManager manager;

    protected TCPProcessingProtocolHandler(final TCPProxyManager manager) {
        this.manager = manager;
    }

    @Override
    public void handleAccept(SelectionKey key) {
        try {
            ServerSocketChannel listenChannel = (ServerSocketChannel) key.channel();
            SocketChannel channel = listenChannel.accept();
            channel.configureBlocking(false);

            manager.subscribe(channel, this);

            ProxyProcessingTCPConversation conversation = getConversation(channel);

            conversation.updateSubscription(key.selector());
        } catch (Exception ex) {
            logger.error("{} : error on accept handling", this);
            logErrorInDetail(logger,ex);
        }
    }

    @Override
    public void handleRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ProxyProcessingTCPConversation conversation = (ProxyProcessingTCPConversation) key.attachment();
        try {

            ByteBuffer buffer = conversation.getReadBuffer();
            TCPStreamHandler handler = conversation.getHandler();

            int bytesRead = channel.read(buffer);

            if (bytesRead == -1) {
                handler.handleEndOfInput();
                finish(conversation);
            } else if (bytesRead > 0) {
                buffer.flip();

                /*
                 *  In this case, as the proxy is the processing server, it does not
                 *  need to connect to other entity, so ServerConnection can be null
                 */
                handler.handleRead(buffer,null);

                conversation.updateSubscription(key.selector());
            }
        } catch (Exception ex) {
            logger.error("{} : error on read handling.", this);
            logErrorInDetail(logger,ex);
            finish(conversation);
        }
    }

    private void finish(ProxyProcessingTCPConversation conversation) {
        manager.unsubscribe(conversation.getChannel());
        conversation.closeChannels();
    }

    @Override
    public void handleWrite(SelectionKey key) {
        ProxyProcessingTCPConversation conversation = (ProxyProcessingTCPConversation) key.attachment();
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = conversation.getWriteBuffer();
            buffer.flip();
            channel.write(buffer);
            buffer.compact();

            conversation.updateSubscription(key.selector());

            if (conversation.hasExit()) {
                conversation.closeChannels();
            }
        } catch (Exception ex) {
            logger.error("{} : error on write handling.", this);
            logErrorInDetail(logger,ex);
            finish(conversation);
        }
    }

    @Override
    public void handleConnect(SelectionKey key) {
        // Must override, but should never be called
        logger.error("{} : connect called",this);
    }

    protected abstract ProxyProcessingTCPConversation getConversation(SocketChannel channel);

    protected void closeAfterTimeout(SocketChannel channel, long millis) {
        manager.closeAfterTimeout(channel, millis);
    }
}

