package net.sf.asyncobjects.nio;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.CoreFunctionUtil;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.Cell;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
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
        final Cell<Long> result = new Cell<Long>(0L);
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
                                result.setValue(result.getValue() + value);
                            }
                            buffer.flip();
                            return output.write(buffer).then(new ACallable<Boolean>() {
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
        }).then(new ACallable<Long>() {
            @Override
            public Promise<Long> call() throws Throwable {
                if (flush.isEmpty()) {
                    return aSuccess(result.getValue());
                } else {
                    return flush.getValue().then(CoreFunctionUtil.constantCallable(result.getValue()));
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
        final Cell<Long> result = new Cell<Long>(0L);
        return aSeqLoop(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                return input.read(buffer).map(new AFunction<Boolean, Integer>() {
                    @Override
                    public Promise<Boolean> apply(final Integer value) throws Throwable {
                        if (value < 0) {
                            return aFalse();
                        } else {
                            result.setValue(result.getValue() + value);
                            buffer.clear();
                            return aTrue();
                        }
                    }
                });
            }
        }).then(new ACallable<Long>() {
            @Override
            public Promise<Long> call() throws Throwable {
                return aSuccess(result.getValue());
            }
        });
    }
}
