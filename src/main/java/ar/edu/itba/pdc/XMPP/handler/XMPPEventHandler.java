package ar.edu.itba.pdc.XMPP.handler;

import ar.edu.itba.pdc.XMPP.element.PartialXMPPElement;

public interface XMPPEventHandler {

    //The class that implements this interface, will define how the Proxy will manipulate the XMPP data

    void handleStart(PartialXMPPElement xmppElement);

    void handleEnd(PartialXMPPElement xmppElement);

    void handleCharacters(PartialXMPPElement xmppElement);

}
