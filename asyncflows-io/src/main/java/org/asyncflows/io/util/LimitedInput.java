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

package org.asyncflows.io.util;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AInputProxyFactory;
import org.asyncflows.io.IOUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.RequestQueue;

import java.nio.Buffer;

import static org.asyncflows.io.IOUtil.isEof;

/**
 * The stream that is limited by the specified size, the {@link #close()} does not close underlying stream.
 *
 * @param <B> the buffer type
 */
public class LimitedInput<B extends Buffer>
        extends CloseableInvalidatingBase implements AInput<B>, NeedsExport<AInput<B>> {
    /**
     * Read requests.
     */
    private final RequestQueue reads = new RequestQueue();
    /**
     * the input stream.
     */
    private final AInput<B> input;
    /**
     * The limit.
     */
    private final long limit;
    /**
     * The read amount.
     */
    private long readAmount;

    /**
     * The constructor for the stream.
     *
     * @param input the input stream
     * @param limit the limit
     */
    protected LimitedInput(final AInput<B> input, final long limit) {
        this.input = input;
        this.limit = limit;
    }

    /**
     * Return limited stream.
     *
     * @param input the stream to limit
     * @param limit the limit
     * @param <B>   the buffer type
     * @return the limited stream
     */
    public static <B extends Buffer> AInput<B> limit(final AInput<B> input, final long limit) {
        return new LimitedInput<>(input, limit).export();
    }

    @Override
    public Promise<Integer> read(final B buffer) {
        return reads.run(() -> {
            ensureValidAndOpen();
            if (readAmount > limit) {
                throw new IllegalStateException("Stream has read too much!");
            }
            if (readAmount == limit) {
                return IOUtil.EOF_PROMISE;
            }
            final int savedLimit = buffer.limit();
            if (limit - readAmount < buffer.remaining()) {
                buffer.limit(savedLimit - buffer.remaining() + (int) (limit - readAmount));
            }
            return input.read(buffer).listen(resolution -> {
                if (resolution.isSuccess() && !isEof(resolution.value())) {
                    readAmount += resolution.value();
                }
                buffer.limit(savedLimit);
            });
        }).listen(outcomeChecker());
    }

    @Override
    public AInput<B> export(final Vat vat) {
        return AInputProxyFactory.createProxy(vat, this);
    }
}
