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

package org.asyncflows.protocol.http.common;

/**
 * The utility for handling HTTP methods.
 */
public final class HttpMethodUtil {
    /**
     * The <a href="http://tools.ietf.org/search/rfc7231#section-4.3.1">GET</a> method.
     */
    public static final String GET = "GET";
    /**
     * The <a href="http://tools.ietf.org/search/rfc7231#section-4.3.2">HEAD</a> method.
     */
    public static final String HEAD = "HEAD";
    /**
     * The <a href="http://tools.ietf.org/search/rfc7231#section-4.3.3">POST</a> method.
     */
    public static final String POST = "POST";
    /**
     * The <a href="http://tools.ietf.org/search/rfc7231#section-4.3.4">PUT</a> method.
     */
    public static final String PUT = "PUT";
    /**
     * The <a href="http://tools.ietf.org/search/rfc7231#section-4.3.5">DELETE</a> method.
     */
    public static final String DELETE = "DELETE";
    /**
     * The <a href="http://tools.ietf.org/search/rfc7231#section-4.3.6">CONNECT</a> method.
     */
    public static final String CONNECT = "CONNECT";
    /**
     * The <a href="http://tools.ietf.org/search/rfc7231#section-4.3.7">OPTIONS</a> method.
     */
    public static final String OPTIONS = "OPTIONS";
    /**
     * The <a href="http://tools.ietf.org/html/draft-ietf-httpbis-http2-14#section-3.5">PRI</a>
     * pseudo-method for switching to HTTP 2.0.
     */
    public static final String PRI = "PRI";
    /**
     * The <a href="http://tools.ietf.org/search/rfc7231#section-4.3.8">TRACE</a> method.
     */
    private static final String TRACE = "TRACE";

    /**
     * The private constructor for the utility class.
     */
    private HttpMethodUtil() {
        // do nothing
    }

    /**
     * Check if the method is the CONNECT method.
     *
     * @param method the method to check
     * @return true if the method matches
     */
    public static boolean isConnect(final String method) {
        return CONNECT.equals(method);
    }

    /**
     * Check if the method is the HEAD method.
     *
     * @param method the method to check
     * @return true if the method matches
     */
    public static boolean isHead(final String method) {
        return HEAD.equals(method);
    }

    /**
     * Check if the method is the HEAD method.
     *
     * @param method the method to check
     * @return true if the method matches
     */
    public static boolean isTrace(final String method) {
        return TRACE.equals(method);
    }

    /**
     * Check if the method is POST method.
     *
     * @param method the method to check
     * @return true if the post method.
     */
    public static boolean isPost(final String method) {
        return POST.equals(method);
    }

    /**
     * Check if the method is the GET method.
     *
     * @param method the method to check
     * @return true if GET
     */
    public static boolean isGet(final String method) {
        return GET.equals(method);
    }

    /**
     * Check if the method is an options method.
     *
     * @param method the method to check
     * @return true if OPTIONS
     */
    public static boolean isOptions(final String method) {
        return OPTIONS.equals(method);
    }
}
