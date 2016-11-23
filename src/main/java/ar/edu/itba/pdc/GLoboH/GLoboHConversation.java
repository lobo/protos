package ar.edu.itba.pdc.GLoboH;

import ar.edu.itba.pdc.GLoboH.handler.GLoboHStreamHandler;
import ar.edu.itba.pdc.TCP.ChannelTimerCloser;
import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.TCP.handler.stream.TCPStreamHandler;
import ar.edu.itba.pdc.proxy.ProxyProcessingTCPConversation;

import javax.xml.stream.XMLStreamException;
import java.nio.channels.SocketChannel;

public class GLoboHConversation extends ProxyProcessingTCPConversation {

    public GLoboHConversation(SocketChannel clientChannel, ChannelTimerCloser terminator) {
        super(clientChannel, terminator);
        TCPStream globohStream = getStream();

        TCPStreamHandler globohHandler;
        try {
            globohHandler = new GLoboHStreamHandler(this, globohStream);
            globohStream.setHandler(globohHandler);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
