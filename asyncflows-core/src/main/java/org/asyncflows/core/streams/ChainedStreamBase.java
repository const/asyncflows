package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.ChainedClosable;

import static org.asyncflows.core.CoreFlows.aFailure;

/**
 * Build stream that works above some resource.
 *
 * @param <O> the stream element type
 * @param <I> the type of underlying resource
 */
public abstract class ChainedStreamBase<O, I extends ACloseable>
        extends ChainedClosable<I> implements AStream<O>, NeedsExport<AStream<O>> {
    /**
     * The observer for the stream outcome.
     */
    private final AResolver<Maybe<O>> streamOutcomeObserver = resolution -> {
        if (!resolution.isSuccess()) {
            invalidate(resolution.failure());
            startClosing();
        } else if (resolution.value() != null && resolution.value().isEmpty()) {
            startClosing();
        }
    };

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     */
    protected ChainedStreamBase(final I wrapped) {
        super(wrapped);
    }

    @Override
    public Promise<Maybe<O>> next() {
        if (!isValidAndOpen()) {
            return invalidationPromise();
        }
        Promise<Maybe<O>> result;
        try {
            result = produce();
        } catch (Throwable t) {
            result = aFailure(t);
        }
        return result.listen(streamOutcomeObserver);
    }

    /**
     * @return the next produced element
     */
    protected abstract Promise<Maybe<O>> produce();

    @Override
    public AStream<O> export(final Vat vat) {
        return StreamExportUtil.export(vat, this);
    }
}
