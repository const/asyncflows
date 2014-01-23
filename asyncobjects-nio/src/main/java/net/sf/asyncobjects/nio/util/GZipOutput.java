package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.IOUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * GZip output stream. The format is specified in
 * <a href="http://tools.ietf.org/html/rfc1952">RFC 1952: GZIP file format specification version 4.3</a>.
 */
public class GZipOutput extends DeflateOutput {
    /**
     * The CRC code.
     */
    private final CRC32 crc = new CRC32();
    /**
     * The header to handle.
     */
    private final GZipHeader header;
    /**
     * The total length.
     */
    private long totalLength;


    /**
     * The constructor.
     *
     * @param output     the output
     * @param compressed the compressed
     * @param header     the GZip header to write
     */
    public GZipOutput(final AOutput<ByteBuffer> output, final ByteBuffer compressed, final GZipHeader header) {
        super(new Deflater(getCompressionLevel(header), true), output, compressed);
        this.header = header;
    }

    /**
     * Get compression level from deflater.
     *
     * @param header the header
     * @return the compression level
     */
    private static int getCompressionLevel(final GZipHeader header) {
        if (header.getCompressionMethod() != GZipHeader.DEFLATE_COMPRESSION) {
            throw new IllegalArgumentException("Unsupported compression method: " + header.getCompressionMethod());
        }
        switch (header.getExtraFlags()) {
            case GZipHeader.FASTEST_COMPRESSION:
                return Deflater.BEST_SPEED;
            case GZipHeader.MAXIMUM_COMPRESSION:
                return Deflater.BEST_COMPRESSION;
            default:
                throw new IllegalArgumentException("Unsupported compression speed: " + header.getExtraFlags());
        }
    }

    /**
     * Run GZip over the stream.
     *
     * @param output the output.
     * @return the stream
     */
    public static AOutput<ByteBuffer> gzip(final AOutput<ByteBuffer> output) {
        final ByteBuffer buffer = ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE);
        return new GZipOutput(output, buffer, new GZipHeader()).export();
    }

    @Override
    protected Promise<Void> handleHeader(final AOutput<ByteBuffer> output, final ByteBuffer compressed) {
        final OutputContext context = new OutputContext(output, compressed);
        return GZipHeader.writeHeader(context, header);
    }

    @Override
    protected void handleWrittenData(final byte[] data, final int offset, final int len) {
        crc.update(data, offset, len);
        totalLength += len;
    }

    @Override
    protected Promise<Void> handleFinish(final AOutput<ByteBuffer> output, final ByteBuffer compressed) {
        final OutputContext context = new OutputContext(output, compressed);
        return context.ensureAvailable(GZipHeader.FOOTER_LENGTH).thenDo(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                final ByteOrder order = compressed.order();
                compressed.order(ByteOrder.LITTLE_ENDIAN);
                compressed.putInt((int) crc.getValue());
                compressed.putInt((int) totalLength);
                compressed.order(order);
                return context.send().toVoid();
            }
        });
    }
}
