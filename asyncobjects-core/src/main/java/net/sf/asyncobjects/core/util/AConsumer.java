package net.sf.asyncobjects.core.util;

/**
 * Generic consumer interface. Used to notify about events in one-way manner.
 * This is interface is best used when communication from non-asynchronous code
 * is required. Note, that this class does not create a back-pressure, so it
 * could consume unlimited amount of resources if it is not used carefully.
 * If back-pressure is required, use {@link net.sf.asyncobjects.core.AFunction}
 * that returns promise.
 *
 * @param <T> the event type
 */
@FunctionalInterface
public interface AConsumer<T> {
    /**
     * Notify about event.
     *
     * @param event the event
     */
    void accept(T event);
}
