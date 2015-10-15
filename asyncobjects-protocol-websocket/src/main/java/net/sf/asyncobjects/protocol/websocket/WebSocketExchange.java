package net.sf.asyncobjects.protocol.websocket;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.protocol.http.server.HttpExchange;

import java.util.List;

/**
 * The web socket connection context.
 */
public interface WebSocketExchange extends ACloseable {
    /**
     * @return the http exchange context.
     */
    HttpExchange getHttpExchange();

    /**
     * @return the list of web socket protocols.
     */
    List<String> getWebSocketProtocols();

    /***
     * Switch to the specific protocol.
     *
     * @param protocol the protocol to use (from {@link #getWebSocketProtocols()}).
     * @return a web socket channel to use.
     */
    Promise<AWebSocketChannel> switchToWebSocket(String protocol);
}
