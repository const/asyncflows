package net.sf.asyncobjects.nio.net.selector;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.nio.net.ASocketFactory;

import java.lang.reflect.UndeclaredThrowableException;

import static net.sf.asyncobjects.core.AsyncControl.aNow;

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
    public static <T> T runThrowable(final AFunction<T, ASocketFactory> action) throws Throwable {
        final Object stopKey = new Object();
        final SelectorVat vat = new SelectorVat(stopKey);
        final Cell<Outcome<T>> value = new Cell<Outcome<T>>();
        vat.execute(vat, () -> aNow(() -> {
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
    public static <T> T run(final AFunction<T, ASocketFactory> action) {
        try {
            return runThrowable(action);
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
