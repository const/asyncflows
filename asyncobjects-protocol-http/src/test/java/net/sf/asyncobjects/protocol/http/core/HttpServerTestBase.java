package net.sf.asyncobjects.protocol.http.core;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.util.AFunction2;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.net.SocketUtil;
import net.sf.asyncobjects.nio.net.selector.SelectorVatUtil;
import net.sf.asyncobjects.protocol.http.common.HttpMethodUtil;
import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;
import net.sf.asyncobjects.protocol.http.common.content.ContentUtil;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeadersUtil;
import net.sf.asyncobjects.protocol.http.core.handlers.ConnectHandler;
import net.sf.asyncobjects.protocol.http.core.handlers.EchoUpgradeHandler;
import net.sf.asyncobjects.protocol.http.core.handlers.GetTestHandler;
import net.sf.asyncobjects.protocol.http.core.handlers.HelloHandler;
import net.sf.asyncobjects.protocol.http.core.handlers.PostMirrorHandler;
import net.sf.asyncobjects.protocol.http.server.AHttpHandler;
import net.sf.asyncobjects.protocol.http.server.HttpExchange;
import net.sf.asyncobjects.protocol.http.server.HttpHandlerBase;
import net.sf.asyncobjects.protocol.http.server.core.HttpServer;
import net.sf.asyncobjects.protocol.http.server.util.DelegatingHandler;
import net.sf.asyncobjects.protocol.http.server.util.NotFoundHandler;
import net.sf.asyncobjects.protocol.http.server.util.ResponseUtil;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The base class for the tests.
 */
public abstract class HttpServerTestBase {
    /**
     * Make a http request using {@link java.net.HttpURLConnection}.
     *
     * @param request the request object
     * @return result.
     */
    protected abstract Promise<Response> makeRequest(final Request request);

    /**
     * Create HTTP sever over socket factory and with provide handler registered. The server is initialized and it is
     * created on anonymous port.
     *
     * @param factory the factory
     * @param handler the handler
     * @return the promise for HTTP server.
     */
    private Promise<Tuple2<HttpServer, SocketAddress>> createServer(final ASocketFactory factory,
                                                                    final AHttpHandler handler) {
        return SocketUtil.anonymousServerSocket(factory).map(
                value -> {
                    final HttpServer server = new HttpServer();
                    server.setSocket(value.getValue1());
                    server.setHandler(handler);
                    return aValue(Tuple2.of(server, value.getValue2()));
                });
    }

    /**
     * Run a simple request using {@link java.net.HttpURLConnection}.
     *
     * @param method         the request method
     * @param path           the path
     * @param headers        the headers
     * @param requestContent the request content
     * @param chunked        the request encoding
     * @return the response
     */
    protected Response runRequest(final String method, final String path, final Map<String, List<String>> headers,
                                  final byte[] requestContent, final boolean chunked) {
        final AFunction2<Response, ASocketFactory, SocketAddress> action =
                (socketFactory, address) -> {
                    final int port = ((InetSocketAddress) address).getPort();
                    return makeRequest(new Request(method, new URI("http://localhost:" + port + path),
                            headers, requestContent, chunked, socketFactory));
                };
        return runAction(action);
    }

    /**
     * Run a single action with the server and then exit.
     *
     * @param action the action to run
     * @param <T>    the action type
     * @return the action result
     */
    protected <T> T runAction(final AFunction2<T, ASocketFactory, SocketAddress> action) {
        return SelectorVatUtil.run(
                socketFactory -> createServer(socketFactory, handler(socketFactory)).map(
                        value -> {
                            final HttpServer server = value.getValue1();
                            final SocketAddress socketAddress = value.getValue2();
                            return aAll(
                                    server::run
                            ).and(
                                    () -> aSeq(
                                            () -> action.apply(socketFactory, socketAddress)
                                    ).finallyDo(
                                            server::close
                                    )
                            ).selectValue2();
                        }));
    }

