package ar.edu.itba.pdc.utilities.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ChannelUtilities {

    private static final Logger logger = LoggerFactory.getLogger("Ignoring close");
    public static void ignoringClose(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException ignored){
            logger.error("ignoring close ignored: ",ignored);
        }
    }
}
