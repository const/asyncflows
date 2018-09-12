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

package org.asyncflows.io;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Cell;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.util.CoreFlowsResource;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.function.AsyncFunctionUtil.promiseSupplier;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

/**
 * The generic IO util class.
 *
 * @param <B> the buffer type
 * @param <A> the array type
 */
public class IOUtil<B extends Buffer, A> {
    /**
     * The EOF value.
     */
    public static final int EOF = -1;
    // TODO make it private and expose processes instead
    /**
     * The EOF promise.
     */
    public static final Promise<Integer> EOF_PROMISE = aValue(EOF);
    /**
     * The EOF Maybe promise (used in loops).
     */
    public static final Promise<Maybe<Integer>> EOF_MAYBE_PROMISE = aMaybeValue(EOF);
    /**
     * The default buffer size used by utilities if the buffer size is omitted.
     */
    public static final int DEFAULT_BUFFER_SIZE = 1024;
    /**
     * Byte version of IO utils.
     */
    public static final IOUtil<ByteBuffer, byte[]> BYTE = new IOUtil<>(BufferOperations.BYTE);
    /**
     * Character version of IO utils.
     */
    public static final IOUtil<CharBuffer, char[]> CHAR = new IOUtil<>(BufferOperations.CHAR);
    /**
     * The buffer operations.
     */
    private final BufferOperations<B, A> operations;

    /**
     * The constructor.
     *
     * @param operations the operations over buffer
     */
    public IOUtil(final BufferOperations<B, A> operations) {
        this.operations = operations;
    }

    /**
     * Check if the value returned from stream is EOF.
     *
     * @param value the value to check
     * @return true if EOF
     */
    public static boolean isEof(final int value) {
        return value < 0;
    }

    /**
     * Try action for the channel.
     *
     * @param channel the channel create action
     * @param <B>     the buffer type
     * @param <C>     the channel type
     * @return the action
     */
    public static <B extends Buffer, C extends AChannel<B>>
    CoreFlowsResource.Try3<C, AInput<B>, AOutput<B>> aTryChannel(final ASupplier<C> channel) {
        return aTry(channel).andChain(AChannel::getInput).andChainFirst(AChannel::getOutput);

    }


    /**
     * Try action for the channel.
     *
     * @param channel the channel promise
     * @param <B>     the buffer type
     * @param <C>     the channel type
     * @return the action
     */
    public static <B extends Buffer, C extends AChannel<B>>
    CoreFlowsResource.Try3<C, AInput<B>, AOutput<B>> aTryChannel(final Promise<C> channel) {
        return aTryChannel(promiseSupplier(channel));
    }

    /**
     * Try action for the channel.
     *
     * @param channel the channel promise
     * @param <B>     the buffer type
     * @param <C>     the channel type
     * @return the action
     */
    public static <B extends Buffer, C extends AChannel<B>>
    CoreFlowsResource.Try3<C, AInput<B>, AOutput<B>> aTryChannel(final C channel) {
        return aTryChannel(aValue(channel));
    }

    /**
     * Copy operation stream operation. The stream is copied until EOF on input is reached.
     *
     * @param input     the input stream
     * @param output    the output stream
     * @param autoFlush if true, the stream is flushed after each write
     * @param buffer    the buffer to use (the buffer might have a data in it and the buffer is assumed to be in state
     *                  ready for the stream read operation). If operation fails, the state of buffer is undefined.
     * @return the amount of bytes read then written (bytes already in the buffer are not counted)
     */
    public final Promise<Long> copy(final AInput<B> input, final AOutput<B> output,
                                    final boolean autoFlush, final B buffer) {
        if (buffer.capacity() <= 0) {
            throw new IllegalArgumentException("The buffer capacity must be positive: " + buffer.capacity());
        }
        final long[] result = new long[1];
        final Cell<Promise<Void>> flush = new Cell<>();
        return aSeqWhile(
                () -> input.read(buffer).flatMap(value -> {
                    if (isEof(value) && buffer.position() == 0) {
                        return aFalse();
                    } else {
                        if (value > 0) {
                            result[0] += +value;
                        }
                        buffer.flip();
                        return output.write(buffer).thenFlatGet(() -> {
                            operations.compact(buffer);
                            if (autoFlush) {
                                flush.setValue(output.flush());
                            }
                            return aTrue();
                        });
                    }
                })
        ).thenFlatGet(() -> {
            if (flush.isEmpty()) {
                return aValue(result[0]);
            } else {
                return flush.getValue().thenValue(result[0]);
            }
        });
    }

    /**
     * Discard data from stream operation. The stream is discarded until EOF on input is reached.
     *
     * @param input  the input stream
     * @param buffer the buffer to use (the buffer might have a data in it) The buffer is assumed to be in state
     *               ready for the stream read operation.
     * @return the amount of bytes discarded
     */
    public final Promise<Long> discard(final AInput<B> input, final B buffer) {
        final long[] result = new long[1];
        return aSeqWhile(
                () -> input.read(buffer).flatMap(value -> {
                    if (isEof(value)) {
                        return aFalse();
                    } else {
                        result[0] += value;
                        buffer.clear();
                        return aTrue();
                    }
                })
        ).thenFlatGet(() -> aValue(result[0]));
    }

    /**
     * Allocate a buffer of the specified size with no data available in it.
     *
     * @param size the buffer size
     * @return the allocated buffer
     */
    public final B writeBuffer(final int size) {
        final B buffer = operations.buffer(size);
        buffer.limit(0);
        return buffer;
    }

    /**
     * @return the empty buffer for write operation.
     */
    public final B writeBuffer() {
        return writeBuffer(DEFAULT_BUFFER_SIZE);
    }
}
