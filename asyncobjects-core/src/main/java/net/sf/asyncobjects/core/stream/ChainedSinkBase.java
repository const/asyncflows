package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.core.util.ChainedClosable;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;

/**
 * The chained sink.
 *
 * @param <I> the sink element type
 * @param <U> the underlying resource type
 */
public abstract class ChainedSinkBase<I, U extends ACloseable> extends ChainedClosable<U>
        implements ASink<I>, ExportsSelf<ASink<I>> {
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
        if (finished.getState() == Promise.State.INITIAL) {
            notifyFailure(finished.resolver(), throwable);
            startClosing();
        }
    }

    @Override
    public Promise<Void> finished() {
        return finished;
    }

    @Override
    public ASink<I> export() {
        return export(Vat.current());
    }

    @Override
    public ASink<I> export(final Vat vat) {
        return StreamExportUtil.export(vat, this);
    }
}
