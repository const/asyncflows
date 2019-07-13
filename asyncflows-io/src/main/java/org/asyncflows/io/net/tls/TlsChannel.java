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

package org.asyncflows.io.net.tls; // NOPMD

import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AInputProxyFactory;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.AOutputProxyFactory;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.ChainedClosable;
import org.asyncflows.core.util.RequestQueue;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.asyncflows.core.AsyncContext.aDaemonRun;
import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;
import static org.asyncflows.core.function.AsyncFunctionUtil.promiseSupplier;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;

/**
 * Implementation of channel semantics over {@link SSLEngine}.
 *
 * @param <T> the wrapped channel type
 */
public class TlsChannel<T extends AChannel<ByteBuffer>> extends ChainedClosable<T> implements AChannel<ByteBuffer> {
    // TODO consider ByteGeneratorContext, ByteParserContext
    /**
     * The always empty buffer.
     */
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);
    /**
     * The tasks.
     */
    private final RequestQueue tasks = new RequestQueue();
    /**
     * The configured SSL engine.
     */
    private SSLEngine engine;
    /**
     * The SSL input.
     */
    private SSLInput input;
    /**
     * The SSL output.
     */
    private SSLOutput output;
    /**
     * The current handshake.
     */
    private Promise<Void> currentHandshake;
    /**
     * The network data, that was fetched from input before it was decided to proceed with SSL. It might
     * happen with protocols that detect SSL based on stream format or for protocols that might switch to SSL
     * in the middle like BEEP or HTTP/1.1 SSL upgrade.
     */
    private ByteBuffer netData;
    /**
     * When network data finishes to be consumed, the resolver is notified. The resolver might be notified
     * with null in case of the problem.
     */
    private AResolver<Void> netDataResolver;
    /**
     * The invalidation observer.
     */
    private final AResolver<Object> invalidationObserver = resolution -> {
        if (!resolution.isSuccess()) {
            invalidateChannel(resolution.failure());
        }
    };

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     */
    protected TlsChannel(final T wrapped) {
        super(wrapped);
    }

    /**
     * Invalidate channel with the problem, so most of the operations on it will fail.
     *
     * @param t the problem
     */
    protected final void invalidateChannel(final Throwable t) {
        if (!isValid()) {
            return;
        }
        invalidate(t);
        if (netDataResolver != null) {
            notifyFailure(netDataResolver, invalidation());
            netDataResolver = null;
        }
        if (currentHandshake != null) {
            notifyFailure(currentHandshake.resolver(), invalidation());
            currentHandshake = null;
        }
        if (input != null) {
            input.invalidateInput(t);
        }
        if (output != null) {
            output.invalidateOutput(t);
        }
    }

    /**
     * Supply prefetched net data to the channel. The method should be called before
     * {@link #init(SSLEngine)}.
     *
     * @param buffer the buffer with data
     * @return the promise that is resolved when data is consumed
     */
    public final Promise<Void> supplyNetData(final ByteBuffer buffer) {
        if (engine != null) {
            throw new IllegalStateException("The channel is already initialized, supply net data before init() method");
        }
        if (buffer == null) {
            throw new IllegalArgumentException("The buffer is null");
        }
        final Promise<Void> rc = new Promise<>();
        netData = buffer;
        netDataResolver = rc.resolver();
        return rc;
    }

    /**
     * Initialize TlsChannel with input and output streams.
     *
     * @param initEngine the configured {@link SSLEngine}
     * @return the promise that resolves when initialization finishes
     */
    public final Promise<Void> init(final SSLEngine initEngine) {
        if (!isValidAndOpen()) {
            return invalidationPromise();
        }
        if (this.engine != null) {
            throw new IllegalStateException("The channel is already initialized");
        }
        this.engine = initEngine;
        return aAll(promiseSupplier(wrapped.getInput())).and(promiseSupplier(wrapped.getOutput())).map(
                (i, o)-> {
                    input = new SSLInput(i);
                    output = new SSLOutput(o);
                    doTasks();
                    return aVoid();
                }).listen(invalidationObserver);
    }

    /**
     * Loop that walks over the tasks.
     */
    public void doTasks() {
        tasks.runSeqWhile(() -> {
            ensureValid();
            if (!isOpen()) {
                return aFalse();
            }
            final SSLEngineResult.HandshakeStatus handshakeStatus = engine.getHandshakeStatus();
            switch (handshakeStatus) {
                case FINISHED:
                case NOT_HANDSHAKING:
                    ensureHandshakeEnded();
                    input.unwraps.resume();
                    output.wraps.resume();
                    return tasks.suspendThenTrue();
                case NEED_TASK:
                    ensureHandshakeStarted();
                    final Runnable delegatedTask = engine.getDelegatedTask();
                    if (delegatedTask == null) {
                        throw new IllegalStateException("No task to run");
                    }
                    return aDaemonRun(delegatedTask).thenValue(true);
                case NEED_WRAP:
                    ensureHandshakeStarted();
                    output.wraps.resume();
                    return tasks.suspendThenTrue();
                case NEED_UNWRAP:
                    ensureHandshakeStarted();
                    input.unwraps.resume();
                    return tasks.suspendThenTrue();
                default:
                    throw new IllegalStateException("Unknown handshake status: " + handshakeStatus);
            }
        }).listen(invalidationObserver);
    }

    @Override
    protected Promise<Void> beforeClose() {
        output.wraps.resume();
        output.writes.resume();
        input.unwraps.resume();
        input.reads.resume();
        tasks.resume();
        engine.closeOutbound();
        return aAll(promiseSupplier(input.unwrapsDone)).andLast(promiseSupplier(output.wrapsDone)).toVoid();
    }

    /**
     * Ensure that handshake has ended, if it was in the progress.
     */
    private void ensureHandshakeEnded() {
        if (currentHandshake != null) {
            output.wraps.resume();
            input.unwraps.resume();
            if (isValid()) {
                notifySuccess(currentHandshake.resolver(), null);
            } else {
                notifyFailure(currentHandshake.resolver(), invalidation());
            }
            currentHandshake = null;
        }
    }

    /**
     * Ensure that handshake is started.
     */
    private void ensureHandshakeStarted() {
        if (isValidAndOpen() && currentHandshake == null) {
            currentHandshake = new Promise<>();
            final SSLEngineResult.HandshakeStatus status = engine.getHandshakeStatus();
            if (status == SSLEngineResult.HandshakeStatus.FINISHED
                    || status == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                try {
                    engine.beginHandshake();
                } catch (Throwable e) {
                    invalidateChannel(e);
                }
            }
        }
    }

    /**
     * @return the result of handshake operation
     */
    public Promise<Void> doHandshake() {
        if (!isValidAndOpen()) {
            return invalidationPromise();
        }
        ensureHandshakeStarted();
        if (currentHandshake != null) {
            return currentHandshake;
        }
        if (!isValidAndOpen()) {
            return invalidationPromise();
        }
        return aFailure(new IllegalStateException("BUG: Something strange with handshake"));
    }

    @Override
    public Promise<AInput<ByteBuffer>> getInput() {
        if (!isValidAndOpen()) {
            return invalidationPromise();
        }
        if (input == null) {
            return aFailure(new IOException("Not connected yet"));
        }
        return aValue(AInputProxyFactory.createProxy(Vat.current(), input));
    }

    @Override
    public Promise<AOutput<ByteBuffer>> getOutput() {
        if (!isValidAndOpen()) {
            return invalidationPromise();
        }
        if (output == null) {
            return aFailure(new IOException("Not connected yet"));
        }
        return aValue(AOutputProxyFactory.createProxy(Vat.current(), output));
    }

    /**
     * @return the SSL session
     */
    public Promise<SSLSession> getSession() {
        return aValue(engine.getSession());
    }

    /**
     * @return the engine
     */
    protected SSLEngine getEngine() {
        return engine;
    }

    /**
     * The SSL input.
     */
    private final class SSLInput extends ChainedClosable<AInput<ByteBuffer>> implements AInput<ByteBuffer> {
        /**
         * The read requests.
         */
        private final RequestQueue reads = new RequestQueue();
        /**
         * The unwrap process.
         */
        private final RequestQueue unwraps = new RequestQueue();
        /**
         * The promise that resolves when unwraps are done.
         */
        private final Promise<Void> unwrapsDone;
        /**
         * User buffer.
         */
        private ByteBuffer user;
        /**
         * App buffer.
         */
        private ByteBuffer app;
        /**
         * Packet buffer.
         */
        private ByteBuffer packet;
        /**
         * The EOF has been read from the stream.
         */
        private boolean eofUnwrapped;

        /**
         * The constructor from the underlying object.
         *
         * @param wrapped the underlying object
         */
        protected SSLInput(final AInput<ByteBuffer> wrapped) {
            super(wrapped);
            unwrapsDone = doUnwraps();
        }

        /**
         * @return the unwrap loop outcome
         */
        private Promise<Void> doUnwraps() { // NOPMD
            return unwraps.runSeqWhile(() -> { // NOPMD
                ensureValid();
                if (engine.isInboundDone()) {
                    eofUnwrapped = true;
                    output.wraps.resume();
                    reads.resume();
                    return aFalse();
                }
                final SSLEngineResult.HandshakeStatus handshakeStatus = engine.getHandshakeStatus();
                switch (handshakeStatus) {
                    case FINISHED:
                    case NOT_HANDSHAKING:
                        ensureHandshakeEnded();
                        if (user == null && isOpen()) {
                            // no need to unwrap anything, since there is no active read operation
                            return unwraps.suspendThenTrue();
                        }
                        break;
                    case NEED_TASK:
                        ensureHandshakeStarted();
                        tasks.resume();
                        return unwraps.suspendThenTrue();
                    case NEED_WRAP:
                        ensureHandshakeStarted();
                        output.wraps.resume();
                        return unwraps.suspendThenTrue();
                    case NEED_UNWRAP:
                        ensureHandshakeStarted();
                        break;
                    default:
                        throw new IllegalStateException("Unknown handshake status: " + handshakeStatus);
                }

                if (packet == null) {
                    packet = IOUtil.BYTE.writeBuffer(engine.getSession().getPacketBufferSize());
                }
                SSLEngineResult unwrap = null;
                if (user != null && (app == null || app.position() == 0)) {
                    unwrap = engine.unwrap(packet, user);
                    if (unwrap.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                        unwrap = null;
                    }
                } else if (app == null) {
                    unwrap = engine.unwrap(packet, EMPTY);
                    if (unwrap.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                        unwrap = null;
                    }
                }
                if (unwrap == null) {
                    if (app == null) {
                        app = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
                    }
                    unwrap = engine.unwrap(packet, app);
                }
                switch (unwrap.getStatus()) {
                    case BUFFER_UNDERFLOW:
                        if (netData != null) {
                            BufferOperations.BYTE.append(packet, netData);
                            if (!netData.hasRemaining()) {
                                notifySuccess(null, netDataResolver);
                                netDataResolver = null;
                            }
                            return aTrue();
                        }
                        packet.compact();
                        final SSLEngineResult finalUnwrap = unwrap;
                        return wrapped.read(packet).flatMap(value -> {
                            packet.flip();
                            if (IOUtil.isEof(value)) {
                                engine.closeInbound();
                                if (packet.hasRemaining()) {
                                    invalidateChannel(new IOException("SSLEngine wants more data, "
                                            + "but none is available " + finalUnwrap));
                                }
                            }
                            return aTrue();
                        });
                    case BUFFER_OVERFLOW:
                        if (app.position() > 0) {
                            reads.resume();
                            return unwraps.suspendThenTrue();
                        }
                        final int newCapacity = engine.getSession().getApplicationBufferSize();
                        if (app.capacity() <= newCapacity) {
                            throw new IllegalStateException("OVERFLOW, but capacity did not change" + unwrap);
                        }
                        final ByteBuffer newApp = ByteBuffer.allocate(newCapacity);
                        app.flip();
                        newApp.put(app);
                        return aTrue();
                    case CLOSED:
                    case OK:
                        reads.resume();
                        return aTrue();
                    default:
                        throw new IllegalStateException("Unknown status: " + handshakeStatus);
                }
            }).listen(invalidationObserver);
        }

        /**
         * Invalidate input.
         *
         * @param t the problem to invalidate with
         */
        private void invalidateInput(final Throwable t) {
            invalidate(t);
            unwraps.resume();
            reads.resume();
        }

        @Override
        public Promise<Integer> read(final ByteBuffer buffer) {
            final int savedPosition = buffer.position();
            return reads.runSeqUntilValue(() -> {
                ensureValidAndOpen();
                if (buffer.position() > savedPosition) {
                    // something has been read into the buffer, just return diff
                    user = null;
                    return aMaybeValue(buffer.position() - savedPosition);
                }
                if (buffer.position() < savedPosition) {
                    user = null;
                    return aFailure(new IllegalArgumentException("Input buffer changes while "
                            + "read operation is in progress " + (buffer.position() - savedPosition)));
                }
                if (buffer.position() == buffer.capacity()) {
                    user = null;
                    return aFailure(new IllegalArgumentException("Buffer is full: " + buffer));
                }
                if (app != null && app.position() > 0) {
                    app.flip();
                    final int rc = BufferOperations.BYTE.put(buffer, app);
                    app.compact();
                    user = null;
                    unwraps.resume();
                    return aMaybeValue(rc);
                }
                if (eofUnwrapped) {
                    user = null;
                    return IOUtil.EOF_MAYBE_PROMISE;
                }
                user = buffer;
                unwraps.resume();
                return reads.suspendThenEmpty();
            }).listen(resolution -> {
                if (user == buffer) { // NOPMD
                    user = null;
                }
            });
        }

        @Override
        protected Promise<Void> closeAction() {
            reads.resume();
            unwraps.resume();
            return aVoid();
        }

    }

    /**
     * The SSL output stream.
     */
    private final class SSLOutput extends ChainedClosable<AOutput<ByteBuffer>> implements AOutput<ByteBuffer> {
        /**
         * Magic padding for the buffer.
         */
        public static final int MAGIC_PAD = 16;
        /**
         * The write requests.
         */
        private final RequestQueue writes = new RequestQueue();
        /**
         * The wrap process.
         */
        private final RequestQueue wraps = new RequestQueue();
        /**
         * The promise that resolves when unwraps are done.
         */
        private final Promise<Void> wrapsDone;
        /**
         * User buffer.
         */
        private ByteBuffer user;
        /**
         * Packet buffer.
         */
        private ByteBuffer packet;
        /**
         * If not null, a flush is needed.
         */
        private AResolver<Void> flushNeeded;

        /**
         * The constructor from the underlying object.
         *
         * @param wrapped the underlying object
         */
        protected SSLOutput(final AOutput<ByteBuffer> wrapped) {
            super(wrapped);
            wrapsDone = doWraps();
        }

        /**
         * @return the wrap loop outcome
         */
        private Promise<Void> doWraps() { // NOPMD
            return wraps.runSeqWhile(() -> { // NOPMD
                ensureValid();
                if (engine.isOutboundDone()) {
                    return aFalse();
                }
                if (flushNeeded != null) {
                    return wrapped.flush().thenFlatGet(() -> {
                        notifySuccess(null, flushNeeded);
                        flushNeeded = null;
                        return aTrue();
                    });
                }
                final SSLEngineResult.HandshakeStatus handshakeStatus = engine.getHandshakeStatus();
                switch (handshakeStatus) {
                    case FINISHED:
                    case NOT_HANDSHAKING:
                        ensureHandshakeEnded();
                        if ((user == null || !user.hasRemaining()) && !isClosed()) {
                            return wraps.suspendThenTrue();
                        }
                        break;
                    case NEED_TASK:
                        ensureHandshakeStarted();
                        tasks.resume();
                        return wraps.suspendThenTrue();
                    case NEED_UNWRAP:
                        ensureHandshakeStarted();
                        input.unwraps.resume();
                        return wraps.suspendThenTrue();
                    case NEED_WRAP:
                        ensureHandshakeStarted();
                        break;
                    default:
                        throw new IllegalStateException("Unsupported handshake status: " + handshakeStatus);
                }
                if (packet == null) {
                    packet = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
                }
                final SSLEngineResult wrap = engine.wrap(user == null ? EMPTY : user, packet);
                switch (wrap.getStatus()) { // NOPMD
                    // PMD is confused by ifs, and thinks that break is missing
                    // http://sourceforge.net/p/pmd/bugs/795/
                    case CLOSED:
                    case OK:
                        if (user != null && !user.hasRemaining()) {
                            writes.resume();
                        }
                        if (packet.position() > 0) {
                            return writePacketAndContinue();
                        } else {
                            if (wrap.bytesConsumed() == 0 && wrap.bytesProduced() == 0) {
                                return wraps.suspendThenTrue();
                            } else {
                                return aTrue();
                            }
                        }
                    case BUFFER_OVERFLOW:
                        int newSize = engine.getSession().getPacketBufferSize();
                        if (packet.capacity() >= newSize) {
                            newSize = packet.capacity() + packet.remaining() + MAGIC_PAD;
                        }
                        final ByteBuffer newPacket = ByteBuffer.allocate(newSize);
                        packet.flip();
                        newPacket.put(packet);
                        packet = newPacket;
                        if (packet.position() > 0) {
                            return writePacketAndContinue();
                        } else {
                            return aTrue();
                        }
                    case BUFFER_UNDERFLOW:
                        writes.resume();
                        if (wrap.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
                                || wrap.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                            return wraps.suspendThenTrue();
                        } else {
                            return aTrue();
                        }
                    default:
                        throw new IllegalStateException("Unknown wrap outcome: " + wrap);
                }
            }).listen(invalidationObserver);
        }

        /**
         * @return write packet and continue loop
         */
        private Promise<Boolean> writePacketAndContinue() {
            packet.flip();
            return wrapped.write(packet).thenFlatGet(() -> {
                packet.compact();
                return aTrue();
            });
        }

        @Override
        public Promise<Void> write(final ByteBuffer buffer) {
            return writes.runSeqWhile(() -> {
                ensureValidAndOpen();
                if (!buffer.hasRemaining()) {
                    return aFalse();
                }
                user = buffer;
                wraps.resume();
                return writes.suspendThenTrue();
            }).listen(resolution -> {
                if (user == buffer) { // NOPMD
                    user = null;
                }
            });
        }

        @Override
        public Promise<Void> flush() {
            return writes.run(() -> {
                ensureValidAndOpen();
                final Promise<Void> rc = new Promise<>();
                flushNeeded = rc.resolver();
                wraps.resume();
                return rc;
            });
        }

        @Override
        protected Promise<Void> closeAction() {
            writes.resume();
            wraps.resume();
            return aVoid();
        }

        /**
         * Invalidate output stream.
         *
         * @param t the invalidation
         */
        private void invalidateOutput(final Throwable t) {
            invalidate(t);
            writes.resume();
            wraps.resume();
            if (flushNeeded != null) {
                notifyFailure(flushNeeded, t);
                flushNeeded = null;
            }
        }
    }
}
