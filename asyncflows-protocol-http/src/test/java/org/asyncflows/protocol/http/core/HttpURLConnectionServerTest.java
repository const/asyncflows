package org.asyncflows.protocol.http.core;

import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.apache.commons.io.IOUtils;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.core.util.LogUtil;
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
public class HttpURLConnectionServerTest extends HttpServerTestBase { // NOPMD
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
