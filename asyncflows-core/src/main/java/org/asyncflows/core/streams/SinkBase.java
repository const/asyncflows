package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableInvalidatingBase;

import static org.asyncflows.core.AsyncControl.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The base class for sink.
 *
 * @param <A> the element type
 */
public abstract class SinkBase<A> extends CloseableInvalidatingBase implements ASink<A>, NeedsExport<ASink<A>> {
    /**
     * The finished promise.
     */
    private final Promise<Void> finished = new Promise<>();

    @Override
    public Promise<Void> fail(final Throwable error) {
        invalidate(error);
        return aVoid();
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        if (finished.isUnresolved()) {
            notifyFailure(finished.resolver(), throwable);
            startClosing();
        }
    }

    @Override
    protected Promise<Void> closeAction() {
        if (finished.isUnresolved()) {
            notifySuccess(finished.resolver(), null);
            startClosing();
        }
        return super.closeAction();
    }

    @Override
    public Promise<Void> finished() {
        return finished;
    }

    @Override
    public ASink<A> export(final Vat vat) {
        return StreamExportUtil.export(vat, this);
    }
}
