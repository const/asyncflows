/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.util.CoreFlowsResource;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.adapters.blocking.Adapters;

import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

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
        return aSeqWhile(new ASupplier<Boolean>() {
            private static final int LINE_SIZE = 72;
            private static final int BEFORE_LINE = 0;
            private static final int IN_LINE = 1;
            private static final int AFTER_LINE = 2;
            private long generated;
            private int line;
            private int state = BEFORE_LINE;

            @Override
            public Promise<Boolean> get() throws Exception {
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
        }).thenFlatGet(() -> context.send().toVoid());
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
        return CoreFlowsResource.aTryResource(output).run(
                value -> IOUtil.BYTE.copy(input, output, false, ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE))
        ).thenPromise(promise);
    }

}
