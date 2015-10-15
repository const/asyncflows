package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;

/**
 * The base for the simple streams.
 *
 * @param <A> the element type
 */
public abstract class StreamBase<A> extends CloseableInvalidatingBase implements AStream<A>, ExportsSelf<AStream<A>> {

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
        return result.observe(streamOutcomeObserver);
    }

    /**
     * The producer the next element.
     *
     * @return the next produced element
     * @throws Throwable in case if the next element could not be produced.
     */
    protected abstract Promise<Maybe<A>> produce() throws Throwable;

    @Override
    public AStream<A> export() {
        return export(Vat.current());
    }

    @Override
    public AStream<A> export(final Vat vat) {
        return StreamExportUtil.export(vat, this);
    }
}
