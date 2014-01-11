package net.sf.asyncobjects.protocol.mime;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.protocol.InputContext;
import net.sf.asyncobjects.protocol.OutputContext;
import net.sf.asyncobjects.protocol.ProtocolCharsetException;
import net.sf.asyncobjects.protocol.ProtocolException;
import net.sf.asyncobjects.protocol.ProtocolLimitExceededException;
import net.sf.asyncobjects.protocol.ProtocolStreamTruncatedException;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;
import static net.sf.asyncobjects.protocol.LineUtil.BYTE_MASK;
import static net.sf.asyncobjects.protocol.LineUtil.writeASCII;

/**
 * Some header utils.
 */
public final class HeaderUtil {
    /**
     * The space character code.
     */
    public static final int SPACE = 32;
    /**
     * The DEL character code.
     */
    public static final int DEL = 127;

    /**
     * Private constructor for utility class.
     */
    private HeaderUtil() {
    }

    /**
     * Check if characters is a valid field name character.
     *
     * @param ch the character to check
     * @return true if character is a valid field name character.
     */
    public static boolean isFieldNameChar(final char ch) {
        return ch > SPACE && ch < DEL && ch != ':';
    }

    /**
     * Read MIME headers.
     *
     * @param input the head input
     * @param limit the limit for the total header size
     * @return the headers
     */
    public static Promise<HeaderSet> readHeaders(final InputContext input, final int limit) { // NOPMD
        final HeaderSet headers = new HeaderSet();
        return aSeqLoop(new ACallable<Boolean>() {
            private static final int LINE_START = 0;
            private static final int NAME = 1;
            private static final int NAME_SP = 2;
            private static final int VALUE = 3;
            private static final int VALUE_AFTER_CR = 4;
            private static final int END_BEFORE_CR = 5;
            private static final int END_AFTER_LF = 6;

            private final StringBuilder current = new StringBuilder(); // NOPMD
            private int size;
            private int state = LINE_START;

            @Override
            public Promise<Boolean> call() throws Throwable { // NOPMD
                if (!input.hasRemaining()) {
                    if (input.isEofSeen()) {
                        throw new ProtocolStreamTruncatedException("EOF before MIME headers ends");
                    } else {
                        return input.readMore().thenValue(true);
                    }
                }
                final ByteBuffer buffer = input.buffer();
                do {
                    if (size + 1 >= limit) {
                        throw new ProtocolLimitExceededException("The MIME headers total size is more than " + limit);
                    }
                    final char c = (char) (buffer.get() & BYTE_MASK);
                    size++;
                    if (c > DEL) {
                        throw new ProtocolCharsetException("The MIME headers could contains only "
                                + "ASCII characters: " + (int) c);
                    }
                    if (state == LINE_START) {
                        if (isLWSP(c)) {
                            if (current.length() == 0) {
                                throw new ProtocolException("Invalid MIME header name start character is encountered: "
                                        + (int) c);
                            }
                            state = VALUE;
                        } else {
                            if (current.length() > 0) {
                                headers.addHeader(current.toString());
                                current.setLength(0);
                            }
                            if (c == '\r') {
                                state = END_BEFORE_CR;
                            } else {
                                // the next MIME header
                                state = NAME;
                            }
                        }
                    }
                    current.append(c);
                    switch (state) {
                        case NAME:
                            if (!isFieldNameChar(c)) {
                                if (c == ':') {
                                    state = VALUE;
                                } else if (isLWSP(c)) {
                                    state = NAME_SP;
                                } else {
                                    throw new ProtocolException("Invalid MIME header name character is encountered: "
                                            + (int) c);
                                }
                            }
                            break;
                        case NAME_SP:
                            if (c == ':') {
                                state = VALUE;
                            } else if (!isLWSP(c)) {
                                throw new ProtocolException("Invalid MIME LWSP character is encountered after name: "
                                        + (int) c);
                            }
                            break;
                        case VALUE:
                            if (c == '\r') {
                                state = VALUE_AFTER_CR;
                            }
                            break;
                        case VALUE_AFTER_CR:
                            if (c == '\n') {
                                state = LINE_START;
                            } else if (c != '\r') {
                                state = VALUE;
                            }
                            break;
                        case END_BEFORE_CR:
                            state = END_AFTER_LF;
                            break;
                        case END_AFTER_LF:
                            if (c == '\n') {
                                return aFalse();
                            } else {
                                throw new ProtocolException("Invalid MIME header name character is encountered: 13 "
                                        + (int) c);
                            }
                        default:
                            throw new IllegalStateException("Invalid state: " + state);
                    }
                } while (buffer.hasRemaining());
                return input.readMore().thenValue(true);
            }
        }).thenValue(headers);
    }

    /**
     * Check if the character is MIME LWSP.
     *
     * @param c the character to check
     * @return true if LWSP character
     */
    private static boolean isLWSP(final char c) {
        return c == '\t' || c == ' ';
    }

    /**
     * Write MIME headers.
     *
     * @param output  the output
     * @param headers the headers to write
     * @return the the promise that resolves when headers are added to output (they are sent yet)
     */
    public static Promise<Void> writeHeaders(final OutputContext output, final HeaderSet headers) {
        return writeASCII(output, headers.toText());
    }
}
