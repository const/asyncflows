package net.sf.asyncobjects.nio.text;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.stream.AStream;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.codec.ObjectDecoder;
import net.sf.asyncobjects.nio.codec.ObjectDecoderStream;

import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aSuccess;

/**
 * The line decoder.
 */
public class LineDecoder implements ObjectDecoder<CharBuffer, String> {
    /**
     * Unicode code point for carriage return.
     */
    private static final int CR = 0x000D;
    /**
     * Unicode code point for carriage return.
     */
    private static final int LF = 0x000A;
    /**
     * The capacity after which the string builder is recreated.
     */
    private static final int CAPACITY_THRESHOLD = 1024;
    /**
     * If true, new lines are included into the results.
     */
    private final boolean includeNewLine;
    /**
     * The current status.
     */
    private DecodeResult status = DecodeResult.BUFFER_UNDERFLOW;
    /**
     * The current line.
     */
    private StringBuilder line = new StringBuilder(); // NOPMD
    /**
     * If true, buffer is after CR.
     */
    private boolean afterCr;

    /**
     * The constructor.
     *
     * @param includeNewLine if true, new lines are included into the result, if false they are stripped.
     */
    public LineDecoder(final boolean includeNewLine) {
        this.includeNewLine = includeNewLine;
    }

    @Override
    public String getObject() {
        if (status == DecodeResult.OBJECT_READY) {
            final String result = line.toString();
            if (line.capacity() > CAPACITY_THRESHOLD) {
                line = new StringBuilder();
            } else {
                line.setLength(0);
            }
            status = DecodeResult.BUFFER_UNDERFLOW;
            return result;
        } else {
            throw new IllegalStateException("The object is not ready: " + status);
        }
    }

    @Override
    public Exception getFailure() {
        throw new IllegalStateException("The line decoder never fails!");
    }

    @Override
    public Promise<DecodeResult> decode(final CharBuffer buffer, final boolean eof) {
        if (status == DecodeResult.OBJECT_READY
                || status == DecodeResult.FAILURE
                || status == DecodeResult.EOF) {
            return aSuccess(status);
        }
        while (buffer.hasRemaining()) {
            if (afterCr) {
                final char c = buffer.charAt(0);
                if (c == LF) {
                    buffer.get();
                    if (includeNewLine) {
                        line.append(c);
                    }
                }
                afterCr = false;
                status = DecodeResult.OBJECT_READY;
                return aSuccess(status);
            } else {
                final char c = buffer.get();
                if (c == CR) {
                    if (includeNewLine) {
                        line.append(c);
                    }
                    afterCr = true;
                } else if (isNewline(c)) {
                    if (includeNewLine) {
                        line.append(c);
                    }
                    status = DecodeResult.OBJECT_READY;
                    return aSuccess(status);
                } else {
                    line.append(c);
                }
            }
        }
        if (eof && !buffer.hasRemaining()) {
            if (line.length() == 0) {
                status = DecodeResult.EOF;
                return aSuccess(status);
            } else {
                status = DecodeResult.OBJECT_READY;
                return aSuccess(status);
            }
        }
        return aSuccess(status);
    }

    /**
     * <p>Check if codepoint is a new line. The Unicode standard specifies the following new lines.</p>
     * <table>
     * <tr><td>CR</td><td>carriage return</td><td>000D</td></tr>
     * <tr><td>LF</td><td>line feed</td><td>000A</td></tr>
     * <tr><td>CRLF</td><td> carriage return and    line feed</td><td>000D 000A</td></tr>
     * <tr><td>NEL</td><td> next line </td><td>0085</td></tr>
     * <tr><td>VT</td><td>vertical tab</td><td>000B</td></tr>
     * <tr><td>FF</td><td> form feed</td><td> 000C</td></tr>
     * <tr><td>LS</td><td> line separator</td><td>2028</td></tr>
     * <tr><td>PS</td><td> paragraph separator</td><td>2029</td></tr>
     * </table>
     *
     * @param codepoint the codepoint to check
     * @return the new lines
     */
    private static boolean isNewline(final int codepoint) {
        // CHECKSTYLE:OFF
        switch (codepoint) {
            case LF:     // Cc: LINE FEED (LF)
            case 0x000B: // Cc: LINE TABULATION
            case 0x000C: // Cc: FORM FEED (FF)
            case CR:     // Cc: CARRIAGE RETURN (CR)
            case 0x0085: // Cc: NEXT LINE (NEL)
            case 0x2029: // Zp: PARAGRAPH SEPARATOR
            case 0x2028: // Zl: LINE SEPARATOR
                return true;
            default:
                return false;
        }
        // CHECKSTYLE:ON
    }

    /**
     * Decode lines as stream.
     *
     * @param input  the input
     * @param buffer the buffer to use
     * @return the line stream
     */
    public static AStream<String> decodeLines(final AInput<CharBuffer> input, final CharBuffer buffer) {
        return ObjectDecoderStream.decodeChars(input, new LineDecoder(false), buffer);

    }

    /**
     * Decode lines as stream.
     *
     * @param input the input
     * @return the line stream
     */
    public static AStream<String> decodeLines(final AInput<CharBuffer> input) {
        return ObjectDecoderStream.decodeChars(input, new LineDecoder(false));
    }
}
