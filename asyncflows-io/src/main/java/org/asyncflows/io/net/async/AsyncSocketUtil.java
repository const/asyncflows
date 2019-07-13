/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

package org.asyncflows.io.net.async;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.io.net.ASocketFactory;

import java.nio.channels.AsynchronousChannelGroup;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.AsyncContext.doAsyncThrowable;

/**
 * The asynchronous utilities.
 */
public final class AsyncSocketUtil {
    /**
     * Private constructor for utility class.
     */
    private AsyncSocketUtil() {
    }

    /**
     * Run action with socket factory on the new vat.
     *
     * @param action the action
     * @param <T>    the result type
     * @return the action result
     */
    public static <T> T run(final AFunction<ASocketFactory, T> action) {
        return doAsync(() -> runAction(action));
    }

    /**
     * Run action with socket factory on the new vat.
     *
     * @param action the action
     * @param <T>    the result type
     * @return the action result
     * @throws Throwable in case of the problem
     */
    public static <T> T runThrowable(final AFunction<ASocketFactory, T> action) throws Throwable {
        return doAsyncThrowable(() -> runAction(action));
    }

    private static <T> Promise<T> runAction(AFunction<ASocketFactory, T> action) throws Throwable {
        final AsyncSocketFactory socketFactory = new AsyncSocketFactory();
        socketFactory.setChannelGroup(AsynchronousChannelGroup.withThreadPool(Vats.DAEMON_EXECUTOR));
        final ASocketFactory factory = socketFactory.export();
        return action.apply(factory);
    }
}
