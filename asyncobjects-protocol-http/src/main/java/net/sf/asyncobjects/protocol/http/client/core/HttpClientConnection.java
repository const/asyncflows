package net.sf.asyncobjects.protocol.http.client.core;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.util.ReflectionExporter;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.util.ByteGeneratorContext;
import net.sf.asyncobjects.nio.util.ByteParserContext;
import net.sf.asyncobjects.protocol.http.client.AHttpRequest;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;

/**
 * The client connection for HTTP 1.x.
 */
public class HttpClientConnection extends CloseableInvalidatingBase
        implements AHttpConnection, ExportsSelf<AHttpConnection> {
    // TODO connection ids, propagate to requests
    /**
     * The protocol.
     */
    private final String protocol;
    /**
     * The host to which this connection is connected.
     */
    private final String host;
    /**
     * The connection output.
     */
    private final ByteGeneratorContext output;
    /**
     * The connection input.
     */
    private final ByteParserContext input;
    /**
     * The request queue.
     */
    private final RequestQueue requestQueue = new RequestQueue();
    /**
     * The channel.
     */
    private final AChannel<ByteBuffer> channel;
    /**
     * The user agent.
     */
    private final String userAgent;
    /**
     * Remote address for the hop.
     */
    private final SocketAddress remoteAddress;
    /**
     * Local address for the hop.
     */
    private final SocketAddress localAddress;
    /**
     * True if finished.
     */
    private boolean finished;
    /**
     * The current action.
     */
    private HttpClientAction current;


    /**
     * The constructor.
     *
     * @param protocol      the protocol for the connection.
     * @param host          the host to which connection is created
     * @param output        the output
     * @param input         the input
     * @param channel       the channel that is closed when connection is closed.
     * @param userAgent     the user agent.
     * @param remoteAddress the remote address of the hop
     * @param localAddress  the local address for the hop
     */
    // CHECKSTYLE:OFF
    public HttpClientConnection(final String protocol, final String host, final ByteGeneratorContext output,
                                final ByteParserContext input,
                                final AChannel<ByteBuffer> channel, final String userAgent,
                                final SocketAddress remoteAddress, final SocketAddress localAddress) {
        // CHECKSTYLE:ON
        this.protocol = protocol;
        this.host = host;
        this.output = output;
        this.input = input;
        this.channel = channel;
        this.userAgent = userAgent;
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
    }

    /**
     * @return the connection host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the output
     */
    public ByteGeneratorContext getOutput() {
        return output;
    }

    /**
     * @return the input
     */
    public ByteParserContext getInput() {
        return input;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return the user agent.
     */
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public Promise<Maybe<AHttpRequest>> next() {
        try {
            ensureValidAndOpen();
            final Promise<Maybe<AHttpRequest>> promise = new Promise<>();
            requestQueue.run(() -> {
                if (finished) {
                    notifySuccess(promise.resolver(), Maybe.<AHttpRequest>empty());
                    return aVoid();
                } else {
                    current = new HttpClientAction(HttpClientConnection.this);
                    notifySuccess(promise.resolver(), Maybe.value(current.export()));
                    return aSeq(
                            current::finish
                    ).map(value -> {
                        finished = !value;
                        return aVoid();
                    }).finallyDo(() -> {
                        current = null;
                        return aVoid();
                    });
                }
            });
            return promise;
        } catch (final Throwable throwable) {
            return aFailure(throwable);
        }
    }

    @Override
    protected Promise<Void> closeAction() {
        return aSeq(() -> {
            if (current == null) {
                return aVoid();
            } else {
                return current.close();
            }
        }).thenDo(
                () -> output.send().toVoid()
        ).thenDo(
                () -> aAll(
                        () -> output.getOutput().close()
                ).andLast(
                        () -> input.input().close()
                ).toVoid()
        ).finallyDo(channel::close);
    }

    @Override
    public AHttpConnection export() {
        return export(Vat.current());
    }

    @Override
    public AHttpConnection export(final Vat vat) {
        return ReflectionExporter.export(vat, this);
    }

    @Override
    public Promise<SocketAddress> getRemoteAddress() {
        return aValue(remoteAddress);
    }

    @Override
    public Promise<SocketAddress> getLocalAddress() {
        return aValue(localAddress);
    }
}
