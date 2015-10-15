package net.sf.asyncobjects.protocol.http.core.handlers;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.LogUtil;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeadersUtil;
import net.sf.asyncobjects.protocol.http.server.HttpExchange;
import net.sf.asyncobjects.protocol.http.server.HttpHandlerBase;
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
        return exchange.respond(HttpStatusUtil.OK, headers, length).map(
                output -> IOUtil.BYTE.copy(exchange.getInput(), output, false,
                        ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE))
                        .observe(LogUtil.checkpoint(LOG, "POST handler finished")).toVoid()
        );
    }
}
