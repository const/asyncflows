package org.asyncflows.io;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

import java.nio.Buffer;

/**
 * A bidirectional channel interface.
 *
 * @param <B> the buffer type
 */
public interface AChannel<B extends Buffer> extends ACloseable {
    /**
     * @return the input for the channel.
     */
    Promise<AInput<B>> getInput();

    /**
     * @return the output for the channel.
     */
    Promise<AOutput<B>> getOutput();
}
