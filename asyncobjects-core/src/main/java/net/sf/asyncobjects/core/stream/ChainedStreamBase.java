package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.core.util.ChainedClosable;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;

/**
 * Build stream that works above some resource.
 *
 * @param <O> the stream element type
 * @param <I> the type of underlying resource
 */
public abstract class ChainedStreamBase<O, I extends ACloseable>
        extends ChainedClosable<I> implements AStream<O>, ExportsSelf<AStream<O>> {
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
        return result.observe(streamOutcomeObserver);
    }

    /**
     * @return the next produced element
     */
    protected abstract Promise<Maybe<O>> produce();

    @Override
    public AStream<O> export() {
        return export(Vat.current());
    }

    @Override
    public AStream<O> export(final Vat vat) {
        return StreamExportUtil.export(vat, this);
    }
}
