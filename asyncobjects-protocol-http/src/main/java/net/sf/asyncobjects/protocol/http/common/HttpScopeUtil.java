package net.sf.asyncobjects.protocol.http.common;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeadersUtil;
import net.sf.asyncobjects.protocol.http.common.headers.TransferEncoding;

import javax.net.ssl.SSLSession;
import java.util.List;

import static net.sf.asyncobjects.core.CoreFunctionUtil.constantCallable;

/**
 * The common keys used for client and server to indicate specific protocol features.
 */
public final class HttpScopeUtil {
    /**
     * Set this key to true on the exchange scope before responding in order to ensure that this exchange is
     * the last on the connection. Usually there is no reason to force closing connection, since the container
     * does it automatically when appropriate.
     */
    public static final Scope.Key<Boolean> LAST_EXCHANGE = new Scope.Key<>(HttpScopeUtil.class, "lastExchange", false);
    /**
     * The key used to force a specific version of http protocol. In practice it could be used only to force HTTP/1.0,
     * since for HTTP/2.0 other ways are used.
     */
    public static final Scope.Key<String> FORCE_VERSION = new Scope.Key<>(HttpScopeUtil.class, "forceVersion");
    /**
     * The names of trailers.
     */
    public static final Scope.Key<List<String>> TRAILERS_NAMES = new Scope.Key<>(HttpScopeUtil.class, "trailersNames");
    /**
     * The provider for trailers.
     */
    public static final Scope.Key<ACallable<HttpHeaders>> TRAILERS_PROVIDER = new Scope.Key<>(
            HttpScopeUtil.class, "trailersProvider");
    /**
     * The SSL session.
     */
    public static final Scope.Key<SSLSession> SSL_SESSION = new Scope.Key<>(HttpScopeUtil.class, "sslSession");

    /**
     * The private constructor.
     */
    private HttpScopeUtil() {

    }

    /**
     * Get trailers provider basing on the scope.
     *
     * @param scope     the scope
     * @param headers   the headers
     * @param encodings the encodings
     * @return the trailers provider
     */
    public static ACallable<HttpHeaders> trailersProvider(final Scope scope,
                                                          final HttpHeaders headers,
                                                          final List<TransferEncoding> encodings) {
        if (TransferEncoding.isChunked(encodings)) {
            final ACallable<HttpHeaders> provider = scope.get(TRAILERS_PROVIDER);
            if (provider != null) {
                final List<String> names = scope.get(TRAILERS_NAMES);
                if (names != null) { // NOPMD
                    headers.setHeader(HttpHeadersUtil.TRAILER_HEADER, HttpHeadersUtil.listValue(names));
                }
                return provider;
            }
        }
        return constantCallable(null);
    }

}
