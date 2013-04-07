package net.sf.asyncobjects.nio.adapters;

import net.sf.asyncobjects.nio.BufferOperations;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * The adapter for writer.
 */
public class WriterAdapter extends BlockingOutputAdapter<CharBuffer, Writer, char[]> {

    /**
     * The constructor.
     *
     * @param stream the writer
     */
    public WriterAdapter(final Writer stream) {
        super(stream);
    }

    @Override
    protected BufferOperations<CharBuffer, char[]> operations() {
        return BufferOperations.CHAR;
    }

    @Override
    protected void write(final Writer output, final char[] array,
                         final int offset, final int length) throws IOException {
        output.write(array, offset, length);
    }

    @Override
    protected void flush(final Writer output) throws IOException {
        output.flush();
    }
}
