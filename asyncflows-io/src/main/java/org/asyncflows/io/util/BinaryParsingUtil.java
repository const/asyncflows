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

import java.nio.ByteBuffer;

/**
 * Utilities for parsing binary protocols.
 */
public final class BinaryParsingUtil {
    /**
     * The offset.
     */
    private static final int B1_OFFSET = 8;
    /**
     * The offset.
     */
    private static final int B2_OFFSET = 16;
    /**
     * The offset.
     */
    private static final int B3_OFFSET = 24;
    /**
     * The offset.
     */
    private static final int B4_OFFSET = 32;
    /**
     * The offset.
     */
    private static final int B5_OFFSET = 40;
    /**
     * The offset.
     */
    private static final int B6_OFFSET = 48;
    /**
     * The offset.
     */
    private static final int B7_OFFSET = 56;
    /**
     * The offset.
     */
    private static final int BYTE_MASK = 0xFF;

    /**
     * The private constructor.
     */
    private BinaryParsingUtil() {
        // do nothing
    }

    /**
     * Get 16-bit unsigned integer.
     *
     * @param context the context
     * @return the value
     */
    public static int getUInt16(final ByteParserContext context) {
        final ByteBuffer buffer = context.buffer();
        assert buffer.remaining() > Short.BYTES;
        return (maskedByte(buffer) << B1_OFFSET) + maskedByte(buffer);
    }


    /**
     * Get long.
     *
     * @param context the context
     * @return the value
     */
    public static long getLong(final ByteParserContext context) {
        final ByteBuffer buffer = context.buffer();
        assert buffer.remaining() > Short.BYTES;
        return (((long) maskedByte(buffer)) << B7_OFFSET)
                + (((long) maskedByte(buffer)) << B6_OFFSET)
                + (((long) maskedByte(buffer)) << B5_OFFSET)
                + (((long) maskedByte(buffer)) << B4_OFFSET)
                + (((long) maskedByte(buffer)) << B3_OFFSET)
                + (((long) maskedByte(buffer)) << B2_OFFSET)
                + (((long) maskedByte(buffer)) << B1_OFFSET)
                + maskedByte(buffer);
    }

    /**
     * Get masked byte from buffer.
     *
     * @param buffer the buffer.
     * @return the masked byte
     */
    public static int maskedByte(final ByteBuffer buffer) {
        return ((int) buffer.get()) & BYTE_MASK;
    }
}
