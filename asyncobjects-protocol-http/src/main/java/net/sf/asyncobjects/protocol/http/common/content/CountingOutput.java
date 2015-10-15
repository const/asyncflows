package net.sf.asyncobjects.protocol.http.common.content;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.AConsumer;
import net.sf.asyncobjects.nio.AOutput;

import java.nio.Buffer;

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
    public CountingOutput(final AOutput<B> output, final AConsumer<StreamFinishedEvent> listener) {
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
                                                              final AConsumer<StreamFinishedEvent> listener) {
        if (listener == null) {
            return output;
        } else {
            return new CountingOutput<B>(output, listener);
        }
    }

    @Override
    public Promise<Void> write(final B buffer) {
        final int amount = buffer.remaining();
        return output.write(buffer).observe(resolution -> {
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
        return output.close().observe(this::fireFinished);
    }
}
