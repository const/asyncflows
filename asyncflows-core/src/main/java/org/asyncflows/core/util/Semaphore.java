package org.asyncflows.core.util;

import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;

/**
 * Asynchronous semaphore class.
 */
public final class Semaphore implements ASemaphore, NeedsExport<ASemaphore> {
    /**
     * The acquire requests.
     */
    private final RequestQueue requests = new RequestQueue();
    /**
     * The permits.
     */
    private int permits;

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
        return requests.runSeqWhile(() -> {
            if (requestedPermits <= permits) {
                permits -= requestedPermits;
                return aFalse();
            } else {
                return requests.suspendThenTrue();
            }
        });
    }

    @Override
    public ASemaphore export(final Vat vat) {
        return UtilExporter.export(vat, this);
    }
}
