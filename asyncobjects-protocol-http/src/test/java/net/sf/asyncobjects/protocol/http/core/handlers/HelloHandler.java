package net.sf.asyncobjects.protocol.http.core.handlers;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;
import net.sf.asyncobjects.protocol.http.server.HttpExchange;
import net.sf.asyncobjects.protocol.http.server.HttpHandlerBase;
import net.sf.asyncobjects.protocol.http.server.util.ResponseUtil;

import static net.sf.asyncobjects.protocol.http.common.XmlUtil.escapeXml;

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
