package net.sf.asyncobjects.protocol.http.common;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.protocol.http.HttpException;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;

/**
 * The class that provide some utilities related to control flow.
 */
public final class HttpRuntimeUtil {
    /**
     * The constant action in case when text is null.
     */
    private static final AFunction<Object, Throwable> TO_HTTP_EXCEPTION = failure -> aFailure(wrap(failure, null));

    /**
     * The private constructor for the utility class.
     */
    private HttpRuntimeUtil() {
    }

    /**
     * The function that translates any exception to {@link net.sf.asyncobjects.protocol.http.HttpException}.
     *
     * @param text the text
     * @param <T>  the return type
     * @return the action
     */
    @SuppressWarnings("unchecked")
    public static <T> AFunction<T, Throwable> toHttpException(final String text) {
        if (text == null) {
            //noinspection RedundantCast
            return (AFunction<T, Throwable>) (Object) TO_HTTP_EXCEPTION;
        } else {
            return failure -> aFailure(wrap(failure, text));
        }
    }

    /**
     * The function that translates any exception to {@link net.sf.asyncobjects.protocol.http.HttpException}.
     *
     * @param <T> the return type
     * @return the action
     */
    public static <T> AFunction<T, Throwable> toHttpException() {
        return toHttpException(null);
    }


    /**
     * Wrap exception to HTTP exception if needed.
     *
     * @param failure the exception to wrap
     * @param message the message text
     * @return the exception
     */
    public static HttpException wrap(final Throwable failure, final String message) {
        if (failure instanceof HttpException) {
            return (HttpException) failure;
        } else {
            return new HttpException(message != null ? message : failure.getMessage(), failure);
        }
    }
}
