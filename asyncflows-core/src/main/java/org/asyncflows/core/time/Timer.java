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

package org.asyncflows.core.time;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.ThreadSafe;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.function.AsyncFunctionUtil;
import org.asyncflows.core.function.FunctionExporter;
import org.asyncflows.core.streams.AStream;
import org.asyncflows.core.streams.AsyncStreams;
import org.asyncflows.core.streams.StreamBase;
import org.asyncflows.core.util.AsynchronousService;
import org.asyncflows.core.util.SimpleQueue;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.time.Instant;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The timer class. The implementation uses {@link java.util.Timer} and it is mostly hosted in it.
 * The streams that are created are local to the caller vat. Wrap timer into the proxy, if different
 * behaviour is desired.
 * <p>
 * The timer keeps track of its streams and auto-close them if they become unreferenced.
 */
@ThreadSafe
@SuppressWarnings("squid:S1700")
public class Timer implements ATimer, AsynchronousService {
    // DO NOT REPEAT DANGEROUS DESIGN PATTERNS FROM THIS CLASS, UNLESS YOU KNOW WHAT YOU ARE DOING
    /**
     * The counter for anonymous timers.
     */
    private static final AtomicInteger ANONYMOUS_TIMER_COUNT = new AtomicInteger(0);
    /**
     * The interval to check stale streams.
     */
    private static final long QUEUE_CHECK = 1000L;
    /**
     * The timer to use.
     */
    private final java.util.Timer timer;
    /**
     * The reference queue that is periodically checked.
     */
    private final ReferenceQueue<AStream<Long>> referenceQueue = new ReferenceQueue<>();

    /**
     * The constructor. Note that the timer is owned by the thread.
     *
     * @param timer the time to use
     */
    public Timer(final java.util.Timer timer) {
        this.timer = timer;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                while (true) {
                    final PhantomStreamReference ref = (PhantomStreamReference) referenceQueue.poll();
                    if (ref != null) {
                        ref.getTask().cancel();
                    } else {
                        break;
                    }
                }
            }
        }, QUEUE_CHECK, QUEUE_CHECK);
    }

    /**
     * Created a timer that uses daemon thread for execution.
     */
    public Timer() {
        this(new java.util.Timer("AsyncObjects Timer " + ANONYMOUS_TIMER_COUNT.incrementAndGet(), true));
    }

    @Override
    public Promise<Long> sleep(final long delay) {
        final Promise<Long> rc = new Promise<>();
        final AResolver<Long> resolver = rc.resolver();
        timer.schedule(getOneshotTask(resolver), delay);
        return rc;
    }

    @Override
    public Promise<Long> waitFor(final Instant time) {
        final Promise<Long> rc = new Promise<>();
        final AResolver<Long> resolver = rc.resolver();
        timer.schedule(getOneshotTask(resolver), Date.from(time));
        return rc;
    }

    /**
     * Get One-shot task that notifies resolver on complete or cancel.
     *
     * @param resolver a resolver
     * @return the task
     */
    private TimerTask getOneshotTask(final AResolver<Long> resolver) {
        return new TimerTask() {
            private final AtomicBoolean done = new AtomicBoolean(false);

            @Override
            public void run() {
                if (done.compareAndSet(false, true)) {
                    notifySuccess(resolver, this.scheduledExecutionTime());
                }
            }

            @Override
            public boolean cancel() {
                if (done.compareAndSet(false, true)) {
                    notifyFailure(resolver, new CancellationException("The task has been cancelled."));
                }
                return super.cancel();
            }
        };
    }

    @Override
    public Promise<AStream<Long>> fixedRate(final Instant firstTime, final long period) {
        final SimpleQueue<Outcome<Long>> queue = new SimpleQueue<>();
        final AResolver<Long> putResolver = FunctionExporter.exportResolver(queue::put);
        final FixedRateTask task = new FixedRateTask(putResolver);
        final AStream<Long> stream = new TimerStream(task, queue).export();
        task.reference = new PhantomStreamReference(stream, task, referenceQueue);
        timer.scheduleAtFixedRate(task, Date.from(firstTime), period);
        return aValue(stream);
    }

    @Override
    public Promise<AStream<Long>> fixedDelay(final Instant firstTime, final long delay) {
        return aValue(AsyncStreams.aForProducer(new ASupplier<Maybe<Long>>() {
            private boolean first = true;

            @Override
            public Promise<Maybe<Long>> get() throws Exception {
                if (first) {
                    first = false;
                    return waitFor(firstTime).flatMap(AsyncFunctionUtil.<Long>maybeMapper());
                } else {
                    return sleep(delay).flatMap(AsyncFunctionUtil.<Long>maybeMapper());

                }
            }
        }).stream());
    }

    @Override
    public Promise<AStream<Long>> fixedRate(final long initialDelay, final long period) {
        return fixedRate(Instant.now().plusMillis(initialDelay), period);
    }

    @Override
    public Promise<AStream<Long>> fixedDelay(final long initialDelay, final long delay) {
        return fixedDelay(Instant.now().plusMillis(initialDelay), delay);
    }

    @Override
    public Promise<Void> close() {
        timer.cancel();
        return aVoid();
    }

    /**
     * The reference used to cancel tasks if their streams are garbage collected.
     */
    private static final class PhantomStreamReference extends PhantomReference<AStream<Long>> {
        /**
         * The task to cancel..
         */
        private final TimerTask task;

        /**
         * The constructor.
         *
         * @param referent the referent
         * @param task     the task
         * @param q        the queue
         */
        private PhantomStreamReference(final AStream<Long> referent, final TimerTask task,
                                       final ReferenceQueue<? super AStream<Long>> q) {
            super(referent, q);
            this.task = task;
        }

        /**
         * @return the task to cancel if reference is lost
         */
        private TimerTask getTask() {
            return task;
        }
    }

    /**
     * The fixed rate task.
     */
    private static final class FixedRateTask extends TimerTask {
        /**
         * Resolver used for the notification.
         */
        private final AResolver<Long> queue;
        /**
         * The reference to track if someone still cares about the stream.
         */
        private PhantomStreamReference reference;

        /**
         * The constructor.
         *
         * @param queue the queue to notify
         */
        private FixedRateTask(final AResolver<Long> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                queue.resolve(Outcome.success(scheduledExecutionTime()));
            } catch (Throwable t) {
                cancel();
            }
        }

        @Override
        public boolean cancel() {
            if (reference != null) {
                reference.clear();
            }
            notifyFailure(queue, new IllegalStateException("The timer is cancelled!"));
            return super.cancel();
        }
    }

    /**
     * The buffered stream that is notified by resolver.
     */
    private static final class TimerStream extends StreamBase<Long> {
        /**
         * The timer task that produces values.
         */
        private final TimerTask task;
        /**
         * The queue of values.
         */
        private final SimpleQueue<Outcome<Long>> queue;

        /**
         * The constructor.
         *
         * @param task  the task to use
         * @param queue the queue to read from
         */
        private TimerStream(final TimerTask task, final SimpleQueue<Outcome<Long>> queue) {
            this.task = task;
            this.queue = queue;
        }

        @Override
        protected Promise<Maybe<Long>> produce() throws Throwable {
            ensureValidAndOpen();
            return queue.take().flatMap(value -> {
                ensureValidAndOpen();
                if (value.isSuccess()) {
                    return aMaybeValue(value.value());
                } else {
                    return aFailure(value.failure());
                }
            });
        }

        @Override
        protected Promise<Void> closeAction() {
            task.cancel();
            return super.closeAction();
        }
    }
}
