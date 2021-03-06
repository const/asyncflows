/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

package org.asyncflows.io.net.selector;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.net.ADatagramSocket;
import org.asyncflows.io.net.ADatagramSocketProxyFactory;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.io.net.SocketUtil;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The datagram socket for the selector vat.
 */
class SelectorDatagramSocket extends CloseableBase implements ADatagramSocket, ExportableComponent<ADatagramSocket> {
    /**
     * Amount unready receives without result.
     */
    private static final int BROKEN_SELECT_LIMIT = 5;
    /**
     * The queue for receive operations.
     */
    private final RequestQueue receives = new RequestQueue();
    /**
     * The queue for send operations.
     */
    private final RequestQueue sends = new RequestQueue();
    /**
     * The socket datagram channel.
     */
    private final DatagramChannel datagramChannel;
    /**
     * The datagramChannel context.
     */
    private final ChannelContext channelContext;

    /**
     * The constructor.
     *
     * @param selector the selector
     * @throws IOException if problem registering datagram channel
     */
    SelectorDatagramSocket(final Selector selector) throws IOException {
        this(DatagramChannel.open(), selector);
    }


    /**
     * The constructor.
     *
     * @param datagramChannel the datagram channel
     * @param selector        the selector
     * @throws IOException if problem registering datagram channel
     */
    SelectorDatagramSocket(final DatagramChannel datagramChannel, final Selector selector) throws IOException {
        this.datagramChannel = datagramChannel;
        datagramChannel.configureBlocking(false);
        this.channelContext = new ChannelContext(datagramChannel, selector);
    }

    @Override
    public Promise<Void> setOptions(final SocketOptions options) {
        try {
            SocketUtil.applyOptions(datagramChannel.socket(), options);
            return aVoid();
        } catch (SocketException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> connect(final SocketAddress address) {
        try {
            datagramChannel.connect(address);
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> disconnect() {
        try {
            datagramChannel.disconnect();
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<SocketAddress> getRemoteAddress() {
        return aValue(datagramChannel.socket().getRemoteSocketAddress());
    }

    @Override
    public Promise<SocketAddress> getLocalAddress() {
        return aValue(datagramChannel.socket().getLocalSocketAddress());
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address) {
        try {
            datagramChannel.socket().bind(address);
            return getLocalAddress();
        } catch (SocketException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> send(final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("Empty datagrams are not supported "
                    + "(it is impossible to distinguish not sent and empty)!");
        }
        return sends.runSeqWhile(() -> {
            final int write = datagramChannel.write(buffer);
            if (write != 0) {
                return aFalse();
            }
            return channelContext.waitForWrite();
        });
    }

    @Override
    public Promise<Void> send(final SocketAddress address, final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("Empty datagrams are not supported "
                    + "(it is impossible to distinguish not sent and empty)!");
        }
        return sends.runSeqWhile(() -> {
            final int write = datagramChannel.send(buffer, address);
            if (write != 0) {
                return aFalse();
            }
            return channelContext.waitForWrite();
        });
    }

    @Override
    public Promise<SocketAddress> receive(final ByteBuffer buffer) {
        final int[] count = new int[1];
        return receives.runSeqUntilValue(() -> {
            final SocketAddress address = datagramChannel.receive(buffer);
            if (address != null) {
                return aMaybeValue(address);
            } else {
                count[0]++;
                if (count[0] >= BROKEN_SELECT_LIMIT) {
                    count[0] = 0;
                    channelContext.changeSelector();
                }
                return channelContext.waitForRead();
            }
        });
    }

    @Override
    protected Promise<Void> closeAction() {
        try {
            datagramChannel.close();
            return super.closeAction();
        } catch (IOException e) {
            return aFailure(e);
        } finally {
            channelContext.close();
        }
    }

    @Override
    public ADatagramSocket export(final Vat vat) {
        return ADatagramSocketProxyFactory.createProxy(vat, this);
    }
}
