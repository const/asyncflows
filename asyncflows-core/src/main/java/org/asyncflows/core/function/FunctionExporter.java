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

package org.asyncflows.core.function;

import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;

import java.util.function.Consumer;

import static org.asyncflows.core.CoreFlows.aSend;

/**
 * Utilities to export functions.
 */
public final class FunctionExporter {

    /**
     * Private constructor for utility class.
     */
    private FunctionExporter() {
        // do nothing
    }

    /**
     * Export consumer on the current vat.
     *
     * @param listener the listener to export
     * @param <T>      the event type
     * @return the exported listener
     */
    public static <T> Consumer<T> exportConsumer(final Consumer<T> listener) {
        return exportConsumer(Vats.defaultVat(), listener);
    }

    /**
     * Export resolver to the default vat.
     *
     * @param resolver the resolver.
     * @param <T>      the value type
     * @return the wrapper resolver
     */
    public static <T> AResolver<T> exportResolver(final AResolver<T> resolver) {
        return exportResolver(Vats.defaultVat(), resolver);
    }


    /**
     * Export resolver.
     *
     * @param vat      the vat
     * @param resolver the resolver.
     * @param <T>      the value type
     * @return the wrapper resolver
     */
    public static <T> AResolver<T> exportResolver(final Vat vat, final AResolver<T> resolver) {
        return AResolverProxyFactory.createProxy(vat, resolver);
    }

    /**
     * Export supplier to the default vat.
     *
     * @param supplier the supplier
     * @param <T>      the value type
     * @return the exported function
     */
    public static <T> ASupplier<T> exportSupplier(final ASupplier<T> supplier) {
        return exportSupplier(Vats.defaultVat(), supplier);
    }


    /**
     * Export supplier.
     *
     * @param <T>      the result type
     * @param vat      the vat
     * @param supplier the supplier.
     * @return the exported supplier
     */
    public static <T> ASupplier<T> exportSupplier(final Vat vat, final ASupplier<T> supplier) {
        return ASupplierProxyFactory.createProxy(vat, supplier);
    }

    /**
     * Export consumer on the current vat.
     *
     * @param vat      the vat
     * @param listener the listener to export
     * @param <T>      the event type
     * @return the exported consumer
     */
    public static <T> Consumer<T> exportConsumer(final Vat vat, final Consumer<T> listener) {
        return event -> aSend(vat, () -> listener.accept(event));
    }
}
