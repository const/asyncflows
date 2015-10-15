package net.sf.asyncobjects.protocol.websocket.core;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.util.ResourceUtil;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.protocol.websocket.AWebSocketOutput;
import net.sf.asyncobjects.protocol.websocket.WebSocketMessage;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * The implementation of web socket output message stream.
 */
public class WebSocketOutput extends CloseableInvalidatingBase implements AWebSocketOutput {
    /**
     * Send short binary message (buffer should not be used until method promise finishes).
     *
     * @param message the message to send.
     * @return when stream is ready for the next message.
     */
    @Override
    public Promise<Void> send(final ByteBuffer message) {
        return null;
    }

    /**
     * Send short text message (buffer should not be used until method promise finishes).
     *
     * @param message the message to send.
     * @return when stream is ready of the next message.
     */
    @Override
    public Promise<Void> send(final CharBuffer message) {
        return null;
    }

    /**
     * Send a message. Note if any streams are specified, they are closed by this operation, even if
     * if message sending failed.
     *
     * @param message the message to send.
     * @return when message is sent
     */
    @Override
    public Promise<Void> send(final WebSocketMessage message) {
        switch (message.getType()) {
            case BINARY:
                return send(message.getBinary());
            case BINARY_STREAM:
                return ResourceUtil.aTryResource(message.getBinaryStream())
                        .andOther(startBinary(message.getLength()))
                        .run((value1, value2) ->
                                IOUtil.BYTE.copy(value1, value2, false, IOUtil.BYTE.writeBuffer()).toVoid());
            case TEXT:
                return send(message.getText());
            case TEXT_STREAM:
                return ResourceUtil.aTryResource(message.getTextStream())
                        .andOther(startText(message.getLength()))
                        .run((value1, value2) ->
                                IOUtil.CHAR.copy(value1, value2, false, IOUtil.CHAR.writeBuffer()).toVoid());
            default:
                throw new IllegalArgumentException("Unsupported message type: " + message.getType());
        }
    }

    /**
     * If message is too big or is of unknown size.
     *
     * @param length the length of message or null.
     * @return the output for the text message. The message finish sending when output is closed
     * (or when length bytes are written).
     */
    @Override
    public Promise<AOutput<CharBuffer>> startText(final Long length) {
        return null;
    }

    /**
     * If message is too big or is of unknown size.
     *
     * @param length the length of message or null.
     * @return the output for the text message. The message finish sending when output is closed
     * (or when length bytes are written).
     */
    @Override
    public Promise<AOutput<ByteBuffer>> startBinary(final Long length) {
        return null;
    }

}
