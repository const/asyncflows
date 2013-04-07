package net.sf.asyncobjects.nio;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;

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
