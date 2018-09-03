package org.asyncflows.protocol.http.core.handlers;

import org.asyncflows.io.IOUtil;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.HttpHandlerBase;
import org.asyncflows.core.Promise;
import org.asyncflows.core.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * The handler that just return type with the same content-type and content-encoding.
 */
public class PostMirrorHandler extends HttpHandlerBase {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PostMirrorHandler.class);

    @Override
    public Promise<Void> handle(final HttpExchange exchange) {
        final Long length = exchange.getInputLength();
        final HttpHeaders headers = new HttpHeaders();
        headers.setHeaders(HttpHeadersUtil.CONTENT_TYPE_HEADER,
                exchange.getRequestHeaders().getHeaders(HttpHeadersUtil.CONTENT_TYPE_HEADER));
        headers.setHeaders(HttpHeadersUtil.CONTENT_ENCODING_HEADER,
                exchange.getRequestHeaders().getHeaders(HttpHeadersUtil.CONTENT_ENCODING_HEADER));
        return exchange.respond(HttpStatusUtil.OK, headers, length).flatMap(
                output -> IOUtil.BYTE.copy(exchange.getInput(), output, false,
                        ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE))
                        .listen(LogUtil.checkpoint(LOG, "POST handler finished")).toVoid()
        );
    }
}
