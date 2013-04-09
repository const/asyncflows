package net.sf.asyncobjects.streams.util;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.SeqControl;
import net.sf.asyncobjects.streams.AStream;
import net.sf.asyncobjects.streams.StreamUtil;

import java.util.List;

/**
 * The stream builder.
 *
 * @param <T> the stream element type
 */
public class StreamBuilder<T> {
    /**
     * The current stream.
     */
    private final AStream<T> current;

    /**
     * The constructor for the builder.
     *
     * @param current the current stream.
     */
    public StreamBuilder(final AStream<T> current) {
        this.current = current;
    }

    /**
     * @return the sequence for builder
     */
    public SeqControl.SeqForBuilder<T> seqFor() {
        return new SeqControl.SeqForBuilder<T>(StreamUtil.producerFromStream(current));
    }

    /**
     * @return the stream to the list
     */
    public Promise<List<T>> toList() {
        return seqFor().toList();
    }

    /**
     * Start building a stream.
     *
     * @param stream the stream to wrap
     * @param <T>    the stream element type
     * @return the stream builder
     */
    public static <T> StreamBuilder<T> forStream(final AStream<T> stream) {
        return new StreamBuilder<T>(stream);
    }

}
