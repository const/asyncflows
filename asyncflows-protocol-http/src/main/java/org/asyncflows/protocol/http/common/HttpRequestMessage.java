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

package org.asyncflows.protocol.http.common;

import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;

import java.net.URI;

/**
 * The HTTP request message. It is used to accumulate information about the HTTP request on client or server side.
 */
public class HttpRequestMessage extends HttpMessageBase {
    /**
     * The request target.
     */
    private String requestTarget;
    /**
     * The request method.
     */
    private String method;
    /**
     * The effective URL of the request.
     */
    private URI effectiveUri;

    /**
     * @return the HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Set HTTP method.
     *
     * @param method the method
     */
    public void setMethod(final String method) {
        this.method = method;
    }

    /**
     * @return the request target (as it is specified in the request)
     */
    public String getRequestTarget() {
        return requestTarget;
    }

    /**
     * Set the request target (as it is specified in the request).
     *
     * @param requestTarget the new value
     */
    public void setRequestTarget(final String requestTarget) {
        this.requestTarget = requestTarget;
    }

    /**
     * @return get the effective URL.
     */
    public URI getEffectiveUri() {
        return effectiveUri;
    }

    /**
     * Set the effective URL.
     *
     * @param effectiveUri the effective URL.
     */
    public void setEffectiveUri(final URI effectiveUri) {
        this.effectiveUri = effectiveUri;
    }

    /**
     * @return the content length (or null if undefined)
     */
    public Long getContentLength() {
        return contentLength;
    }

    /**
     * Set the content length.
     *
     * @param contentLength the content length
     */
    public void setContentLength(final Long contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * @return true, if continuation is expected.
     */
    public boolean expectsContinue() {
        if (HttpVersionUtil.isHttp11(getVersion())) {
            for (final String expect : getHeaders().getHeaders(HttpHeadersUtil.EXPECT_HEADER)) {
                if (expect.equalsIgnoreCase(HttpHeadersUtil.EXPECT_CONTINUE)) {
                    return true;
                }
            }
        }
        return false;
    }


}
