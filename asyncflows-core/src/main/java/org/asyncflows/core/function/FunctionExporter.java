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

package org.asyncflows.core.function;

import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;

import java.util.function.Consumer;

import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aSend;

public class FunctionExporter {
    /**
     * Export consumer on the current vat.
     *
     * @param listener the listener to export
     * @param <T>      the event type
     * @return the exported listener
     */
    public static <T> Consumer<T> exportConsumer(final Consumer<T> listener) {
        return exportConsumer(listener, Vats.defaultVat());
    }

    public static <T> AResolver<T> exportResolver(final AResolver<T> resolver) {
        return exportResolver(resolver, Vats.defaultVat());
    }


    public static <T> AResolver<T> exportResolver(final AResolver<T> resolver, Vat vat) {
        return o -> vat.execute(() -> resolver.resolve(o));
    }

    public static <T> ASupplier<T> exportSupplier(final ASupplier<T> supplier) {
        return exportSupplier(supplier, Vats.defaultVat());
    }


    public static <T> ASupplier<T> exportSupplier(final ASupplier<T> supplier, Vat vat) {
        return () -> aLater(vat, supplier);
    }
    /**
     * Export consumer on the current vat.
     *
     * @param vat      the vat
     * @param listener the listener to export
     * @param <T>      the event type
     * @return the exported consumer
     */
    public static <T> Consumer<T> exportConsumer(final Consumer<T> listener, final Vat vat) {
        return event -> aSend(vat, () -> listener.accept(event));
    }
}
