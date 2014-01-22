package net.sf.asyncobjects.nio;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

import static net.sf.asyncobjects.core.AsyncControl.aBoolean;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoopFair;

/**
 * Character IO utilities.
 */
public final class CharIOUtil {
    /**
     * UTF-8 charset.
     */
    public static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * The private constructor for utility class.
     */
    private CharIOUtil() {
    }

    /**
     * Get content of input as a string. The buffer is considered as ready for write into it.
     *
     * @param input  the input to read
     * @param buffer the buffer to read with (it should be ready for read operation)
     * @return the content
     */
    public static Promise<String> getContent(final AInput<CharBuffer> input, final CharBuffer buffer) {
        if (buffer.capacity() < 1) {
            throw new IllegalArgumentException("The buffer capacity must be positive: " + buffer.capacity());
        }
        final StringBuilder builder = new StringBuilder();
        return aSeqLoopFair(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                return input.read(buffer).map(new AFunction<Boolean, Integer>() {
                    @Override
                    public Promise<Boolean> apply(final Integer value) throws Throwable {
                        buffer.flip();
                        builder.append(buffer);
                        buffer.clear();
                        return aBoolean(value >= 0);
                    }
                });
            }
        }).thenDo(new ACallable<String>() {
            @Override
            public Promise<String> call() throws Throwable {
                return aValue(builder.toString());
            }
        });
    }

}
