/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

package org.asyncflows.protocol.http.common.content;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AInputProxyFactory;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.RequestQueue;

import java.nio.ByteBuffer;

import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The base class for the message body input.
 */
public abstract class MessageInput extends CloseableInvalidatingBase
        implements AInput<ByteBuffer>, NeedsExport<AInput<ByteBuffer>> {
    /**
     * Read requests.
     */
    protected final RequestQueue reads = new RequestQueue();
    /**
     * the input stream.
     */
    protected final ByteParserContext input;
    /**
     * The tracker ofr the state of input.
     */
    private final AResolver<InputState> stateTracker;
    /**
     * Notify that the state has changed.
     */
    private InputState lastState = InputState.IDLE;


    /**
     * The constructor.
     *
     * @param input        the input tracker
     * @param stateTracker the state tracker
     */
    protected MessageInput(final ByteParserContext input, final AResolver<InputState> stateTracker) {
        this.input = input;
        this.stateTracker = stateTracker;
    }

    /**
     * Notify that the state has been changed.
     *
     * @param state the state change
     */
    protected final void stateChanged(final InputState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        if (isValid() && lastState != state) { // NOPMD
            if (state.ordinal() < lastState.ordinal()) {
                throw new IllegalStateException("Shifting state backward: " + lastState + " -> " + state);
            }
            notifySuccess(stateTracker, state);
        }
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        notifyFailure(stateTracker, throwable);
        lastState = InputState.ERROR;
        super.onInvalidation(throwable);
    }

    @Override
    public AInput<ByteBuffer> export(final Vat vat) {
        return AInputProxyFactory.createProxy(vat, this);
    }
}
