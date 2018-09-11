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

import org.asyncflows.io.AOutput;
import org.asyncflows.io.IOExportUtil;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;

import java.nio.ByteBuffer;

import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The base class for the message output.
 */
public abstract class MessageOutput extends CloseableInvalidatingBase
        implements AOutput<ByteBuffer>, NeedsExport<AOutput<ByteBuffer>> {
    /**
     * Read requests.
     */
    protected final RequestQueue writes = new RequestQueue();
    /**
     * the input stream.
     */
    protected final ByteGeneratorContext output;
    /**
     * The tracker of the state of input.
     */
    private final AResolver<OutputState> stateTracker;
    /**
     * Notify that the state has changed.
     */
    private OutputState lastState = OutputState.NOT_STARTED;

    /**
     * The constructor.
     *
     * @param output       the output
     * @param stateTracker the state tracker
     */
    protected MessageOutput(final ByteGeneratorContext output, final AResolver<OutputState> stateTracker) {
        this.output = output;
        this.stateTracker = stateTracker;
    }

    /**
     * Notify that the state has been changed.
     *
     * @param state the state change
     */
    protected final void stateChanged(final OutputState state) {
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
    public Promise<Void> flush() {
        return writes.run(() -> {
            ensureValidAndOpen();
            return output.send().thenFlatGet(() -> output.getOutput().flush()).listen(outcomeChecker());
        });
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        notifyFailure(stateTracker, throwable);
        lastState = OutputState.ERROR;
        super.onInvalidation(throwable);
    }

    @Override
    public AOutput<ByteBuffer> export(final Vat vat) {
        return IOExportUtil.export(vat, this);
    }
}
