package ar.edu.itba.pdc.proxy;

import ar.edu.itba.pdc.TCP.handler.protocol.TCPProtocolHandler;

import static ar.edu.itba.pdc.utilities.LoggerUtilities.logErrorInDetail;
import static ar.edu.itba.pdc.utilities.ValidateUtilities.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.*;
import java.util.*;

import static ar.edu.itba.pdc.utilities.nio.ChannelUtilities.ignoringClose;
import static java.lang.System.currentTimeMillis;

public class TCPProxyManagerImpl implements TCPProxyManager {

    private static final int TIMEOUT = ProxyData.getInstance().getTimeout(); //ms

    private final Logger logger = LoggerFactory.getLogger(TCPProxyManagerImpl.class);

    private final Map<Integer, TCPProtocolHandler> handlersByPort = new HashMap<>();

    // SocketChannels have none hashcode nor equals, so compare them by identity
    private final Map<SocketChannel, TCPProtocolHandler> handlersBySocketChannel = new IdentityHashMap<>();
    private final Map<SocketChannel, Long> timeoutTimesByChannel = new IdentityHashMap<>();

    private boolean running = false;


    public void addProtocolHandler(TCPProtocolHandler handler, int port) {

        if(!handlersByPort.containsKey(port)) {
            handlersByPort.put(port, handler);
        }
        logger.info("Port {} added to handler {}", port, handler);
    }

    public void subscribe(SocketChannel socketChannel, TCPProtocolHandler handler) {
        if(!handlersBySocketChannel.containsKey(socketChannel)) {
            handlersBySocketChannel.put(socketChannel, handler);
            logger.info("Socket channel {} subscribed to handler {}", socketChannel, handler);
        }else
            logger.warn("Handler {} already has socket channel {}", handler, socketChannel);
    }

    public void unsubscribe(SocketChannel socketChannel) {
        handlersBySocketChannel.remove(socketChannel);
        if (socketChannel != null) {
            logger.info("Socket channel {} handling done", socketChannel);
        }else{
            logger.warn("Unsubscribe method called with null socket channel");
        }
    }

    public void closeAfterTimeout(SocketChannel channel, long millis) {
        if(timeoutTimesByChannel.containsKey(channel)){
            logger.warn("Socket channel {} is already on timeout control, ignoring new configuration", channel);
        }else{
            long timeoutTime = currentTimeMillis() + millis;
            timeoutTimesByChannel.put(channel, timeoutTime);
            logger.info("Socket channel {} is now on timeout control, with {} ms", channel, millis);
        }
    }

    public void start(){
        control(!running,"{}'s start method has been called meanwhile it is running", this);
        control(!handlersByPort.isEmpty(),"None handler added before the {}'s start method",this);

        try {

            running = true;
            // Create a selector for multiplexing connections
            Selector selector = Selector.open();
            // Create listening socket channel for each port and register selector
            for (Integer port : handlersByPort.keySet()) {
                startListening(selector, port);
                logger.debug("Handler {} subscribed as a listener on port {}", handlersByPort.get(port), port);
            }

            logger.info("{} is now running", this);

            while (running) {
                try {
                    checkTimeouts();
                    //If any channel is ready to interact, the select() is != 0
                    int debug = selector.select(TIMEOUT);
                        if (debug != 0) {
                        handleSelectionKeys(selector.selectedKeys().iterator());
                    }
                } catch (Exception e) {
                    logger.error("{} fails",this);
                    logErrorInDetail(logger,e);
                }
            }
        }catch (Exception e){
            logger.error("{} fails with exception.", this);
            logErrorInDetail(logger,e);
        }
    }

    private void startListening(Selector selector, int port) throws IOException {

        //Create the SocketChannel
        ServerSocketChannel listenerChannel = ServerSocketChannel.open();

        //Create the address by port
        InetSocketAddress address = new InetSocketAddress(ProxyData.getInstance().getListenAddress(), port);

        //Binding the socket
        ServerSocket serverSocket = listenerChannel.socket();
        serverSocket.bind(address);

        //Must be nonblocking for concurrency
        listenerChannel.configureBlocking(false);
        listenerChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void handleSelectionKeys(Iterator<SelectionKey> sKeyIterator) throws IOException{
        while(sKeyIterator.hasNext()){
            SelectionKey key = sKeyIterator.next();
            TCPProtocolHandler protocolHandler = getHandlerForChannel(key.channel());
            if (protocolHandler != null) {
                if (key.isValid() && key.isAcceptable()) {
                    logger.info("{} handling TCP accept", protocolHandler);
                    protocolHandler.handleAccept(key);
                }

                if (key.isValid() && key.isConnectable()) {
                    logger.info("{} handling TCP connect", protocolHandler);
                    protocolHandler.handleConnect(key);
                }

                if (key.isValid() && key.isReadable()) {
                    logger.info("{} handling TCP read", protocolHandler);
                    protocolHandler.handleRead(key);
                }

                if (key.isValid() && key.isWritable()) {
                    logger.info("{} handling TCP write", protocolHandler);
                    protocolHandler.handleWrite(key);
                }
            } else {
                logger.info("Handler not found for channel: {}", key.channel());
            }
            //The Key iterator must be empty
            sKeyIterator.remove();
        }
    }


    private TCPProtocolHandler getHandlerForChannel(SelectableChannel channel) throws IOException {
        if (channel instanceof SocketChannel) {
            SocketChannel socketChannel = (SocketChannel) channel;
            return handlersBySocketChannel.get(socketChannel);
        }
        if (channel instanceof ServerSocketChannel) {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) channel;
            InetSocketAddress address = (InetSocketAddress) serverSocketChannel.getLocalAddress();
            int port = address.getPort();
            return handlersByPort.get(port);
        }
        throw new IllegalArgumentException("Unknown channel type: " + channel);
    }

    private void checkTimeouts() {
        long time = currentTimeMillis();

        List<SocketChannel> toRemove = new LinkedList<>();
        for (Map.Entry<SocketChannel, Long> entry : timeoutTimesByChannel.entrySet()) {
            SocketChannel channel = entry.getKey();
            long timeoutTime = entry.getValue();
            if (time >= timeoutTime) {
                ignoringClose(channel);
                unsubscribe(channel);
                toRemove.add(channel);
            }
        }
        toRemove.forEach(timeoutTimesByChannel::remove);
    }
}
