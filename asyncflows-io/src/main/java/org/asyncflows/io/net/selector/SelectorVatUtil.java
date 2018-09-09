package org.asyncflows.io.net.selector;

import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.core.Outcome;
import org.asyncflows.core.data.Cell;
import org.asyncflows.core.function.AFunction;

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
