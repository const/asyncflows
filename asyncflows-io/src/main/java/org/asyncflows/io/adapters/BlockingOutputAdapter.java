package org.asyncflows.io.adapters;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.NIOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;

import java.io.Closeable;
import java.io.IOException;
import java.nio.Buffer;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The base class for blocking output adapters.
 *
 * @param <B> the buffer type
 * @param <O> the output type
 * @param <A> the array type
 */
public abstract class BlockingOutputAdapter<B extends Buffer, O extends Closeable, A>
        extends CloseableAdapter<O> implements AOutput<B>, NeedsExport<AOutput<B>> {
    /**
     * The data array.
     */
    private A dataArray;

    /**
     * The constructor.
     *
     * @param stream the output stream
     */
    public BlockingOutputAdapter(final O stream) {
        super(stream);
    }

    /**
     * @return the buffer operations
     */
    protected abstract BufferOperations<B, A> operations();

    /**
     * Write data to output from the array.
     *
     * @param output the output to write to
     * @param array  the array
     * @param offset the offset in array
     * @param length the max length to read
     * @throws java.io.IOException in case of IO problem
     */
    protected abstract void write(O output, A array, int offset, int length) throws IOException;

    /**
     * Flush the stream.
     *
     * @param output the output
     * @throws IOException in case of IO problem
     */
    protected abstract void flush(O output) throws IOException;

    @Override
    public final Promise<Void> write(final B buffer) {
        if (buffer.remaining() == 0) {
            return aVoid();
        }
        final BufferOperations<B, A> operations = operations();
        try {
            if (buffer.hasArray()) {
                final A b = operations.array(buffer);
                final int offset = buffer.arrayOffset();
                final int length = buffer.remaining();
                write(stream, b, offset, length);
                buffer.position(buffer.position() + length);
            } else {
                if (dataArray == null) {
                    dataArray = operations.allocate(IOUtil.DEFAULT_BUFFER_SIZE);
                }
                final A b = dataArray;
                final int offset = 0;
                int length;
                do {
                    length = Math.min(IOUtil.DEFAULT_BUFFER_SIZE, buffer.remaining());
                    operations.get(buffer, b, offset, length);
                    write(stream, b, offset, length);
                } while (buffer.hasRemaining());
            }
            return aVoid();
        } catch (IOException ex) {
            return aFailure(ex);
        }
    }

    @Override
    public final Promise<Void> flush() {
        try {
            flush(stream);
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public final AOutput<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }

    /**
     * @return the proxy for blocking stream
     */
    public final AOutput<B> exportBlocking() {
        return NIOExportUtil.exportBlocking(this);
    }
}
