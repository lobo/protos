package ar.edu.itba.pdc.TCP.handler.protocol;

import ar.edu.itba.pdc.TCP.handler.stream.TCPStreamHandler;
import ar.edu.itba.pdc.proxy.ProxyControlledTCPConversation;
import ar.edu.itba.pdc.proxy.ProxyData;
import ar.edu.itba.pdc.proxy.TCPProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import static ar.edu.itba.pdc.utilities.LoggerUtilities.logErrorInDetail;

public abstract class TCPProxiedProtocolHandler implements TCPProtocolHandler {

    private final Logger logger = LoggerFactory.getLogger(TCPProxiedProtocolHandler.class);


    private final TCPProxyManager manager;
    private final ProxyData proxyData = ProxyData.getInstance();

    protected TCPProxiedProtocolHandler(TCPProxyManager manager){
        this.manager = manager;
    }

    public void handleAccept(SelectionKey key){
        try {
            //Create channel to the server
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();

            //Get the selected channel from the client, and set non blocking for concurrency
            SocketChannel clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);
            serverChannel.configureBlocking(false);

            //Add channel to the manager*/
            manager.subscribe(clientChannel, this);

            ProxyControlledTCPConversation conversation = getConversation(clientChannel);

            conversation.updateSubscription(key.selector());
            proxyData.newAccess();


            //manager.subscribe(serverChannel, this);
        }catch (IOException e){
            logger.error("{} : error on accept handling. {}", this, e);
            logErrorInDetail(logger,e);
        }
    }

    public void handleConnect(SelectionKey key){

        ProxyControlledTCPConversation conversation = (ProxyControlledTCPConversation) key.attachment();
        try {
            SocketChannel serverChannel = (SocketChannel) key.channel();
            if (serverChannel.finishConnect()) {
                conversation.updateSubscription(key.selector());
            } else {
                endConversation(conversation);
            }
        } catch (Exception e) {
            logger.error("{} : error on connect handling. {}", this, e);
            logErrorInDetail(logger,e);
            endConversation(conversation);
        }
    }

    public void handleRead(SelectionKey key){
        ProxyControlledTCPConversation conversation = (ProxyControlledTCPConversation) key.attachment();
        try {
            SocketChannel channel = (SocketChannel) key.channel();

            ByteBuffer buffer = conversation.getReadBufferForChannel(channel);
            TCPStreamHandler handler = conversation.getHandlerForChannel(channel);

            int readBytes = channel.read(buffer);

            if (readBytes == -1) {
                // Channel closed
                handler.handleEndOfInput();
                endConversation(conversation);
            } else if (readBytes > 0) {

                logger.debug("READ from {} -> buf content: {}",conversation.getHandlerForChannel(channel),(new String (buffer.array())).substring(0,buffer.position()));

                proxyData.newRead(readBytes);
                buffer.flip();

                //The handler will modify and process the buffer
                handler.handleRead(buffer,socketAddress -> connectToServer(conversation,socketAddress));

                //buffer.compact();
                conversation.updateSubscription(key.selector());
            }
        } catch (Exception e) {
            logger.error("{} : error on read handling. {}", this, e);
            logErrorInDetail(logger,e);
            endConversation(conversation);
        }
    }

    public void handleWrite(SelectionKey key) {

        ProxyControlledTCPConversation conversation = (ProxyControlledTCPConversation) key.attachment();
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = conversation.getWriteBufferForChannel(channel);
            buffer.flip();

            logger.debug("write {} -> buf content: {}",conversation.getHandlerForChannel(channel),(new String (buffer.array())).substring(buffer.position(), buffer.limit()));

            int writtenBytes = channel.write(buffer);
            proxyData.newWrite(writtenBytes);

            //Here could be a handler with a handle write, not applicable for this project scope

            buffer.compact();
            conversation.updateSubscription(key.selector());

        } catch (Exception e) {
            logger.error("{} : error on write handling. {}", this, e);
            logErrorInDetail(logger,e);
            endConversation(conversation);
        }
    }

    private void endConversation(ProxyControlledTCPConversation conversation) {
        try {
            manager.unsubscribe(conversation.getClientChannel());
            manager.unsubscribe(conversation.getServerChannel());
            conversation.closeChannels();
        } catch (Exception ex) {
            logger.error(this + " have an error ending the conversation. Trying again",ex);
            endConversation(conversation);
        }
    }

    //Set the configuration of server
    private void connectToServer(ProxyControlledTCPConversation conversation, InetSocketAddress address) {
        try {
            SocketChannel serverChannel = SocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.connect(address);
            manager.subscribe(serverChannel, this);
            //Here complete the stream data
            conversation.setServerConnection(serverChannel);
        } catch (Exception ex) {
            logger.error(this + " have an error connecting to server", ex);
            endConversation(conversation);
        }
    }


    protected abstract ProxyControlledTCPConversation getConversation(SocketChannel clientChannel);

    protected void closeAfterTimeout(SocketChannel channel, long millis) {
        manager.closeAfterTimeout(channel, millis);
    }

}
