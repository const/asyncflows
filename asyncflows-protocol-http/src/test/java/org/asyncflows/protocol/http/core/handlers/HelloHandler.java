package org.asyncflows.protocol.http.core.handlers;

import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.HttpHandlerBase;
import org.asyncflows.protocol.http.server.util.ResponseUtil;
import org.asyncflows.core.Promise;

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
                + escapeXml(exchange.getServerScope().get(HttpExchange.SERVER_ADDRESS).toString()) + "</td></tr>"
                + "</table></body></html>");
    }
}
