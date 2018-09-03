package org.asyncflows.protocol.websocket;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

/**
 * Web Socket client.
 */
public interface AWebSocketClient extends ACloseable {
    /**
     * @return create a unconnected web socket channel.
     */
    Promise<AWebSocketChannel> create();
}
