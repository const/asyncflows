package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.OptionalValue;
import net.sf.asyncobjects.core.util.ResourceUtil;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aLater;

/**
 * The export utility class for the streams.
 */
public final class StreamExportUtil {
    /**
     * The private constructor for utility class.
     */
    private StreamExportUtil() {

    }

    /**
     * Export a stream.
     *
     * @param vat    the vat to use
     * @param stream the stream
     * @param <T>    the stream element type
     * @return the stream
     */
    public static <T> AStream<T> export(final Vat vat, final AStream<T> stream) {
        return new AStream<T>() {
            @Override
            public Promise<OptionalValue<T>> next() {
                return aLater(vat, new ACallable<OptionalValue<T>>() {
                    @Override
                    public Promise<OptionalValue<T>> call() throws Throwable {
                        return stream.next();
                    }
                });
            }

            @Override
            public Promise<Void> close() {
                return ResourceUtil.closeResource(vat, stream);
            }
        };
    }
}
