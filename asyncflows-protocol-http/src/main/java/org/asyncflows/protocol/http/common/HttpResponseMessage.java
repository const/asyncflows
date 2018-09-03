package org.asyncflows.protocol.http.common;

/**
 * The response message for the HTTP.
 */
public class HttpResponseMessage extends HttpMessageBase {
    /**
     * The status code.
     */
    private Integer statusCode;
    /**
     * The status message.
     */
    private String statusMessage;

    /**
     * @return the status code
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * Set status code.
     *
     * @param statusCode the code
     */
    public void setStatusCode(final Integer statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return the status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Set status message.
     *
     * @param statusMessage the status message
     */
    public void setStatusMessage(final String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
