package org.asyncflows.core.util;


import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

import static org.asyncflows.core.AsyncControl.aVoid;
import static org.asyncflows.core.util.AsyncSeqControl.aSeq;

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

    @Override
    protected Promise<Void> closeAction() {
        return aSeq(this::beforeClose).finallyDo(ResourceUtil.closeResourceAction(wrapped));
    }

    /**
     * @return before close action
     */
    protected Promise<Void> beforeClose() {
        return aVoid();
    }
}
