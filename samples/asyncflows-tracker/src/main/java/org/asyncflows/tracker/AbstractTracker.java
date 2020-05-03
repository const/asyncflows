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

package org.asyncflows.tracker;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.util.ASubscription;
import org.asyncflows.core.util.ASubscriptionProxyFactory;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.vats.Vat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;

public class AbstractTracker<T> implements ATracker<T>, ExportableComponent<ATracker<T>> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTracker.class);
    private final List<Consumer<Outcome<T>>> subscriptions = new LinkedList<>();
    private Outcome<T> value;

    /**
     * Supply value.
     *
     * @param newValue the value
     */
    protected final void supplyValue(final Outcome<T> newValue) {
        if (Objects.equals(value, newValue)) {
            return;
        }
        value = newValue;
        if (newValue != null) {
            subscriptions.forEach(this::notifyListener);
        }
    }

    protected void subscriptionStarted() {
        // override in subclass if needed
    }


    protected void subscriptionEnded() {
        // override in subclass if needed
    }

    @Override
    public Promise<Maybe<Outcome<T>>> value() {
        if (value == null) {
            return aMaybeEmpty();
        }
        return aMaybeValue(value);
    }

    @Override
    public Promise<ASubscription> listen(Consumer<Outcome<T>> listener) {
        subscriptions.add(listener);
        if (value != null) {
            notifyListener(listener);
        }
        if (subscriptions.size() == 1) {
            subscriptionStarted();
        }
        return aValue(ASubscriptionProxyFactory.createProxy(Vat.current(), () -> {
            subscriptions.remove(listener);
            if (subscriptions.isEmpty()) {
                value = null;
                subscriptionEnded();
            }
            return aVoid();
        }));
    }

    private void notifyListener(Consumer<Outcome<T>> listener) {
        try {
            listener.accept(value);
        } catch (Throwable t) {
            LOG.error("Listener failed", t);
        }
    }

    @Override
    public ATracker<T> export(Vat vat) {
        return ATrackerProxyFactory.createProxy(vat, this);
    }
}
