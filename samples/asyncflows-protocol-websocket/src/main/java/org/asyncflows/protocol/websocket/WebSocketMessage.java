/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.protocol.websocket;

import org.asyncflows.io.AInput;
import org.asyncflows.io.util.BufferBackedInput;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/**
 * The web socket message.
 */
public final class WebSocketMessage {
    /**
     * The message type.
     */
    private final MessageType type;
    /**
     * The text for the message.
     */
    private final CharBuffer text;
    /**
     * The binary data for the message.
     */
    private final ByteBuffer binary;
    /**
     * The text stream for the message.
     */
    private final AInput<CharBuffer> textStream;
    /**
     * If message is fragmented or larger than limit.
     */
    private final AInput<ByteBuffer> binaryStream;
    /**
     * The length if it is known.
     */
    private final Long length;

    /**
     * The constructor.
     *
     * @param type         the message type
     * @param text         the text
     * @param binary       the binary data
     * @param textStream   the text stream
     * @param binaryStream the binary stream
     * @param length       the data length
     */
    private WebSocketMessage(final MessageType type, final CharBuffer text, final ByteBuffer binary,
                             final AInput<CharBuffer> textStream, final AInput<ByteBuffer> binaryStream,
                             final Long length) {
        this.type = type;
        this.text = text;
        this.binary = binary;
        this.textStream = textStream;
        this.binaryStream = binaryStream;
        this.length = length;
    }

    /**
     * The text message.
     *
     * @param data the data (the buffer should not be used after call of this method).
     * @return the message
     */
    public static WebSocketMessage text(final CharBuffer data) {
        return new WebSocketMessage(MessageType.TEXT, data, null, null, null, (long) data.remaining());
    }

    /**
     * The text stream message.
     *
     * @param data the data.
     * @return the message
     */
    public static WebSocketMessage text(final AInput<CharBuffer> data) {
        return text(data, null);
    }

    /**
     * The text stream message.
     *
     * @param data   the data.
     * @param length the length if it is known
     * @return the message
     */
    public static WebSocketMessage text(final AInput<CharBuffer> data, final Long length) {
        return new WebSocketMessage(MessageType.TEXT_STREAM, null, null, data, null, length);
    }

    /**
     * The binary message.
     *
     * @param data the data (the buffer should not be used after call of this method).
     * @return the message
     */
    public static WebSocketMessage binary(final ByteBuffer data) {
        return new WebSocketMessage(MessageType.BINARY, null, data, null, null, (long) data.remaining());
    }

    /**
     * The ping message.
     *
     * @param data the data (the buffer should not be used after call of this method).
     * @return the message
     */
    public static WebSocketMessage ping(final ByteBuffer data) {
        return new WebSocketMessage(MessageType.PING, null, data, null, null, (long) data.remaining());
    }

    /**
     * The pong message.
     *
     * @param data the data (the buffer should not be used after call of this method).
     * @return the message
     */
    public static WebSocketMessage pong(final ByteBuffer data) {
        return new WebSocketMessage(MessageType.PONG, null, data, null, null, (long) data.remaining());
    }


    /**
     * The binary message.
     *
     * @param data the data.
     * @return the message
     */
    public static WebSocketMessage binary(final AInput<ByteBuffer> data) {
        return binary(data, null);
    }

    /**
     * The close message.
     *
     * @param code    the code
     * @param message the message
     * @return the created message
     */
    public static WebSocketMessage close(short code, String message) {
        final byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 2);
        buffer.putShort(code);
        buffer.put(bytes);
        buffer.flip();
        return new WebSocketMessage(MessageType.CLOSE, null, buffer, null, null, (long) buffer.remaining());
    }

    /**
     * The binary message.
     *
     * @param data   the data.
     * @param length the data length
     * @return the message
     */
    public static WebSocketMessage binary(final AInput<ByteBuffer> data, final Long length) {
        return new WebSocketMessage(MessageType.BINARY_STREAM, null, null, null, data, length);
    }

    /**
     * @return the message type.
     */
    public MessageType getType() {
        return type;
    }

    /**
     * @return the message text (a new read-only buffer)
     */
    public CharBuffer getText() {
        expectType(MessageType.TEXT);
        return text.asReadOnlyBuffer();
    }

    private void expectType(MessageType messageType) {
        if (type != messageType) {
            throw new IllegalStateException(unexpectedType());
        }
    }

    private String unexpectedType() {
        return String.format("The message has type: %s", type);
    }

    /**
     * @return the message data (a new read-only buffer)
     */
    public ByteBuffer getBinary() {
        expectType(MessageType.BINARY);
        return binary.asReadOnlyBuffer();
    }

    /**
     * @return the text stream (in case of array text message, the array is wrapped)
     */
    public AInput<CharBuffer> getTextStream() {
        if (type == MessageType.TEXT_STREAM) {
            return textStream;
        } else if (type == MessageType.TEXT) {
            return BufferBackedInput.wrap(text);
        } else {
            throw new IllegalStateException(unexpectedType());
        }
    }

    /**
     * @return the binary stream (in case of array text message, the array is wrapped)
     */
    public AInput<ByteBuffer> getBinaryStream() {
        if (type == MessageType.BINARY_STREAM) {
            return binaryStream;
        } else if (type == MessageType.BINARY) {
            return BufferBackedInput.wrap(binary);
        } else {
            throw new IllegalStateException(unexpectedType());
        }
    }

    /**
     * @return the length of data or null if it is not known.
     */
    public Long getLength() {
        return length;
    }

    /**
     * Message type.
     */
    public enum MessageType {
        /**
         * The message is binary.
         */
        BINARY,
        /**
         * The message is binary stream.
         */
        BINARY_STREAM,
        /**
         * The message is text.
         */
        TEXT,
        /**
         * The message is a text stream.
         */
        TEXT_STREAM,
        /**
         * The close message. This is last message in the stream and it could carry a payload.
         */
        CLOSE,
        /**
         * Ping message.
         */
        PING,
        /**
         * Pong message.
         */
        PONG,
    }
}
