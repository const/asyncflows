/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

package org.asyncflows.io.net.async;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.adapters.nio.ByteChannelAdapter;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketProxyFactory;
import org.asyncflows.io.net.SocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import static org.asyncflows.core.AsyncContext.aDaemonOneWay;
import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.io.adapters.nio.AsyncNioFlows.aCompletionHandler;

class AsyncSocket extends ByteChannelAdapter<AsynchronousSocketChannel> implements ASocket, NeedsExport<ASocket> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncSocket.class);
    private Integer timeoutMillis;

    /**
     * The constructor from channel.
     *
     * @param channel the channel
     */
    AsyncSocket(AsynchronousSocketChannel channel) throws IOException {
        super(channel);
        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
    }

    /**
     * This method should e overridden if the channel needs to support half-close.
     *
     * @return when complete
     */
    @Override
    protected Promise<Void> closeInput() {
        return aNow(() -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("%s Shutting down input", channelId()));
            }
            return aDaemonOneWay(channel::shutdownInput);
        });
    }

    /**
     * This method should e overridden if the channel needs to support half-close.
     *
     * @return when complete
     */
    @Override
    protected Promise<Void> closeOutput() {
        return aNow(() -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("%s Shutting down output", channelId()));
            }
            return aDaemonOneWay(channel::shutdownOutput);
        });
    }

    @Override
    public ASocket export(Vat vat) {
        return ASocketProxyFactory.createProxy(vat, this);
    }

    /**
     * Set socket options.
     *
     * @param options the options to set
     * @return when options are set
     */
    @Override
    public Promise<Void> setOptions(SocketOptions options) {
        return aNow(() -> {
            applyOption(options.getBroadcast(), StandardSocketOptions.SO_BROADCAST);
            applyOption(options.getKeepAlive(), StandardSocketOptions.SO_KEEPALIVE);
            applyOption(options.getLinger(), StandardSocketOptions.SO_LINGER);
            applyOption(options.getReceiveBufferSize(), StandardSocketOptions.SO_RCVBUF);
            applyOption(options.getSendBufferSize(), StandardSocketOptions.SO_SNDBUF);
            applyOption(options.getReuseAddress(), StandardSocketOptions.SO_REUSEADDR);
            applyOption(options.getTpcNoDelay(), StandardSocketOptions.TCP_NODELAY);
            applyOption(options.getTrafficClass(), StandardSocketOptions.IP_TOS);
            timeoutMillis = options.getTimeout();
            return aVoid();
        });
    }

    private <T> void applyOption(T value, SocketOption<T> option) throws IOException {
        if (value != null) {
            channel.setOption(option, value);
        }
    }

    /**
     * Read from channel.
     *
     * @param buffer buffer
     * @param r      resolver
     * @param h      completion handler
     */
    @Override
    @SuppressWarnings("squid:S3358")
    protected void readFromChannel(ByteBuffer buffer, AResolver<Integer> r, CompletionHandler<Integer, AResolver<Integer>> h) {
        AResolver<Integer> tr = !LOGGER.isDebugEnabled() ? r : o -> {
            if (o.isSuccess() && LOGGER.isDebugEnabled()) {
                LOGGER.debug(channelId() + (o.value() == -1 ? " EOF reached on input" : " read " + o.value() + " bytes"));
            }
            r.resolve(o);
        };
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("%s starting read %d of %d", channelId(), buffer.remaining(), buffer.capacity()));
        }
        if (timeoutMillis == null) {
            super.readFromChannel(buffer, tr, h);
        } else {
            channel.read(buffer, timeoutMillis, TimeUnit.MILLISECONDS, tr, h);
        }
    }

    /**
     * Write to channel.
     *
     * @param buffer buffer
     * @param r      resolver
     * @param h      completion handler
     */
    @Override
    @SuppressWarnings({"squid:S1854", "squid:S1481"})
    protected void writeToChannel(ByteBuffer buffer, AResolver<Integer> r, CompletionHandler<Integer, AResolver<Integer>> h) {
        final int initial = buffer.remaining();
        AResolver<Integer> tr = !LOGGER.isDebugEnabled() ? r : o -> {
            if (o.isSuccess() && LOGGER.isDebugEnabled()) {
                LOGGER.debug(channelId() + " data written " + initial + " -> " + buffer.remaining() + " (" + o.value() + ")");
            }
            r.resolve(o);
        };
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("%s starting write %d of %d", channelId(), buffer.remaining(), buffer.capacity()));
        }
        if (timeoutMillis == null) {
            super.writeToChannel(buffer, tr, h);
        } else {
            channel.write(buffer, timeoutMillis, TimeUnit.MILLISECONDS, tr, h);
        }
    }

    /**
     * Connect to the remote address.
     *
     * @param address the address to connect to
     * @return when connected
     */
    @Override
    public Promise<Void> connect(SocketAddress address) {
        final Promise<Void> connectionResult = aCompletionHandler((r, c) -> channel.connect(address, r, c));
        if (LOGGER.isDebugEnabled()) {
            connectionResult.listenSync(o -> {
                if (LOGGER.isDebugEnabled()) {
                    if (o.isSuccess()) {
                        LOGGER.debug(String.format("%s connected", channelId()));
                    } else {
                        LOGGER.debug(String.format("%s failed to connect", address), o.failure());
                    }
                }
            });
        }
        return connectionResult;
    }

    @Override
    public Promise<SocketAddress> getRemoteAddress() {
        try {
            return aValue(channel.getRemoteAddress());
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<SocketAddress> getLocalAddress() {
        try {
            return aValue(channel.getLocalAddress());
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    String channelId() {
        try {
            return channel.getLocalAddress() + "->" + channel.getRemoteAddress();
        } catch (IOException e) {
            return "<not connected>";
        }
    }
}
