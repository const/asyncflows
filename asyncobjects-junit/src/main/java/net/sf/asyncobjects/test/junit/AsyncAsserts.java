package net.sf.asyncobjects.test.junit;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;

import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static org.junit.Assert.assertEquals;

/**
 * The test utilities.
 */
public final class AsyncAsserts {
    /**
     * The private constructor for utility class.
     */
    private AsyncAsserts() {
    }

    /**
     * Check for equality.
     *
     * @param expected the expected value
     * @param body     the body that computes the value
     * @param <R>      the value
     * @return the promise for the value.
     */
    public static <R> Promise<R> assertEqualsAsync(final R expected, final ACallable<R> body) {
        return aNow(body).map(new AFunction<R, R>() {
            @Override
            public Promise<R> apply(final R value) throws Throwable {
                assertEquals(expected, value);
                return aSuccess(value);
            }
        });
    }
}
