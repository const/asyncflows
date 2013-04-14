package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;

/**
 * A Closeable instance above other closeable instance.
 *
 * @param <T> the underlying object type
 */
public abstract class ChainedClosable<T extends ACloseable> extends CloseableInvalidatingBase {
    /**
     * The wrapped closeable object.
     */
    protected final T wrapped;

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     */
    protected ChainedClosable(final T wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * The close action. Override it to implement a custom close operation.
     *
     * @return promise that resolves when close is complete
     */
    @Override
    protected Promise<Void> closeAction() {
        return aSeq(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return beforeClose();
            }
        }).finallyDo(ResourceUtil.closeResourceAction(wrapped));
    }

    /**
     * @return before close action
     */
    protected Promise<Void> beforeClose() {
        return aVoid();
    }
}
