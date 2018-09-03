package org.asyncflows.protocol.http.common.content;

import org.asyncflows.protocol.http.HttpException;

/**
 * The exception indicating unknown transfer encoding.
 */
public class UnknownTransferEncodingException extends HttpException {
    /**
     * The constructor.
     *
     * @param message the message
     */
    public UnknownTransferEncodingException(final String message) {
        super(message);
    }
}
