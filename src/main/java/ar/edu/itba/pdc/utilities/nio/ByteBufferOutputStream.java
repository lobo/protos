package ar.edu.itba.pdc.utilities.nio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

public class ByteBufferOutputStream extends OutputStream {


    /*
     * Based on http://jcs.mobile-utopia.com/jsc/3399_ByteBufferOutputStream.java
     */

    private ByteBuffer outBuffer;

    public ByteBufferOutputStream(final ByteBuffer buf) {
        this.outBuffer = buf;
    }

    @Override
    public void write(final int data) throws IOException {
                outBuffer.put((byte) data);
    }

    @Override
    public void close() throws IOException {
        //not supported
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }

    public void setBuffer(ByteBuffer buffer){
        outBuffer = buffer;
    }
}
