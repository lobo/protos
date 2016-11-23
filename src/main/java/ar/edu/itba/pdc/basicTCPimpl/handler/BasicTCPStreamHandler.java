package ar.edu.itba.pdc.basicTCPimpl.handler;

import ar.edu.itba.pdc.TCP.ServerConnector;
import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.TCP.handler.stream.TCPStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static ar.edu.itba.pdc.utilities.LoggerUtilities.logErrorInDetail;


public class BasicTCPStreamHandler extends TCPStreamHandler {

    private final Logger logger = LoggerFactory.getLogger(BasicTCPStreamHandler.class);

    private final TCPStream stream;

    public BasicTCPStreamHandler(TCPStream streamTo){
        this.stream = streamTo;
    }

    @Override
    public void handleRead(ByteBuffer buf, ServerConnector serverConnector) {
        try {
            byte[] b = buf.array();
            for(int a = 0; a < buf.array().length; a++){
                switch (b[a]){
                    case 'a':
                        b[a] = (byte)'4';
                        break;
                    case 'e':
                        b[a] = (byte)'3';
                        break;
                    case 'i':
                        b[a] = (byte)'1';
                        break;
                    case 'o':
                        b[a] = (byte)'0';
                        break;
                    case 'c':
                        b[a] = (byte)'<';
                        break;
                    default:
                        break;
                }
            }
            stream.getDestinyChannel().write(ByteBuffer.wrap(b));
            /*stream.getDestinyChannel().write(buf);/**/

        }catch (IOException e){
            logger.error("Error in write");
            logErrorInDetail(logger,e);
        }
    }

    @Override
    public void handleEndOfInput() {
        try {
            stream.getDestinyChannel().close();
            stream.getOriginChannel().close();
        }catch (IOException e){
            logger.error("Can not close the channels");
            logErrorInDetail(logger,e);
        }
    }

    @Override
    public void handleException(Exception e){
        try {
            stream.getDestinyChannel().write(ByteBuffer.wrap(((String)"Error message").getBytes()));
            logErrorInDetail(logger,e);
        }catch (IOException ex) {
            logger.error("Can not send error message");
            logErrorInDetail(logger,ex);
        }
    }

}
