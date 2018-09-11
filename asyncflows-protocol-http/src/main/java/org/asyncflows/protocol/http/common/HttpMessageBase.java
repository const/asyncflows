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

import org.asyncflows.protocol.http.common.headers.HttpHeaders;

/**
 * Base type for the http messages.
 */
public class HttpMessageBase {
    /**
     * The content length.
     */
    protected Long contentLength;
    /**
     * The protocol for the message. It is different from the scheme,
     */
    private String protocol;
    /**
     * The server address for the connection.
     */
    private String serverAddress;
    /**
     * The request method.
     */
    private String version;
    /**
     * The headers.
     */
    private HttpHeaders headers;

    /**
     * @return the HTTP version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set version.
     *
     * @param version the version
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * @return the HTTP headers
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    /**
     * Set headers.
     *
     * @param headers the headers
     */
    public void setHeaders(final HttpHeaders headers) {
        this.headers = headers;
    }

    /**
     * @return get the message protocol.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set message underlying protocol.
     *
     * @param protocol the message protocol.
     */
    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the server address for the connection.
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Set server address for the connection.
     *
     * @param serverAddress the server address
     */
    public void setServerAddress(final String serverAddress) {
        this.serverAddress = serverAddress;
    }
}
