package net.sf.asyncobjects.nio;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.CoreFunctionUtil;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.util.ResourceUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
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
     * The default buffer size used by utilities if the buffer size is omitted.
     */
    public static final int DEFAULT_BUFFER_SIZE = 1024;
    /**
     * Byte version of IO utils.
     */
    public static final IOUtil<ByteBuffer, byte[]> BYTE = new IOUtil<ByteBuffer, byte[]>(BufferOperations.BYTE);
    /**
     * Character version of IO utils.
     */
    public static final IOUtil<CharBuffer, char[]> CHAR = new IOUtil<CharBuffer, char[]>(BufferOperations.CHAR);
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
     * Try action for the channel.
     *
     * @param channel the channel
     * @param <C>     the channel type
     * @return the action
     */
    public <C extends AChannel<B>> ResourceUtil.Try3<C, AInput<B>, AOutput<B>> tryChannel(final ACallable<C> channel) {
        return aTry(channel).andChain(new AFunction<AInput<B>, C>() {
            @Override
            public Promise<AInput<B>> apply(final C value) throws Throwable {
                return value.getInput();
            }
        }).andChainFirst(new AFunction<AOutput<B>, C>() {
            @Override
            public Promise<AOutput<B>> apply(final C value) throws Throwable {
                return value.getOutput();
            }
        });

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
        final Cell<Promise<Void>> flush = new Cell<Promise<Void>>();
        return aSeqLoop(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                return input.read(buffer).map(new AFunction<Boolean, Integer>() {
                    @Override
                    public Promise<Boolean> apply(final Integer value) throws Throwable {
                        if (value < 0 && buffer.position() == 0) {
                            return aFalse();
                        } else {
                            if (value > 0) {
                                result[0] += +value;
                            }
                            buffer.flip();
                            return output.write(buffer).thenDo(new ACallable<Boolean>() {
                                @Override
                                public Promise<Boolean> call() throws Throwable {
                                    operations.compact(buffer);
                                    if (autoFlush) {
                                        flush.setValue(output.flush());
                                    }
                                    return aTrue();
                                }
                            });
                        }
                    }
                });
            }
        }).thenDo(new ACallable<Long>() {
            @Override
            public Promise<Long> call() throws Throwable {
                if (flush.isEmpty()) {
                    return aValue(result[0]);
                } else {
                    return flush.getValue().thenDo(CoreFunctionUtil.constantCallable(result[0]));
                }
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
        return aSeqLoop(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                return input.read(buffer).map(new AFunction<Boolean, Integer>() {
                    @Override
                    public Promise<Boolean> apply(final Integer value) throws Throwable {
                        if (value < 0) {
                            return aFalse();
                        } else {
                            result[0] += value;
                            buffer.clear();
                            return aTrue();
                        }
                    }
                });
            }
        }).thenDo(new ACallable<Long>() {
            @Override
            public Promise<Long> call() throws Throwable {
                return aValue(result[0]);
            }
        });
    }
}
