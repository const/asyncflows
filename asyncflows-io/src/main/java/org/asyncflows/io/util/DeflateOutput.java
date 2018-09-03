package org.asyncflows.io.util;

import org.asyncflows.io.AOutput;
import org.asyncflows.io.NIOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;

import static org.asyncflows.core.AsyncControl.aFailure;
import static org.asyncflows.core.AsyncControl.aFalse;
import static org.asyncflows.core.AsyncControl.aTrue;
import static org.asyncflows.core.AsyncControl.aVoid;
import static org.asyncflows.core.util.AsyncSeqControl.aSeq;
import static org.asyncflows.core.util.AsyncSeqControl.aSeqWhile;

/**
 * The deflate output stream. Note that flush operation is for this stream is no-op. The {@link DeflateOutput}
 * does not close the underlying stream, because it is mostly used to encode a subset of data.
 * The class uses {@link Deflater} to compress data. The format is specified in
 * <a href="http://tools.ietf.org/html/rfc1951">RFC 1951: DEFLATE Compressed Data Format Specification version 1.3</a>
 * and <a href="http://tools.ietf.org/html/rfc1951">RFC 1950: ZLIB Compressed Data Format Specification version 3.3</a>.
 *
 * @see InflateInput
 */
public class DeflateOutput extends CloseableInvalidatingBase implements
        AOutput<ByteBuffer>, NeedsExport<AOutput<ByteBuffer>> {
    /**
     * The size of temporary array.
     */
    private static final int TEMP_ARRAY_SIZE = 1024;
    /**
     * The deflater.
     */
    private final Deflater deflater;
    /**
     * The underling output stream.
     */
    private final AOutput<ByteBuffer> output;
    /**
     * The compressed output buffer.
     */
    private final ByteBuffer compressed;
    /**
     * The requests.
     */
    private final RequestQueue writes = new RequestQueue();
    /**
     * The temporary array.
     */
    private byte[] tempArray;
    /**
     * true if header has been written.
     */
    private boolean headerWritten;
    // TODO flush method


    /**
     * The constructor.
     *
     * @param deflater   the deflater
     * @param output     the output
     * @param compressed the compressed
     */
    public DeflateOutput(final Deflater deflater, final AOutput<ByteBuffer> output, final ByteBuffer compressed) {
        this.deflater = deflater;
        this.output = output;
        this.compressed = compressed;
        if (!compressed.hasArray()) {
            throw new IllegalArgumentException("The buffer with compressed data must have an array: " + compressed);
        }
    }

    /**
     * Get deflated stream.
     *
     * @param output     the stream
     * @param bufferSize the buffer size
     * @return the deflated output
     */
    public static AOutput<ByteBuffer> deflated(final AOutput<ByteBuffer> output, final int bufferSize) {
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        final Deflater deflater = new Deflater();
        return new DeflateOutput(deflater, output, buffer).export();
    }

    /**
     * Handle custom header if it exists (for the case of {@link GZipOutput}).
     *
     * @param compressedOutput the output
     * @param buffer           the compressed data
     * @return the promise for void
     */
    protected Promise<Void> handleHeader(final AOutput<ByteBuffer> compressedOutput, final ByteBuffer buffer) {
        return aVoid();
    }

    /**
     * Handle read data.
     *
     * @param data   the array with data
     * @param offset the offset in the array
     * @param len    the length of the data in array
     */
    protected void handleWrittenData(final byte[] data, final int offset, final int len) {
        // the default implementation does nothing.
    }

    /**
     * Handle finish of the stream. The subclasses like {@link GZipOutput} use this data to write post-stream data.
     *
     * @param compressedOutput the output
     * @param buffer           the compressed data
     * @return the promise for void.
     */
    protected Promise<Void> handleFinish(final AOutput<ByteBuffer> compressedOutput, final ByteBuffer buffer) {
        return aVoid();
    }

    @Override
    public Promise<Void> write(final ByteBuffer buffer) {
        return writes.runSeqWhile(() -> {
            ensureValidAndOpen();
            if (!headerWritten) {
                headerWritten = true;
                return handleHeader(output, compressed).thenValue(true);
            }
            if (!buffer.hasRemaining()) {
                return aFalse();
            }
            final byte[] data;
            final int offset;
            final int len;
            if (buffer.hasArray()) {
                data = buffer.array();
                offset = buffer.arrayOffset() + buffer.position();
                len = Math.min(buffer.remaining(), compressed.capacity());
            } else {
                if (tempArray == null) {
                    tempArray = new byte[TEMP_ARRAY_SIZE];
                }
                data = tempArray;
                offset = 0;
                len = Math.min(Math.min(tempArray.length, buffer.remaining()), compressed.capacity());
                final int p = buffer.position();
                buffer.get(tempArray, 0, len);
                buffer.position(p);
            }
            deflater.setInput(data, offset, len);
            return aSeqWhile(() -> {
                if (deflater.needsInput()) {
                    buffer.position(buffer.position() + len);
                    handleWrittenData(data, offset, len);
                    return aFalse();
                } else {
                    return deflateAndWrite();
                }
            }).thenValue(true);
        }).listen(outcomeChecker());
    }

    /**
     * Deflate and write currently set input.
     *
     * @return the promise for true
     */
    private Promise<Boolean> deflateAndWrite() {
        final int count = deflater.deflate(compressed.array(),
                compressed.arrayOffset() + compressed.position(), compressed.remaining());
        if (count > 0) {
            compressed.position(compressed.position() + count);
            compressed.flip();
            return output.write(compressed).flatMapOutcome(value -> {
                compressed.compact();
                if (value.isSuccess()) {
                    return aTrue();
                } else {
                    return aFailure(value.failure());
                }
            }).listen(outcomeChecker());
        } else {
            return aTrue();
        }
    }

    @Override
    public Promise<Void> flush() {
        return writes.run(() -> {
            ensureValidAndOpen();
            return output.flush().listen(outcomeChecker());
        });
    }

    @Override
    protected Promise<Void> closeAction() {
        if (isValid()) {
            return aSeq(() -> {
                if (headerWritten) {
                    return aVoid();
                } else {
                    headerWritten = true;
                    return handleHeader(output, compressed);
                }
            }).thenDo(() -> {
                deflater.setInput(ByteIOUtil.EMPTY_ARRAY, 0, 0);
                deflater.finish();
                return aSeqWhile(() -> {
                    if (deflater.finished()) {
                        return aFalse();
                    } else {
                        return deflateAndWrite();
                    }
                });
            }).thenDoLast(() -> handleFinish(output, compressed));
        } else {
            return invalidationPromise();
        }
    }

    @Override
    public AOutput<ByteBuffer> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
