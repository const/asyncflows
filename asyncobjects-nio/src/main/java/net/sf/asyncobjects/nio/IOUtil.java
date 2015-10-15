package net.sf.asyncobjects.nio;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.CoreFunctionUtil;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.ResourceUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.CoreFunctionUtil.promiseCallable;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

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
     * @param <C>     the channel type
     * @return the action
     */
    public <C extends AChannel<B>> ResourceUtil.Try3<C, AInput<B>, AOutput<B>> tryChannel(final ACallable<C> channel) {
        return aTry(channel).andChain(AChannel::getInput).andChainFirst(AChannel::getOutput);

    }


    /**
     * Try action for the channel.
     *
     * @param channel the channel promise
     * @param <C>     the channel type
     * @return the action
     */
    public <C extends AChannel<B>> ResourceUtil.Try3<C, AInput<B>, AOutput<B>> tryChannel(final Promise<C> channel) {
        return tryChannel(promiseCallable(channel));

    }

    /**
     * Copy operation stream operation. The stream is copied until EOF on input is reached.
     *
     * @param input     the input stream
     * @param output    the output stream
     * @param autoFlush if true, the stream is flushed after each write
     * @param buffer    the buffer to use (the buffer might have a data in it and the buffer is assumed to be in state
     *                  ready for the stream read operation). If operation fails, the state of buffer is undefined.
     * @return the amount of bytes read then written (bytes arleady in the buffer are not counted)
     */
    public final Promise<Long> copy(final AInput<B> input, final AOutput<B> output,
                                    final boolean autoFlush, final B buffer) {
        if (buffer.capacity() <= 0) {
            throw new IllegalArgumentException("The buffer capacity must be positive: " + buffer.capacity());
        }
        final long[] result = new long[1];
        final Cell<Promise<Void>> flush = new Cell<>();
        return aSeqLoop(
                () -> input.read(buffer).map(value -> {
                    if (isEof(value) && buffer.position() == 0) {
                        return aFalse();
                    } else {
                        if (value > 0) {
                            result[0] += +value;
                        }
                        buffer.flip();
                        return output.write(buffer).thenDo(() -> {
                            operations.compact(buffer);
                            if (autoFlush) {
                                flush.setValue(output.flush());
                            }
                            return aTrue();
                        });
                    }
                })
        ).thenDo(() -> {
            if (flush.isEmpty()) {
                return aValue(result[0]);
            } else {
                return flush.getValue().thenDo(CoreFunctionUtil.constantCallable(result[0]));
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
        return aSeqLoop(
                () -> input.read(buffer).map(value -> {
                    if (isEof(value)) {
                        return aFalse();
                    } else {
                        result[0] += value;
                        buffer.clear();
                        return aTrue();
                    }
                })
        ).thenDo(() -> aValue(result[0]));
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
