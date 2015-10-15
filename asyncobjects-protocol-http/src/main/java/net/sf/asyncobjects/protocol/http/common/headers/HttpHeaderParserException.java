package net.sf.asyncobjects.protocol.http.common.headers;

import net.sf.asyncobjects.protocol.http.HttpException;

/**
 * The exception related to header parser.
 */
public class HttpHeaderParserException extends HttpException {
    /**
     * The constructor.
     *
     * @param message the message
     */
    public HttpHeaderParserException(final String message) {
        super(message);
    }
}
