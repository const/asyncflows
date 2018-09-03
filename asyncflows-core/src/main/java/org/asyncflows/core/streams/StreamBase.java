package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.CloseableInvalidatingBase;

import static org.asyncflows.core.CoreFlows.aFailure;

/**
 * The base for the simple streams.
 *
 * @param <A> the element type
 */
public abstract class StreamBase<A> extends CloseableInvalidatingBase implements AStream<A>, NeedsExport<AStream<A>> {

    /**
     * The observer for the stream outcome.
     */
    private final AResolver<Maybe<A>> streamOutcomeObserver = resolution -> {
        if (!resolution.isSuccess()) {
            invalidate(resolution.failure());
            startClosing();
        } else if (resolution.value() != null && resolution.value().isEmpty()) {
            startClosing();
        }
    };

    @Override
    public final Promise<Maybe<A>> next() {
        Promise<Maybe<A>> result;
        try {
            result = produce();
        } catch (Throwable t) {
            result = aFailure(t);
        }
        return result.listen(streamOutcomeObserver);
    }

    /**
     * The producer the next element.
     *
     * @return the next produced element
     * @throws Throwable in case if the next element could not be produced.
     */
    protected abstract Promise<Maybe<A>> produce() throws Throwable;

    @Override
    public AStream<A> export(final Vat vat) {
        return StreamExportUtil.export(vat, this);
    }
}
