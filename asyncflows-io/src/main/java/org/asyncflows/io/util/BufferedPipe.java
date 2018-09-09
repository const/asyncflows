package org.asyncflows.io.util;

import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.IOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The buffered pipe.
 *
 * @param <B> the buffer type
 */
public class BufferedPipe<B extends Buffer> implements AChannel<B>, NeedsExport<AChannel<B>> {
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
    private final AInput<B> exportedInput = IOExportUtil.export(Vat.current(), input);
    /**
     * The exported output.
     */
    private final AOutput<B> exportedOutput = IOExportUtil.export(Vat.current(), output);

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
        final ByteBuffer buffer = IOUtil.BYTE.writeBuffer(size);
        return new BufferedPipe<>(BufferOperations.BYTE, buffer).export();
    }

    /**
     * Allocate character pipe.
     *
     * @param size the pipe size
     * @return the pipe working on the current vat
     */
    public static AChannel<CharBuffer> charPipe(final int size) {
        final CharBuffer buffer = IOUtil.CHAR.writeBuffer(size);
        return new BufferedPipe<>(BufferOperations.CHAR, buffer).export();
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
    public AChannel<B> export(final Vat vat) {
        return IOExportUtil.export(vat, this);
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
            final Promise<Integer> result = new Promise<>();
            return requests.runSeqWhile(() -> {
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
            }).thenPromise(result);
        }
    }

    /**
     * The output implementation.
     */
    private class Output extends StreamBase implements AOutput<B> {
        @Override
        public Promise<Void> write(final B buffer) {
            return requests.runSeqWhile(() -> {
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
            });
        }

        @Override
        public Promise<Void> flush() {
            return requests.runSeqWhile(() -> {
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
