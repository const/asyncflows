/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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
import org.asyncflows.protocol.ProtocolStreamTruncatedException;

import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aOutcome;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.io.IOUtil.isEof;

/**
 * The stream that is limited by the specified size, the {@link #close()} does not close underlying stream.
 */
public class ContentLengthInput extends MessageInput {
    /**
     * The limit.
     */
    private final long limit;
    /**
     * The read amount.
     */
    private long readAmount;
    /**
     * True, if eof notified.
     */
    private boolean eofNotified;

    /**
     * The constructor for the stream.
     *
     * @param stateTracker the state tracker
     * @param input        the input stream
     * @param limit        the limit
     */
    protected ContentLengthInput(final AResolver<InputState> stateTracker, final ByteParserContext input,
                                 final long limit) {
        super(input, stateTracker);
        this.limit = limit;
        if (limit == 0) {
            stateChanged(InputState.EOF_NO_TRAILERS);
        }
    }


    @Override
    @SuppressWarnings("squid:S3776")
    public Promise<Integer> read(final ByteBuffer buffer) {
        return reads.run(() -> {
            ensureValidAndOpen();
            if (readAmount > limit) {
                throw new IllegalStateException("Stream has read too much!");
            }
            if (readAmount == limit) {
                if (!eofNotified) {
                    stateChanged(InputState.EOF_NO_TRAILERS);
                    eofNotified = true;
                }
                return IOUtil.EOF_PROMISE;
            }
            stateChanged(InputState.DATA);
            final int savedLimit = buffer.limit();
            if (limit - readAmount < buffer.remaining()) {
                buffer.limit(savedLimit - buffer.remaining() + (int) (limit - readAmount));
            }
            if (input.buffer().hasRemaining()) {
                final int rc = BufferOperations.BYTE.put(buffer, input.buffer());
                readAmount += rc;
                buffer.limit(savedLimit);
                return aValue(rc);
            }
            return input.input().read(buffer).flatMapOutcome(resolution -> {
                buffer.limit(savedLimit);
                if (resolution.isSuccess()) {
                    final boolean eof = isEof(resolution.value());
                    if (resolution.isSuccess() && !eof) {
                        readAmount += resolution.value();
                    }
                    if (eof && readAmount != limit) {
                        throw new ProtocolStreamTruncatedException(
                                "EOF before all data is read: " + (limit - readAmount));
                    }
                }
                return aOutcome(resolution);
            });
        }).listen(outcomeChecker());
    }

    @Override
    protected Promise<Void> closeAction() {
        stateChanged(readAmount == limit ? InputState.CLOSED : InputState.CLOSED_BEFORE_EOF);
        return super.closeAction();
    }
}
