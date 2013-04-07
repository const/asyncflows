package net.sf.asyncobjects.nio;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;

import java.nio.Buffer;

/**
 * An output based on byte buffer.
 *
 * @param <B> the buffer type
 */
public interface AOutput<B extends Buffer> extends ACloseable {

    /**
     * Write data to stream. Note that the data might be remaining in the buffer after write operation.
     * If data could not be completely processed, it should be saved in stream's buffer.
     *
     * @param buffer a buffer with data to write
     * @return a promise that resolves when data finished writing
     */
    Promise<Void> write(B buffer);

    /**
     * Flush as much data from buffers as possible. Note that some data could remain in the buffers if it could
     * not be readily converted (for example, incomplete code points in internal character buffer for
     * {@link net.sf.asyncobjects.nio.text.EncoderOutput}).
     *
     * @return a promise that resolves when data finishes flushing
     */
    Promise<Void> flush();
}
