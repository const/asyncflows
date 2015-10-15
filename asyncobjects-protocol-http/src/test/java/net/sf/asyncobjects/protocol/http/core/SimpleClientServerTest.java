package net.sf.asyncobjects.protocol.http.core;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.core.util.ResourceUtil;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.util.AbstractDigestingStream;
import net.sf.asyncobjects.nio.util.ByteIOUtil;
import net.sf.asyncobjects.nio.util.DigestingInput;
import net.sf.asyncobjects.nio.util.DigestingOutput;
import net.sf.asyncobjects.nio.util.LimitedInput;
import net.sf.asyncobjects.protocol.http.client.AHttpClient;
import net.sf.asyncobjects.protocol.http.client.AHttpRequest;
import net.sf.asyncobjects.protocol.http.client.core.SimpleHttpClient;
import net.sf.asyncobjects.protocol.http.common.HttpMethodUtil;
import net.sf.asyncobjects.protocol.http.common.Scope;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeadersUtil;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

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
                                           final AFunction<T, AHttpClient> action) {
        final SimpleHttpClient client = new SimpleHttpClient();
        client.setUserAgent("test-client/1.0");
        client.setSocketFactory(socketFactory);
        return ResourceUtil.aTryResource(client.export()).run(action::apply);
    }

    /**
     * Run with the specified client.
     *
     * @param request the request for the test
     * @param client  the client
     * @return when request finishes
     */
    protected final Promise<Response> runWithClient(final Request request, final AHttpClient client) {
        return ResourceUtil.aTryResource(client).andChain(AHttpClient::newRequest).runWithSecond(
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
        return exchange.getResponse().map(
                response -> ByteIOUtil.getContent(response.getResponseStream()).map(
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
        return ResourceUtil.aTryResource(client).andChain(AHttpClient::newRequest).runWithSecond(
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
                        assertNotNull(String.valueOf(httpResponse.getStatusCode()), httpResponse.getSwitchedChannel());
                        return aAll(
                                () -> DigestingOutput.generateDigested(httpResponse.getSwitchedChannel().getOutput(),
                                        AbstractDigestingStream.SHA_256, output -> {
                                            return ByteIOUtil.charGen(output, 10000, 113);
                                        })
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
