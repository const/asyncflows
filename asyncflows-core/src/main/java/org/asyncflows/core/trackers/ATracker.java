package org.asyncflows.core.trackers;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.util.ASubscription;
import org.asyncflows.core.streams.ASink;
import org.asyncflows.core.streams.AStream;

import java.util.function.Consumer;

/**
 * The tracker for some values.
 *
 * @param <T>
 */
public interface ATracker<T> {
    /**
     * Subscribe to tracker. The subscription ends when {@link ASink#finished()} method finishes.
     * On subscription registration a value is pushed when it is available. If there is no listeners
     * for watchers, the values are not tracked normally.
     *
     * @param sink the sink based
     */
    Promise<ASubscription> subscribe(Consumer<Outcome<T>> sink);

    /**
     * Open watcher as stream. This method just uses pipe to track values.
     *
     * @return the stream.
     */
    Promise<AStream<Outcome<T>>> open();
}

