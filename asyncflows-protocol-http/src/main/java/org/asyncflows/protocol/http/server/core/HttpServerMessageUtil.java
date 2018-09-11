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

package org.asyncflows.protocol.http.server.core;

import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.protocol.LineUtil;
import org.asyncflows.protocol.http.HttpStatusException;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.asyncflows.protocol.http.common.HttpRequestMessage;
import org.asyncflows.protocol.http.common.HttpResponseMessage;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.HttpURIUtil;
import org.asyncflows.protocol.http.common.HttpVersionUtil;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.util.ResourceClosedException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqUntilValue;

/**
 * Utilities for HTTP message parsing and generation on the server side.
 */
public final class HttpServerMessageUtil {

    /**
     * The private constructor for the utility class.
     */
    private HttpServerMessageUtil() {
        // do nothing
    }

    /**
     * Write response message.
     *
     * @param context the output
     * @param message the message
     * @return the true when message is written. The message bytes are actually sent to underlying stream.
     */
    public static Promise<Void> writeResponseMessage(final ByteGeneratorContext context,
                                                     final HttpResponseMessage message) {
        // TODO validate status text
        final String statusLine = message.getVersion() + " " + message.getStatusCode() + " "
                + message.getStatusMessage() + LineUtil.CRLF;
        return LineUtil.writeLatin1(context, statusLine).thenFlatGet(
                () -> message.getHeaders().write(context)
        ).thenFlatGet(
                () -> context.send().toVoid());
    }

    /**
     * The parse HTTP 1.1 and 1.0 start line for the request message.
     *
     * @param message the message information holder
     * @param value   the line
     */
    public static void parseHttpRequestLine(final HttpRequestMessage message, final String value) {
        final int p = value.indexOf(' ');
        if (p == -1) {
            throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "No method in the request line");
        }
        final String method = value.substring(0, p);
        message.setMethod(method);
        final int l = value.lastIndexOf(' ');
        if (p == l) {
            throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "No version in the request line");
        }
        final String version = value.substring(l + 1, value.length());
        message.setVersion(version);
        final String requestTarget = value.substring(p + 1, l).trim();
        if (requestTarget.length() == 0) {
            throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Empty request target");
        }
        message.setRequestTarget(requestTarget);
    }

    /**
     * Parse the request message.
     *
     * @param input   the input
     * @param message the message
     * @return true if the message has been read, false if EOF is encountered.
     */
    public static Promise<Boolean> parseRequestMessage(final ByteParserContext input,
                                                       final HttpRequestMessage message) {
        return aSeqUntilValue(
                () -> LineUtil.readLineCRLF(input, HttpLimits.MAX_START_LINE_SIZE).flatMap(
                        value -> {
                            if (value != null && value.isEmpty()) {
                                return aMaybeEmpty();
                            } else {
                                return aMaybeValue(value);
                            }
                        }
                )
        ).flatMap(startLine -> {
            if (startLine == null) {
                return aFalse();
            } else {
                parseHttpRequestLine(message, startLine);
                return HttpHeaders.readHeaders(input, HttpLimits.MAX_HEADERS_SIZE).flatMap(
                        headers -> {
                            message.setHeaders(headers);
                            inferEffectiveUri(message);
                            return aTrue();
                        });
            }
        }).flatMapFailure(value -> {
            if (value instanceof ResourceClosedException) {
                return aFalse();
            } else if (value instanceof HttpStatusException) {
                return aFailure(value);
            } else {
                return aFailure(new HttpStatusException(HttpStatusUtil.BAD_REQUEST, value.getMessage(), value));
            }
        });
    }

    /**
     * Process the message, extracting message parameters.
     *
     * @param message the message
     */
    private static void inferEffectiveUri(final HttpRequestMessage message) {
        // Note that this version does not massage the requestTarget beyond trimming the spaces.
        final String version = message.getVersion();
        final boolean http11 = HttpVersionUtil.isHttp11(version);
        final boolean http10 = HttpVersionUtil.isHttp10(version);
        if (!http11 && !http10) {
            throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST,
                    "Unknown version of HTTP: " + version);
        }
        final String host = getHost(message.getHeaders(), http11);
        final String effectiveHost = LineUtil.isEmpty(host) ? message.getServerAddress() : host;
        final String target = message.getRequestTarget();
        final String method = message.getMethod();
        try {
            if ("CONNECT".equals(method)) {
                // the connect method has a special processing.
                // It has just authority as request target.
                if (host != null && !target.equals(host)) {
                    throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST,
                            "Host header and target should match for CONNECT method: " + host + " != " + target);
                }
                message.setEffectiveUri(filteredUri(message.getProtocol() + "://" + target));
                return;
            }
            if (target.charAt(0) == '/') {
                // origin form of the request target
                // The request target is never blank.
                message.setEffectiveUri(filteredUri(message.getProtocol() + "://" + effectiveHost + target));
            } else if ("*".equals(target)) {
                // asterisk-form of request target, there two cases currently:
                // - OPTIONS request
                // - Start of HTTP 2.0 protocol ("PRI" method)
                // So it is treated as empty path just in case.
                message.setEffectiveUri(filteredUri(message.getProtocol() + "://" + effectiveHost));
            } else {
                // else it must be absolute form of the URL.
                final URI uri = filteredUri(target);
                message.setEffectiveUri(uri);
            }
        } catch (URISyntaxException | MalformedURLException ex) {
            throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Bad request "
                    + effectiveHost + " " + method + " " + target + " " + version, ex);
        }
    }

    /**
     * The URL with filtered authority component and without fragment.
     *
     * @param uriText the URL text
     * @return the URI
     * @throws MalformedURLException in case of bad uri
     * @throws URISyntaxException    in case of bad uri
     */
    private static URI filteredUri(final String uriText) throws MalformedURLException, URISyntaxException {
        final URI uri = new URI(uriText);
        // strip fragment and user information from URI.
        if (uri.getUserInfo() != null) {
            throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "User info presents in host header");
        }
        if (uri.getFragment() == null) {
            return uri;
        }
        final StringBuilder rc = new StringBuilder();
        rc.append(uri.getScheme()).append("://").append(uri.getHost()).append(':').append(HttpURIUtil.getPort(uri));
        if (uri.getRawPath() != null) {
            rc.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            rc.append(uri.getRawQuery());
        }
        return new URI(rc.toString());
    }

    /**
     * A value of the hosts header. It ensures that only one header happens in the request.
     *
     * @param headers the headers.
     * @param http11  the http 1.1 flag
     * @return the value of the host header
     */
    public static String getHost(final HttpHeaders headers, final boolean http11) {
        final List<String> hosts = headers.getHeaders(HttpHeadersUtil.HOST_HEADER);
        if (hosts.size() > 1) {
            throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Multiple Host headers detected");
        }
        if (hosts.isEmpty()) {
            if (http11) {
                throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "No 'Host' header for HTTP/1.1 request");
            } else {
                return null;
            }
        }
        final String host = hosts.get(0).trim();
        if (LineUtil.isEmpty(host)) {
            return null;
        }
        try {
            final URI uri = new URI("http://" + host);
            if (uri.getUserInfo() != null) {
                throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "User info presents in host header");
            }
            if (!uri.getAuthority().equals(host)) {
                throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Invalid host header: " + host);
            }
            return uri.getHost() + ":" + HttpURIUtil.getPort(uri);
        } catch (URISyntaxException e) {
            throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Invalid host header: " + e.getMessage(), e);
        }
    }
}

