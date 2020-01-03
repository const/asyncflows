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

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.io.AOutput;

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
