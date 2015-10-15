package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.AsyncControl;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.IOUtil;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

/**
 * The parser context for the protocol implementation. It contains the data is needed for parsing binary streams.
 */
public class ByteParserContext {
    /**
     * Default size of buffer for the input.
     */
    public static final int DEFAULT_BUFFER_SIZE = 8096;
    /**
     * The input stream.
     */
    private final AInput<ByteBuffer> input;
    /**
     * The buffer.
     */
    private ByteBuffer buffer;
    /**
     * true, if the eof has been read from the stream.
     */
    private boolean eofRead;
    /**
     * an exception in the case of reading failed.
     */
    private Throwable invalidation;
    /**
     * true if the context is in process of reading.
     */
    private boolean readMode;

    /**
     * The constructor.
     *
     * @param input  the input stream
     * @param buffer the buffer
     */
    public ByteParserContext(final AInput<ByteBuffer> input, final ByteBuffer buffer) {
        this.input = input;
        this.buffer = buffer;
    }

    /**
     * The constructor.
     *
     * @param input      the input stream
     * @param bufferSize the buffer size
     */
    public ByteParserContext(final AInput<ByteBuffer> input, final int bufferSize) {
        this(input, IOUtil.BYTE.writeBuffer(bufferSize));
    }


    /**
     * The constructor with default buffer size ({@value #DEFAULT_BUFFER_SIZE}).
     *
     * @param input the input stream
     */
    public ByteParserContext(final AInput<ByteBuffer> input) {
        this(input, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Read more data into the buffer. The method should be only called when the current data is unsufficient
     * for the operation. The method should be called only if it known that EOF has not been read yet.
     *
     * @return a promise that resolves to true when more data is read or EOF is detected, or fails
     * no more data is possible to read.
     */
    public Promise<Boolean> readMore() {
        if (eofRead) {
            throw new IllegalStateException("EOF has been already read!");
        }
        if (buffer.capacity() == buffer.remaining()) {
            throw new IllegalStateException("Buffer is full, extend it first!");
        }
        if (invalidation != null) {
            return aFailure(invalidation);
        }
        readMode = true;
        buffer.compact();
        return input.read(buffer).mapOutcome(value -> {
            buffer.flip();
            readMode = false;
            if (value.isSuccess()) {
                if (IOUtil.isEof(value.value())) {
                    eofRead = true;
                }
                return aTrue();
            } else {
                invalidation = value.failure();
                return aFailure(invalidation);
            }
        });
    }

    /**
     * Read more data then return a promise for empty value.
     *
     * @param <T> the type of maybe
     * @return the promise for empty value
     */
    public <T> Promise<Maybe<T>> readMoreEmpty() {
        return readMore().thenPromise(AsyncControl.<T>aMaybeEmpty());
    }

    /**
     * Ensure that specified amount of bytes is available in the buffer. Fail if it could not be read.
     *
     * @param amount the amount to read
     * @return a promise that resolves when bytes are available or fails.
     */
    public Promise<Void> ensureAvailable(final int amount) {
        if (buffer.remaining() >= amount) {
            return aVoid();
        }
        return aSeqLoop(() -> {
            if (buffer.remaining() >= amount) {
                return aFalse();
            }
            return readMore();
        });
    }

    /**
     * @return true, if there is some data in the buffer.
     */
    public boolean hasRemaining() {
        ensureValid();
        return buffer.hasRemaining();
    }

    /**
     * @return true if EOF has been seen on the input, so all the data in the buffer is the last one.
     */
    public boolean isEofSeen() {
        ensureValid();
        return eofRead;
    }

    /**
     * @return if there is no more data available.
     */
    public boolean isEof() {
        return !buffer.hasRemaining() && isEofSeen();
    }

    /**
     * Ensure that context did not fail and that there are no read operation in the progress.
     */
    private void ensureValid() {
        if (invalidation != null) {
            throw new IllegalStateException("The stream has been failed", invalidation);
        }
        if (readMode) {
            throw new IllegalStateException("The method is called while read operation is in progress.");
        }
    }

    /**
     * @return the current buffer
     */
    public ByteBuffer buffer() {
        ensureValid();
        return buffer;
    }

    /**
     * @return get underlying input.
     */
    public AInput<ByteBuffer> input() {
        return input;
    }
}
