package ar.edu.itba.pdc.TCP;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface ServerConnector {
    void connectToServer(InetSocketAddress socketAddress);
}
