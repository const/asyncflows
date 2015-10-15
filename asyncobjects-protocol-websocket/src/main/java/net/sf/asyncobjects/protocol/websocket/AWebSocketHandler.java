package net.sf.asyncobjects.protocol.websocket;

import net.sf.asyncobjects.core.Promise;

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
