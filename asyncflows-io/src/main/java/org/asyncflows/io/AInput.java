package org.asyncflows.io;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

import java.nio.Buffer;

/**
 * An input based on NIO buffers.
 *
 * @param <B> the buffer type
 *            <p>
 *            TODO buffer specific subtypes.
 */
public interface AInput<B extends Buffer> extends ACloseable {
    /**
     * Read some bytes into the buffer. Note that if there are some bytes in the buffer
     * (for example leftovers from previous reads), they do not affect return result.
     * So -1 could be returned even if some data is still in the buffer.
     *
     * @param buffer the buffer to read into
     * @return the promise, that finishes with -1 in case of EOF, or with amount of read bytes.
     */
    Promise<Integer> read(B buffer);
}
