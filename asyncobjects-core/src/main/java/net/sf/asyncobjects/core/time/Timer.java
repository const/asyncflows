package net.sf.asyncobjects.core.time;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.CoreExportUtil;
import net.sf.asyncobjects.core.CoreFunctionUtil;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.Success;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.stream.AStream;
import net.sf.asyncobjects.core.stream.StreamBase;
import net.sf.asyncobjects.core.stream.Streams;
import net.sf.asyncobjects.core.util.SimpleQueue;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * The timer class. The implementation uses {@link java.util.Timer} and it is mostly hosted in it.
 * The streams that are created are local to the caller vat. Wrap timer into the proxy, if different
 * behaviour is desired.
 * <p/>
 * The timer keeps track of its streams and auto-close them if they become unreferenced.
 */
public class Timer implements ATimer {
    // DO NOT REPEAT DANGEROUS DESIGN PATTERNS FROM THIS CLASS, UNLESS YOU KNOW WHAT YOU ARE DOING
    /**
     * The counter for anonymous timers.
     */
    private static final AtomicInteger ANONYMOUS_TIMER_COUNT = new AtomicInteger(0);
    /**
     * The interval to check stale streams.
     */
    private static final long QUEUE_CHECK = 10000L;
    /**
     * The timer to use.
     */
    private final java.util.Timer timer;
    /**
     * The reference queue that is periodically checked.
     */
    private final ReferenceQueue<AStream<Long>> referenceQueue = new ReferenceQueue<AStream<Long>>();

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
                    final WeakStreamReference ref = (WeakStreamReference) referenceQueue.poll();
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
        final Promise<Long> rc = new Promise<Long>();
        final AResolver<Long> resolver = rc.resolver();
        timer.schedule(getOneshotTask(resolver), delay);
        return rc;
    }

    @Override
    public Promise<Long> waitFor(final Date time) {
        final Promise<Long> rc = new Promise<Long>();
        final AResolver<Long> resolver = rc.resolver();
        timer.schedule(getOneshotTask(resolver), time);
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
    public Promise<AStream<Long>> fixedRate(final Date firstTime, final long period) {
        final SimpleQueue<Outcome<Long>> queue = new SimpleQueue<Outcome<Long>>();
        final AResolver<Long> putResolver = CoreExportUtil.export(new AResolver<Long>() {
            @Override
            public void resolve(final Outcome<Long> resolution) throws Throwable {
                queue.put(resolution);
            }
        });
        final FixedRateTask task = new FixedRateTask(putResolver);
        final AStream<Long> stream = new TimerStream(task, queue).export();
        task.reference = new WeakStreamReference(stream, task, referenceQueue);
        timer.scheduleAtFixedRate(task, firstTime, period);
        return aValue(stream);
    }

    @Override
    public Promise<AStream<Long>> fixedDelay(final Date firstTime, final long delay) {
        return aValue(Streams.aForProducer(new ACallable<Maybe<Long>>() {
            private boolean first = true;

            @Override
            public Promise<Maybe<Long>> call() throws Throwable {
                if (first) {
                    first = false;
                    return waitFor(firstTime).map(CoreFunctionUtil.<Long>maybeMapper());
                } else {
                    return sleep(delay).map(CoreFunctionUtil.<Long>maybeMapper());

                }
            }
        }).stream());
    }

    @Override
    public Promise<AStream<Long>> fixedRate(final long initialDelay, final long period) {
        return fixedRate(new Date(System.currentTimeMillis() + initialDelay), period);
    }

    @Override
    public Promise<AStream<Long>> fixedDelay(final long initialDelay, final long delay) {
        return fixedDelay(new Date(System.currentTimeMillis() + initialDelay), delay);
    }

    @Override
    public Promise<Void> close() {
        timer.cancel();
        return aVoid();
    }

    /**
     * The reference used to cancel tasks if their streams are garbage collected.
     */
    private static final class WeakStreamReference extends WeakReference<AStream<Long>> {
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
        private WeakStreamReference(final AStream<Long> referent, final TimerTask task,
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
        private WeakStreamReference reference;

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
            if (reference.get() != null) {
                try {
                    queue.resolve(new Success<Long>(scheduledExecutionTime()));
                } catch (Throwable t) {
                    cancel();
                }
            } else {
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
            return queue.take().map(new AFunction<Maybe<Long>, Outcome<Long>>() {
                @Override
                public Promise<Maybe<Long>> apply(final Outcome<Long> value) throws Throwable {
                    ensureValidAndOpen();
                    if (value.isSuccess()) {
                        return aMaybeValue(value.value());
                    } else {
                        return aFailure(value.failure());
                    }
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
