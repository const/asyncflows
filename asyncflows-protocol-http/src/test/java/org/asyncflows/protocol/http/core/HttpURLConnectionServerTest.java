/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

package org.asyncflows.protocol.http.core;

import org.apache.commons.io.IOUtils;
import org.asyncflows.core.Promise;
import org.asyncflows.core.util.LogUtil;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aValue;

/**
 * The server test. It use HttpUrlConnection on the separate thread.
 */
@SuppressWarnings("squid:S2187")
public class HttpURLConnectionServerTest extends HttpServerTestBase {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(HttpURLConnectionServerTest.class);


    @Override
    protected Promise<Response> makeRequest(final HttpServerTestBase.Request request) {
        return aLater(Vats.daemonVat(), () -> {
            final HttpURLConnection connection = (HttpURLConnection) request.getUri().toURL().openConnection();
            connection.setDoInput(true);
            if (request.getRequestContent() != null) {
                connection.setDoOutput(true);
                if (request.isChunked()) {
                    connection.setChunkedStreamingMode(request.getRequestContent().length / 3);
                } else {
                    connection.setFixedLengthStreamingMode(request.getRequestContent().length);
                }
            }
            connection.setRequestMethod(request.getMethod());
            for (final Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
                for (final String value : entry.getValue()) {
                    connection.addRequestProperty(entry.getKey(), value);
                }
            }
            connection.connect();
            if (request.getRequestContent() != null) {
                try (final OutputStream stream = connection.getOutputStream()) {
                    stream.write(request.getRequestContent());
                    stream.flush();
                }
            }
            final byte[] responseBytes;
            try (InputStream input = connection.getInputStream()) {
                responseBytes = IOUtils.toByteArray(input);
            }
            return aValue(new Response(connection.getResponseCode(), connection.getResponseMessage(),
                    HttpHeaders.fromMap(connection.getHeaderFields()), responseBytes));
        }).listen(LogUtil.checkpoint(LOG, "Request Finished: "));
    }


}
