package ar.edu.itba.pdc.XMPP.handler.server2client;

public enum ServerToClientXMPPStreamHandlerState {
    INIT, WAITING_AUTH_FEATURES, WAITING_SERVER_ACCREDITATION, AUTH_OK, CONNECTED, MUTED,
}
