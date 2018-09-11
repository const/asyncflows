/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

import org.asyncflows.io.AOutput;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.util.AbstractDigestingStream;
import org.asyncflows.io.util.ByteIOUtil;
import org.asyncflows.io.util.DigestingInput;
import org.asyncflows.io.util.DigestingOutput;
import org.asyncflows.io.util.LimitedInput;
import org.asyncflows.protocol.http.client.AHttpClient;
import org.asyncflows.protocol.http.client.AHttpRequest;
import org.asyncflows.protocol.http.client.core.SimpleHttpClient;
import org.asyncflows.protocol.http.common.HttpMethodUtil;
import org.asyncflows.protocol.http.common.Scope;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.util.CoreFlowsResource;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.asyncflows.core.util.CoreFlowsResource.aTryResource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The test of the server with the simple client.
 */
public class SimpleClientServerTest extends HttpServerTestBase {
    // TODO tests for trailers
    // TODO tests for transfer encodings
    @Override
    protected Promise<Response> makeRequest(final HttpServerTestBase.Request request) {
        return runWithClient(request.getSocketFactory(), request.getUri(), client -> runWithClient(request, client));
    }

    /**
     * Run an action with the client
     *
     * @param socketFactory the socket factory.
     * @param uri           the uri
     * @param action        the action
     * @return the result of action execution.
     */
    protected <T> Promise<T> runWithClient(final ASocketFactory socketFactory, final URI uri,
                                           final AFunction<AHttpClient, T> action) {
        final SimpleHttpClient client = new SimpleHttpClient();
        client.setUserAgent("test-client/1.0");
        client.setSocketFactory(socketFactory);
        return aTryResource(client.export()).run(action::apply);
    }

    /**
     * Run with the specified client.
     *
     * @param request the request for the test
     * @param client  the client
     * @return when request finishes
     */
    protected final Promise<Response> runWithClient(final Request request, final AHttpClient client) {
        return aTryResource(client).andChain(AHttpClient::newRequest).runWithSecond(
                exchange -> aAll(
                        () -> doRequest(request, exchange)
                ).and(
                        () -> doResponse(exchange)
                ).selectValue2()
        );
    }

    /**
     * Do response.
     *
     * @param exchange the exchange
     * @return the response for the test
     */
    private Promise<Response> doResponse(final AHttpRequest exchange) {
        return exchange.getResponse().flatMap(
                response -> ByteIOUtil.getContent(response.getResponseStream()).flatMap(
                        content -> aValue(new Response(response.getStatusCode(), response.getReason(),
                                response.getHeaders(), content))
                ));
    }

    /**
     * Do the request.
     *
     * @param request  the request for the test
     * @param exchange the exchange
     * @return when request finishes
     */
    private Promise<Void> doRequest(final Request request, final AHttpRequest exchange) {
        final Long length;
        if (request.getRequestContent() == null) {
            length = AHttpRequest.NO_CONTENT;
        } else {
            if (request.isChunked()) {
                length = null;
            } else {
                length = (long) request.getRequestContent().length;
            }
        }
        final Scope scope = new Scope();
        final Promise<AOutput<ByteBuffer>> requestResult = exchange.request(
                scope,
                request.getMethod(),
                request.getUri(),
                HttpHeaders.fromMap(request.getHeaders()),
                length);
        return aTry(requestResult).run(value -> {
            if (request.getRequestContent() != null) {
                return value.write(ByteBuffer.wrap(request.getRequestContent()));
            } else {
                return aVoid();
            }
        });
    }

    @Test
    public void testEchoUpgrade() {
        final Tuple2<byte[], byte[]> result = runAction(
                (socketFactory, socketAddress) -> {
                    final URI uri = new URI("http://localhost:"
                            + ((InetSocketAddress) socketAddress).getPort() + "/echo");
                    return runWithClient(socketFactory, uri, client -> checkEchoHandler(client, uri));
                });
        assertArrayEquals(result.getValue1(), result.getValue2());
    }

    /**
     * Test echo handler with the specified HTTP client.
     *
     * @param client the client.
     * @param uri    the uri
     * @return the promise for the digests.
     */
    private Promise<Tuple2<byte[], byte[]>> checkEchoHandler(final AHttpClient client, final URI uri) {
        return CoreFlowsResource.aTryResource(client).andChain(AHttpClient::newRequest).runWithSecond(
                request -> {
                    final HttpHeaders headers = new HttpHeaders();
                    headers.setHeader(HttpHeadersUtil.CONNECTION_HEADER, HttpHeadersUtil.UPGRADE_HEADER);
                    headers.setHeader(HttpHeadersUtil.UPGRADE_HEADER, "echo");
                    return aSeq(
                            () -> request.request(new Scope(), HttpMethodUtil.OPTIONS, uri, headers, 0L)
                    ).map(
                            ACloseable::close
                    ).thenDo(
                            request::getResponse
                    ).mapLast(httpResponse -> {
                        assertNotNull(httpResponse.getSwitchedChannel(), String.valueOf(httpResponse.getStatusCode()));
                        return aAll(
                                () -> DigestingOutput.generateDigested(httpResponse.getSwitchedChannel().getOutput(),
                                        AbstractDigestingStream.SHA_256, output ->
                                                ByteIOUtil.charGen(output, 10000, 113))
                        ).andLast(
                                () -> aTry(
                                        httpResponse.getSwitchedChannel().getInput()
                                ).run(
                                        input -> DigestingInput.digestAndDiscardInput(
                                                LimitedInput.limit(input, 10000),
                                                AbstractDigestingStream.SHA_256)
                                ));
                    });
                });
    }
}
