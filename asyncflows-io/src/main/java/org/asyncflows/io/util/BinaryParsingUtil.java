package org.asyncflows.io.util;

import java.nio.ByteBuffer;

/**
 * Utilities for parsing binary protocols.
 */
public final class BinaryParsingUtil {
    private static int B1_OFFSET = 8;
    private static int B2_OFFSET = 16;
    private static int B3_OFFSET = 24;
    private static int B4_OFFSET = 32;
    private static int B5_OFFSET = 40;
    private static int B6_OFFSET = 48;
    private static int B7_OFFSET = 56;
    private static int BYTE_MASK = 0xFF;

    /**
     * The private constructor.
     */
    private BinaryParsingUtil() {
        // do nothing
    }

    public static int getUInt16(ByteParserContext context) {
        final ByteBuffer buffer = context.buffer();
        assert buffer.remaining() > Short.BYTES;
        return (maskedByte(buffer) << B1_OFFSET) + maskedByte(buffer);
    }


    public static long getLong(ByteParserContext context) {
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
