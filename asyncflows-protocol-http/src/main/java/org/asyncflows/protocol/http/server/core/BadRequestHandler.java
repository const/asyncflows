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

import org.asyncflows.protocol.http.HttpStatusException;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.XmlUtil;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.HttpHandlerBase;
import org.asyncflows.protocol.http.server.util.ResponseUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * The simple handler for the bad requests. The handlers logs bad requests on own logger.
 */
public class BadRequestHandler extends HttpHandlerBase {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BadRequestHandler.class);
    /**
     * The default template.
     */
    private static final String DEFAULT_BAD_REQUEST_TEMPLATE = "<html><head><title>Bad Request</title></head>"
            + "<body><h1>{0} {1}</h1><p>{2}</p></body></html>";
    /**
     * The template.
     */
    private String template = DEFAULT_BAD_REQUEST_TEMPLATE;

    /**
     * Handle the exchange. After the result promise is resolved, the both input and output are closed. If input
     * has not been fully read, the HTTP connection might be aborted, and not handle next requests pipelined
     * in it. If output is with the specified content length, it is closed. If the protocol switch happened,
     * the underlying channel is closed with the method finishes.
     *
     * @param exchange the exchange
     * @return a promise that resolves when request is completely handled.
     */
    @Override
    public Promise<Void> handle(final HttpExchange exchange) {
        //noinspection ThrowableResultOfMethodCallIgnored
        final Throwable problem = exchange.getExchangeScope().get(HttpServer.BAD_REQUEST_PROBLEM);
        final int code;
        if (problem instanceof HttpStatusException) {
            final HttpStatusException statusException = (HttpStatusException) problem;
            if (HttpStatusUtil.isClientError(statusException.getStatusCode())
                    || HttpStatusUtil.isServerError(statusException.getStatusCode())) {
                code = statusException.getStatusCode();
            } else {
                code = HttpStatusUtil.INTERNAL_SERVER_ERROR;
            }
        } else {
            code = HttpStatusUtil.INTERNAL_SERVER_ERROR;
        }
        final String status = HttpStatusUtil.getDefaultText(code);
        final String text = XmlUtil.escapeXml(problem == null ? "Unknown Problem" : problem.getMessage());
        final String formatted = MessageFormat.format(getTemplate(), code, status, text);
        if (LOG.isDebugEnabled()) {
            LOG.debug(code + " " + status, problem);
        }
        return ResponseUtil.shortReply(exchange, code, status, formatted);
    }

    /**
     * @return the template
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Set the template. The template must conform to the content type
     * {@value HttpHeadersUtil#CONTENT_TYPE_HTML_UTF8}. It also has
     * the following parameters.
     * <ol start="0">
     * <li>The status code (java.lang.Integer)</li>
     * <li>The reason text (java.lang.String)</li>
     * <li>The XML escaped message from the exception (java.lang.String)</li>
     * </ol>
     *
     * @param template the new template
     */
    public void setTemplate(final String template) {
        this.template = template;
    }
}
