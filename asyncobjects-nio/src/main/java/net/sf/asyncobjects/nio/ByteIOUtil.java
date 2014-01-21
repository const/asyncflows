package net.sf.asyncobjects.nio;

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
}
