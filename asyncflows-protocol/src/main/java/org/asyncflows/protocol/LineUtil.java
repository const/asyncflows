package org.asyncflows.protocol;

import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ASupplier;

import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqUntilValue;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

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
     * Check if the string is blank.
     *
     * @param value the value to check
     * @return true if the string is empty.
     */
    public static boolean isBlank(final String value) {
        if (isEmpty(value)) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isSpaceChar(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the sting is empty.
     *
     * @param value the value to check
     * @return true if null or zero-size
     */
    public static boolean isEmpty(final String value) {
        return value == null || value.isEmpty();
    }


    /**
     * Read Latin-1 line into string builder that is ended by CRLF. Many protocols require lines to be ended by these
     * characters. The method might fail for the following conditions.
     * <ul>
     * <li>Line is too long. It is common to limit size of the input in order to prevent DoS attacks.
     * Fails with {@link ProtocolLimitExceededException}</li>
     * <li>Line does not ends with CRLF before EOF. Fails with
     * {@link ProtocolStreamTruncatedException}. The data will be still in the buffer.</li>
     * <li>Line contains control characters. Fails with
     * {@link ProtocolCharsetException}</li>
     * </ul>
     *
     * @param input the input stream
     * @param limit the maximum size of the line
     * @return a promise with read string or null if EOF is encountered
     */
    public static Promise<String> readLineCRLF(final ByteParserContext input, final int limit) {
        return readLineCRLF(input, limit, false);
    }

    /**
     * Read Latin-1 line into string builder that is ended by CRLF. Many protocols require lines to be ended by these
     * characters. The method might fail for the following conditions.
     * <ul>
     * <li>Line is too long. It is common to limit size of the input in order to prevent DoS attacks.
     * Fails with {@link ProtocolLimitExceededException}</li>
     * <li>Line does not ends with CRLF before EOF. Fails with {@link ProtocolStreamTruncatedException}.
     * The data will be still in the buffer.</li>
     * <li>Line contains control characters. Fails with {@link ProtocolCharsetException}</li>
     * </ul>
     *
     * @param input         the input stream
     * @param limit         the maximum size of the line
     * @param allowLfEnding true if just LF without CR is also allowed ending
     * @return a promise with read string or null if EOF is encountered
     */
    public static Promise<String> readLineCRLF(final ByteParserContext input,
                                               final int limit, final boolean allowLfEnding) {
        final StringBuilder builder = new StringBuilder();
        return aSeqUntilValue(new ASupplier<Maybe<String>>() {
            private boolean afterCR;
            private boolean eof = true;

            @Override
            public Promise<Maybe<String>> get() throws Exception {
                if (!input.hasRemaining()) {
                    if (input.isEofSeen()) {
                        if (eof) {
                            return aMaybeValue(null);
                        } else {
                            throw new ProtocolStreamTruncatedException("The EOF before CRLF in the input");
                        }
                    } else {
                        return input.readMoreEmpty();
                    }
                }
                eof = false;
                final ByteBuffer buffer = input.buffer();
                do {
                    final int b = buffer.get() & MAX_ISO_8859_1;
                    if (b == DEL || b < SPACE && b != '\n' && b != '\r' && b != '\t') {
                        throw new ProtocolCharsetException("Control character is encountered: "
                                + Integer.toHexString(b));
                    }
                    if (afterCR) {
                        if (b == '\n') {
                            return aMaybeValue(builder.toString());
                        } else {
                            throw new ProtocolCharsetException("CR without LF encountered");
                        }
                    } else {
                        if (b == '\r') {
                            afterCR = true;
                        } else if (b == '\n') {
                            if (allowLfEnding) {
                                return aMaybeValue(builder.toString());
                            } else {
                                throw new ProtocolCharsetException("Control character in line: " + b);
                            }
                        } else {
                            builder.append((char) b);
                            if (builder.length() >= limit) {
                                throw new ProtocolLimitExceededException("The string is larger than set up limit: "
                                        + limit);
                            }
                            afterCR = false;
                        }
                    }
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
    public static Promise<Void> writeASCII(final ByteGeneratorContext context, final String text) {
        return aSeqWhile(new ASupplier<Boolean>() {
            private int pos;

            @Override
            public Promise<Boolean> get() throws Exception {
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

    /**
     * Write Latin-1 text. Note that the method does not sends the text, if it fits to the buffer.
     * Only Latin-1 characters are allowed otherwise {@link ProtocolCharsetException} is thrown.
     *
     * @param context the context
     * @param text    the text
     * @return the text to write
     */
    public static Promise<Void> writeLatin1(final ByteGeneratorContext context, final String text) {
        return aSeqWhile(new ASupplier<Boolean>() {
            private int pos;

            @Override
            public Promise<Boolean> get() throws Exception {
                final ByteBuffer buffer = context.buffer();
                while (pos < text.length() && buffer.hasRemaining()) {
                    final char ch = text.charAt(pos++);
                    if (ch > MAX_ISO_8859_1) {
                        throw new ProtocolCharsetException("Non-Latin1 codepoint in string: " + (int) ch);
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
