/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

package org.asyncflows.io.adapters.nio;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;

import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.function.BiConsumer;

import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aResolver;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The asynchronous file utility.
 */
public class AsyncNioFlows {
    /**
     * The single universal completion handler.
     */
    private static final CompletionHandler<Object, AResolver<Object>> HANDLER = new CompletionHandler<Object, AResolver<Object>>() {
        @Override
        public void completed(Object result, AResolver<Object> attachment) {
            attachment.resolve(Outcome.success(result));
        }

        @Override
        public void failed(Throwable exc, AResolver<Object> attachment) {
            attachment.resolve(Outcome.failure(exc));
        }
    };

    /**
     * The private constructor for utility class.
     */
    private AsyncNioFlows() {
        // do nothing
    }

    /**
     * Execute action with completion handler.
     *
     * @param consumer the consumer
     * @param <T>      the result value type
     * @return promise for the result
     */
    @SuppressWarnings("unchecked")
    public static <T> Promise<T> aCompletionHandler(BiConsumer<AResolver<T>, CompletionHandler<T, AResolver<T>>> consumer) {
        return aResolver(r -> consumer.accept(r, (CompletionHandler<T, AResolver<T>>) (Object) HANDLER));
    }

    /**
     * Close the channel.
     *
     * @param channel the channel
     * @return promise for operation
     */
    public static Promise<Void> closeChannel(final Channel channel) {
        return aNow(() -> {
            channel.close();
            return aVoid();
        });
    }
}
