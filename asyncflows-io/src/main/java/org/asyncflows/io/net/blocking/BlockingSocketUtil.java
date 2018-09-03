package org.asyncflows.io.net.blocking;

import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.core.function.AFunction;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.AsyncContext.doAsyncThrowable;

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
    public static <T> T run(final AFunction<ASocketFactory, T> action) {
        return doAsync(() -> {
            final ASocketFactory factory = new BlockingSocketFactory().export();
            return action.apply(factory);
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
    public static <T> T runThrowable(final AFunction<ASocketFactory, T> action) throws Throwable {
        return doAsyncThrowable(() -> {
            final ASocketFactory factory = new BlockingSocketFactory().export();
            return action.apply(factory);
        });
    }
}
