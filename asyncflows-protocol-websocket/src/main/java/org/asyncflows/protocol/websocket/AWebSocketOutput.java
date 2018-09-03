package org.asyncflows.protocol.websocket;

import org.asyncflows.io.AOutput;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * A output for the web socket.
 */
public interface AWebSocketOutput extends ACloseable {
    /**
     * Send short binary message (buffer should not be used until method promise finishes).
     *
     * @param message the message to send.
     * @return when stream is ready for the next message.
     */
    Promise<Void> send(ByteBuffer message);

    /**
     * Send short text message (buffer should not be used until method promise finishes).
     *
     * @param message the message to send.
     * @return when stream is ready of the next message.
     */
    Promise<Void> send(CharBuffer message);

    /**
     * Send a message. Note if any streams are specified, they are closed by this operation, even if
     * if message sending failed.
     *
     * @param message the message to send.
     * @return when message is sent
     */
    Promise<Void> send(WebSocketMessage message);

    /**
     * If message is too big or is of unknown size.
     *
     * @param length the length of message or null.
     * @return the output for the text message. The message finish sending when output is closed
     * (or when length bytes are written).
     */
    Promise<AOutput<CharBuffer>> startText(Long length);

    /**
     * If message is too big or is of unknown size.
     *
     * @param length the length of message or null.
     * @return the output for the text message. The message finish sending when output is closed
     * (or when length bytes are written).
     */
    Promise<AOutput<ByteBuffer>> startBinary(Long length);
}
