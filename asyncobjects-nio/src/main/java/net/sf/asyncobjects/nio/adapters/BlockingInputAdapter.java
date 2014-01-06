package net.sf.asyncobjects.nio.adapters;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.BufferOperations;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.Buffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aValue;

/**
 * The simple adapter for JDK input. The adapter is not thread safe.
 *
 * @param <B> the buffer type
 * @param <I> the input type
 * @param <A> the array type
 */
public abstract class BlockingInputAdapter<B extends Buffer, I extends Closeable, A>
        extends CloseableAdapter<I> implements AInput<B>, ExportsSelf<AInput<B>> {
    /**
     * The data array.
     */
    private A dataArray;

    /**
     * The constructor.
     *
     * @param stream the input stream
     */
    public BlockingInputAdapter(final I stream) {
        super(stream);
    }

    /**
     * @return the buffer operations
     */
    protected abstract BufferOperations<B, A> operations();

    /**
     * Read data from input into the array.
     *
     * @param input  the input to read from
     * @param array  the array
     * @param offset the offset in array
     * @param length the max length to read
     * @return the read size
     * @throws IOException in case of IO problem
     */
    protected abstract int read(I input, A array, int offset, int length) throws IOException;

    @Override
    public final Promise<Integer> read(final B buffer) {
        if (buffer.remaining() == 0) {
            return aValue(0);
        }
        final BufferOperations<B, A> operations = operations();
        A b;
        int offset;
        int length;
        if (buffer.hasArray()) {
            b = operations.array(buffer);
            offset = buffer.arrayOffset();
            length = buffer.remaining();
        } else {
            if (dataArray == null) {
                dataArray = operations.allocate(IOUtil.DEFAULT_BUFFER_SIZE);
            }
            b = dataArray;
            offset = 0;
            length = Math.min(IOUtil.DEFAULT_BUFFER_SIZE, buffer.remaining());
        }
        try {
            final int rc = read(stream, b, offset, length);
            if (rc > 0) {
                if (buffer.hasArray()) {
                    buffer.position(buffer.position() + rc);
                } else {
                    operations.put(buffer, b, offset, rc);
                }
            }
            return aValue(rc);
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public final AInput<B> export() {
        return export(Vat.current());
    }

    @Override
    public final AInput<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }

    /**
     * @return the proxy for blocking stream
     */
    public final AInput<B> exportBlocking() {
        return NIOExportUtil.exportBlocking(this);
    }
}
