package net.sf.asyncobjects.protocol.http.common.content;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.AConsumer;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.IOUtil;

import java.nio.Buffer;

/**
 * The counting input.
 *
 * @param <B> the buffer type
 */
public class CountingInput<B extends Buffer> extends CountingStreamBase implements AInput<B> {
    /**
     * The input.
     */
    private final AInput<B> input;
    /**
     * The read resolver.
     */
    private final AResolver<Integer> readResolver = resolution -> {
        if (resolution.isFailure()) {
            fireFinished(resolution.failure());
        } else {
            if (resolution.value() == null || IOUtil.isEof(resolution.value())) {
                fireFinished();
            } else {
                transferred(resolution.value());
            }
        }
    };

    /**
     * The constructor.
     *
     * @param input    the input
     * @param listener the listener (must be exported, or be otherwise safe for use from other threads)
     */
    public CountingInput(final AInput<B> input, final AConsumer<StreamFinishedEvent> listener) {
        super(listener);
        this.input = input;
    }

    /**
     * Count stream content if needed.
     *
     * @param input    the input
     * @param listener the listener (must be exported, or be otherwise safe for use from other threads)
     * @param <B>      the buffer type
     * @return if listener is null, returns original stream otherwise a stram wrapper is returned
     */
    public static <B extends Buffer> AInput<B> countIfNeeded(final AInput<B> input,
                                                             final AConsumer<StreamFinishedEvent> listener) {
        if (listener == null) {
            return input;
        } else {
            return new CountingInput<>(input, listener);
        }
    }

    @Override
    public Promise<Integer> read(final B buffer) {
        return input.read(buffer).observe(readResolver);
    }

    @Override
    public Promise<Void> close() {
        return input.close().observe(this::fireFinished);
    }
}
