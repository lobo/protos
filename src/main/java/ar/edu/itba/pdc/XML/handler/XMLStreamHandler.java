package ar.edu.itba.pdc.XML.handler;

import ar.edu.itba.pdc.TCP.ServerConnector;
import ar.edu.itba.pdc.TCP.TCPStream;
import ar.edu.itba.pdc.TCP.handler.stream.TCPStreamHandler;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.fasterxml.aalto.AsyncXMLStreamReader.EVENT_INCOMPLETE;
import static javax.xml.stream.XMLStreamConstants.*;

public abstract class XMLStreamHandler extends TCPStreamHandler implements XMLEventHandler {
    private static final byte LTvalue = 0x3C;
    private static final byte GTvalue = 0x3E;

    private ServerConnector futureServerConnector;

    private static final AsyncXMLInputFactory xmlInputFactory = new InputFactoryImpl();

    private AsyncXMLStreamReader<AsyncByteBufferFeeder> reader;
    private int GTcount = 0;
    private boolean slept = false;

    protected XMLStreamHandler() throws XMLStreamException {
        this.reader = resetReader();
    }

    @Override
    public void handleRead(ByteBuffer buf, ServerConnector serverConnector) {
        try {
            //If its the first read, may have to multiplex. Making it available
            futureServerConnector = serverConnector;

            reader.getInputFeeder().feedInput(buf);

            while (!slept && reader.hasNext()) {
                int type = reader.next();
                switch (type) {
                    case START_DOCUMENT:
                        GTcount++;
                        break;
                    case START_ELEMENT:
                        GTcount++;
                        handleElementStart(reader);
                        break;
                    case CHARACTERS:
                        handleCharacters(reader);
                        break;
                    case END_ELEMENT:
                        GTcount++;
                        handleElementEnding(reader);
                        break;
                }
                if (type == EVENT_INCOMPLETE) {
                    break;
                }
            }
        } catch (XMLStreamException xmlExc) {
            handleException(xmlExc);
        }
        //In any other occasion, MUST NOT call connect
        futureServerConnector = null;

        buf.position(getNewPosition(buf));
        buf.compact();

        /*XML Parser do not control this*/
        if (buf.limit() == buf.capacity()) {
            buf.clear();
        }
    }

    private int getNewPosition(ByteBuffer buffer) {
        byte[] bytes = buffer.array();
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            switch (bytes[i]) {
            case LTvalue:
                if (GTcount == 0) {
                    return i; // return last LT position
                }
                break;
            case GTvalue:
                if (GTcount > 0) {
                    GTcount--;
                }
                break;
            default:
                // do nothing
            }
        }

        return buffer.limit();
    }

    protected void resetStream() {
        try {
            reader.close();
            reader = resetReader();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void toSleep() {
        this.slept = true;

    }

    protected void wakeUp() {
        this.slept = false;
    }

    @Override
    public void handleEndOfInput() {
        reader.getInputFeeder().endOfInput();
    }

    protected void writeStringInStream(String string, TCPStream stream) {
        try {
            stream.getOutputStream().write(string.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            handleException(e);
        }
    }

    private AsyncXMLStreamReader<AsyncByteBufferFeeder> resetReader()
            throws XMLStreamException {
        return xmlInputFactory.createAsyncFor(ByteBuffer.allocate(0));
    }

    protected ServerConnector getFutureServerConnector(){
        return futureServerConnector;
    }
}
