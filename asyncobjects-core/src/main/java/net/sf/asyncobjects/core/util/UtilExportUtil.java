package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aSend;

/**
 * Exports for utility classes.
 */
public final class UtilExportUtil {
    /**
     * A private constructor for utility class.
     */
    private UtilExportUtil() {
    }

    /**
     * Export for semaphore.
     *
     * @param vat       the vat
     * @param semaphore the semaphore
     * @return the exported class
     */
    public static ASemaphore export(final Vat vat, final ASemaphore semaphore) {
        // TODO refactor all exports use actually inner classes with inheritance
        return new ASemaphore() {
            @Override
            public void release(final int permits) {
                aSend(vat, () -> semaphore.release(permits));
            }

            @Override
            public void release() {
                aSend(vat, semaphore::release);
            }

            @Override
            public Promise<Void> acquire() {
                return aLater(vat, semaphore::acquire);
            }

            @Override
            public Promise<Void> acquire(final int permits) {
                return aLater(vat, () -> semaphore.acquire(permits));
            }
        };
    }

    /**
     * The export for queue.
     *
     * @param vat   the vat
     * @param queue the queue
     * @param <T>   the element type
     * @return the exported queue
     */
    public static <T> AQueue<T> export(final Vat vat, final AQueue<T> queue) {
        return new AQueue<T>() {
            @Override
            public Promise<T> take() {
                return aLater(vat, queue::take);
            }

            @Override
            public Promise<Void> put(final T element) {
                return aLater(vat, () -> queue.put(element));
            }
        };
    }

    /**
     * Export consumer on the current vat.
     *
     * @param listener the listener to export
     * @param <T>      the event type
     * @return the exported listener
     */
    public static <T> AConsumer<T> export(final AConsumer<T> listener) {
        return export(Vat.current(), listener);
    }

    /**
     * Export consumer on the current vat.
     *
     * @param vat      the vat
     * @param listener the listener to export
     * @param <T>      the event type
     * @return the exported consumer
     */
    public static <T> AConsumer<T> export(final Vat vat, final AConsumer<T> listener) {
        return event -> aSend(vat, () -> listener.accept(event));
    }
}
