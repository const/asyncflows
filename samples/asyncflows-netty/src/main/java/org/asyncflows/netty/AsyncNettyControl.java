/*
 * Copyright (c) 2018 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
        DefaultPromise<T> rc = new DefaultPromise<>(vat.getEventLoop());
        promise.listen(vat, o -> {
            if (o.isSuccess()) {
                rc.setSuccess(o.value());
            } else {
                rc.setFailure(o.failure());
            }
        });
        return rc;
    }
}
