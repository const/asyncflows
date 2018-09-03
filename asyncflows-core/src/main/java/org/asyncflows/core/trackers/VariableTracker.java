package org.asyncflows.core.trackers;

import org.asyncflows.core.Outcome;

public final class VariableTracker<T> extends AbstractTracker<T> {
    public void set(T value) {
        push(Outcome.success(value));
    }

    public void clear() {
        push(null);

    }

    public void fail(Throwable failure) {
        push(Outcome.failure(failure));
    }

    @Override
    protected void start() {
        // ignore it
    }

    @Override
    protected void stop() {
        // ignore it
    }
}
