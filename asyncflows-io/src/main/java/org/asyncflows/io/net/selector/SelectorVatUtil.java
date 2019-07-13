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

package org.asyncflows.io.net.selector;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.data.Cell;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.io.net.ASocketFactory;

import java.lang.reflect.UndeclaredThrowableException;

import static org.asyncflows.core.CoreFlows.aNow;

/**
 * Utilities for the selector vat.
 */
public final class SelectorVatUtil {
    /**
     * The socket factory.
     */
    private static final ThreadLocal<ASocketFactory> SOCKET_FACTORY = new ThreadLocal<ASocketFactory>();

    /**
     * The private constructor for the utility class.
     */
    private SelectorVatUtil() {
        // do nothing
    }

    /**
     * Start selector vat in the current thread and run action in its context with the specified socket factory.
     *
     * @param action the action to run
     * @param <T>    the result type
     * @return the action result
     * @throws Throwable if there is a problem with running action
     */
    public static <T> T doAsyncIoThrowable(final AFunction<ASocketFactory, T> action) throws Throwable {
        final Object stopKey = new Object();
        final SelectorVat vat = new SelectorVat(stopKey);
        final Cell<Outcome<T>> value = new Cell<Outcome<T>>();
        vat.execute(() -> aNow(() -> {
            final ASocketFactory selectorSocketFactory = new SelectorSocketFactory(vat).export(vat);
            SOCKET_FACTORY.set(selectorSocketFactory);
            return action.apply(selectorSocketFactory);
        }).listen(resolution -> {
            value.setValue(resolution);
            vat.stop(stopKey);
        }));
        try {
            vat.runInCurrentThread();
        } finally {
            SOCKET_FACTORY.remove();
        }
        return value.getValue().force();
    }

    /**
     * Start selector vat in the current thread and run action in its context with the specified socket factory.
     *
     * @param action the action to run
     * @param <T>    the result type
     * @return the action result
     */
    public static <T> T doAsyncIo(final AFunction<ASocketFactory, T> action) {
        try {
            return doAsyncIoThrowable(action);
        } catch (Error | RuntimeException t) { // NOPMD
            throw t;
        } catch (Throwable t) {
            throw new UndeclaredThrowableException(t, "A checked exception is received");
        }
    }

    /**
     * @return get socket factory
     */
    public static ASocketFactory getSocketFactory() {
        return SOCKET_FACTORY.get();
    }
}
