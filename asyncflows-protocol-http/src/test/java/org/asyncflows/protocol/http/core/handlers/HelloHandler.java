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

package org.asyncflows.protocol.http.core.handlers;

import org.asyncflows.core.Promise;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.HttpExchangeUtil;
import org.asyncflows.protocol.http.server.HttpHandlerBase;
import org.asyncflows.protocol.http.server.util.ResponseUtil;

import static org.asyncflows.protocol.http.common.XmlUtil.escapeXml;

/**
 * The hello handler.
 */
public class HelloHandler extends HttpHandlerBase {
    @Override
    public Promise<Void> handle(final HttpExchange exchange) {
        return ResponseUtil.shortReply(exchange, HttpStatusUtil.OK, null, "<html><title>Hello World!</title><body>"
                + "<table>"
                + "<tr><td>Method:</td><td>" + escapeXml(exchange.getMethod()) + "</td></tr>"
                + "<tr><td>URL:</td><td>" + escapeXml(exchange.getRequestUri().toString()) + "</td></tr>"
                + "<tr><td>Headers:</td><td><pre>" + escapeXml(exchange.getRequestHeaders().toString())
                + "</pre></td></tr>"
                + "<tr><td>Local Address:</td><td>" + escapeXml(exchange.getLocalAddress().toString())
                + "</td></tr>"
                + "<tr><td>Remote Address:</td><td>" + escapeXml(exchange.getRemoteAddress().toString())
                + "</td></tr>"
                + "<tr><td>Server Address:</td><td>"
                + escapeXml(exchange.getServerScope().get(HttpExchangeUtil.SERVER_ADDRESS).toString()) + "</td></tr>"
                + "</table></body></html>");
    }
}
