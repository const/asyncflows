package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * The base class for sink.
 *
 * @param <A> the element type
 */
public abstract class SinkBase<A> extends CloseableInvalidatingBase implements ASink<A>, ExportsSelf<ASink<A>> {
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
        if (finished.getState() == Promise.State.INITIAL) {
            notifyFailure(finished.resolver(), throwable);
            startClosing();
        }
    }

    @Override
    protected Promise<Void> closeAction() {
        if (finished.getState() == Promise.State.INITIAL) {
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
    public ASink<A> export() {
        return export(Vat.current());
    }

    @Override
    public ASink<A> export(final Vat vat) {
        return StreamExportUtil.export(vat, this);
    }
}
