/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

package org.asyncflows.protocol.http;

import org.asyncflows.protocol.http.common.HttpStatusUtil;

/**
 * The exception with associated HTTP status.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
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
