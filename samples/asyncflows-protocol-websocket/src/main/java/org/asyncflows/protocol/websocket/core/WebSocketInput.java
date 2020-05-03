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

package org.asyncflows.protocol.websocket.core;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.streams.AStream;
import org.asyncflows.core.streams.AStreamProxyFactory;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.protocol.websocket.WebSocketMessage;

import java.nio.ByteBuffer;

import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The web socket input.
 */
public class WebSocketInput extends CloseableInvalidatingBase
        implements AStream<WebSocketMessage>, ExportableComponent<AStream<WebSocketMessage>> {
    /**
     * The queue for the next requests.
     */
    private final RequestQueue requestQueue = new RequestQueue();
    /**
     * The context.
     */
    private final ByteParserContext input;

    public WebSocketInput(final ByteParserContext input) {
        this.input = input;
    }

    @Override
    public Promise<Maybe<WebSocketMessage>> next() {
        final Promise<Maybe<WebSocketMessage>> next = new Promise<>();
        final AResolver<Maybe<WebSocketMessage>> resolver = next.resolver();
        requestQueue.run(() -> FrameHeader.read(input).map(
                header -> {
                    final int opCode = header.getOpCode();
                    if (opCode == FrameHeader.OP_CLOSE) {
                        notifySuccess(resolver, Maybe.empty());
                        return IOUtil.BYTE.discard(input.input(), header.getLength(), ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE)).toVoid();
                    }
                    return null;
                }
        ));
        return next;
    }

    @Override
    public AStream<WebSocketMessage> export(final Vat vat) {
        return AStreamProxyFactory.createProxy(vat, this);
    }
}
