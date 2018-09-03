package org.asyncflows.core.function;

import org.asyncflows.core.vats.Vats;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.asyncflows.core.AsyncControl.aLater;
import static org.asyncflows.core.AsyncControl.aSend;

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


    public static <T> AResolver<T> exportResolver(final AResolver<T> resolver, Executor vat) {
        return o -> vat.execute(() -> resolver.resolve(o));
    }

    public static <T> ASupplier<T> exportSupplier(final ASupplier<T> supplier) {
        return exportSupplier(supplier, Vats.defaultVat());
    }


    public static <T> ASupplier<T> exportSupplier(final ASupplier<T> supplier, Executor vat) {
        return () -> aLater(supplier, vat);
    }
    /**
     * Export consumer on the current vat.
     *
     * @param vat      the vat
     * @param listener the listener to export
     * @param <T>      the event type
     * @return the exported consumer
     */
    public static <T> Consumer<T> exportConsumer(final Consumer<T> listener, final Executor vat) {
        return event -> aSend(() -> listener.accept(event), vat);
    }
}
