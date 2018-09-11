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

package org.asyncflows.protocol.http.client.core;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.AChannel;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.protocol.http.client.AHttpRequest;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.ObjectExporter;
import org.asyncflows.core.util.RequestQueue;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifySuccess;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;

/**
 * The client connection for HTTP 1.x.
 */
public class HttpClientConnection extends CloseableInvalidatingBase
        implements AHttpConnection, NeedsExport<AHttpConnection> {
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
        return ObjectExporter.export(vat, this);
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
