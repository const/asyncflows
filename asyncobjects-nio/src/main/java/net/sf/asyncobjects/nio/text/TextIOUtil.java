package net.sf.asyncobjects.nio.text;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.nio.AInput;

import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqMaybeLoop;

/**
 * The text util.
 */
public final class TextIOUtil {
    /**
     * Unicode code point for NEXT LINE (NEL).
     */
    public static final char NEL = 0x0085;
    /**
     * Unicode code point for PARAGRAPH SEPARATOR.
     */
    public static final char PS = 0x2029;
    /**
     * Unicode code point for LINE SEPARATOR.
     */
    public static final char LS = 0x2028;
    /**
     * Unicode code point for carriage return.
     */
    public static final char CR = 0x000D;
    /**
     * Unicode code point for carriage return.
     */
    public static final char LF = 0x000A;
    /**
     * Unicode code point for LINE TABULATION.
     */
    public static final char LT = 0x000B;
    /**
     * Unicode code point for FORM FEED (FF).
     */
    public static final char FF = 0x000C;

    /**
     * Private constructor for utility class.
     */
    private TextIOUtil() {
    }

    /**
     * <p>Check if codepoint is a new line. The Unicode standard specifies the following new lines.</p>
     * <table>
     * <tr><td>CR</td><td>carriage return</td><td>000D</td></tr>
     * <tr><td>LF</td><td>line feed</td><td>000A</td></tr>
     * <tr><td>CRLF</td><td>carriage return and line feed</td><td>000D 000A</td></tr>
     * <tr><td>NEL</td><td>next line </td><td>0085</td></tr>
     * <tr><td>VT</td><td>vertical tab</td><td>000B</td></tr>
     * <tr><td>FF</td><td>form feed</td><td> 000C</td></tr>
     * <tr><td>LS</td><td>line separator</td><td>2028</td></tr>
     * <tr><td>PS</td><td>paragraph separator</td><td>2029</td></tr>
     * </table>
     *
     * @param codepoint the codepoint to check
     * @return the new lines
     */
    public static boolean isNewline(final int codepoint) {
        switch (codepoint) {
            case LF:  // Cc: LINE FEED (LF)
            case LT:  // Cc: LINE TABULATION
            case FF:  // Cc: FORM FEED (FF)
            case CR:  // Cc: CARRIAGE RETURN (CR)
            case NEL: // Cc: NEXT LINE (NEL)
            case PS:  // Zp: PARAGRAPH SEPARATOR
            case LS:  // Zl: LINE SEPARATOR
                return true;
            default:
                return false;
        }
    }

    /**
     * Read a line from input.
     *
     * @param input          the input stream
     * @param buffer         the buffer to use (ready-for-get)
     * @param line           the string builder to use (remaining chars will be included into the line,
     *                       the buffer is cleared after exit)
     * @param includeNewLine if true, new line characters should be included into result
     * @return the resulting line or null if the last line has been read.
     */
    public static Promise<String> readLine(final AInput<CharBuffer> input, final CharBuffer buffer,
                                           final StringBuilder line, final boolean includeNewLine) {
        return aSeqMaybeLoop(new ACallable<Maybe<String>>() {
            private boolean afterCr;
            private boolean skippedSomething;

            @Override
            public Promise<Maybe<String>> call() throws Throwable {
                while (buffer.hasRemaining()) {
                    if (afterCr) {
                        final char c = buffer.charAt(0);
                        if (c == LF) {
                            buffer.get();
                            appendNewLine(c);
                        }
                        afterCr = false;
                        return result();
                    } else {
                        final char c = buffer.get();
                        if (c == CR) {
                            appendNewLine(c);
                            afterCr = true;
                        } else if (isNewline(c)) {
                            appendNewLine(c);
                            return result();
                        } else {
                            line.append(c);
                        }
                    }
                }
                buffer.compact();
                return input.read(buffer).map(new AFunction<Maybe<String>, Integer>() {
                    @Override
                    public Promise<Maybe<String>> apply(final Integer value) throws Throwable {
                        buffer.flip();
                        if (value < 0) {
                            if (line.length() > 0 || skippedSomething) {
                                return result();
                            } else {
                                return aMaybeValue(null);
                            }
                        }
                        return aMaybeEmpty();
                    }

                });
            }

            private void appendNewLine(final char c) {
                if (includeNewLine) {
                    line.append(c);
                } else {
                    skippedSomething = true;
                }
            }

            private Promise<Maybe<String>> result() {
                final String rc = line.toString();
                line.setLength(0);
                return aMaybeValue(rc);
            }
        });
    }
}
