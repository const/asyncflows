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

import org.asyncflows.io.util.CharIOUtil;
import org.asyncflows.protocol.http.HttpException;

import java.io.UnsupportedEncodingException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The utilities for working with HTTP urls.
 */
public final class HttpURIUtil {
    /**
     * The default port for HTTP.
     */
    public static final int DEFAULT_HTTP_PORT = 80;
    /**
     * The default port for HTTPS.
     */
    public static final int DEFAULT_HTTPS_PORT = 443;

    /**
     * The private constructor for utility class.
     */
    private HttpURIUtil() {
        // do nothing
    }

    /**
     * Parse parameters from URL query part (assuming UTF-8).
     *
     * @param uri the uri parse
     * @return the parsed parameters (empty if none)
     * @throws URISyntaxException           in case of bad URI
     * @throws UnsupportedEncodingException in case of missing UTF-8
     */
    public static Map<String, String> getQueryParameters(final URI uri)
            throws URISyntaxException, UnsupportedEncodingException {
        final String query = uri.getRawQuery();
        if (query == null) {
            return Collections.emptyMap();
        }
        final LinkedHashMap<String, String> map = new LinkedHashMap<>();
        int l = 0;
        while (l != -1) {
            final int p = query.indexOf('&', l);
            final String element;
            if (p == -1) {
                element = query.substring(l, query.length());
                l = -1;
            } else {
                element = query.substring(l, p);
                l = p + 1;
            }
            if (!element.isEmpty()) {
                final int eq = element.indexOf('=');
                if (eq == -1) {
                    map.put(decode(element), "");
                } else {
                    map.put(decode(element.substring(0, eq)), decode(element.substring(eq + 1, element.length())));
                }
            }
        }
        return map;
    }

    /**
     * Get the host representation from the address.
     *
     * @param address the address
     * @return the host
     */
    public static String getHost(final SocketAddress address) {
        if (address instanceof InetSocketAddress) {
            final InetSocketAddress sa = (InetSocketAddress) address;
            final InetAddress a = sa.getAddress();
            if (a == null) {
                return sa.getHostName() + ':' + sa.getPort();
            } else if (a instanceof Inet6Address) {
                return "[" + a.getHostAddress() + "]:" + sa.getPort();
            } else {
                return a.getHostAddress() + ':' + sa.getPort();
            }
        } else {
            throw new HttpException("Unknown address type");
        }
    }

    /**
     * Get port for URI.
     *
     * @param uri the uri to examine.
     * @return the port
     */
    public static int getPort(final URI uri) {
        final int port = uri.getPort();
        if (port != -1) {
            return port;
        } else if ("http".equalsIgnoreCase(uri.getScheme())) {
            return DEFAULT_HTTP_PORT;
        } else if ("https".equalsIgnoreCase(uri.getScheme())) {
            return DEFAULT_HTTPS_PORT;
        } else {
            throw new HttpException("Unknown URI scheme: " + uri.getScheme());
        }
    }

    /**
     * Decode a single element.
     *
     * @param element the element to decode
     * @return the decoded string
     * @throws UnsupportedEncodingException if there is a problem decoding
     */
    private static String decode(final String element) throws UnsupportedEncodingException {
        return URLDecoder.decode(element, CharIOUtil.UTF8.name());
    }
}
