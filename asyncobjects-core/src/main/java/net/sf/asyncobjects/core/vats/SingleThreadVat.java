package net.sf.asyncobjects.core.vats;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The single thread vat.
 */
public final class SingleThreadVat extends BatchedVat {
    /**
     * The semaphore.
     */
    private final Semaphore semaphore = new Semaphore(1);
    /**
     * The stop object.
     */
    private final Object myStopKey;
    /**
     * If true, the vat is stopped.
     */
    private final AtomicBoolean stopped = new AtomicBoolean();


    /**
     * The constructor with the specified batch size.
     *
     * @param stopKey the key used to stop the vat
     */
    public SingleThreadVat(final Object stopKey) {
        super(Integer.MAX_VALUE);
        myStopKey = stopKey;
    }

    /**
     * Start the vat in the current thread.
     */
    public void runInCurrentThread() {
        while (!stopped.get()) {
            semaphore.acquireUninterruptibly();
            runBatch();
            if (stopped.get()) {
                return;
            }
        }
    }

    /**
     * Stop the vat.
     *
     * @param stopKey the key used to stop the vat
     */
    public void stop(final Object stopKey) {
        if (myStopKey != stopKey) { // NOPMD
            throw new IllegalArgumentException("The stop key is invalid");
        }
        if (stopped.compareAndSet(false, true)) {
            semaphore.release();
        }
    }

    @Override
    protected void schedule() {
        if (stopped.get()) {
            throw new IllegalStateException("The vat is already stopped");
        }
        semaphore.release();
    }
}
