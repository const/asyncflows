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
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.util.ASubscription;
import org.asyncflows.core.util.RequestQueue;

import java.util.Objects;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.function.AsyncFunctionUtil.evaluate;
import static org.asyncflows.core.function.FunctionExporter.exportConsumer;

public abstract class FunctionTracker<T> extends AbstractTracker<T> {
    protected final RequestQueue evaluations = new RequestQueue();

    protected abstract void stateChanged();

    public static <A, R> ATracker<R> track(AFunction<A, R> function, ATracker<A> arg) {
        return new FunctionTracker<R>() {
            final ArgListener<A> argument = new ArgListener<A>(arg, this::stateChanged);

            @Override
            protected void start() {
                argument.start();
            }

            @Override
            protected void stop() {
                argument.stop();
            }

            public void stateChanged() {
                if(argument.isValueReady()) {
                    evaluations.run(() -> {
                        if(argument.value.isFailure()) {
                            push(Outcome.failure(argument.value.failure()));
                            return aVoid();
                        } else {
                            return evaluate(function, argument.value.value()).mapOutcome(o -> {
                                push(o);
                                return null;
                            });
                        }
                    });
                }
            }

        };
    }


    private final static class ArgListener<A>  {
        private final ATracker<A> tracker;
        private final Runnable onChange;
        private Outcome<A> value;
        private Promise<ASubscription> subscription;

        private ArgListener(ATracker<A> tracker, Runnable onChange) {
            this.tracker = tracker;
            this.onChange = onChange;
        }


        private void start() {
            if(subscription != null) {
                return;
            }
            subscription = tracker.subscribe(exportConsumer(o -> {
                if(Objects.equals(o, value)) {
                    return;
                }
                value = o;
                onChange.run();
            }));
        }

        private void stop() {
            if(subscription != null) {
                subscription.flatMap(ACloseable::close);
            }
            value = null;
            onChange.run();
        }

        private boolean isValueReady() {
            return value != null;
        }
    }
}
