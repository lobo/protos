package ar.edu.itba.pdc;

import ar.edu.itba.pdc.GLoboH.GLoboH;
import ar.edu.itba.pdc.XMPP.XMPPProtocol;
import ar.edu.itba.pdc.proxy.TCPProxyManager;
import ar.edu.itba.pdc.proxy.TCPProxyManagerImpl;
import ar.edu.itba.pdc.basicTCPimpl.BasicTCPProxiedProtocol;

public class Main
{
    public static void main( String[] args )
    {

        TCPProxyManager manager = new TCPProxyManagerImpl();

        manager.addProtocolHandler(new BasicTCPProxiedProtocol(manager), 8520);
        manager.addProtocolHandler(new GLoboH(manager), 9630);
        manager.addProtocolHandler(new XMPPProtocol(manager),7410);


        manager.start();
    }
}
