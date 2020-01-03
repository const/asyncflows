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

package org.asyncflows.protocol.http.common.content;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.io.util.ByteGeneratorContext;

import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The output for the rest of the stream.
 */
public class RestOfStreamOutput extends MessageOutput {
    /**
     * The constructor.
     *
     * @param output       the output
     * @param stateTracker the state tracker
     */
    public RestOfStreamOutput(final ByteGeneratorContext output, final AResolver<OutputState> stateTracker) {
        super(output, stateTracker);
    }

    @Override
    public Promise<Void> write(final ByteBuffer buffer) {
        return writes.run(() -> {
            ensureValidAndOpen();
            stateChanged(OutputState.DATA_CLOSEABLE);
            if (output.isSendNeeded()) {
                return output.send().thenFlatGet(() -> output.getOutput().write(buffer));
            } else {
                return output.getOutput().write(buffer);
            }
        }).listen(outcomeChecker());
    }

    @Override
    protected Promise<Void> closeAction() {
        return output.send().thenFlatGet(() -> {
            stateChanged(OutputState.CLOSED_LAST);
            return aVoid();
        });
    }
}
