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

package org.asyncflows.io.net.selector; // NOPMD

import org.asyncflows.core.CoreFlows;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AInputProxyFactory;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.AOutputProxyFactory;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketProxyFactory;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.io.net.SocketUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static org.asyncflows.core.CoreFlows.aBoolean;
import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

/**
 * The selector socket.
 */
class SelectorSocket extends CloseableBase implements ASocket, NeedsExport<ASocket> {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SelectorSocket.class);
    /**
     * The write limit when copying to direct buffer.
     */
    private static final int WRITE_LIMIT = 4096;
    /**
     * The channel context.
     */
    private final ChannelContext channelContext;
    /**
     * The socket channel.
     */
    private final SocketChannel socketChannel;
    /**
     * The requests.
     */
    private final RequestQueue requests = new RequestQueue();
    /**
     * The socket output.
     */
    private final SocketInput input = new SocketInput();
    /**
     * The socket input.
     */
    private final SocketOutput output = new SocketOutput();

    /**
     * The constructor for the socket.
     *
     * @param selector the context selector
     * @throws IOException if there is an IO problem
     */
    public SelectorSocket(final Selector selector) throws IOException {
        this(selector, SocketChannel.open());
    }

    /**
     * The constructor for the socket.
     *
     * @param selector      the context selector
     * @param socketChannel the socket channel
     * @throws IOException if there is an IO problem
     */
    public SelectorSocket(final Selector selector, final SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        this.channelContext = new ChannelContext(socketChannel, selector);
    }

    @Override
    public Promise<Void> setOptions(final SocketOptions options) {
        try {
            ensureOpen();
            if (options != null) {
                SocketUtil.applyOptions(socketChannel.socket(), options);
            }
            return aVoid();
        } catch (SocketException ex) {
            return aFailure(ex);
        }
    }

    @Override
    public Promise<Void> connect(final SocketAddress address) {
        return requests.run(() -> {
            final boolean b = socketChannel.connect(address);
            if (b) {
                return aVoid();
            }
            return aSeqWhile(() -> {
                ensureOpen();
                if (socketChannel.finishConnect()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Socket connected: " + socketChannel.socket().getLocalSocketAddress()
                                + " -> " + socketChannel.socket().getRemoteSocketAddress());
                    }
                    return aFalse();
                }
                return channelContext.waitForConnect();
            });
        });
    }

    @Override
    public Promise<SocketAddress> getRemoteAddress() {
        return aValue(socketChannel.socket().getRemoteSocketAddress());
    }

    @Override
    public Promise<SocketAddress> getLocalAddress() {
        return aValue(socketChannel.socket().getLocalSocketAddress());
    }

    @Override
    public Promise<AInput<ByteBuffer>> getInput() {
        return aValue(input.export());
    }

    @Override
    public Promise<AOutput<ByteBuffer>> getOutput() {
        return aValue(output.export());
    }

    @Override
    public ASocket export(final Vat vat) {
        return ASocketProxyFactory.createProxy(vat, this);
    }

    @Override
    protected Promise<Void> closeAction() {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Closing socket: " + socketChannel.socket().getLocalSocketAddress()
                        + " -> " + socketChannel.socket().getRemoteSocketAddress());
            }
            socketChannel.close();
            return super.closeAction();
        } catch (IOException e) {
            return aFailure(e);
        } finally {
            channelContext.close();
        }
    }

    /**
     * The socket input stream.
     */
    private class SocketInput extends CloseableBase implements AInput<ByteBuffer>, NeedsExport<AInput<ByteBuffer>> {
        /**
         * Limit for zero reads.
         */
        public static final int ZERO_COUNT_LIMIT = 8;
        /**
         * Requests for the stream.
         */
        private final RequestQueue requests = new RequestQueue();
        /**
         * If true, eof has been seen.
         */
        private boolean eofSeen;

        @Override
        public Promise<Integer> read(final ByteBuffer buffer) {
            return requests.runSeqUntilValue(new ASupplier<Maybe<Integer>>() {
                private boolean firstTime = true;
                private int zeroCount;

                @Override
                public Promise<Maybe<Integer>> get() throws Exception {
                    if (eofSeen) {
                        return IOUtil.EOF_MAYBE_PROMISE;
                    }
                    if (buffer.remaining() == 0) {
                        return aMaybeValue(0);
                    }
                    ByteBuffer readBuffer;
                    if (!buffer.isDirect()) {
                        readBuffer = channelContext.getDirect();
                        readBuffer.clear();
                        readBuffer.limit(Math.min(buffer.remaining(), readBuffer.capacity()));
                    } else {
                        readBuffer = buffer;
                    }
                    try {
                        try {
                            final int read = socketChannel.read(readBuffer);
                            if (read < 0) {
                                eofSeen = true;
                                return IOUtil.EOF_MAYBE_PROMISE;
                            } else if (read > 0) {
                                if (!buffer.isDirect()) {
                                    readBuffer.flip();
                                    buffer.put(readBuffer);
                                }
                                return aMaybeValue(read);
                            }
                            zeroCount++;
                            if (zeroCount > ZERO_COUNT_LIMIT) {
                                zeroCount = 0;
                                // Workaround for the Java NIO bug on Windows 7
                                // When localhost connection on windows host happens with the same JVM, sometimes
                                // a wrong end of the connection is notified as ready for the read. In that case
                                // opening a new selector and re-registering keys is required.
                                //
                                // This scenario so far had happened during the testing of socket layer.
                                channelContext.changeSelector();
                            }
                        } catch (IOException ex) {
                            if (firstTime) {
                                firstTime = false;
                            } else {
                                return aFailure(ex);
                            }
                        }
                        return channelContext.waitForRead();
                    } finally {
                        if (!buffer.isDirect()) {
                            channelContext.releaseDirect(readBuffer);
                        }
                    }
                }
            });
        }

        @Override
        public AInput<ByteBuffer> export(final Vat vat) {
            return AInputProxyFactory.createProxy(vat, this);
        }

        @Override
        protected Promise<Void> closeAction() {
            try {
                socketChannel.socket().shutdownInput();
                return aVoid();
            } catch (ClosedChannelException e) {
                return aVoid();
            } catch (IOException e) {
                return aFailure(e);
            }
        }
    }

    /**
     * The socket output stream.
     */
    private class SocketOutput extends CloseableBase implements AOutput<ByteBuffer>, NeedsExport<AOutput<ByteBuffer>> {
        /**
         * Requests for the stream.
         */
        private final RequestQueue requests = new RequestQueue();

        @Override
        public Promise<Void> write(final ByteBuffer buffer) {
            return requests.runSeqWhile(new ASupplier<Boolean>() {
                private boolean firstRun = true;

                @Override
                public Promise<Boolean> get() throws Exception {
                    if (buffer.remaining() == 0) {
                        return aFalse();
                    }
                    ByteBuffer writeBuffer;
                    if (buffer.isDirect()) {
                        writeBuffer = buffer;
                    } else {
                        writeBuffer = channelContext.getDirect();
                        writeBuffer.clear();
                        writeBuffer.limit(Math.min(writeBuffer.limit(), WRITE_LIMIT));
                        final int p = buffer.position();
                        BufferOperations.BYTE.put(writeBuffer, buffer);
                        buffer.position(p);
                        writeBuffer.flip();
                    }
                    try {
                        try {
                            final int write = socketChannel.write(writeBuffer);
                            if (write > 0) {
                                if (!buffer.isDirect()) {
                                    buffer.position(write + buffer.position());
                                }
                                if (!buffer.hasRemaining()) {
                                    return aFalse();
                                }
                                if (!writeBuffer.hasRemaining()) {
                                    return aTrue();
                                }
                                firstRun = true;
                                return aBoolean(buffer.hasRemaining());
                            } else {
                                return channelContext.waitForWrite();
                            }
                        } catch (IOException ex) {
                            if (firstRun) {
                                // On the first run of write operation it is possible that an Exception is thrown
                                // instead of returning zero. In that case, the operation will be repeated until
                                // after socket is ready for write. This happens on windows platform.
                                firstRun = false;
                                return channelContext.waitForWrite();
                            } else {
                                throw ex;
                            }
                        }
                    } finally {
                        if (!buffer.isDirect()) {
                            channelContext.releaseDirect(writeBuffer);
                        }
                    }
                }
            });
        }

        @Override
        public Promise<Void> flush() {
            // data is sent as soon as it is possible, so the method is just waiting
            // until previous requests are processed.
            return requests.run(CoreFlows::aVoid);
        }

        @Override
        public AOutput<ByteBuffer> export() {
            return export(Vat.current());
        }

        @Override
        public AOutput<ByteBuffer> export(final Vat vat) {
            return AOutputProxyFactory.createProxy(vat, this);
        }

        @Override
        protected Promise<Void> closeAction() {
            try {
                socketChannel.socket().shutdownOutput();
                return aVoid();
            } catch (ClosedChannelException e) {
                return aVoid();
            } catch (IOException e) {
                return aFailure(e);
            }
        }
    }
}
