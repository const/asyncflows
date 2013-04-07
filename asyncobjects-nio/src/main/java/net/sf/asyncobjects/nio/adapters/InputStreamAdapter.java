package net.sf.asyncobjects.nio.adapters;

import net.sf.asyncobjects.nio.BufferOperations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * The simple adapter for input stream. The adapter is not thread safe.
 */
public class InputStreamAdapter extends BlockingInputAdapter<ByteBuffer, InputStream, byte[]> {
    /**
     * The constructor.
     *
     * @param stream the input stream
     */
    public InputStreamAdapter(final InputStream stream) {
        super(stream);
    }

    @Override
    protected final BufferOperations<ByteBuffer, byte[]> operations() {
        return BufferOperations.BYTE;
    }

    @Override
    protected final int read(final InputStream input, final byte[] array, final int offset, final int length)
            throws IOException {
        return input.read(array, offset, length);
    }
}
