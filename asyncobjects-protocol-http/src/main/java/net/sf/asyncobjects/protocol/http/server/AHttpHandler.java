package net.sf.asyncobjects.protocol.http.server;

import net.sf.asyncobjects.core.Promise;

/**
 * The basic HTTP handler interface.The more complex interfaces are supposed to be built upon it.
 * The handler provides access to the very raw HTTP request.
 */
public interface AHttpHandler {
    /**
     * Handle the exchange. After the result promise is resolved, the both input and output are closed. If input
     * has not been fully read, the HTTP connection might be aborted, and not handle next requests pipelined
     * in it. If output is with the specified content length, it is closed. If the protocol switch happened,
     * the underlying channel is closed with the method finishes.
     *
     * @param exchange the exchange
     * @return a promise that resolves when request is completely handled.
     */
    Promise<Void> handle(HttpExchange exchange);
}
