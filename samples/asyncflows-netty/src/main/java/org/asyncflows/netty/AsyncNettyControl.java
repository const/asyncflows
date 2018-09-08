package org.asyncflows.netty;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;

import static org.asyncflows.core.CoreFlows.aResolver;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * Utilities for asynchronous netty operations.
 */
public final class AsyncNettyControl {
    /**
     * Convert netty promise to asyncflows promise.
     *
     * @param promise the netty promise
     * @param <T>     the type
     * @return asyncflows promise.
     */
    @SuppressWarnings("unchecked")
    public static <T> Promise<T> aNettyFuture(Future<T> promise) {
        return aResolver(r -> {
            promise.addListener(future -> {
                if (future.isSuccess()) {
                    notifySuccess(r, (T) (Object) future.getNow());
                } else {
                    notifyFailure(r, future.cause());
                }
            });
        });
    }

    /**
     * Convert to netty future for use where it is needed.
     *
     * @param promise the asyncflows promise
     * @param <T>     the result type
     * @return the netty future
     */
    public static <T> Future<T> toNettyFuture(Promise<T> promise) {
        NettyVat vat = NettyVat.currentNettyVat();
        Outcome<T> outcome = promise.getOutcome();
        DefaultPromise<T> rc = new DefaultPromise<>(vat.getEventLoop());
        promise.listen(o -> {
            if (o.isSuccess()) {
                rc.setSuccess(o.value());
            } else {
                rc.setFailure(o.failure());
            }
        }, vat);
        return rc;
    }
}
