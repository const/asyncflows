package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

/**
 * Asynchronous semaphore class.
 */
public final class Semaphore implements ASemaphore, ExportsSelf<ASemaphore> {
    /**
     * The permits.
     */
    private int permits;
    /**
     * The acquire requests.
     */
    private final RequestQueue requests = new RequestQueue();

    /**
     * The constructor.
     *
     * @param permits amount of permits
     */
    public Semaphore(final int permits) {
        this.permits = permits;
    }

    @Override
    public void release(final int releasedPermits) {
        if (releasedPermits <= 0) {
            return;
        }
        permits += releasedPermits;
        requests.resume();
    }

    @Override
    public void release() {
        release(1);
    }

    @Override
    public Promise<Void> acquire() {
        return acquire(1);
    }

    @Override
    public Promise<Void> acquire(final int requestedPermits) {
        if (requestedPermits <= 0) {
            return aFailure(new IllegalArgumentException("The requestedPermits must be positive: " + requestedPermits));
        }
        return requests.run(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return aSeqLoop(new ACallable<Boolean>() {
                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        if (requestedPermits <= permits) {
                            permits -= requestedPermits;
                            return aFalse();
                        } else {
                            return requests.suspendThenTrue();
                        }
                    }
                });
            }
        });
    }

    @Override
    public ASemaphore export(final Vat vat) {
        return UtilExportUtil.export(vat, this);
    }

    @Override
    public ASemaphore export() {
        return export(Vat.current());
    }
}
