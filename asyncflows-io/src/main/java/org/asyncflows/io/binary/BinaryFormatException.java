package org.asyncflows.io.binary;

public class BinaryFormatException extends RuntimeException {

    public BinaryFormatException() {
        super();
    }

    public BinaryFormatException(String message) {
        super(message);
    }

    public BinaryFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public BinaryFormatException(Throwable cause) {
        super(cause);
    }

    protected BinaryFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
