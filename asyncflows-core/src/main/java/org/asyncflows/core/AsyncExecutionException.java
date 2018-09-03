package org.asyncflows.core;

public class AsyncExecutionException extends RuntimeException {

    public AsyncExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AsyncExecutionException(Throwable cause) {
        super(cause);
    }

    protected AsyncExecutionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
