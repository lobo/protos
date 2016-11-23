package ar.edu.itba.pdc.proxy;

import ar.edu.itba.pdc.GLoboH.GLoboHData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class ProxyData {

    private static final ProxyData INSTANCE = new ProxyData();
    private static final String CONFIG_PATH = "/globoh.proxy.properties";

    private static final Logger logger = LoggerFactory.getLogger(GLoboHData.class);

    private final AtomicLong writtenBytes = new AtomicLong();
    private final AtomicLong readBytes = new AtomicLong();
    private final AtomicLong accessCount = new AtomicLong();

    private String listenAddress;

    private int bufferSize;
    private int timeout;

    public static ProxyData getInstance() {
        return INSTANCE;
    }

    private ProxyData(){

        Properties properties = new Properties();

        InputStream is = getClass().getResourceAsStream(CONFIG_PATH);
        try {
            properties.load(is);
        } catch (IOException e) {
            logger.error("Could not load properties");
            System.exit(1);
        }
        load(properties);
    }

    private void load(Properties properties) {
        try {
            timeout = Integer.parseInt(properties.getProperty("timeout.value.ms", "4000"));
        }catch (Exception e){
            timeout = 4000;
        }
        try {
            bufferSize = Integer.parseInt(properties.getProperty("buffer.size.bytes", "1024"));
        }catch (Exception e){
            bufferSize = 1024;
        }
        try {
            listenAddress = properties.getProperty("listen.address", "0.0.0.0");
        }catch (Exception e){
            listenAddress = "0.0.0.0";
        }
    }

    public void newWrite(int written) {
        this.writtenBytes.addAndGet(written);
    }

    public long getWrittenBytes() {
        return this.writtenBytes.get();
    }

    public void newAccess() {
        this.accessCount.incrementAndGet();
    }

    public long getAccesses() {
        return this.accessCount.get();
    }

    public void newRead(int read) {
        this.readBytes.addAndGet(read);
    }

    public long getReadBytes() {
        return this.readBytes.get();
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getListenAddress() {
        return listenAddress;
    }
}
