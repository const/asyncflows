package net.sf.asyncobjects.protocol.http.core.handlers;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.protocol.http.common.HttpLimits;
import net.sf.asyncobjects.protocol.http.common.HttpMethodUtil;
import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeadersUtil;
import net.sf.asyncobjects.protocol.http.server.HttpExchange;
import net.sf.asyncobjects.protocol.http.server.HttpHandlerBase;
import net.sf.asyncobjects.protocol.http.server.util.ResponseUtil;

import java.nio.ByteBuffer;

/**
 * Upgrade to echo protocol.
 */
public class EchoUpgradeHandler extends HttpHandlerBase {

    @Override
    public Promise<Void> handle(final HttpExchange exchange) {
        if (!HttpMethodUtil.isOptions(exchange.getMethod())) {
            final HttpHeaders headers = new HttpHeaders();
            headers.setHeader(HttpHeadersUtil.ALLOW_HEADER, HttpMethodUtil.OPTIONS);
            return ResponseUtil.shortReply(exchange, HttpStatusUtil.METHOD_NOT_ALLOWED,
                    "<html><head><title>405 Method not allowed</title></head>"
                            + "<h1>426 Method not allowed</h1><p>Use OPTIONS method.</p></html>",
                    headers);
        }
        String protocol = null;
        for (final String value :
                exchange.getRequestHeaders().getCommaSeparatedValues(HttpHeadersUtil.UPGRADE_HEADER)) {
            if ("echo".equalsIgnoreCase(value)) {
                protocol = value;
                break;
            }
        }
        if (protocol == null) {
            final HttpHeaders headers = new HttpHeaders();
            headers.setHeader(HttpHeadersUtil.UPGRADE_HEADER, "echo");
            headers.addHeader(HttpHeadersUtil.CONNECTION_HEADER, HttpHeadersUtil.UPGRADE_HEADER);
            return ResponseUtil.shortReply(exchange, HttpStatusUtil.UPGRADE_REQUIRED,
                    "<html><head><title>426 Upgrade Required</title></head>"
                            + "<h1>426 Upgrade Required</h1><p>Upgrade to 'echo'.</p></html>",
                    headers);
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.setHeader(HttpHeadersUtil.UPGRADE_HEADER, protocol);
        headers.addHeader(HttpHeadersUtil.CONNECTION_HEADER, HttpHeadersUtil.UPGRADE_HEADER);
        return IOUtil.BYTE.tryChannel(exchange.switchProtocol(HttpStatusUtil.SWITCHING_PROTOCOLS, null, headers)).run(
                (channel, input, output) -> IOUtil.BYTE.copy(input, output, false,
                        ByteBuffer.allocate(HttpLimits.DEFAULT_BUFFER_SIZE)).toVoid()
        );
    }
}
