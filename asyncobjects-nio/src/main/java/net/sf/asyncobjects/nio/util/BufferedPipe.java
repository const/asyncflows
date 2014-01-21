package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.BufferOperations;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * The buffered pipe.
 *
 * @param <B> the buffer type
 */
public class BufferedPipe<B extends Buffer> implements AChannel<B>, ExportsSelf<AChannel<B>> {
    /**
     * The buffer operations.
     */
    private final BufferOperations<B, ?> operations;
    /**
     * The buffer contains data that is ready for reading.
     */
    private final B pipeBuffer;
    /**
     * The input for the pipe channel.
     */
    private final Input input = new Input();
    /**
     * The output for the pipe channel.
     */
    private final Output output = new Output();
    /**
     * The exported input.
     */
    private final AInput<B> exportedInput = NIOExportUtil.export(Vat.current(), input);
    /**
     * The exported output.
     */
    private final AOutput<B> exportedOutput = NIOExportUtil.export(Vat.current(), output);

    /**
     * The constructor.
     *
     * @param operations the buffer operations
     * @param pipeBuffer the pipe buffer
     */
    public BufferedPipe(final BufferOperations<B, ?> operations, final B pipeBuffer) {
        this.operations = operations;
        this.pipeBuffer = pipeBuffer;
    }

    /**
     * Allocate byte pipe.
     *
     * @param size the pipe size
     * @return the pipe working on the current vat
     */
    public static AChannel<ByteBuffer> bytePipe(final int size) {
        final ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.limit(0);
        return new BufferedPipe<ByteBuffer>(BufferOperations.BYTE, buffer).export();
    }

    /**
     * Allocate character pipe.
     *
     * @param size the pipe size
     * @return the pipe working on the current vat
     */
    public static AChannel<CharBuffer> charPipe(final int size) {
        final CharBuffer buffer = CharBuffer.allocate(size);
        buffer.limit(0);
        return new BufferedPipe<CharBuffer>(BufferOperations.CHAR, buffer).export();
    }

    /**
     * @return the input for the channel.
     */
    @Override
    public Promise<AInput<B>> getInput() {
        return aValue(exportedInput);
    }

    @Override
    public Promise<AOutput<B>> getOutput() {
        return aValue(exportedOutput);
    }

    /**
     * The close operation.
     *
     * @return promise that finishes when close operation is complete
     */
    @Override
    public Promise<Void> close() {
        input.close();
        output.close();
        return aVoid();
    }

    @Override
    public AChannel<B> export() {
        return export(Vat.current());
    }

    @Override
    public AChannel<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }

    /**
     * The base class for streams.
     */
    private class StreamBase implements ACloseable {
        /**
         * The request queue for input.
         */
        protected final RequestQueue requests = new RequestQueue();
        /**
         * If true the stream is closed.
         */
        protected boolean closed;

        @Override
        public Promise<Void> close() {
            closed = true;
            input.requests.resume();
            output.requests.resume();
            return aVoid();
        }

    }

    /**
     * The input implementation.
     */
    private class Input extends StreamBase implements AInput<B> {
        @Override
        public Promise<Integer> read(final B buffer) {
            final Promise<Integer> result = new Promise<Integer>();
            return requests.runSeqLoop(new ACallable<Boolean>() {
                @Override
                public Promise<Boolean> call() throws Throwable {
                    if (closed) {
                        return aFailure(new IllegalStateException("The stream is closed"));
                    }
                    if (!buffer.hasRemaining()) {
                        notifySuccess(result.resolver(), 0);
                        return aFalse();
                    }
                    if (pipeBuffer.hasRemaining()) {
                        notifySuccess(result.resolver(), operations.put(buffer, pipeBuffer));
                        output.requests.resume();
                        return aFalse();
                    }
                    if (output.closed) {
                        notifySuccess(result.resolver(), -1);
                        return aFalse();
                    }
                    return requests.suspendThenTrue();
                }
            }).thenPromise(result);
        }
    }

    /**
     * The output implementation.
     */
    private class Output extends StreamBase implements AOutput<B> {
        @Override
        public Promise<Void> write(final B buffer) {
            return requests.runSeqLoop(new ACallable<Boolean>() {
                @Override
                public Promise<Boolean> call() throws Throwable {
                    if (closed) {
                        return outputClosed();
                    }
                    if (!buffer.hasRemaining()) {
                        return aFalse();
                    }
                    if (input.closed) {
                        return inputClosed("write");
                    }
                    operations.append(pipeBuffer, buffer);
                    if (!buffer.hasRemaining()) {
                        return aFalse();
                    }
                    input.requests.resume();
                    return requests.suspendThenTrue();
                }
            });
        }

        @Override
        public Promise<Void> flush() {
            return requests.runSeqLoop(new ACallable<Boolean>() {
                @Override
                public Promise<Boolean> call() throws Throwable {
                    if (!pipeBuffer.hasRemaining()) {
                        return aFalse();
                    }
                    if (closed) {
                        return outputClosed();
                    }
                    if (input.closed) {
                        return inputClosed("flush");
                    }
                    return requests.suspendThenTrue();
                }
            });
        }

        /**
         * Create a failure.
         *
         * @param operation the output operation name
         * @return a failure indicating that input is closed
         */
        private Promise<Boolean> inputClosed(final String operation) {
            return aFailure(new IllegalStateException("The input stream is closed: "
                    + operation + " is impossible"));
        }

        /**
         * @return a failure that specifies that output is already closed
         */
        private Promise<Boolean> outputClosed() {
            return aFailure(new IllegalStateException("The output stream is closed"));
        }
    }
}
