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

package org.asyncflows.tracker;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.util.ASubscription;

import java.util.Objects;
import java.util.function.Consumer;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.function.FunctionExporter.exportConsumer;

/**
 * The adapter that performs typical functions needed for listening to some tracker.
 *
 * @param <A> the value type
 */
public final class TrackerAdapter<A> implements Consumer<Outcome<A>> {
    /**
     * The wrapped tracker.
     */
    private final ATracker<A> tracker;
    /**
     * The on-change action.
     */
    private final Runnable onChange;
    /**
     * The opened subscription if active.
     */
    private ASubscription subscription;
    /**
     * The open key. It is used to detect situation when there are multiple start/stop in quick succession.
     */
    private Object key;
    /**
     * The current value.
     */
    private Outcome<A> value;

    /**
     * The constructor.
     * @param tracker the wrapped tracker
     * @param onChange the callback invoked on change
     */
    public TrackerAdapter(ATracker<A> tracker, Runnable onChange) {
        this.tracker = tracker;
        this.onChange = onChange;
    }

    /**
     * @return the current value
     */
    public Outcome<A> value() {
        return value;
    }

    /**
     * Start listening.
     */
    public void start() {
        key = new Object();
        final Object savedKey = key;
        tracker.listen(exportConsumer(this)).flatMapOutcome(o -> {
            if (o.isFailure()) {
                value = Outcome.failure(o.failure());
                return aVoid();
            }
            if (key != savedKey) {
                o.value().close();
                return aVoid();
            }
            subscription = o.value();
            return aVoid();
        });
    }

    /**
     * Stop listening
     */
    public void stop() {
        key = null;
        value = null;
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }


    @Override
    public void accept(Outcome<A> newValue) {
        if (!Objects.equals(value, newValue)) {
            value = newValue;
            onChange.run();
        }
    }
}
