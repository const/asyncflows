package org.asyncflows.io.adapters;

import org.asyncflows.io.BufferOperations;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * The simple adapter for Reader. The adapter is not thread safe.
 */
public class ReaderAdapter extends BlockingInputAdapter<CharBuffer, Reader, char[]> {
    /**
     * The constructor.
     *
     * @param stream the reader
     */
    public ReaderAdapter(final Reader stream) {
        super(stream);
    }

    @Override
    protected final BufferOperations<CharBuffer, char[]> operations() {
        return BufferOperations.CHAR;
    }

    @Override
    protected final int read(final Reader input, final char[] array, final int offset, final int length)
            throws IOException {
        return input.read(array, offset, length);
    }
}
