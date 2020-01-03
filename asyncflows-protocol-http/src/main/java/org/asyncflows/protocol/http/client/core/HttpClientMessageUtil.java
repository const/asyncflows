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

package org.asyncflows.protocol.http.client.core;

import org.asyncflows.core.Promise;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.protocol.LineUtil;
import org.asyncflows.protocol.ProtocolLineParser;
import org.asyncflows.protocol.http.HttpException;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.asyncflows.protocol.http.common.HttpMethodUtil;
import org.asyncflows.protocol.http.common.HttpRequestMessage;
import org.asyncflows.protocol.http.common.HttpResponseMessage;
import org.asyncflows.protocol.http.common.HttpRuntimeUtil;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.HttpVersionUtil;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;

/**
 * The utility class for handling client messages.
 */
public final class HttpClientMessageUtil {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientMessageUtil.class);

    /**
     * Private constructor for utility class.
     */
    private HttpClientMessageUtil() {
        // do nothing
    }

    /**
     * Write the prepared request message.
     *
     * @param context the context
     * @param message the message
     * @return when writing finished
     */
    public static Promise<Void> writeRequestMessage(final ByteGeneratorContext context,
                                                    final HttpRequestMessage message) {
        return aSeq(() -> {
            final String startLine = message.getMethod() + ' ' + message.getRequestTarget()
                    + ' ' + message.getVersion() + LineUtil.CRLF;
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Writing a message: %s%s", startLine, message.getHeaders()));
            }
            return LineUtil.writeLatin1(context, startLine);
        }).thenDo(
                () -> message.getHeaders().write(context)
        ).thenDo(
                () -> context.send().toVoid()
        ).failedLast(HttpRuntimeUtil.toHttpException("Failed write request message"));
    }

    /**
     * Infer host and request target basing on the effective URL.
     *
     * @param message        the message
     * @param connectionHost the host to which connection is connected
     */
    @SuppressWarnings("squid:S3776")
    public static void inferRequestTarget(final HttpRequestMessage message, final String connectionHost) {
        final URI uri = message.getEffectiveUri();
        if (!LineUtil.isEmpty(uri.getUserInfo())) {
            throw new HttpException("The URI contains user info component. It should be removed");
        }
        final String host = uri.getRawAuthority();
        final String method = message.getMethod();
        message.getHeaders().setFirstHeader(HttpHeadersUtil.HOST_HEADER, host);
        if (HttpMethodUtil.isConnect(method)) {
            message.setRequestTarget(uri.getRawAuthority());
        } else {
            if (host.equalsIgnoreCase(connectionHost)) {
                final String query = uri.getRawQuery();
                final String path = uri.getRawPath() + (LineUtil.isEmpty(query) ? "" : "?" + query);
                if (LineUtil.isEmpty(path)) {
                    if (HttpMethodUtil.isOptions(method)) {
                        message.setRequestTarget("*");
                    } else {
                        message.setRequestTarget("/");
                    }
                } else {
                    message.setRequestTarget(path);
                }
            } else {
                message.setRequestTarget(uri.toASCIIString());
            }
        }
    }

    /**
     * Read response message.
     *
     * @param input   the input
     * @param message the message
     * @return when finished
     */
    public static Promise<Void> readResponseMessage(final ByteParserContext input, final HttpResponseMessage message) {
        return aSeq(
                () -> LineUtil.readLineCRLF(input, HttpLimits.MAX_START_LINE_SIZE)
        ).map(statusLine -> {
            parseStatusLine(message, statusLine);
            return HttpHeaders.readHeaders(input, HttpLimits.MAX_HEADERS_SIZE);
        }).mapLast(headers -> {
            message.setHeaders(headers);
            return aVoid();
        });
    }

    /**
     * Parse status line.
     *
     * @param message    the message
     * @param statusLine the status line
     */
    private static void parseStatusLine(final HttpResponseMessage message, final String statusLine) {
        final ProtocolLineParser p = new ProtocolLineParser(statusLine);
        final String version = p.untilWhiteSpace();
        if (!HttpVersionUtil.isHttp10(version) && !HttpVersionUtil.isHttp11(version)) {
            throw new HttpException("Unknown response version: " + statusLine);
        }
        message.setVersion(version);
        p.rws();
        final String status = p.untilWhiteSpace();
        try {
            final int statusCode = Integer.parseInt(status);
            if (!HttpStatusUtil.isValidStatus(statusCode)) {
                throw new HttpException("Bad status code: " + statusCode);
            }
            message.setStatusCode(statusCode);
        } catch (final NumberFormatException ex) {
            throw new HttpException("Bad status code: " + statusLine, ex);
        }
        p.rws();
        final String reason = p.rest();
        message.setStatusMessage(reason);
    }
}
