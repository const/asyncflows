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