    /**
     * Make handler for the test.
     *
     * @param socketFactory the socket factory
     * @return the handler.
     */
    private AHttpHandler handler(final ASocketFactory socketFactory) {
        final DelegatingHandler delegatingHandler = new DelegatingHandler(new NotFoundHandler());
        delegatingHandler.addDelegate(new DelegatingHandler.PathDelegate(HttpMethodUtil.GET, null, "/hello.html",
                new HelloHandler()));
        delegatingHandler.addDelegate(new DelegatingHandler.PathDelegate(HttpMethodUtil.GET, null, "/",
                new HttpHandlerBase() {
                    @Override
                    public Promise<Void> handle(final HttpExchange exchange) {
                        return ResponseUtil.redirect(exchange, "/hello.html");
                    }
                }));
        delegatingHandler.addDelegate(new DelegatingHandler.PrefixPathDelegate(HttpMethodUtil.GET, null, "/chargen.txt",
                new GetTestHandler()));
        delegatingHandler.addDelegate(new DelegatingHandler.PathDelegate(HttpMethodUtil.POST, null, "/mirror",
                new PostMirrorHandler().export()));
        delegatingHandler.addDelegate(new DelegatingHandler.PathDelegate(HttpMethodUtil.OPTIONS, null, "/echo",
                new EchoUpgradeHandler().export()));
        delegatingHandler.addDelegate(new DelegatingHandler.PathDelegate(HttpMethodUtil.CONNECT, null, null,
                new ConnectHandler(socketFactory)));
        return delegatingHandler;
    }


    @Test
    public void testGet() {
        final String method = HttpMethodUtil.GET;
        final String path = "/chargen.txt?length=100";
        final Map<String, List<String>> headers = Collections.emptyMap();
        final Response value = runRequest(method, path, headers, null, false);
        assertEquals(HttpStatusUtil.OK, value.getStatus());
        assertEquals(100, value.getContent().length);
        assertEquals(HttpStatusUtil.getDefaultText(HttpStatusUtil.OK), value.getMessage());
        assertEquals("100", value.getHeaders().getHeaders(HttpHeadersUtil.CONTENT_LENGTH_HEADER).get(0));
    }


    @Test
    public void testGetChunked() {
        final String method = HttpMethodUtil.GET;
        final String path = "/chargen.txt?length=1000&chunk=30";
        final Map<String, List<String>> headers = Collections.emptyMap();
        final Response value = runRequest(method, path, headers, null, false);
        assertEquals(HttpStatusUtil.OK, value.getStatus());
        assertEquals(1000, value.getContent().length);
        assertEquals(HttpStatusUtil.getDefaultText(HttpStatusUtil.OK), value.getMessage());
        assertTrue(value.getHeaders().getHeaders(HttpHeadersUtil.CONTENT_LENGTH_HEADER).isEmpty());
        assertEquals(ContentUtil.CHUNKED_ENCODING, value.getHeaders().getHeaders(
                HttpHeadersUtil.TRANSFER_ENCODING_HEADER).get(0));
    }


    @Test
    public void testPost() {
        final boolean chunked = false;
        final String method = HttpMethodUtil.POST;
        final String path = "/mirror";
        final Map<String, List<String>> headers = new LinkedHashMap<>(); // NOPMD
        final List<String> contentEncoding = Collections.singletonList(HttpHeadersUtil.CONTENT_TYPE_TEXT_UTF8);
        headers.put(HttpHeadersUtil.CONTENT_TYPE_HEADER, contentEncoding);
        final byte[] requestContent = new byte[10000];
        new Random().nextBytes(requestContent);
        final Response value = runRequest(method, path, headers, requestContent, chunked);
        assertEquals(HttpStatusUtil.OK, value.getStatus());
        assertArrayEquals(requestContent, value.getContent());
        assertEquals(HttpStatusUtil.getDefaultText(HttpStatusUtil.OK), value.getMessage());
        assertEquals("10000", value.getHeaders().getHeaders(HttpHeadersUtil.CONTENT_LENGTH_HEADER).get(0));
        assertEquals(contentEncoding, value.getHeaders().getHeaders(HttpHeadersUtil.CONTENT_TYPE_HEADER));
    }

