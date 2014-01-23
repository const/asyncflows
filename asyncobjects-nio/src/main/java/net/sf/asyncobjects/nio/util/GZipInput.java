package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.IOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * GZip input stream.
 */
public class GZipInput extends InflateInput {
    /**
     * The CRC code.
     */
    private final CRC32 crc = new CRC32();
    /**
     * The total length.
     */
    private long totalLength;
    /**
     * The GZip header.
     */
    private AResolver<GZipHeader> headerResolver;

    /**
     * The constructor.
     *
     * @param input          the compressed input stream.
     * @param compressed     the compressed buffer (in ready for read state).
     * @param headerResolver the resolver for the header (optional)
     */
    public GZipInput(final AInput<ByteBuffer> input, final ByteBuffer compressed,
                     final AResolver<GZipHeader> headerResolver) {
        super(new Inflater(true), input, compressed);
        this.headerResolver = headerResolver;
    }

    /**
     * Decompress data.
     *
     * @param input          the input
     * @param headerResolver the resolver
     * @return unzipped data
     */
    public static AInput<ByteBuffer> gunzip(final AInput<ByteBuffer> input,
                                            final AResolver<GZipHeader> headerResolver) {
        final ByteBuffer buffer = ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE);
        buffer.limit(0);
        return new GZipInput(input, buffer, headerResolver).export();
    }

    /**
     * Decompress data.
     *
     * @param input the input
     * @return unzipped data
     */
    public static AInput<ByteBuffer> gunzip(final AInput<ByteBuffer> input) {
        return gunzip(input, null);
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        if (headerResolver != null) {
            notifyFailure(headerResolver, throwable);
            headerResolver = null;
        }
        super.onInvalidation(throwable);
    }

    @Override
    protected Promise<Void> handleHeader(final AInput<ByteBuffer> input, final ByteBuffer compressed) {
        final InputContext context = new InputContext(input, compressed);
        return GZipHeader.read(context).map(new AFunction<Void, GZipHeader>() {
            @Override
            public Promise<Void> apply(final GZipHeader value) throws Throwable {
                if (headerResolver != null) {
                    notifySuccess(headerResolver, value);
                    headerResolver = null;
                }
                return aVoid();
            }
        });
    }

    @Override
    protected void handleDataRead(final byte[] data, final int offset, final int len) {
        crc.update(data, offset, len);
        totalLength += len;
    }

    @Override
    protected Promise<Maybe<Integer>> handleFinish(final AInput<ByteBuffer> input, final ByteBuffer compressed) {
        final InputContext context = new InputContext(input, compressed);
        return context.ensureAvailable(GZipHeader.FOOTER_LENGTH).thenDo(new ACallable<Maybe<Integer>>() {
            @Override
            public Promise<Maybe<Integer>> call() throws Throwable {
                final ByteOrder order = compressed.order();
                compressed.order(ByteOrder.LITTLE_ENDIAN);
                final int footerCRC = compressed.getInt(); // NOPMD
                final int footerLength = compressed.getInt();
                compressed.order(order);
                final int accumulatedLength = (int) totalLength;
                if (footerLength != accumulatedLength) {
                    throw new IOException("Footer length does not match actual: file=" + footerLength
                            + " <> actual=" + accumulatedLength
                            + (accumulatedLength == totalLength ? "" : " (64-bit value = " + totalLength + ")"));
                }
                final int accumulatedCRC = (int) crc.getValue();
                if (footerCRC != accumulatedCRC) {
                    throw new IOException("CRC does not match actual: file=" + footerCRC
                            + " <> actual=" + accumulatedCRC);
                }
                return IOUtil.EOF_MAYBE_PROMISE;
            }
        });
    }

    @Override
    protected Promise<Void> closeAction() {
        if (headerResolver != null) {
            notifyFailure(headerResolver, new IOException("closed stream before header has been read"));
        }
        return super.closeAction();
    }
}
