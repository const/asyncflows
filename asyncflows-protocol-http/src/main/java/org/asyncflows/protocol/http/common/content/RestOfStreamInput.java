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
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.util.ByteParserContext;

import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aOutcome;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.io.IOUtil.isEof;

/**
 * The input that tracks state and allows to read until EOF in the socket.
 * This input is used only for reading responses.
 */
public class RestOfStreamInput extends MessageInput {
    /**
     * True, if eof notified.
     */
    private boolean eofNotified;

    /**
     * The constructor.
     *
     * @param input        the input
     * @param stateTracker the stat tracker
     */
    public RestOfStreamInput(final ByteParserContext input, final AResolver<InputState> stateTracker) {
        super(input, stateTracker);
    }

    @Override
    public Promise<Integer> read(final ByteBuffer buffer) {
        return reads.run(() -> {
            ensureValidAndOpen();
            if (eofNotified) {
                return IOUtil.EOF_PROMISE;
            }
            stateChanged(InputState.DATA);
            if (input.buffer().hasRemaining()) {
                final int rc = BufferOperations.BYTE.put(buffer, input.buffer());
                return aValue(rc);
            }
            return input.input().read(buffer).flatMapOutcome(resolution -> {
                if (resolution.isSuccess() && isEof(resolution.value())) {
                    eofNotified = true;
                    stateChanged(InputState.EOF_NO_TRAILERS);
                }
                return aOutcome(resolution);
            });
        }).listen(outcomeChecker());
    }


    @Override
    protected Promise<Void> closeAction() {
        stateChanged(eofNotified ? InputState.CLOSED : InputState.CLOSED_BEFORE_EOF);
        return super.closeAction();
    }
}
