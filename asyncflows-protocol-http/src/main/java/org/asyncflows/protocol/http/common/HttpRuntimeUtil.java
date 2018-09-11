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

package org.asyncflows.protocol.http.common;

import org.asyncflows.protocol.http.HttpException;
import org.asyncflows.core.function.AFunction;

import static org.asyncflows.core.CoreFlows.aFailure;

/**
 * The class that provide some utilities related to control flow.
 */
public final class HttpRuntimeUtil {
    /**
     * The constant action in case when text is null.
     */
    private static final AFunction<Throwable, Object> TO_HTTP_EXCEPTION = failure -> aFailure(wrap(failure, null));

    /**
     * The private constructor for the utility class.
     */
    private HttpRuntimeUtil() {
    }

    /**
     * The function that translates any exception to {@link HttpException}.
     *
     * @param text the text
     * @param <T>  the return type
     * @return the action
     */
    @SuppressWarnings("unchecked")
    public static <T> AFunction<Throwable, T> toHttpException(final String text) {
        if (text == null) {
            //noinspection RedundantCast
            return (AFunction<Throwable, T>) (Object) TO_HTTP_EXCEPTION;
        } else {
            return failure -> aFailure(wrap(failure, text));
        }
    }

    /**
     * The function that translates any exception to {@link HttpException}.
     *
     * @param <T> the return type
     * @return the action
     */
    public static <T> AFunction<Throwable, T> toHttpException() {
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
