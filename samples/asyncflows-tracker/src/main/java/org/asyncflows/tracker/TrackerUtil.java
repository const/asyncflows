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

package org.asyncflows.tracker;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.streams.AStream;
import org.asyncflows.core.streams.StreamBase;
import org.asyncflows.core.time.ATimer;
import org.asyncflows.core.util.RequestQueue;

import java.util.Objects;

import static org.asyncflows.core.CoreFlows.aMaybeValue;

public final class TrackerUtil {

    private TrackerUtil() {
    }

    public static <A> AStream<Outcome<A>> streamTracker(ATracker<A> tracker) {
        return new StreamBase<Outcome<A>>() {
            private final RequestQueue requestQueue = new RequestQueue();
            private TrackerAdapter<A> adapter;
            private Outcome<A> lastReported;

            private void changed() {
                requestQueue.resume();
            }

            @Override
            protected Promise<Maybe<Outcome<A>>> produce() {
                return requestQueue.runSeqUntilValue(() -> {
                    if (adapter == null) {
                        adapter = new TrackerAdapter<>(tracker, this::changed);
                        adapter.start();
                    }
                    if (Objects.equals(lastReported, adapter.value())) {
                        return requestQueue.suspendThenEmpty();
                    }
                    lastReported = adapter.value();
                    return aMaybeValue(Maybe.value(lastReported));
                });
            }

            @Override
            protected Promise<Void> closeAction() {
                if (adapter != null) {
                    adapter.stop();
                }
                return super.closeAction();
            }
        }.export();

    }

    public static <A> ATracker<A> trottle(ATracker<A> tracker, ATimer timer, long delay) {
        return new AbstractTracker<A>() {
            private Outcome<A> value;
            private final TrackerAdapter<A> adapter = new TrackerAdapter<>(tracker, this::changed);

            @Override
            protected void subscriptionStarted() {
                adapter.start();
            }

            @Override
            protected void subscriptionEnded() {
                adapter.stop();
                value = null;
            }

            private void changed() {
                final Outcome<A> newValue = adapter.value();
                value = newValue;
                timer.sleep(delay).thenGet(() -> {
                    if (newValue == value) {
                        supplyValue(newValue);
                    }
                    return null;
                });
            }
        }.export();
    }
}
