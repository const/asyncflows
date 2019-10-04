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

package org.asyncflows.core.streams;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Cell;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ASupplier;

import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aOutcome;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.closeResourceAction;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

/**
 * The stream utilities.
 */
public final class StreamUtil {
    /**
     * The private constructor for utility class.
     */
    private StreamUtil() {
    }

    /**
     * The producer for the stream.
     *
     * @param stream the stream.
     * @param <O>    the stream element type
     * @return the callable that invokes a read operation
     */
    public static <O> ASupplier<Maybe<O>> producerFromStream(final AStream<O> stream) {
        return stream::next;
    }

    /**
     * Take first elements.
     *
     * @param stream the stream to examine
     * @param n      the maximum amount of elements
     * @param <T>    the element type
     * @return a new stream
     */
    public static <T> AStream<T> head(final AStream<T> stream, final int n) {
        return new ChainedStreamBase<T, AStream<T>>(stream) {
            private int count;

            @Override
            protected Promise<Maybe<T>> produce() {
                if (count >= n) {
                    return aMaybeEmpty();
                } else {
                    count++;
                    return stream.next();
                }
            }
        };
    }

    /**
     * Copy the stream to sink and then close the stream in any case.
     *
     * @param stream the stream to copy
     * @param sink   the target sink
     * @param <O>    the element type
     * @return the amount of elements copied
     */
    @SuppressWarnings("squid:S3776")
    public static <O> Promise<Long> connect(final AStream<O> stream, final ASink<? super O> sink) {
        final long[] count = new long[1];
        final Cell<Outcome<Void>> stopped = new Cell<>();
        sink.finished().listen(resolution -> {
            stream.close();
            stopped.setValue(resolution);
        });
        return aSeq(
                () -> aSeqWhile(() -> {
                    if (!stopped.isEmpty()) {
                        return aFalse();
                    }
                    final Promise<Maybe<O>> next = aNow(StreamUtil.producerFromStream(stream));
                    return next.flatMapOutcome(value -> {
                        try {
                            if (value.isSuccess()) {
                                if (value.value().isEmpty()) {
                                    return sink.close().thenValue(false);
                                } else {
                                    if (!stopped.isEmpty()) {
                                        return aFalse();
                                    }
                                    count[0]++;
                                    return sink.put(value.value().value()).thenValue(true);
                                }
                            } else {
                                return sink.fail(value.failure()).<Boolean>thenFailure(value.failure());
                            }
                        } catch (Throwable problem) {
                            return sink.fail(problem).<Boolean>thenFailure(problem);
                        }
                    });
                })
        ).thenDo(
                () -> stopped.isEmpty() ? aVoid() : aOutcome(stopped.getValue())
        ).thenDo(
                () -> aValue(count[0])
        ).finallyDo(
                () -> aAll(closeResourceAction(sink)).andLast(closeResourceAction(stream)).toVoid()
        );
    }

}
