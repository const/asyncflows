package net.sf.asyncobjects.protocol;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.AInput;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * The context for the protocol implementation. It contains the data is needed for different protocol parts.
 */
public class InputContext {
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
    public InputContext(final AInput<ByteBuffer> input, final ByteBuffer buffer) {
        this.input = input;
        this.buffer = buffer;
    }

    /**
     * The constructor.
     *
     * @param input      the input stream
     * @param bufferSize the buffer size
     */
    public InputContext(final AInput<ByteBuffer> input, final int bufferSize) {
        this(input, ByteBuffer.allocate(bufferSize));
        buffer.limit(0);
    }

    /**
     * Read more data into the buffer. The method should be only called when the current data is unsufficient
     * for the operation. The method should be called only if it known that EOF has not been read yet.
     *
     * @return a promise that resolves when more data is read or EOF is detected, or fails no more data is possible
     */
    public Promise<Void> readMore() {
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
        return input.read(buffer).mapOutcome(new AFunction<Void, Outcome<Integer>>() {
            @Override
            public Promise<Void> apply(final Outcome<Integer> value) throws Throwable {
                buffer.flip();
                readMode = false;
                if (value.isSuccess()) {
                    if (value.value() < 0) {
                        eofRead = true;
                    }
                    return aVoid();
                } else {
                    invalidation = value.failure();
                    return aFailure(invalidation);
                }
            }
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
}
