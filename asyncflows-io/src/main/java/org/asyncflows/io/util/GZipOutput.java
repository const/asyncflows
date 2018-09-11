/*
 * Copyright (c) 2018 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.io.util;

import org.asyncflows.io.AOutput;
import org.asyncflows.io.IOUtil;
import org.asyncflows.core.Promise;

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
        final ByteGeneratorContext context = new ByteGeneratorContext(output, compressed);
        return GZipHeader.writeHeader(context, header);
    }

    @Override
    protected void handleWrittenData(final byte[] data, final int offset, final int len) {
        crc.update(data, offset, len);
        totalLength += len;
    }

    @Override
    protected Promise<Void> handleFinish(final AOutput<ByteBuffer> output, final ByteBuffer compressed) {
        final ByteGeneratorContext context = new ByteGeneratorContext(output, compressed);
        return context.ensureAvailable(GZipHeader.FOOTER_LENGTH).thenFlatGet(() -> {
            final ByteOrder order = compressed.order();
            compressed.order(ByteOrder.LITTLE_ENDIAN);
            compressed.putInt((int) crc.getValue());
            compressed.putInt((int) totalLength);
            compressed.order(order);
            return context.send().toVoid();
        });
    }
}
