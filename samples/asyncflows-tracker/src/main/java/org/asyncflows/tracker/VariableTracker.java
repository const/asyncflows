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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class VariableTracker<T> extends AbstractTracker<T> {
    private static final Logger LOG = LoggerFactory.getLogger(VariableTracker.class);
    private final Consumer<SubscriptionStatus> statusListener;
    private Outcome<T> currentValue;
    private SubscriptionStatus subscriptionStatus;

    public VariableTracker(Consumer<SubscriptionStatus> statusListener) {
        this.statusListener = statusListener;
    }

    public VariableTracker() {
        this(null);
    }


    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public Outcome<T> getValue() {
        return currentValue;
    }

    public void setValue(Outcome<T> value) {
        this.currentValue = value;
        supplyValue(value);
    }

    @Override
    protected void subscriptionStarted() {
        supplyValue(currentValue);
        changeStatus(SubscriptionStatus.SUBSCRIPTIONS_EXISTS);
    }

    @Override
    protected void subscriptionEnded() {
        changeStatus(SubscriptionStatus.NO_SUBSCRIPTIONS);
    }

    private void changeStatus(SubscriptionStatus newStatus) {
        subscriptionStatus = newStatus;
        if (statusListener != null) {
            try {
                statusListener.accept(subscriptionStatus);
            } catch (Throwable t) {
                LOG.error("Listener failed", t);
            }
        }
    }
}
