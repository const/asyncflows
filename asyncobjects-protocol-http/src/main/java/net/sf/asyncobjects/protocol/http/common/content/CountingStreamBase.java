package net.sf.asyncobjects.protocol.http.common.content;

import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.util.AConsumer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The base for the counting stream.
 */
public class CountingStreamBase {
    /**
     * The time when observing started.
     */
    private final long started = System.currentTimeMillis();
    /**
     * The listener for the event.
     */
    private final AConsumer<StreamFinishedEvent> listener;
    /**
     * True if event fired.
     */
    private final AtomicBoolean firedEvent = new AtomicBoolean(false);
    /**
     * Amount of bytes transferred.
     */
    private final AtomicLong transferred = new AtomicLong(0L);

    /**
     * The constructor.
     *
     * @param listener the listener (must be exported, or be otherwise safe for use from other threads)
     */
    public CountingStreamBase(final AConsumer<StreamFinishedEvent> listener) {
        this.listener = listener;
    }

    /**
     * Count transferred bytes.
     *
     * @param count the transferred amount.
     */
    protected final void transferred(final int count) {
        transferred.addAndGet(count);
    }

    /**
     * Fire event.
     *
     * @param failure the failure (null if success)
     */
    protected final void fireFinished(final Throwable failure) {
        if (firedEvent.compareAndSet(false, true)) {
            listener.accept(new StreamFinishedEvent(started, System.currentTimeMillis(), transferred.get(), failure));
        }
    }

    /**
     * Fire event.
     */
    protected final void fireFinished() {
        fireFinished((Throwable) null);
    }

    /**
     * Fire event.
     *
     * @param resolution the failure source
     */
    protected final void fireFinished(final Outcome<?> resolution) {
        //noinspection ThrowableResultOfMethodCallIgnored
        fireFinished(resolution.isSuccess() ? null : resolution.failure());
    }

}
