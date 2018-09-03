package org.asyncflows.protocol.http.core;

import org.asyncflows.protocol.http.common.HttpMethodUtil;
import org.asyncflows.protocol.http.common.HttpURIUtil;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.core.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.asyncflows.core.AsyncControl.aLater;
import static org.asyncflows.core.AsyncControl.aValue;

/**
 * The server tests based on Apache HTTP client.
 */
public class HttpClientServerTest extends HttpServerTestBase { // NOPMD
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientServerTest.class);

    @Override
    protected Promise<Response> makeRequest(final HttpServerTestBase.Request requestObject) {
        return aLater(() -> {
            final HttpRequestBase request;
            if (HttpMethodUtil.isGet(requestObject.getMethod())) {
                request = new HttpGet(requestObject.getUri());
            } else if (HttpMethodUtil.isPost(requestObject.getMethod())) {
                final HttpPost httpPost = new HttpPost(requestObject.getUri());
                if (requestObject.getRequestContent() != null) {
                    if (requestObject.isChunked()) {
                        httpPost.setEntity(new InputStreamEntity(
                                new ByteArrayInputStream(requestObject.getRequestContent())));
                    } else {
                        httpPost.setEntity(new ByteArrayEntity(requestObject.getRequestContent()));
                    }
                } else {
                    httpPost.setEntity(new ByteArrayEntity(new byte[0]));
                }
                request = httpPost;
            } else {
                throw new IllegalArgumentException("Method is not yet supported");
            }
            request.setProtocolVersion(HttpVersion.HTTP_1_1);
            final ArrayList<Header> hs = new ArrayList<>();
            for (final Map.Entry<String, List<String>> entry : requestObject.getHeaders().entrySet()) {
                for (final String value : entry.getValue()) {
                    hs.add(new BasicHeader(entry.getKey(), value)); // NOPMD
                }
            }
            request.setHeaders(hs.toArray(new Header[hs.size()]));
            try (final CloseableHttpClient client = HttpClients.createDefault()) {
                final HttpHost host = new HttpHost(requestObject.getUri().getHost(),
                        HttpURIUtil.getPort(requestObject.getUri()));
                try (final CloseableHttpResponse response = client.execute(host, request)) {
                    final HttpHeaders responseHeaders = new HttpHeaders();
                    for (final Header header : response.getAllHeaders()) {
                        responseHeaders.addHeader(header.getName(), header.getValue());
                    }
                    final byte[] content;
                    if (response.getEntity() != null) {
                        try (InputStream contentStream = response.getEntity().getContent()) {
                            content = IOUtils.toByteArray(contentStream);
                        }
                    } else {
                        content = new byte[0];
                    }
                    return aValue(new Response(response.getStatusLine().getStatusCode(),
                            response.getStatusLine().getReasonPhrase(), responseHeaders, content));
                }
            }
        }, Vats.daemonVat()).listen(LogUtil.checkpoint(LOG, "Request Finished: "));
    }
}
