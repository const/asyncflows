package org.asyncflows.io.util;

import org.asyncflows.io.AInput;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.NIOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.Inflater;

import static org.asyncflows.core.AsyncControl.aMaybeEmpty;
import static org.asyncflows.core.AsyncControl.aMaybeValue;
import static org.asyncflows.core.AsyncControl.aVoid;

/**
 * Inflate input. The input is able to use data from the underlying stream. If inflater fails, the buffer data should
 * be considered as invalid.
 *
 * @see DeflateOutput
 */
public class InflateInput extends CloseableInvalidatingBase implements
        AInput<ByteBuffer>, NeedsExport<AInput<ByteBuffer>> {
    /**
     * The size of temporary array.
     */
    private static final int TEMP_ARRAY_SIZE = 1024;
    /**
     * The inflater.
     */
    private final Inflater inflater;
    /**
     * The underling input stream.
     */
    private final AInput<ByteBuffer> input;
    /**
     * The compressed input buffer.
     */
    private final ByteBuffer compressed;
    /**
     * The requests.
     */
    private final RequestQueue reads = new RequestQueue();
    /**
     * If true, the eof has been read.
     */
    private boolean eofRead;
    /**
     * The temporary array.
     */
    private byte[] tempArray;
    /**
     * True if the header has been read.
     */
    private boolean headerRead;

    /**
     * The constructor.
     *
     * @param inflater   the inflater to use.
     * @param input      the compressed input stream.
     * @param compressed the compressed buffer (in ready for read state).
     */
    public InflateInput(final Inflater inflater, final AInput<ByteBuffer> input, final ByteBuffer compressed) {
        this.inflater = inflater;
        this.input = input;
        this.compressed = compressed;
        if (!compressed.hasArray()) {
            throw new IllegalArgumentException("The buffer with compressed data must have an array: " + compressed);
        }
    }

    /**
     * The inflated input.
     *
     * @param input      the input
     * @param bufferSize the size of compressed buffer
     * @return the inflating input
     */
    public static AInput<ByteBuffer> inflated(final AInput<ByteBuffer> input, final int bufferSize) {
        final ByteBuffer buffer = IOUtil.BYTE.writeBuffer(bufferSize);
        return new InflateInput(new Inflater(), input, buffer);
    }

    @Override
    public Promise<Integer> read(final ByteBuffer buffer) {
        return reads.runSeqUntilValue(() -> {
            try {
                ensureValidAndOpen();
                if (!headerRead) {
                    headerRead = true;
                    return handleHeader(input, compressed)
                            .listen(outcomeChecker())
                            .thenValue(Maybe.<Integer>empty());
                }
                if (!buffer.hasRemaining()) {
                    throw new IllegalArgumentException("The buffer must have some bytes remaining.");
                }
                if (inflater.finished()) {
                    return handleFinish(input, compressed);
                }
                if (compressed.hasRemaining()) {
                    inflater.setInput(compressed.array(),
                            compressed.arrayOffset() + compressed.position(), compressed.remaining());
                    byte[] data;
                    int offset;
                    int len;
                    if (buffer.hasArray()) {
                        data = buffer.array();
                        offset = buffer.arrayOffset() + buffer.position();
                        len = buffer.remaining();
                    } else {
                        if (tempArray == null) {
                            tempArray = new byte[TEMP_ARRAY_SIZE];
                        }
                        data = tempArray;
                        offset = 0;
                        len = Math.min(tempArray.length, buffer.remaining());
                    }
                    final int rc = inflater.inflate(data, offset, len);
                    compressed.position(compressed.limit() - inflater.getRemaining());
                    if (rc == 0) {
                        if (inflater.finished()) {
                            return handleFinish(input, compressed);
                        } else {
                            return aMaybeEmpty();
                        }
                    } else if (rc > 0) {
                        if (buffer.hasArray()) {
                            buffer.position(buffer.position() + rc);
                        } else {
                            buffer.put(data, offset, rc);
                        }
                        handleDataRead(data, offset, rc);
                        return aMaybeValue(rc);
                    } else {
                        throw new IllegalStateException("Negative return code: " + rc);
                    }
                } else {
                    if (!eofRead) {
                        compressed.compact();
                        return input.read(compressed).listen(outcomeChecker()).flatMap(
                                value -> {
                                    compressed.flip();
                                    if (IOUtil.isEof(value)) {
                                        eofRead = true;
                                    }
                                    return aMaybeEmpty();
                                });
                    } else {
                        throw new IOException("The EOF should not be read by inflater, it should finish earlier");
                    }
                }
            } catch (Throwable t) {
                invalidate(t);
                throw t;
            }
        });
    }

    /**
     * Handle custom header if it exists (for the case of {@link GZipInput}).
     *
     * @param compressedInput the input
     * @param buffer          the compressed data
     * @return the promise
     */
    protected Promise<Void> handleHeader(final AInput<ByteBuffer> compressedInput, final ByteBuffer buffer) {
        return aVoid();
    }

    /**
     * Handle read data.
     *
     * @param data   the array with data
     * @param offset the offset in the array
     * @param len    the length of the data in array
     */
    protected void handleDataRead(final byte[] data, final int offset, final int len) {
        // the default implementation does nothing.
    }

    /**
     * /**
     * Handle finish of the stream. The subclasses like {@link GZipInput} use this data to read post-stream data.
     *
     * @param compressedInput the input
     * @param buffer          the compressed data
     * @return the promise for EOF.
     */
    protected Promise<Maybe<Integer>> handleFinish(final AInput<ByteBuffer> compressedInput, final ByteBuffer buffer) {
        return IOUtil.EOF_MAYBE_PROMISE;
    }

    @Override
    public AInput<ByteBuffer> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
