package org.asyncflows.io.text;

import org.asyncflows.io.AInput;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.util.CharIOUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.streams.AStream;
import org.asyncflows.core.streams.ChainedStreamBase;
import org.asyncflows.core.util.RequestQueue;

import java.nio.CharBuffer;

import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;

/**
 * The line stream.
 */
public class LineStream extends ChainedStreamBase<String, AInput<CharBuffer>> {
    /**
     * The capacity after which the string builder is recreated.
     */
    private static final int CAPACITY_THRESHOLD = 1024;
    /**
     * If true, new lines are included into the results.
     */
    private final boolean includeNewLine;
    /**
     * The buffer to use.
     */
    private final CharBuffer buffer;
    /**
     * The requests.
     */
    private final RequestQueue requestQueue = new RequestQueue();
    /**
     * The current line.
     */
    private StringBuilder line = new StringBuilder(); // NOPMD


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
        return readLines(input, includeNewLines, IOUtil.CHAR.writeBuffer());
    }

    @Override
    protected Promise<Maybe<String>> produce() {
        return requestQueue.run(() -> CharIOUtil.readLine(wrapped, buffer, line, includeNewLine).flatMap(
                value -> {
                    if (value == null) {
                        return aMaybeEmpty();
                    } else {
                        if (line.capacity() > CAPACITY_THRESHOLD) {
                            line = new StringBuilder();
                        }
                        return aMaybeValue(value);
                    }
                }));
    }
}