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

package org.asyncflows.core.tracker;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.streams.ASink;
import org.asyncflows.core.streams.AStream;
import org.asyncflows.core.streams.RandevuQueue;
import org.asyncflows.core.util.ASubscription;
import org.asyncflows.core.util.RequestQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.asyncflows.core.CoreFlows.aNull;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.UtilExporter.exportSubscription;

/**
 * Base class for trackers.
 *
 * @param <T> the tracker
 */
public abstract class AbstractTracker<T> implements ATracker<T> {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTracker.class);
    /**
     * Notification queue.
     */
    private final RequestQueue notifications = new RequestQueue();
    /**
     * The subscriptions.
     */
    private final List<Consumer<Outcome<T>>> subscriptions = new LinkedList<>();
    /**
     * The state.
     */
    private Outcome<T> state;

    /**
     * The value tracking started.
     */
    protected abstract void start();

    /**
     * The value tracking stopped.
     */
    protected abstract void stop();

    /**
     * Set current value for tracker.
     *
     * @param value the value
     */
    protected final void push(Outcome<T> value) {
        notifications.run(() -> {
            if (Objects.equals(state, value)) {
                return aVoid();
            }
            state = value;
            if (state != null) {
                for (Consumer<Outcome<T>> subscription : subscriptions) {
                    notifyListener(subscription);
                }
            }
            return aVoid();
        });
    }

    private void notifyListener(Consumer<Outcome<T>> subscription) {
        try {
            subscription.accept(state);
        } catch (Throwable t) {
            LOG.error("Failed lister", t);
        }
    }

    @Override
    public Promise<ASubscription> subscribe(Consumer<Outcome<T>> listener) {
        return notifications.run(() -> {
            subscriptions.add(listener);
            if (state != null) {
                notifyListener(listener);
            }
            if (subscriptions.isEmpty()) {
                start();
            }
            return aValue(exportSubscription(() -> {
                subscriptions.remove(listener);
                if (subscriptions.isEmpty()) {
                    stop();
                }
                return aNull();
            }));
        });
    }

    @Override
    public final Promise<AStream<Outcome<T>>> open() {
        final Tuple2<ASink<Outcome<T>>, AStream<Outcome<T>>> exported = RandevuQueue.exported();
        ASink<Outcome<T>> sink = exported.getValue1();
        subscribe(sink::put).flatMap(s -> sink.finished().mapOutcome(o -> s.close()));
        return aValue(exported.getValue2());
    }
}
