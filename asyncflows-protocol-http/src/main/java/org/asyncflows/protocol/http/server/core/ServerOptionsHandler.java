package org.asyncflows.protocol.http.server.core;

import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.content.ContentUtil;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.HttpHandlerBase;
import org.asyncflows.protocol.http.server.util.ResponseUtil;
import org.asyncflows.core.Promise;

/**
 * The handler for "OPTIONS * HTTP/1.x" requests. It provides information on the
 * available server options. Currently it is hardcoded for the entire server.
 */
public class ServerOptionsHandler extends HttpHandlerBase {
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
        final HttpHeaders headers = new HttpHeaders();
        headers.setHeader(HttpHeadersUtil.TE_HEADER,
                ContentUtil.GZIP_ENCODING + ";q=1 " + ContentUtil.DEFLATE_ENCODING + ";q=0.9");
        return ResponseUtil.shortReply(exchange, HttpStatusUtil.OK, "", headers);
    }
}
