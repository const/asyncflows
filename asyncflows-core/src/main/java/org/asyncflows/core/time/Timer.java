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

package org.asyncflows.core.time;

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.ThreadSafe;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.data.Subcription;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.function.AsyncFunctionUtil;
import org.asyncflows.core.streams.AStream;
import org.asyncflows.core.streams.AsyncStreams;
import org.asyncflows.core.util.AsynchronousService;
import org.asyncflows.core.util.Cancellation;
import org.asyncflows.core.util.RequestQueue;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    // This class happens to be threadsafe because of features of {@link java.util.Timer} class
    /**
     * The counter for anonymous timers.
     */
    private static final AtomicInteger ANONYMOUS_TIMER_COUNT = new AtomicInteger(0);
    /**
     * The timer to use.
     */
    private final java.util.Timer timer;

    /**
     * The constructor. Note that the timer is owned by the thread.
     *
     * @param timer the time to use
     */
    public Timer(final java.util.Timer timer) {
        this.timer = timer;
    }

    /**
     * Created a timer that uses daemon thread for execution.
     */
    public Timer() {
        this(new java.util.Timer("AsyncObjects Timer " + ANONYMOUS_TIMER_COUNT.incrementAndGet(), true));
    }

    @Override
    public Promise<Instant> waitFor(final Instant time) {
        if (time.isAfter(Instant.now())) {
            final Promise<Instant> rc = new Promise<>();
            final AResolver<Instant> resolver = rc.resolver();
            timer.schedule(getRunOnceTask(time, resolver), Date.from(time));
            return rc;
        } else {
            return aValue(time);
        }
    }

    /**
     * Get One-shot task that notifies resolver on complete or cancel.
     *
     * @param resolver a resolver
     * @return the task
     */
    private TimerTask getRunOnceTask(Instant scheduledTime, final AResolver<Instant> resolver) {
        class CancellableTimerTask extends TimerTask {
            private final AtomicBoolean done = new AtomicBoolean(false);
            private final Subcription cancellationRegistration;

            private CancellableTimerTask() {
                Cancellation c = Cancellation.currentOrNull();
                cancellationRegistration = c == null ? null : c.onCancelSync(this::cancel);
            }

            @Override
            public void run() {
                if (done.compareAndSet(false, true)) {
                    if (cancellationRegistration != null) {
                        cancellationRegistration.close();
                    }
                    notifySuccess(resolver, scheduledTime);
                }
            }

            @Override
            public boolean cancel() {
                if (done.compareAndSet(false, true)) {
                    if (cancellationRegistration != null) {
                        cancellationRegistration.close();
                    }
                    notifyFailure(resolver, new CancellationException("The task has been cancelled."));
                }
                return super.cancel();
            }
        }
        return new CancellableTimerTask();
    }

    @Override
    public Promise<AStream<Instant>> fixedRate(final Instant firstTime, final Duration period) {
        ASupplier<Maybe<Instant>> producer = new ASupplier<Maybe<Instant>>() {
            private final RequestQueue requests = new RequestQueue();
            private Instant next = firstTime;

            @Override
            public Promise<Maybe<Instant>> get() {
                return requests.run(() -> {
                    Instant now = Instant.now();
                    if (next.isAfter(now)) {
                        return waitFor(next).thenFlatGet(this::produce);
                    } else {
                        return produce();
                    }
                });
            }

            private Promise<Maybe<Instant>> produce() {
                Instant r = this.next;
                next = r.plus(period);
                return aMaybeValue(r);
            }
        };
        return aValue(AsyncStreams.aForProducer(producer).stream());
    }

    @Override
    public Promise<AStream<Instant>> fixedDelay(final Instant firstTime, final Duration delay) {
        return aValue(AsyncStreams.aForProducer(new ASupplier<Maybe<Instant>>() {
            private boolean first = true;

            @Override
            public Promise<Maybe<Instant>> get() {
                if (first) {
                    first = false;
                    return waitFor(firstTime).flatMap(AsyncFunctionUtil.maybeMapper());
                } else {
                    return sleep(delay).flatMap(AsyncFunctionUtil.maybeMapper());

                }
            }
        }).stream());
    }

    @Override
    public Promise<Void> close() {
        timer.cancel();
        return aVoid();
    }
}
