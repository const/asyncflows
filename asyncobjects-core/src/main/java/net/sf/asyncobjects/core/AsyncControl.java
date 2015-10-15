package net.sf.asyncobjects.core;

import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.vats.SingleThreadVat;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.core.vats.Vats;

import java.lang.reflect.UndeclaredThrowableException;

import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * The basic asynchronous control flow utilities.
 */
public final class AsyncControl {
    /**
     * The null promise.
     */
    private static final Promise<?> NULL_PROMISE = aValue(null);
    /**
     * The true promise.
     */
    private static final Promise<Boolean> TRUE_PROMISE = aValue(true);
    /**
     * The false promise.
     */
    private static final Promise<Boolean> FALSE_PROMISE = aValue(false);
    /**
     * Empty value.
     */
    private static final Promise<Maybe<Object>> EMPTY_VALUE = aValue(Maybe.empty());

    /**
     * The private constructor for utility class.
     */
    private AsyncControl() {
        // do nothing
    }

    /**
     * @return a resolved void promise
     */
    public static Promise<Void> aVoid() {
        return aNull();
    }

    /**
     * The promise that resolves to null.
     *
     * @param <T> the promise type
     * @return the result
     */
    @SuppressWarnings("unchecked")
    public static <T> Promise<T> aNull() {
        return (Promise<T>) NULL_PROMISE;
    }

    /**
     * @return a resolved true promise
     */
    public static Promise<Boolean> aTrue() {
        return TRUE_PROMISE;
    }

    /**
     * @return a resolved false promise
     */
    public static Promise<Boolean> aFalse() {
        return FALSE_PROMISE;
    }

    /**
     * Return a promise basing on boolean value.
     *
     * @param value the boolean value
     * @return the corresponding promise
     */
    public static Promise<Boolean> aBoolean(final boolean value) {
        return value ? TRUE_PROMISE : FALSE_PROMISE;
    }

    /**
     * An operation that fails with the specified problem.
     *
     * @param problem the problem
     * @param <T>     the type of promise
     * @return the promise with problem
     */
    public static <T> Promise<T> aFailure(final Throwable problem) {
        return Promise.failure(problem);
    }

    /**
     * An operation that succeed with the specified value.
     *
     * @param value the result value
     * @param <T>   the type of promise
     * @return the promise with value
     */
    public static <T> Promise<T> aValue(final T value) {
        return Promise.success(value);
    }

    /**
     * Execute callable now and returns its promise.
     *
     * @param body the body
     * @param <T>  the body type
     * @return the promise
     */
    public static <T> Promise<T> aNow(final ACallable<T> body) {
        try {
            final Promise<T> promise = body.call();
            if (promise != null) {
                return promise;
            } else {
                return aFailure(new IllegalArgumentException("Body returned null instead of promise: " + body));
            }
        } catch (Throwable throwable) {
            return aFailure(throwable);
        }
    }

    /**
     * Execute operation later on other vat.
     *
     * @param vat  the vat where operation will be executed
     * @param body teh body
     * @param <T>  the result type
     * @return the result promise
     */
    public static <T> Promise<T> aLater(final Vat vat, final ACallable<T> body) {
        final Promise<T> rc = new Promise<>();
        final AResolver<T> resolver = rc.resolver();
        try {
            vat.execute(() -> aNow(body).listen(resolver));
        } catch (Throwable throwable) {
            ResolverUtil.notifyResolver(resolver, new Failure<>(throwable));
        }
        return rc;
    }

    /**
     * Execute body on this vat later.
     *
     * @param body the body
     * @param <T>  the result type
     * @return the result promise
     */
    public static <T> Promise<T> aLater(final ACallable<T> body) {
        final Promise<T> rc = new Promise<>();
        final AResolver<T> resolver = rc.resolver();
        try {
            final Vat vat = Vat.current();
            vat.execute(vat, () -> aNow(body).listen(resolver));
        } catch (Throwable throwable) {
            ResolverUtil.notifyResolver(resolver, new Failure<>(throwable));
        }
        return rc;
    }

    /**
     * Send action to other vat.
     *
     * @param vat    the destination vat
     * @param action the action
     */
    public static void aSend(final Vat vat, final Runnable action) {
        vat.execute(action);
    }

    /**
     * Set action to the current vat.
     *
     * @param action the action to send
     */
    public static void aSend(final Runnable action) {
        final Vat vat = Vat.current();
        vat.execute(vat, action);
    }

    /**
     * Run action on the daemon executor and resolve promise when that action finishes.
     *
     * @param action the action to execute (the action will not have a vat context)
     * @return promise that resolves to the result of the execution
     */
    public static Promise<Void> aDaemonRun(final Runnable action) {
        final Promise<Void> rc = new Promise<>();
        final AResolver<Void> resolver = rc.resolver();
        Vats.DAEMON_EXECUTOR.execute(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                notifyFailure(resolver, t);
                return;
            }
            notifySuccess(resolver, null);
        });
        return rc;
    }

    /**
     * Execute body with throwable.
     *
     * @param body the body to execute
     * @param <T>  the result type
     * @return the result value
     * @throws Throwable the problem if there is any
     */
    public static <T> T doAsyncThrowable(final ACallable<T> body) throws Throwable {
        final Object stopKey = new Object();
        final SingleThreadVat vat = new SingleThreadVat(stopKey);
        final Cell<Outcome<T>> value = new Cell<>();
        vat.execute(vat, () -> aNow(body).listen(resolution -> {
            value.setValue(resolution);
            vat.stop(stopKey);
        }));
        vat.runInCurrentThread();
        return value.getValue().force();
    }

    /**
     * The more simple to use version of {@link #doAsyncThrowable(ACallable)}.
     *
     * @param body the body to use
     * @param <T>  the value type
     * @return the body
     */
    public static <T> T doAsync(final ACallable<T> body) {
        try {
            return doAsyncThrowable(body);
        } catch (Error | RuntimeException t) { // NOPMD
            throw t;
        } catch (Throwable t) {
            throw new UndeclaredThrowableException(t, "A checked exception is received");
        }
    }

    /**
     * Return empty option value.
     *
     * @param <T> the value type
     * @return the resolved promise for empty value
     */
    @SuppressWarnings("unchecked")
    public static <T> Promise<Maybe<T>> aMaybeEmpty() {
        return (Promise<Maybe<T>>) (Promise) EMPTY_VALUE;
    }

    /**
     * The maybe with value.
     *
     * @param value the value
     * @param <T>   the type
     * @return the maybe with value
     */
    public static <T> Promise<Maybe<T>> aMaybeValue(final T value) {
        return aValue(Maybe.value(value));
    }
}
