package net.sf.asyncobjects.protocol.http.client;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.protocol.http.common.Scope;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;

import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * The HTTP request interface.
 *
 * @see net.sf.asyncobjects.protocol.http.common.HttpScopeUtil for addtional keys.
 */
public interface AHttpRequest extends ACloseable {
    // TODO additional transfer encodings
    /**
     * The continue listener. If it the version of the request is 1.1, then the listener is notified when the
     * continue intermediate reply is received. Note that this reply might never be received in case if the server
     * does not understand this header, so the client should watch some timer to check for the response.
     */
    Scope.Key<AResolver<Void>> CONTINUE_LISTENER
            = new Scope.Key<AResolver<Void>>(AHttpRequest.class, "continueListener");

    /**
     * Set this key on the scope, if the request should connect to the host, that is different from the host specified
     * by URL.
     */
    Scope.Key<String> CONNECTION_HOST = new Scope.Key<String>(AHttpRequest.class, "connectionHost");
    /**
     * The constant meaning no-content.
     */
    long NO_CONTENT = -1L;

    /**
     * @return the remote address for the socket (hop only)
     */
    Promise<SocketAddress> getRemoteAddress();

    /**
     * @return the remote address for the socket (hop only)
     */
    Promise<SocketAddress> getLocalAddress();

    /**
     * Connect the specified server.
     *
     * @param scope   the scope that is used to provide additional parameters to the request .
     * @param method  the request method
     * @param uri     the url to connect to
     * @param headers the request headers
     * @param length  the content length or null ({@link #NO_CONTENT value means that there is no content})
     * @return a promise for the output stream associated with the request.
     */
    Promise<AOutput<ByteBuffer>> request(Scope scope, String method, URI uri,
                                         HttpHeaders headers, Long length);

    /**
     * @return the promise for HTTP response, when response type becomes finally known.
     */
    Promise<HttpResponse> getResponse();
}
