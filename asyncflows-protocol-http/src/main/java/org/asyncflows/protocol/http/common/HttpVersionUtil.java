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
 * The HTTP version utilities.
 */
public final class HttpVersionUtil {
    /**
     * The HTTP version 1.0.
     */
    public static final String HTTP_VERSION_1_0 = "HTTP/1.0";
    /**
     * The HTTP version 1.1.
     */
    public static final String HTTP_VERSION_1_1 = "HTTP/1.1";

    /**
     * Private constructor for utility class.
     */
    private HttpVersionUtil() {
        // do nothing
    }

    /**
     * Check if the protocol is HTTP 1.1.
     *
     * @param version the version to check
     * @return true if http 1.1
     */
    public static boolean isHttp11(final String version) {
        return HTTP_VERSION_1_1.equals(version);
    }

    /**
     * Check if the protocol is HTTP 1.0.
     *
     * @param version the version to check
     * @return true if http 1.1
     */
    public static boolean isHttp10(final String version) {
        return HTTP_VERSION_1_0.equals(version);
    }
}
