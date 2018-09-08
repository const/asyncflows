package org.asyncflows.protocol.websocket;

import org.asyncflows.core.Promise;

/**
 * A server web socket handler.
 */
public interface AWebSocketHandler {
    /**
     * Handle the message.
     *
     * @param exchange the exchange
     * @return when finished.
     */
    Promise<Void> handle(WebSocketExchange exchange);
}
