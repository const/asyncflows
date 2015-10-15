package net.sf.asyncobjects.protocol.websocket;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;

/**
 * Web Socket client.
 */
public interface AWebSocketClient extends ACloseable {
    /**
     * @return create a unconnected web socket channel.
     */
    Promise<AWebSocketChannel> create();
}