    @Test
    public void testPostChunked() {
        final boolean chunked = true;
        final String method = HttpMethodUtil.POST;
        final String path = "/mirror";
        final Map<String, List<String>> headers = Collections.emptyMap();
        final byte[] requestContent = new byte[10000];
        new Random().nextBytes(requestContent);
        final Response value = runRequest(method, path, headers, requestContent, chunked);
        assertEquals(HttpStatusUtil.OK, value.getStatus());
        assertArrayEquals(requestContent, value.getContent());
        assertEquals(HttpStatusUtil.getDefaultText(HttpStatusUtil.OK), value.getMessage());
        assertTrue(value.getHeaders().getHeaders(HttpHeadersUtil.CONTENT_LENGTH_HEADER).isEmpty());
        assertEquals(ContentUtil.CHUNKED_ENCODING, value.getHeaders().getHeaders(
                HttpHeadersUtil.TRANSFER_ENCODING_HEADER).get(0));
    }

    /**
     * Http response information class (used to abstract returned results from different sources).
     */
    public static class Response {
        /**
         * The status.
         */
        private final int status;
        /**
         * The message.
         */
        private final String message;
        /**
         * The headers.
         */
        private final HttpHeaders headers;
        /**
         * The content.
         */
        private final byte[] content;

        /**
         * The constructor.
         *
         * @param status  the response code
         * @param message the response message
         * @param headers the response headers
         * @param content the response content
         */
        public Response(final int status, final String message,
                        final HttpHeaders headers, final byte[] content) { // NOPMD
            this.status = status;
            this.message = message;
            this.headers = headers;
            this.content = content;
        }

        /**
         * @return the response status
         */
        public int getStatus() {
            return status;
        }

        /**
         * @return the response message
         */
        public String getMessage() {
            return message;
        }

        /**
         * @return the headers
         */
        public HttpHeaders getHeaders() {
            return headers;
        }

        /**
         * @return the content
         */
        public byte[] getContent() {
            return content; // NOPMD
        }
    }

    /**
     * The request object.
     */
    public static class Request {
        /**
         * The request method.
         */
        private final String method;
        /**
         * The URI.
         */
        private final URI uri;
        /**
         * The headers.
         */
        private final Map<String, List<String>> headers;
        /**
         * The request content.
         */
        private final byte[] requestContent;
        /**
         * The chunked request.
         */
        private final boolean chunked;
        /**
         * The socket factory.
         */
        private final ASocketFactory socketFactory;

        /**
         * @param method         the request method
         * @param uri            the request url
         * @param headers        the headers
         * @param requestContent the content
         * @param chunked        true if the request body should be chunked
         * @param socketFactory  the socket factory
         */
        public Request(final String method, final URI uri, final Map<String, List<String>> headers,
                       final byte[] requestContent, final boolean chunked, final ASocketFactory socketFactory) { //NOPMD
            this.method = method;
            this.uri = uri;
            this.headers = headers;
            this.requestContent = requestContent;
            this.chunked = chunked;
            this.socketFactory = socketFactory;
        }

        /**
         * @return the request method
         */
        public String getMethod() {
            return method;
        }

        /**
         * @return the request uri
         */
        public URI getUri() {
            return uri;
        }

        /**
         * @return the request headers
         */
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        /**
         * @return the request content
         */
        public byte[] getRequestContent() {
            return requestContent; // NOPMD
        }

        /**
         * @return the chunked flag
         */
        public boolean isChunked() {
            return chunked;
        }

        public ASocketFactory getSocketFactory() {
            return socketFactory;
        }
    }
}
