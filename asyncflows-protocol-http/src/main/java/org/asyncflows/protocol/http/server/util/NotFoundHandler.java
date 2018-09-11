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

package org.asyncflows.protocol.http.server.util;

import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.XmlUtil;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.HttpHandlerBase;
import org.asyncflows.core.Promise;

import java.text.MessageFormat;

/**
 * The standard handler that provide a standard "404 Not Found" response according to the template.
 * It is usually the last in the request delegation list.
 */
public class NotFoundHandler extends HttpHandlerBase {
    /**
     * The default template.
     */
    private static final String DEFAULT_TEMPLATE = "<html><head><title>404 Not Found</title></head>"
            + "<body><table><h1>404 Not Found</h1>"
            + "<tr><td>Method:</td><td>{0}</td></tr>"
            + "<tr><td>URL:</td><td>{1}</td></tr>"
            + "</table></body></html>";
    /**
     * The template.
     */
    private String template = DEFAULT_TEMPLATE;

    @Override
    public Promise<Void> handle(final HttpExchange exchange) {
        // The default header set is used. It is actually enriched by server
        // which adds standard fields that are not specified by this response.
        final String body = MessageFormat.format(getTemplate(),
                XmlUtil.escapeXml(exchange.getMethod()),
                XmlUtil.escapeXml(exchange.getRequestUri().toString()));
        return ResponseUtil.shortReply(exchange, HttpStatusUtil.NOT_FOUND, null, body);
    }

    /**
     * @return the current template
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Set template for the reply. The template should be HTML with the following parameters (escaped as XML):
     * <ol start='0'>
     * <li>HTTP method</li>
     * <li>The authority (host + port)</li>
     * <li>The path (empty string if null)</li>
     * </ol>
     *
     * @param template the template
     */
    public void setTemplate(final String template) {
        this.template = template;
    }
}
