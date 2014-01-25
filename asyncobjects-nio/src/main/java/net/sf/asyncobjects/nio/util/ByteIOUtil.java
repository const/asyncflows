package net.sf.asyncobjects.nio.util;

import java.nio.ByteBuffer;

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
        return buffer.put(text.getBytes(CharIOUtil.ISO_8859_1));
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


}
