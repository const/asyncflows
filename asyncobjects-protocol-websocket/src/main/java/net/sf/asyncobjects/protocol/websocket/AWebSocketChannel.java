package net.sf.asyncobjects.protocol.websocket;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.stream.AStream;
import net.sf.asyncobjects.core.util.ACloseable;

import java.net.URI;

/**
 * The web socket channel. This is a low level message.
 */
public interface AWebSocketChannel extends ACloseable {
    /**
     * The connect method (available only for client channels).
     *
     * @param uri       the uri to connect to
     * @param protocols the protocols used to connect
     * @return the protocol associated with the channel.
     */
    Promise<String> connect(URI uri, String... protocols);

    /**
     * @return the WebSocket input messages.
     */
    Promise<AStream<WebSocketMessage>> getInput();

    /**
     * @return the web socket output.
     */
    Promise<AWebSocketOutput> getOutput();
}
