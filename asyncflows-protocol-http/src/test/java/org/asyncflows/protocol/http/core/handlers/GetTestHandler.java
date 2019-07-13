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
import org.asyncflows.io.util.ByteIOUtil;
import org.asyncflows.protocol.LineUtil;
import org.asyncflows.protocol.http.HttpStatusException;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.HttpURIUtil;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.HttpHandlerBase;
import org.asyncflows.protocol.http.server.util.ResponseUtil;

import java.util.Map;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;

/**
 * The handler that produces the specified amount of the random data.
 */
public class GetTestHandler extends HttpHandlerBase {
    @Override
    public Promise<Void> handle(final HttpExchange exchange) { // NOPMD
        try {
            final Map<String, String> parameters = HttpURIUtil.getQueryParameters(exchange.getRequestUri());
            final long length;
            final String lengthText = parameters.get("length");
            if (LineUtil.isBlank(lengthText)) {
                return ResponseUtil.redirect(exchange, "/chargen.txt?length=1024");
            } else {
                try {
                    length = Long.parseLong(lengthText);
                } catch (NumberFormatException ex) {
                    throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Bad length format", ex);
                }
                if (length < 0) {
                    throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "length must be positive");
                }
            }
            final Integer chunkSize;
            final String chunkText = parameters.get("chunk");
            if (LineUtil.isBlank(chunkText)) {
                chunkSize = null;
            } else {
                try {
                    chunkSize = Integer.parseInt(chunkText);
                } catch (NumberFormatException ex) {
                    throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Bad chunk size format", ex);
                }
                if (chunkSize < 0) {
                    throw new HttpStatusException(HttpStatusUtil.BAD_REQUEST, "Chunk size must be positive");
                }
            }
            final HttpHeaders headers = new HttpHeaders();
            headers.setHeader(HttpHeadersUtil.CONTENT_TYPE_HEADER, HttpHeadersUtil.CONTENT_TYPE_TEXT_UTF8);
            return aTry(exchange.respond(HttpStatusUtil.OK, headers, chunkSize == null ? length : null)).run(
                    output -> {
                        int bufferSize;
                        if (chunkSize != null) {
                            bufferSize = chunkSize;
                        } else {
                            bufferSize = (int) Math.min(HttpLimits.DEFAULT_BUFFER_SIZE, length / 3 + 1);
                        }
                        return ByteIOUtil.charGen(output, length, bufferSize);
                    }
            );
        } catch (Exception ex) { // NOPMD
            return aFailure(ex);
        }
    }
}
