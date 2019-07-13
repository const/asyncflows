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
import org.asyncflows.core.util.LogUtil;
import org.asyncflows.io.IOUtil;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.HttpHandlerBase;
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
