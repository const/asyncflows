/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

package org.asyncflows.protocol.http.client;

import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;

import java.nio.ByteBuffer;

/**
 * The response object that represents a response returned from the service.
 */
public class HttpResponse {
    /**
     * The status code.
     */
    private final int statusCode;
    /**
     * The status reason string.
     */
    private final String reason;
    /**
     * The HTTP version.
     */
    private final String version;
    /**
     * The HTTP headers.
     */
    private final HttpHeaders headers;
    /**
     * The response stream (might be null if switched protocol).
     */
    private final AInput<ByteBuffer> responseStream;
    /**
     * The channel in case if stream switched protocol, otherwise null.
     */
    private final AChannel<ByteBuffer> switchedChannel;


    /**
     * The constructor.
     *
     * @param statusCode      the status code
     * @param reason          the status reason
     * @param version         the response version
     * @param headers         the response headers
     * @param responseStream  the response stream
     * @param switchedChannel the response channel in the case of
     */
    public HttpResponse(final int statusCode, final String reason, final String version, final HttpHeaders headers,
                        final AInput<ByteBuffer> responseStream, final AChannel<ByteBuffer> switchedChannel) {
        this.statusCode = statusCode;
        this.reason = reason;
        this.version = version;
        this.headers = headers;
        this.responseStream = responseStream;
        this.switchedChannel = switchedChannel;
    }

    /**
     * @return http status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return http status reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * @return http status version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return http version
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    /**
     * @return http response stream (if protocol switched, null)
     */
    public AInput<ByteBuffer> getResponseStream() {
        return responseStream;
    }

    /**
     * @return the switched channel
     */
    public AChannel<ByteBuffer> getSwitchedChannel() {
        return switchedChannel;
    }
}
