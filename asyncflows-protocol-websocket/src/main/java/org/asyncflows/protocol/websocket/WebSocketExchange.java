package org.asyncflows.protocol.websocket;

import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

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
