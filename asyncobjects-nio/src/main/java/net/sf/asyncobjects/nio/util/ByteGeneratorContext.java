package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.AOutput;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * The context for binary generators. This helper class is used to generate structured binary output.
 * It helps to share buffer and other structures between different components.
 */
public class ByteGeneratorContext {
    /**
     * The output stream.
     */
    private final AOutput<ByteBuffer> output;
    /**
     * The buffer.
     */
    private final ByteBuffer buffer;
    /**
     * an exception in the case of writing failed.
     */
    private Throwable invalidation;
    /**
     * The write mode for the stream.
     */
    private boolean writeMode;

    /**
     * The constructor.
     *
     * @param output the output stream
     * @param buffer the buffer to use
     */
    public ByteGeneratorContext(final AOutput<ByteBuffer> output, final ByteBuffer buffer) {
        this.output = output;
        this.buffer = buffer;
    }

    /**
     * The constructor.
     *
     * @param output     the output stream
     * @param bufferSize the buffer to use
     */
    public ByteGeneratorContext(final AOutput<ByteBuffer> output, final int bufferSize) {
        this(output, ByteBuffer.allocate(bufferSize));
    }


    /**
     * Ensure that the specified size is available in the buffer.
     *
     * @param size the size to make available.
     * @return the size
     */
    public Promise<Void> ensureAvailable(final int size) {
        ensureValid();
        if (buffer.remaining() >= size) {
            return aVoid();
        }
        if (buffer.capacity() < size) {
            throw new IllegalArgumentException("Buffer capacity is too small: " + buffer.capacity());
        }
        return send().thenDo(() -> {
            if (buffer.remaining() < size) {
                throw new IllegalArgumentException("Unable to save data: " + buffer.remaining());
            }
            return aVoid();
        });
    }

    /**
     * Ensure that data is sent to the underlying stream.
     *
     * @return the promise that resolves to true when data is sent.
     */
    public Promise<Boolean> send() {
        ensureValid();
        if (isSendNeeded()) {
            writeMode = true;
            buffer.flip();
            return output.write(buffer).mapOutcome(value -> {
                buffer.compact();
                writeMode = false;
                if (value.isSuccess()) {
                    return aTrue();
                } else {
                    invalidation = value.failure();
                    return aFailure(invalidation);
                }
            });
        } else {
            return aTrue();
        }
    }

    /**
     * @return true if send is needed
     */
    public boolean isSendNeeded() {
        return buffer.position() > 0;
    }

    /**
     * Ensure that context did not fail and that there are no read operation in the progress.
     */
    private void ensureValid() {
        if (invalidation != null) {
            throw new IllegalStateException("The stream has been failed", invalidation);
        }
        if (writeMode) {
            throw new IllegalStateException("The method is called while write operation is in progress.");
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
     * @return the underlying output
     */
    public AOutput<ByteBuffer> getOutput() {
        return output;
    }
}
