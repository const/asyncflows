package net.sf.asyncobjects.nio.net.selector;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.CloseableBase;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.BufferOperations;
import net.sf.asyncobjects.nio.NIOExportUtil;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.SocketExportUtil;
import net.sf.asyncobjects.nio.net.SocketOptions;
import net.sf.asyncobjects.nio.net.SocketUtil;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static net.sf.asyncobjects.core.AsyncControl.aBoolean;
import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoopFair;

/**
 * The selector socket.
 */
class SelectorSocket extends CloseableBase implements ASocket, ExportsSelf<ASocket> {
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
        return requests.run(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                final boolean b = socketChannel.connect(address);
                if (b) {
                    return aVoid();
                }
                return aSeqLoopFair(new ACallable<Boolean>() {
                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        if (socketChannel.finishConnect()) {
                            return aFalse();
                        }
                        return channelContext.waitForConnect().thenValue(true);
                    }
                });
            }
        });
    }

    @Override
    public Promise<SocketAddress> getRemoteAddress() {
        return aValue(socketChannel.socket().getRemoteSocketAddress());
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
    public ASocket export() {
        return export(Vat.current());
    }

    @Override
    public ASocket export(final Vat vat) {
        return SocketExportUtil.export(vat, this);
    }

    @Override
    protected Promise<Void> closeAction() {
        try {
            socketChannel.close();
            return super.closeAction();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    /**
     * The socket input stream.
     */
    private class SocketInput extends CloseableBase implements AInput<ByteBuffer>, ExportsSelf<AInput<ByteBuffer>> {
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
            return requests.runSeqMaybeLoop(new ACallable<Maybe<Integer>>() {
                private boolean firstTime = true;
                private int zeroCount;

                @Override
                public Promise<Maybe<Integer>> call() throws Throwable {
                    if (eofSeen) {
                        return aMaybeValue(-1);
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
                                return aMaybeValue(-1);
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
                        return channelContext.waitForRead().thenDo(new ACallable<Maybe<Integer>>() {
                            @Override
                            public Promise<Maybe<Integer>> call() throws Throwable {
                                return aMaybeEmpty();
                            }
                        });
                    } finally {
                        if (!buffer.isDirect()) {
                            channelContext.releaseDirect(readBuffer);
                        }
                    }
                }
            });
        }

        @Override
        public AInput<ByteBuffer> export() {
            return export(Vat.current());
        }

        @Override
        public AInput<ByteBuffer> export(final Vat vat) {
            return NIOExportUtil.export(vat, this);
        }

        @Override
        protected Promise<Void> closeAction() {
            try {
                socketChannel.socket().shutdownInput();
                return aVoid();
            } catch (IOException e) {
                return aFailure(e);
            }
        }
    }

    /**
     * The socket output stream.
     */
    private class SocketOutput extends CloseableBase implements AOutput<ByteBuffer>, ExportsSelf<AOutput<ByteBuffer>> {
        /**
         * Requests for the stream.
         */
        private final RequestQueue requests = new RequestQueue();

        @Override
        public Promise<Void> write(final ByteBuffer buffer) {
            return requests.runSeqLoop(new ACallable<Boolean>() {
                private boolean firstRun = true;

                @Override
                public Promise<Boolean> call() throws Throwable {
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
                                return channelContext.waitForWrite().thenValue(true);
                            }
                        } catch (IOException ex) {
                            if (firstRun) {
                                // On the first run of write operation it is possible that an Exception is thrown
                                // instead of returning zero. In that case, the operation will be repeated until
                                // after socket is ready for write. This happens on windows platform.
                                firstRun = false;
                                return channelContext.waitForWrite().thenValue(true);
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
            return requests.run(new ACallable<Void>() {
                @Override
                public Promise<Void> call() throws Throwable {
                    return aVoid();
                }
            });
        }

        @Override
        public AOutput<ByteBuffer> export() {
            return export(Vat.current());
        }

        @Override
        public AOutput<ByteBuffer> export(final Vat vat) {
            return NIOExportUtil.export(vat, this);
        }

        @Override
        protected Promise<Void> closeAction() {
            try {
                socketChannel.socket().shutdownOutput();
                return aVoid();
            } catch (IOException e) {
                return aFailure(e);
            }
        }
    }
}
