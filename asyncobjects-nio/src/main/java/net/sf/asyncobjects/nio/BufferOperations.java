package net.sf.asyncobjects.nio;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Buffer operations depending on buffer type. It is used for generic utilities.
 *
 * @param <B> the buffer type
 * @param <A> the array type
 */
public abstract class BufferOperations<B extends Buffer, A> {
    /**
     * The operations for byte buffers.
     */
    public static final BufferOperations<ByteBuffer, byte[]> BYTE = new BufferOperations<ByteBuffer, byte[]>() {
        @Override
        public byte[] allocate(final int size) {
            return new byte[size];
        }

        @Override
        public void put(final ByteBuffer buffer, final byte[] array, final int arrayOffset, final int length) {
            buffer.put(array, arrayOffset, length);
        }

        @Override
        public byte[] array(final ByteBuffer buffer) {
            return buffer.array();
        }

        @Override
        public void get(final ByteBuffer buffer, final byte[] array, final int offset, final int length) {
            buffer.get(array, offset, length);
        }

        @Override
        public void compact(final ByteBuffer buffer) {
            buffer.compact();
        }
    };

    /**
     * The operations for character buffers.
     */
    public static final BufferOperations<CharBuffer, char[]> CHAR = new BufferOperations<CharBuffer, char[]>() {
        @Override
        public char[] allocate(final int size) {
            return new char[size];
        }

        @Override
        public void put(final CharBuffer buffer, final char[] array, final int arrayOffset, final int length) {
            buffer.put(array, arrayOffset, length);
        }

        @Override
        public char[] array(final CharBuffer buffer) {
            return buffer.array();
        }

        @Override
        public void get(final CharBuffer buffer, final char[] array, final int offset, final int length) {
            buffer.get(array, offset, length);
        }

        @Override
        public void compact(final CharBuffer buffer) {
            buffer.compact();
        }
    };

    /**
     * Allocate array of required type.
     *
     * @param size the size to allocate
     * @return the array
     */
    public abstract A allocate(int size);

    /**
     * Put data from array to the buffer.
     *
     * @param buffer      the buffer
     * @param array       the array
     * @param arrayOffset the array offset
     * @param length      the length to put
     */
    public abstract void put(B buffer, A array, int arrayOffset, int length);

    /**
     * Backing array for the buffer.
     *
     * @param buffer the buffer
     * @return the array type
     */
    public abstract A array(B buffer);

    /**
     * Get data into array.
     *
     * @param buffer the buffer
     * @param array  the array
     * @param offset the offset in the array
     * @param length the length to get
     */
    public abstract void get(B buffer, A array, int offset, int length);

    /**
     * Compact the buffer.
     *
     * @param buffer the buffer to compact
     */
    public abstract void compact(B buffer);
}
