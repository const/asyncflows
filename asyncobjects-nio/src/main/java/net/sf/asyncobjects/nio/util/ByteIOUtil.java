package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ResourceUtil;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.BufferOperations;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.adapters.Adapters;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

/**
 * Utilities specific for binary IO.
 */
public final class ByteIOUtil {
    /**
     * The empty array of bytes.
     */
    public static final byte[] EMPTY_ARRAY = new byte[0];
    /**
     * The byte mask.
     */
    public static final int BYTE_MASK = 0xFF;
    /**
     * The 32-bin unsigned integer.
     */
    public static final long UINT32_MASK = 0xFFFFFFFFL;
    /**
     * The mask for 16-bit unsigned integer.
     */
    public static final int UINT16_MASK = 0xFFFF;
    /**
     * The mask for 16-bit unsigned integer.
     */
    public static final int UINT16_LENGTH = 2;

    /**
     * Private constructor for utility class.
     */
    private ByteIOUtil() {
        // do nothing
    }

    /**
     * Convert byte to character in ISO-8859-1.
     *
     * @param b the byte
     * @return the character
     */
    public static char toLatin1(final int b) {
        return (char) (b & BYTE_MASK);
    }

    /**
     * Put ISO-8859-1 bytes to the buffer.
     *
     * @param buffer the destination buffer.
     * @param text   the text to put
     * @return the bytes
     */
    public static ByteBuffer putLatin1(final ByteBuffer buffer, final String text) {
        return buffer.put(text.getBytes(CharIOUtil.ISO8859_1));
    }

    /**
     * Get ISO-8859-1 bytes from the buffer.
     *
     * @param buffer the source buffer.
     * @return the text
     */
    public static String getLatin1(final ByteBuffer buffer) {
        final StringBuilder b = new StringBuilder(buffer.remaining());
        while (buffer.hasRemaining()) {
            b.append(toLatin1(buffer.get()));
        }
        return b.toString();
    }

    /**
     * Convert two bytes to unsigned short value.
     *
     * @param b1 the high byte
     * @param b2 the lower byte
     * @return the char
     */
    public static char toChar(final int b1, final int b2) {
        // CHECKSTYLE:OFF
        return (char) (((b1 & BYTE_MASK) << 8) | (b2 & BYTE_MASK));
        // CHECKSTYLE:ON
    }

    /**
     * Convert 4 bytes to int value.
     *
     * @param b1 the most significant byte
     * @param b2 the byte
     * @param b3 the byte
     * @param b4 the least significant byte
     * @return the singed integer
     */
    public static int toInt(final int b1, final int b2, final int b3, final int b4) {
        // CHECKSTYLE:OFF
        return ((b1 & BYTE_MASK) << 24) | ((b2 & BYTE_MASK) << 16) | ((b3 & BYTE_MASK) << 8) | (b4 & BYTE_MASK);
        // CHECKSTYLE:ON
    }

    /**
     * Convert 4 bytes to 32-bit value.
     *
     * @param b1 the most significant byte
     * @param b2 the byte
     * @param b3 the byte
     * @param b4 the least significant byte
     * @return the unsigned int represented as long
     */
    public static long toUInt32(final int b1, final int b2, final int b3, final int b4) {
        return toInt(b1, b2, b3, b4) & UINT32_MASK;
    }


    /**
     * Generate characters according to <a href="http://tools.ietf.org/html/rfc864">chargen protocol</a>
     * (this method is used for testing).
     *
     * @param output     the output
     * @param length     the length to generate
     * @param bufferSize the buffer size to use
     * @return the characters to generate
     */
    public static Promise<Void> charGen(final AOutput<ByteBuffer> output, final long length, final int bufferSize) {
        final ByteGeneratorContext context = new ByteGeneratorContext(output, bufferSize);
        final int count = '\u007f' - ' ';
        final byte[] data = new byte[count * 2];
        for (int i = 0; i < count; i++) {
            data[i] = (byte) (' ' + i);
            data[i + count] = (byte) (' ' + i);
        }
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        final ByteBuffer crlf = ByteBuffer.wrap(new byte[]{CharIOUtil.CR, CharIOUtil.LF});
        return aSeqLoop(new ACallable<Boolean>() {
            private static final int LINE_SIZE = 72;
            private static final int BEFORE_LINE = 0;
            private static final int IN_LINE = 1;
            private static final int AFTER_LINE = 2;
            private long generated;
            private int line;
            private int state = BEFORE_LINE;

            @Override
            public Promise<Boolean> call() throws Exception {
                while (true) {
                    if (length <= generated) {
                        return aFalse();
                    }
                    switch (state) {
                        case BEFORE_LINE:
                            line = (line + 1) % count;
                            buffer.position(line);
                            buffer.limit(buffer.position() + (int) Math.min(LINE_SIZE, length - generated));
                            state = IN_LINE;
                            break;
                        case IN_LINE:
                            generated += BufferOperations.BYTE.put(context.buffer(), buffer);
                            if (buffer.hasRemaining()) {
                                return context.send();
                            } else {
                                state = AFTER_LINE;
                                crlf.position(0).limit((int) Math.min(crlf.capacity(), length - generated));
                                break;
                            }
                        case AFTER_LINE:
                            generated += BufferOperations.BYTE.put(context.buffer(), crlf);
                            if (crlf.hasRemaining()) {
                                return context.send();
                            } else {
                                state = BEFORE_LINE;
                                break;
                            }
                        default:
                            throw new IllegalStateException("Invalid state: " + state);
                    }
                }
            }
        }).thenDo(() -> context.send().toVoid());
    }

    /**
     * Get content of the input stream.
     *
     * @param input the input
     * @return the content of the stream
     */
    public static Promise<byte[]> getContent(final AInput<ByteBuffer> input) {
        final Promise<byte[]> promise = new Promise<>();
        final AResolver<byte[]> resolver = promise.resolver();
        final AOutput<ByteBuffer> output = Adapters.getByteArrayOutput(resolver);
        return ResourceUtil.aTryResource(output).run(
                value -> IOUtil.BYTE.copy(input, output, false, ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE))
        ).thenPromise(promise);
    }

}
