package org.asyncflows.protocol.http.common.content;

import org.asyncflows.io.AOutput;
import org.asyncflows.core.Promise;

import java.nio.Buffer;
import java.util.function.Consumer;

/**
 * The counting output.
 *
 * @param <B> the buffer type
 */
public class CountingOutput<B extends Buffer> extends CountingStreamBase implements AOutput<B> {
    /**
     * The output.
     */
    private final AOutput<B> output;

    /**
     * The constructor.
     *
     * @param output   the output
     * @param listener the listener (must be exported, or be otherwise safe for use from other threads)
     */
    public CountingOutput(final AOutput<B> output, final Consumer<StreamFinishedEvent> listener) {
        super(listener);
        this.output = output;
    }

    /**
     * Count stream content if needed.
     *
     * @param output   the output
     * @param listener the listener (must be exported, or be otherwise safe for use from other threads)
     * @param <B>      the buffer type
     * @return if listener is null, returns original stream otherwise a stram wrapper is returned
     */
    public static <B extends Buffer> AOutput<B> countIfNeeded(final AOutput<B> output,
                                                              final Consumer<StreamFinishedEvent> listener) {
        if (listener == null) {
            return output;
        } else {
            return new CountingOutput<B>(output, listener);
        }
    }

    @Override
    public Promise<Void> write(final B buffer) {
        final int amount = buffer.remaining();
        return output.write(buffer).listen(resolution -> {
            if (resolution.isSuccess()) {
                transferred(amount - buffer.remaining());
            } else {
                fireFinished(resolution.failure());
            }
        });
    }

    @Override
    public Promise<Void> flush() {
        return output.flush();
    }

    @Override
    public Promise<Void> close() {
        return output.close().listen(this::fireFinished);
    }
}
