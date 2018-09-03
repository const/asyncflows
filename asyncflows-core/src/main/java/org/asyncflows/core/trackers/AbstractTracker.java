package org.asyncflows.core.trackers;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.util.ASubscription;
import org.asyncflows.core.streams.ASink;
import org.asyncflows.core.streams.AStream;
import org.asyncflows.core.streams.RandevuQueue;
import org.asyncflows.core.util.RequestQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.asyncflows.core.AsyncControl.aNull;
import static org.asyncflows.core.AsyncControl.aValue;
import static org.asyncflows.core.AsyncControl.aVoid;
import static org.asyncflows.core.util.ASubscription.exportSubscription;

/**
 * Base class for trackers.
 *
 * @param <T> the tracker
 */
public abstract class AbstractTracker<T> implements ATracker<T> {
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

    protected abstract void start();

    protected abstract void stop();

    protected final void push(Outcome<T> value) {
        notifications.run(() -> {
            if(Objects.equals(state, value)) {
                return aVoid();
            }
            state = value;
            if(state != null) {
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
        } catch(Throwable t) {
            LOG.error("Failed lister", t);
        }
    }

    @Override
    public Promise<ASubscription> subscribe(Consumer<Outcome<T>> listener) {
        return notifications.run(() -> {
            subscriptions.add(listener);
            if(state != null) {
                notifyListener(listener);
            }
            if(subscriptions.isEmpty()) {
                start();
            }
            return aValue(exportSubscription(() -> {
                subscriptions.remove(listener);
                if(subscriptions.isEmpty()) {
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
