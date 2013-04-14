package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

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
        final RandevuQueue<T> queue = new RandevuQueue<T>();
        return Tuple2.of((ASink<T>) queue.sink, (AStream<T>) queue.stream);
    }

    /**
     * Create exported pair of the sink and queue.
     *
     * @param <T> the value type
     * @return the pair of the sink and queue
     */
    public static <T> Tuple2<ASink<T>, AStream<T>> exported() {
        final RandevuQueue<T> queue = new RandevuQueue<T>();
        return Tuple2.of(queue.sink.export(), queue.stream.export());
    }

    /**
     * This is a bit special sink implementation that provides a feed at the stream.
     */
    private class RandevuSink extends CloseableInvalidatingBase implements ASink<T>, ExportsSelf<ASink<T>> {
        /**
         * The request queue for the promise.
         */
        private final RequestQueue requests = new RequestQueue();
        /**
         * The finished promise.
         */
        private final Promise<Void> finished = new Promise<Void>();
        /**
         * True if the sink is closed.
         */
        private boolean eof; // NOPMD

        @Override
        public Promise<Void> put(final T value) {
            return requests.run(new ACallable<Void>() {
                @Override
                public Promise<Void> call() throws Throwable {
                    return aSeqLoop(new ACallable<Boolean>() {
                        @Override
                        public Promise<Boolean> call() throws Throwable {
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
                        }
                    });
                }
            });
        }

        @Override
        public Promise<Void> fail(final Throwable error) {
            return requests.run(new ACallable<Void>() {
                @Override
                public Promise<Void> call() throws Throwable {
                    final Throwable t = error == null ? new IllegalArgumentException("error cannot be null") : error;
                    invalidate(t);
                    if (finished.getState() == Promise.State.INITIAL) {
                        notifyFailure(finished.resolver(), error);
                    }
                    problem = t;
                    if (currentRequest != null) {
                        notifyFailure(currentRequest, t);
                        currentRequest = null;
                    }
                    return aVoid();
                }
            });
        }

        @Override
        public Promise<Void> finished() {
            return finished;
        }

        @Override
        protected Promise<Void> closeAction() {
            return requests.run(new ACallable<Void>() {
                @Override
                public Promise<Void> call() throws Throwable {
                    eof = true;
                    if (currentRequest != null) {
                        notifySuccess(currentRequest, Maybe.<T>empty());
                        currentRequest = null;
                    }
                    return aVoid();
                }
            });
        }

        @Override
        public ASink<T> export() {
            return export(Vat.current());
        }

        @Override
        public ASink<T> export(final Vat vat) {
            return StreamExportUtil.export(vat, this);
        }
    }

    /**
     * The stream part of the randevu queue.
     */
    private class RandevuStream extends StreamBase<T> {
        /**
         * The request queue for stream.
         */
        private final RequestQueue requests = new RequestQueue();

        @Override
        public Promise<Maybe<T>> produce() throws Throwable {
            return requests.run(new ACallable<Maybe<T>>() {
                @Override
                public Promise<Maybe<T>> call() throws Throwable {
                    if (problem != null) {
                        return aFailure(problem);
                    }
                    if (sink.eof) {
                        return aMaybeEmpty();
                    }
                    final Promise<Maybe<T>> promise = new Promise<Maybe<T>>();
                    currentRequest = promise.resolver();
                    sink.requests.resume();
                    return promise;
                }
            });
        }

        @Override
        protected Promise<Void> closeAction() {
            sink.requests.resume();
            return super.closeAction();
        }
    }
}
