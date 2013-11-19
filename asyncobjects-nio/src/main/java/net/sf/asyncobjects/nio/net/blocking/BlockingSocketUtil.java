package net.sf.asyncobjects.nio.net.blocking;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.net.ASocketFactory;

import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.AsyncControl.doAsyncThrowable;

/**
 * Utilities for the blocking sockets.
 */
public final class BlockingSocketUtil {
    /**
     * The private constructor for utility class.
     */
    private BlockingSocketUtil() {
        // do nothing
    }

    /**
     * Run action with socket factory on the new vat.
     *
     * @param action the action
     * @param <T>    the result type
     * @return the action result
     */
    public static <T> T run(final AFunction<T, ASocketFactory> action) {
        return doAsync(new ACallable<T>() {
            @Override
            public Promise<T> call() throws Throwable {
                final ASocketFactory factory = new BlockingSocketFactory().export();
                return action.apply(factory);
            }
        });
    }

    /**
     * Run action with socket factory on the new vat.
     *
     * @param action the action
     * @param <T>    the result type
     * @return the action result
     * @throws Throwable in case of the problem
     */
    public static <T> T runThrowable(final AFunction<T, ASocketFactory> action) throws Throwable {
        return doAsyncThrowable(new ACallable<T>() {
            @Override
            public Promise<T> call() throws Throwable {
                final ASocketFactory factory = new BlockingSocketFactory().export();
                return action.apply(factory);
            }
        });
    }
}
