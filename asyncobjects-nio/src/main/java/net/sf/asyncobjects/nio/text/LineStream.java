package net.sf.asyncobjects.nio.text;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.stream.AStream;
import net.sf.asyncobjects.core.stream.ChainedStreamBase;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.nio.AInput;

import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;

/**
 * The line stream.
 */
public class LineStream extends ChainedStreamBase<String, AInput<CharBuffer>> {
    /**
     * The capacity after which the string builder is recreated.
     */
    private static final int CAPACITY_THRESHOLD = 1024;
    /**
     * Default capacity for {@link #buffer}.
     */
    private static final int DEFAULT_BUFFER_CAPACITY = 1024;
    /**
     * If true, new lines are included into the results.
     */
    private final boolean includeNewLine;
    /**
     * The buffer to use.
     */
    private final CharBuffer buffer;
    /**
     * The current line.
     */
    private StringBuilder line = new StringBuilder(); // NOPMD
    /**
     * The requests.
     */
    private final RequestQueue requestQueue = new RequestQueue();


    /**
     * The constructor from the underlying object.
     *
     * @param wrapped        the underlying object
     * @param includeNewLine if true, new line is included into the result
     * @param buffer         the buffer to use (ready for get)
     */
    protected LineStream(final AInput<CharBuffer> wrapped, final boolean includeNewLine, final CharBuffer buffer) {
        super(wrapped);
        this.includeNewLine = includeNewLine;
        this.buffer = buffer;
    }

    /**
     * Read lines as stream.
     *
     * @param input           the input
     * @param includeNewLines if true, new line characters are included into the result
     * @param buffer          the buffer to use (ready for get)
     * @return the line stream
     */
    public static AStream<String> readLines(final AInput<CharBuffer> input, final boolean includeNewLines,
                                            final CharBuffer buffer) {
        return new LineStream(input, includeNewLines, buffer).export();

    }

    /**
     * Read lines as stream.
     *
     * @param input  the input
     * @param buffer the buffer to use (ready for get)
     * @return the line stream
     */
    public static AStream<String> readLines(final AInput<CharBuffer> input, final CharBuffer buffer) {
        return readLines(input, false, buffer);

    }

    /**
     * Read lines as stream.
     *
     * @param input the input
     * @return the line stream
     */
    public static AStream<String> readLines(final AInput<CharBuffer> input) {
        return readLines(input, false);
    }

    /**
     * Read lines as stream.
     *
     * @param input           the input
     * @param includeNewLines if true, new line characters are included into the result
     * @return the line stream
     */
    public static AStream<String> readLines(final AInput<CharBuffer> input, final boolean includeNewLines) {
        final CharBuffer buffer = CharBuffer.allocate(DEFAULT_BUFFER_CAPACITY);
        buffer.limit(0);
        return readLines(input, includeNewLines, buffer);
    }

    /**
     * @return the next produced element
     */
    @Override
    protected Promise<Maybe<String>> produce() {
        return requestQueue.run(new ACallable<Maybe<String>>() {
            @Override
            public Promise<Maybe<String>> call() throws Throwable {
                return TextIOUtil.readLine(wrapped, buffer, line, includeNewLine).map(
                        new AFunction<Maybe<String>, String>() {
                            @Override
                            public Promise<Maybe<String>> apply(final String value) throws Throwable {
                                if (value == null) {
                                    return aMaybeEmpty();
                                } else {
                                    if (line.capacity() > CAPACITY_THRESHOLD) {
                                        line = new StringBuilder();
                                    }
                                    return aMaybeValue(value);
                                }
                            }
                        });
            }
        });
    }
}
