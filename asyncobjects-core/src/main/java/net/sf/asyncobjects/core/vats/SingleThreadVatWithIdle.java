package net.sf.asyncobjects.core.vats;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The single thread vat with an idle action.
 */
public abstract class SingleThreadVatWithIdle extends BatchedVat {
    /**
     * The stop object.
     */
    protected final Object myStopKey;
    /**
     * If true, the vat is stopped.
     */
    private final AtomicBoolean stopped = new AtomicBoolean();

    /**
     * The constructor.
     *
     * @param maxBatchSize max batch size
     * @param stopKey      the stop key
     */
    public SingleThreadVatWithIdle(final int maxBatchSize, final Object stopKey) {
        super(maxBatchSize);
        myStopKey = stopKey;
    }

    /**
     * Start the vat in the current thread.
     */
    public void runInCurrentThread() {
        boolean hasMore = true;
        while (!stopped.get()) {
            if (hasMore) {
                pollIdle();
            } else {
                idle();
            }
            hasMore = runBatch();
        }
    }

    /**
     * The idle action.
     */
    protected abstract void idle();

    /**
     * The wake up action.
     */
    protected abstract void wakeUp();

    /**
     * Close the vat, wake up if needed.
     */
    protected abstract void closeVat();

    /**
     * Poll idle.
     */
    protected abstract void pollIdle();

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
            closeVat();
        }
    }

    @Override
    protected void schedule() {
        if (stopped.get()) {
            throw new IllegalStateException("The vat is already stopped");
        }
        wakeUp();
    }
}
