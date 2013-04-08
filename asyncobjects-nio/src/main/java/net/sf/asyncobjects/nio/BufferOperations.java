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

        @Override
        public void rawPut(final ByteBuffer source, final ByteBuffer destination) {
            destination.put(source);
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

        @Override
        public void rawPut(final CharBuffer source, final CharBuffer destination) {
            destination.put(source);
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

    /**
     * Put as much as possible bytes from source buffer to the destination buffer.
     *
     * @param source      the source buffer (ready for read)
     * @param destination the destination buffer (ready for write)
     * @return the amount of bytes.
     */
    public int put(final B source, final B destination) {
        final int result;
        if (source.remaining() <= destination.remaining()) {
            result = source.remaining();
            rawPut(source, destination);
        } else {
            result = destination.remaining();
            final int limit = source.limit();
            source.limit(limit - source.remaining() + result);
            rawPut(source, destination);
            source.limit(limit);
        }
        return result;
    }

    /**
     * Append data to the buffer. The push avoid copying data if it is possible.
     *
     * @param destination the destination buffer (ready for read)
     * @param source      the source buffer (ready for read)
     * @return the amount that has been put
     */
    public int append(final B destination, final B source) {
        final int position = destination.position();
        final int result;
        if (position > 0 && destination.remaining() < source.remaining()) {
            compact(destination);
            result = put(source, destination);
            destination.flip();
        } else {
            destination.position(destination.limit());
            destination.limit(destination.capacity());
            result = put(source, destination);
            destination.limit(destination.position());
            destination.position(position);
        }
        return result;
    }

    /**
     * Put the entire buffer into the destination buffer.
     *
     * @param source      the source to put.
     * @param destination the destination to put.
     */
    public abstract void rawPut(final B source, final B destination);
}
