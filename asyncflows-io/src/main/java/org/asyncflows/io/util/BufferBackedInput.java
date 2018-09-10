package org.asyncflows.io.util;

import org.asyncflows.io.AInput;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.IOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.NeedsExport;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static org.asyncflows.core.CoreFlows.aValue;

/**
 * The reader over buffer. It just reads data from the buffer until it ends.
 *
 * @param <B> the buffer type
 */
public class BufferBackedInput<B extends Buffer> extends CloseableBase implements AInput<B>, NeedsExport<AInput<B>> {
    /**
     * The buffer operations.
     */
    private final BufferOperations<B, ?> operations;
    /**
     * The buffer content.
     */
    private final B content;

    /**
     * The constructor.
     *
     * @param operations the operations.
     * @param content    the content.
     */
    public BufferBackedInput(final BufferOperations<B, ?> operations, final B content) {
        this.operations = operations;
        this.content = content;
    }

    /**
     * Wrap buffer.
     *
     * @param buffer the buffer to wrap
     * @return the wrapped buffer
     */
    public static AInput<ByteBuffer> wrap(final ByteBuffer buffer) {
        return new BufferBackedInput<>(BufferOperations.BYTE, buffer).export();
    }

    /**
     * Wrap array.
     *
     * @param data   the data
     * @param offset the offset
     * @param length the length
     * @return the wrapped array
     */
    public static AInput<ByteBuffer> wrap(final byte[] data, final int offset, final int length) {
        return wrap(ByteBuffer.wrap(data, offset, length));
    }

    /**
     * Wrap array.
     *
     * @param data the data
     * @return the wrapped array
     */
    public static AInput<ByteBuffer> wrap(final byte[] data) {
        return wrap(data, 0, data.length);
    }

    /**
     * Wrap buffer.
     *
     * @param buffer the buffer to wrap
     * @return the wrapped buffer
     */
    public static AInput<CharBuffer> wrap(final CharBuffer buffer) {
        return new BufferBackedInput<>(BufferOperations.CHAR, buffer).export();
    }

    /**
     * Wrap array.
     *
     * @param data   the data
     * @param offset the offset
     * @param length the length
     * @return the wrapped array
     */
    public static AInput<CharBuffer> wrap(final char[] data, final int offset, final int length) {
        return wrap(CharBuffer.wrap(data, offset, length));
    }

    /**
     * Wrap array.
     *
     * @param data the data
     * @return the wrapped array
     */
    public static AInput<CharBuffer> wrap(final char[] data) { //NOPMD
        return wrap(data, 0, data.length);
    }

    @Override
    public Promise<Integer> read(final B buffer) {
        ensureOpen();
        if (!content.hasRemaining()) {
            return IOUtil.EOF_PROMISE;
        }
        return aValue(operations.put(buffer, content));
    }

    @Override
    public AInput<B> export(final Vat vat) {
        return IOExportUtil.export(vat, this);
    }

}