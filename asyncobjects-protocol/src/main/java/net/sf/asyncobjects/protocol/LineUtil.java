package net.sf.asyncobjects.protocol;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.nio.util.InputContext;
import net.sf.asyncobjects.nio.util.OutputContext;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqMaybeLoop;

/**
 * Many internet protocols are line oriented and read the line is a basic functionality needed.
 */
public final class LineUtil {
    /**
     * Unicode code point for carriage return.
     */
    public static final char CR = 0x000D;
    /**
     * Unicode code point for carriage return.
     */
    public static final char LF = 0x000A;
    /**
     * Unicode code point for carriage return.
     */
    public static final char HT = '\t';
    /**
     * The byte mask.
     */
    public static final int MAX_ISO_8859_1 = 0xFF;
    /**
     * Max ASCII character.
     */
    public static final int MAX_ASCII = 127;
    /**
     * The CRLF.
     */
    public static final String CRLF = "\r\n";
    /**
     * The max ASCII control text.
     */
    public static final char MAX_ASCII_CONTROL = 0x1F;
    /**
     * The max ASCII control text.
     */
    public static final char DEL = 0x7F;
    /**
     * The space character code.
     */
    public static final int SPACE = 32;

    /**
     * Private constructor for utility class.
     */
    private LineUtil() {
    }

    /**
     * Read ASCII line into string builder that is ended by CRLF. Many protocols require lines to be ended by these
     * characters. The method might fail for the following conditions.
     * <ul>
     * <li>Line is too long. It is common to limit size of the input in order to prevent DoS attacks.
     * Fails with {@link ProtocolLimitExceededException}</li>
     * <li>Line does not ends with CRLF before EOF. Fails with {@link ProtocolStreamTruncatedException}.
     * The data will be still in the buffer.</li>
     * <li>Line contains non-ASCII characters. Fails with {@link ProtocolCharsetException}</li>
     * </ul>
     *
     * @param input          the input stream
     * @param builder        the string builder for the line
     * @param limit          the maximum size of the line
     * @param failOnNonASCII if true, the operation fails on non-ASCII characters
     * @param allowCR        if true, CR without LF is considered as part of the string
     * @return a promise with amount of bytes in the line, null if eof is encountered,
     */
    public static Promise<Integer> readLineCRLF(final InputContext input, final StringBuilder builder,
                                                final int limit, final boolean failOnNonASCII, final boolean allowCR) {
        return aSeqMaybeLoop(new ACallable<Maybe<Integer>>() {
            private boolean afterCR;
            private int rc;

            @Override
            public Promise<Maybe<Integer>> call() throws Throwable {
                if (!input.hasRemaining()) {
                    if (input.isEofSeen()) {
                        if (rc == 0) {
                            return aMaybeValue(null);
                        } else {
                            throw new ProtocolStreamTruncatedException("The EOF before CRLF in the input");
                        }
                    } else {
                        return input.readMoreEmpty();
                    }
                }
                final ByteBuffer buffer = input.buffer();
                do {
                    final byte b = buffer.get();
                    if (b < 0 && failOnNonASCII) {
                        throw new ProtocolCharsetException("Non-ASCII character is encountered: "
                                + (b & MAX_ISO_8859_1));
                    }
                    rc++;
                    if (rc >= limit) {
                        throw new ProtocolLimitExceededException("The string is larger than set up limit: " + limit);
                    }
                    builder.append((char) (b & MAX_ISO_8859_1));
                    if (afterCR) {
                        if (b == '\n') {
                            return aMaybeValue(rc);
                        } else {
                            throw new ProtocolCharsetException("CR without LF encountered");

                        }
                    }
                    afterCR = b == '\r';
                } while (buffer.hasRemaining());
                return aMaybeEmpty();
            }
        });
    }

    /**
     * Write ASCII text. Note that the method does not sends the text, if it fits to the buffer. Only ASCII characters
     * are allowed otherwise {@link ProtocolCharsetException} is thrown.
     *
     * @param context the context
     * @param text    the text
     * @return the text to write
     */
    public static Promise<Void> writeASCII(final OutputContext context, final String text) {
        return aSeqLoop(new ACallable<Boolean>() {
            private int pos;

            @Override
            public Promise<Boolean> call() throws Throwable {
                final ByteBuffer buffer = context.buffer();
                while (pos < text.length() && buffer.hasRemaining()) {
                    final char ch = text.charAt(pos++);
                    if (ch > MAX_ASCII) {
                        throw new ProtocolCharsetException("Non-ASCII codepoint in string: " + (int) ch);
                    }
                    buffer.put((byte) ch);
                }
                if (pos >= text.length()) {
                    return aFalse();
                } else {
                    return context.send();
                }
            }
        });
    }
}
