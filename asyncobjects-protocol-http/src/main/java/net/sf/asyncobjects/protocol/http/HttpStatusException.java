package net.sf.asyncobjects.protocol.http;

import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;

/**
 * The exception with associated HTTP status.
 */
public class HttpStatusException extends HttpException {
    /**
     * The status code.
     */
    private final int statusCode;
    /**
     * The status message.
     */
    private final String statusMessage;

    /**
     * The constructor.
     *
     * @param statusCode    the status code
     * @param statusMessage the status message
     * @param cause         the cause exception
     */
    public HttpStatusException(final int statusCode, final String statusMessage, final Throwable cause) {
        super(statusCode + " " + HttpStatusUtil.getText(statusCode, statusMessage), cause);
        this.statusCode = statusCode;
        this.statusMessage = HttpStatusUtil.getText(statusCode, statusMessage);
    }

    /**
     * The constructor.
     *
     * @param statusCode    the status code
     * @param statusMessage the status message
     */
    public HttpStatusException(final int statusCode, final String statusMessage) {
        this(statusCode, statusMessage, null);
    }

    /**
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the HTTP status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }
}
