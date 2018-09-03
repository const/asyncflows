package org.asyncflows.io.adapters;

import org.asyncflows.io.BufferOperations;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * The adapter for output stream.
 */
public class OutputStreamAdapter extends BlockingOutputAdapter<ByteBuffer, OutputStream, byte[]> {
    /**
     * The constructor.
     *
     * @param stream the output stream
     */
    public OutputStreamAdapter(final OutputStream stream) {
        super(stream);
    }

    @Override
    protected BufferOperations<ByteBuffer, byte[]> operations() {
        return BufferOperations.BYTE;
    }

    @Override
    protected void write(final OutputStream output, final byte[] array, final int offset, final int length)
            throws IOException {
        output.write(array, offset, length);
    }

    @Override
    protected void flush(final OutputStream output) throws IOException {
        output.flush();
    }
}
