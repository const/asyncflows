/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.io.AInput;
import org.asyncflows.io.IOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

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
        final ByteBuffer buffer = IOUtil.BYTE.writeBuffer(IOUtil.DEFAULT_BUFFER_SIZE);
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
        final ByteParserContext context = new ByteParserContext(input, compressed);
        return GZipHeader.read(context).flatMap(value -> {
            if (headerResolver != null) {
                notifySuccess(headerResolver, value);
                headerResolver = null;
            }
            return aVoid();
        });
    }

    @Override
    protected void handleDataRead(final byte[] data, final int offset, final int len) {
        crc.update(data, offset, len);
        totalLength += len;
    }

    @Override
    protected Promise<Maybe<Integer>> handleFinish(final AInput<ByteBuffer> input, final ByteBuffer compressed) {
        final ByteParserContext context = new ByteParserContext(input, compressed);
        return context.ensureAvailable(GZipHeader.FOOTER_LENGTH).thenFlatGet(() -> {
            final ByteOrder order = compressed.order();
            compressed.order(ByteOrder.LITTLE_ENDIAN);
            final int footerCRC = compressed.getInt();
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
