package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.RequestQueue;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * Transform sink base where the next resource is a sink as well.
 *
 * @param <N> the next sink element type
 * @param <T> this sink element type
 */
public abstract class TransformSinkBase<N, T> extends ChainedSinkBase<T, ASink<N>> {
    /**
     * The request queue for this sink.
     */
    private final RequestQueue requests = new RequestQueue();

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     */
    protected TransformSinkBase(final ASink<N> wrapped) {
        super(wrapped);
    }

    @Override
    public Promise<Void> fail(final Throwable error) {
        return requests.run(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return failNext(error);
            }
        });
    }

    /**
     * @return the request queue for the next action (used to serialize next sink notification)
     */
    protected final RequestQueue requestQueue() {
        return requests;
    }

    /**
     * Fail next sink with the specified error.
     *
     * @param error the error to use
     * @return the failure
     */
    protected final Promise<Void> failNext(final Throwable error) {
        invalidate(error);
        return wrapped.fail(error);
    }

    /**
     * @return before close action
     */
    @Override
    protected Promise<Void> beforeClose() {
        return requests.run(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return aVoid();
            }
        });
    }
}
