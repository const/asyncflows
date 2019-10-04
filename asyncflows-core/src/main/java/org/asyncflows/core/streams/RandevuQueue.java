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

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * <p>Randevu Queue is a queue that provides a pair of facets of types {@link AStream} and {@link ASink}.</p>
 * <p>The put operation on randevu queue only resolves when the value is actually sent to the client.
 * After the {@link ASink} facet is closed, the {@link AStream} facet receives EOF on consequent reads.
 * After the {@link AStream} is closed, the the {@link ASink} facet receives exceptions on write.</p>
 *
 * @param <T> the element type
 */
public final class RandevuQueue<T> {
    /**
     * The sink part of the queue.
     */
    private final RandevuSink sink = new RandevuSink();
    /**
     * The stream part of the queue.
     */
    private final RandevuStream stream = new RandevuStream();
    /**
     * The current request.
     */
    private AResolver<Maybe<T>> currentRequest;
    /**
     * The failure written to the stream.
     */
    private Throwable problem;

    /**
     * The private constructor (use static factory methods).
     */
    private RandevuQueue() {
    }

    /**
     * Create non-exported pair of sink and queue. Use it only if you are sure that references do not leak
     * to other vats (for example when this queue is used for builders).
     *
     * @param <T> the value type
     * @return the pair of the sink and stream
     */
    public static <T> Tuple2<ASink<T>, AStream<T>> local() {
        final RandevuQueue<T> queue = new RandevuQueue<>();
        return Tuple2.of(queue.sink, queue.stream);
    }

    /**
     * Create exported pair of the sink and queue.
     *
     * @param <T> the value type
     * @return the pair of the sink and queue
     */
    public static <T> Tuple2<ASink<T>, AStream<T>> exported() {
        final RandevuQueue<T> queue = new RandevuQueue<>();
        return Tuple2.of(queue.sink.export(), queue.stream.export());
    }

    /**
     * This is a bit special sink implementation that provides a feed at the stream.
     */
    private final class RandevuSink extends CloseableInvalidatingBase implements ASink<T>, NeedsExport<ASink<T>> {
        /**
         * The request queue for the promise.
         */
        private final RequestQueue requests = new RequestQueue();
        /**
         * The finished promise.
         */
        private final Promise<Void> finished = new Promise<>();
        /**
         * True if the sink is closed.
         */
        private boolean eof;

        /**
         * The constructor.
         */
        private RandevuSink() {
            // do nothing
        }

        @Override
        public Promise<Void> put(final T value) {
            return requests.runSeqWhile(() -> {
                if (!isValid()) {
                    return invalidationPromise();
                }
                if (stream.isClosed()) {
                    // just discard a value
                    return aFalse();
                }
                if (currentRequest != null) {
                    notifySuccess(currentRequest, Maybe.value(value));
                    currentRequest = null;
                    return aFalse();
                }
                return requests.suspendThenTrue();
            });
        }

        @Override
        public Promise<Void> fail(final Throwable error) {
            return requests.run(() -> {
                final Throwable t = error == null ? new IllegalArgumentException("error cannot be null") : error;
                invalidate(t);
                if (finished.isUnresolved()) {
                    notifyFailure(finished.resolver(), error);
                }
                problem = t;
                if (currentRequest != null) {
                    notifyFailure(currentRequest, t);
                    currentRequest = null;
                }
                return aVoid();
            });
        }

        @Override
        public Promise<Void> finished() {
            return finished;
        }

        @Override
        protected Promise<Void> closeAction() {
            return requests.run(() -> {
                eof = true;
                if (currentRequest != null) {
                    notifySuccess(currentRequest, Maybe.empty());
                    currentRequest = null;
                }
                return aVoid();
            });
        }

        @Override
        public ASink<T> export(final Vat vat) {
            return ASinkProxyFactory.createProxy(vat, this);
        }
    }

    /**
     * The stream part of the randevu queue.
     */
    private final class RandevuStream extends StreamBase<T> {
        /**
         * The request queue for stream.
         */
        private final RequestQueue requests = new RequestQueue();

        /**
         * The constructor.
         */
        private RandevuStream() {
            // do nothing
        }

        @Override
        protected Promise<Maybe<T>> produce() {
            return requests.run(() -> {
                if (problem != null) {
                    return aFailure(problem);
                }
                if (sink.eof) {
                    return aMaybeEmpty();
                }
                final Promise<Maybe<T>> promise = new Promise<>();
                currentRequest = promise.resolver();
                sink.requests.resume();
                return promise;
            });
        }

        @Override
        protected Promise<Void> closeAction() {
            sink.requests.resume();
            return super.closeAction();
        }
    }
}
