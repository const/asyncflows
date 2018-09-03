package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.util.ChainedClosable;
import org.asyncflows.core.util.NeedsExport;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;

/**
 * The chained sink.
 *
 * @param <I> the sink element type
 * @param <U> the underlying resource type
 */
public abstract class ChainedSinkBase<I, U extends ACloseable> extends ChainedClosable<U>
        implements ASink<I>, NeedsExport<ASink<I>> {
    /**
     * The finished promise.
     */
    private final Promise<Void> finished = new Promise<>();

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     */
    protected ChainedSinkBase(final U wrapped) {
        super(wrapped);
    }

    @Override
    public Promise<Void> fail(final Throwable error) {
        invalidate(error);
        return aVoid();
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        if (finished.getOutcome() == null) {
            notifyFailure(finished.resolver(), throwable);
            startClosing();
        }
    }

    @Override
    public Promise<Void> finished() {
        return finished;
    }

    @Override
    public ASink<I> export(final Vat vat) {
        return StreamExportUtil.export(vat, this);
    }
}
