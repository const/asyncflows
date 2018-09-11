/*
 * Copyright (c) 2018 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.io.util;

import org.asyncflows.io.AInput;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ASupplier;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

import static org.asyncflows.io.IOUtil.isEof;
import static org.asyncflows.core.CoreFlows.aBoolean;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqUntilValue;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

/**
 * Character IO utilities.
 */
public final class CharIOUtil {
    /**
     * UTF-8 charset.
     */
    public static final Charset UTF8 = Charset.forName("UTF-8");
    /**
     * UTF-8 charset.
     */
    public static final Charset ISO8859_1 = Charset.forName("ISO-8859-1");
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
        return aSeqWhile(() -> input.read(buffer).flatMap(value -> {
            buffer.flip();
            builder.append(buffer);
            buffer.clear();
            return aBoolean(!isEof(value));
        })).thenFlatGet(() -> aValue(builder.toString()));
    }

    /**
     * <p>Check if codepoint is a new line. The Unicode standard specifies the following new lines.</p>
     * <table>
     * <caption>New Lines</caption>
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
        return aSeqUntilValue(new ASupplier<Maybe<String>>() {
            private boolean afterCr;
            private boolean skippedSomething;

            @Override
            public Promise<Maybe<String>> get() throws Exception {
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
                return input.read(buffer).flatMap(value -> {
                    buffer.flip();
                    if (isEof(value)) {
                        if (line.length() > 0 || skippedSomething) {
                            return result();
                        } else {
                            return aMaybeValue(null);
                        }
                    }
                    return aMaybeEmpty();
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
