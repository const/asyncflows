package org.asyncflows.io.adapters;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

import java.io.Closeable;
import java.io.IOException;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The base adapter for closeable.
 *
 * @param <I> the stream type
 */
public abstract class CloseableAdapter<I extends Closeable> implements ACloseable {
    /**
     * The stream.
     */
    protected final I stream;
    /**
     * The closed flag.
     */
    private boolean closed;

    /**
     * The constructor.
     *
     * @param stream the stream
     */
    public CloseableAdapter(final I stream) {
        this.stream = stream;
    }

    @Override
    public final Promise<Void> close() {
        try {
            if (!closed) {
                closeStream(stream);
                closed = true;
            }
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    /**
     * Close the stream. The close operation for the stream could differ depending on the
     * stream type. For example, for socket steams {@link java.net.Socket#shutdownInput()}
     * and {@link java.net.Socket#shutdownOutput()} should be used.
     *
     * @param streamToClose the stream to close
     * @throws IOException in case of the problem
     */
    protected void closeStream(final I streamToClose) throws IOException {
        streamToClose.close();
    }
}
