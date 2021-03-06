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

package org.asyncflows.io.text;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.ChainedClosable;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.AOutputProxyFactory;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

/**
 * The character output that encodes bytes to byte output. The output caches characters, and performs actual encode
 * only when buffer is full and it should be sent to downstream. This might cause a error later, when encoder
 * detects errors and next write actually tries to encode invalid data that was sent on the previous write.
 * Also {@link #flush()} will send only complete code points downstream, unless it is called as part of the close
 * operation.
 */
public class EncoderOutput extends ChainedClosable<AOutput<ByteBuffer>>
        implements AOutput<CharBuffer>, ExportableComponent<AOutput<CharBuffer>> {
    /**
     * Additional bytes added to byte buffer.
     */
    private static final int BUFFER_PAD = 4;
    /**
     * The request queue.
     */
    private final RequestQueue requests = new RequestQueue();
    /**
     * The charset encoder.
     */
    private final CharsetEncoder encoder;
    /**
     * The bytes to be send to underlying stream.
     */
    private final ByteBuffer bytes;
    /**
     * The buffered characters that might contain data from previous writes.
     */
    private final CharBuffer chars;


    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     * @param encoder the encoder
     * @param bytes   the byte buffer (ready for put)
     * @param chars   the character buffer (ready for get)
     */
    public EncoderOutput(final AOutput<ByteBuffer> wrapped, final CharsetEncoder encoder,
                         final ByteBuffer bytes, final CharBuffer chars) {
        super(wrapped);
        this.encoder = encoder;
        this.bytes = bytes;
        this.chars = chars;
    }

    /**
     * The character set encoder.
     *
     * @param output  the input
     * @param encoder the encoder
     * @param bytes   the byte buffer to use (ready for put)
     * @param chars   the character buffer to use (ready for get)
     * @return the encoder
     */
    public static AOutput<CharBuffer> encode(final AOutput<ByteBuffer> output, final CharsetEncoder encoder,
                                             final ByteBuffer bytes, final CharBuffer chars) {
        return new EncoderOutput(output, encoder, bytes, chars).export();
    }

    /**
     * The character set encoder.
     *
     * @param output     the input
     * @param encoder    the encoder
     * @param bufferSize the character buffer size (byte buffer size is derived from it using
     *                   {@link java.nio.charset.CharsetEncoder#averageBytesPerChar()})
     * @return the encoder
     */
    public static AOutput<CharBuffer> encode(final AOutput<ByteBuffer> output, final CharsetEncoder encoder,
                                             final int bufferSize) {
        final CharBuffer chars = IOUtil.CHAR.writeBuffer(bufferSize);
        final ByteBuffer bytes = ByteBuffer.allocate((int) (bufferSize * encoder.averageBytesPerChar()) + BUFFER_PAD);
        return encode(output, encoder, bytes, chars);
    }

    /**
     * The character set encoder.
     *
     * @param output     the input
     * @param charset    the character set
     * @param bufferSize the character buffer size (byte buffer size is derived from it using
     *                   {@link java.nio.charset.CharsetEncoder#averageBytesPerChar()})
     * @return the encoder
     */
    public static AOutput<CharBuffer> encode(final AOutput<ByteBuffer> output, final Charset charset,
                                             final int bufferSize) {
        return encode(output, charset.newEncoder(), bufferSize);
    }

    /**
     * The character set encoder.
     *
     * @param output  the input
     * @param charset the character set
     * @return the encoder
     */
    public static AOutput<CharBuffer> encode(final AOutput<ByteBuffer> output, final Charset charset) {
        return encode(output, charset, IOUtil.DEFAULT_BUFFER_SIZE);
    }

    /**
     * The character set encoder.
     *
     * @param output   the input
     * @param encoding the encoding name
     * @return the encoder
     */
    public static AOutput<CharBuffer> encode(final AOutput<ByteBuffer> output, final String encoding) {
        return encode(output, Charset.forName(encoding));
    }

    @Override
    public Promise<Void> write(final CharBuffer buffer) {
        return requests.runSeqWhile(() -> {
            BufferOperations.CHAR.append(chars, buffer);
            if (!buffer.hasRemaining()) {
                return aFalse();
            } else {
                final CoderResult result = encoder.encode(chars, bytes, false);
                if (result.isUnderflow()) {
                    return aTrue();
                } else if (result.isOverflow()) {
                    bytes.flip();
                    return wrapped.write(bytes).thenFlatGet(() -> {
                        bytes.compact();
                        return aTrue();
                    });
                } else {
                    result.throwException();
                    return aFalse();
                }
            }
        }).listen(outcomeChecker());
    }

    /**
     * Flush the buffers.
     *
     * @param eof if true, this is called at the close operation, so the
     * @return the promise that finishes when flush is done.
     */
    public Promise<Void> internalFlush(final boolean eof) {
        return aSeqWhile(() -> {
            if (eof ? !isValid() : !isValidAndOpen()) {
                return invalidationPromise();
            }
            final CoderResult result = encoder.encode(chars, bytes, eof);
            if (result.isOverflow() || result.isUnderflow() && bytes.position() > 0) {
                bytes.flip();
                return wrapped.write(bytes).thenFlatGet(() -> {
                    bytes.compact();
                    return aTrue();
                });
            } else {
                if (result.isError()) {
                    result.throwException();
                }
                return aFalse();
            }
        });
    }

    @Override
    public Promise<Void> flush() {
        return requests.run(() -> internalFlush(false).thenFlatGet(wrapped::flush).listen(outcomeChecker()));
    }

    @Override
    protected Promise<Void> beforeClose() {
        return requests.run(() -> isValid() ? internalFlush(true) : aVoid());
    }

    @Override
    public AOutput<CharBuffer> export(final Vat vat) {
        return AOutputProxyFactory.createProxy(vat, this);
    }
}
