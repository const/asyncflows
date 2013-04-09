package net.sf.asyncobjects.streams;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.OptionalValue;

/**
 * The stream utilities.
 */
public final class StreamUtil {
    /**
     * The private constructor for utility class.
     */
    private StreamUtil() {
    }

    /**
     * The producer for the stream.
     *
     * @param stream the stream.
     * @param <O>    the stream element type
     * @return the callable that invokes a read operation
     */
    public static <O> ACallable<OptionalValue<O>> producerFromStream(final AStream<O> stream) {
        return new ACallable<OptionalValue<O>>() {
            @Override
            public Promise<OptionalValue<O>> call() throws Throwable {
                return stream.next();
            }
        };
    }

}
