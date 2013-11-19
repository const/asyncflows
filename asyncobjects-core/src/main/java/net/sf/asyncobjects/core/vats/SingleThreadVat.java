package net.sf.asyncobjects.core.vats;

import java.util.concurrent.Semaphore;

/**
 * The single thread vat.
 */
public final class SingleThreadVat extends SingleThreadVatWithIdle {
    /**
     * The semaphore.
     */
    private final Semaphore semaphore = new Semaphore(1);

    /**
     * The constructor with the specified batch size.
     *
     * @param stopKey the key used to stop the vat
     */
    public SingleThreadVat(final Object stopKey) {
        super(Integer.MAX_VALUE, stopKey);
    }

    @Override
    protected void idle() {
        semaphore.acquireUninterruptibly();
    }

    @Override
    protected void pollIdle() {
        // do nothing
    }

    @Override
    protected void closeVat() {
        wakeUp();
    }

    @Override
    protected void wakeUp() {
        semaphore.release();
    }
}
