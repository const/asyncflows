package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
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
        return new ASemaphore() {
            @Override
            public void release(final int permits) {
                aSend(vat, new Runnable() {
                    @Override
                    public void run() {
                        semaphore.release(permits);
                    }
                });
            }

            @Override
            public void release() {
                aSend(vat, new Runnable() {
                    @Override
                    public void run() {
                        semaphore.release();
                    }
                });
            }

            @Override
            public Promise<Void> acquire() {
                return aLater(vat, new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return semaphore.acquire();
                    }
                });
            }

            @Override
            public Promise<Void> acquire(final int permits) {
                return aLater(vat, new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return semaphore.acquire(permits);
                    }
                });
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
                return aLater(vat, new ACallable<T>() {
                    @Override
                    public Promise<T> call() throws Throwable {
                        return queue.take();
                    }
                });
            }

            @Override
            public Promise<Void> put(final T element) {
                return aLater(vat, new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return queue.put(element);
                    }
                });
            }
        };
    }
}
