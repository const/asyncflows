/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AInputProxyFactory;
import org.asyncflows.io.IOUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;
import static org.asyncflows.io.IOUtil.isEof;

/**
 * The character decoder input.
 */
public class DecoderInput extends ChainedClosable<AInput<ByteBuffer>>
        implements AInput<CharBuffer>, NeedsExport<AInput<CharBuffer>> {
    /**
     * The request queue.
     */
    private final RequestQueue requests = new RequestQueue();
    /**
     * The charset encoder.
     */
    private final CharsetDecoder decoder;
    /**
     * The bytes to be send to underlying stream.
     */
    private final ByteBuffer bytes;
    /**
     * If true, eof has been seen on the stream.
     */
    private boolean eofSeen;
    /**
     * If true, eof has been sent to decoder.
     */
    private boolean eofDecoded;
    /**
     * If true, the previous read was zero bytes. On the second consequential read with zero bytes an exception
     * will be thrown.
     */
    private boolean readZero;

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     * @param decoder the decoder
     * @param bytes   the bytes
     */
    protected DecoderInput(final AInput<ByteBuffer> wrapped, final CharsetDecoder decoder, final ByteBuffer bytes) {
        super(wrapped);
        this.decoder = decoder;
        this.bytes = bytes;
    }

    /**
     * Get decoder input.
     *
     * @param input   the input to decode
     * @param decoder the decoder
     * @param bytes   the byte buffer used to decode
     * @return the decoded text stream
     */
    public static AInput<CharBuffer> decode(final AInput<ByteBuffer> input,
                                            final CharsetDecoder decoder, final ByteBuffer bytes) {
        return new DecoderInput(input, decoder, bytes).export();
    }

    /**
     * Get decoder input.
     *
     * @param input      the input to decode
     * @param charset    the character set
     * @param bufferSize the buffer size
     * @return the decoded text stream
     */
    public static AInput<CharBuffer> decode(final AInput<ByteBuffer> input,
                                            final Charset charset, final int bufferSize) {
        final ByteBuffer buffer = IOUtil.BYTE.writeBuffer(bufferSize);
        final CharsetDecoder charsetDecoder = charset.newDecoder();
        charsetDecoder.onMalformedInput(CodingErrorAction.REPLACE);
        charsetDecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        return decode(input, charsetDecoder, buffer);
    }

    /**
     * Get decoder input.
     *
     * @param input   the input to decode
     * @param charset the character set
     * @return the decoded text stream
     */
    public static AInput<CharBuffer> decode(final AInput<ByteBuffer> input, final Charset charset) {
        return decode(input, charset, IOUtil.DEFAULT_BUFFER_SIZE);
    }

    /**
     * Get decoder input.
     *
     * @param input    the input to decode
     * @param encoding the character set
     * @return the decoded text stream
     */
    public static AInput<CharBuffer> decode(final AInput<ByteBuffer> input, final String encoding) {
        return decode(input, Charset.forName(encoding));
    }

    @Override
    @SuppressWarnings("squid:S3776")
    public Promise<Integer> read(final CharBuffer buffer) {
        return requests.run(() -> {
            if (!isValidAndOpen()) {
                return invalidationPromise();
            }
            if (eofDecoded) {
                return IOUtil.EOF_PROMISE;
            }
            final int[] read = new int[1];
            return aSeqWhile(() -> {
                ensureValidAndOpen();
                final int position = buffer.position();
                final CoderResult result = decoder.decode(bytes, buffer, eofSeen);
                eofDecoded = eofSeen && !bytes.hasRemaining();
                if (eofDecoded && position == buffer.position()) {
                    read[0] = IOUtil.EOF;
                    return aFalse();
                }
                if (result.isOverflow() || buffer.position() > position) {
                    // the buffer might be too small to hold a character
                    // may be next read will be better.
                    final int decodedChars = buffer.position() - position;
                    if (decodedChars == 0) {
                        if (readZero) {
                            return aFailure(new IllegalArgumentException(
                                    "The character does not fit in the buffer"));
                        } else {
                            readZero = true;
                        }
                    } else {
                        readZero = false;
                    }
                    read[0] = decodedChars;
                    return aFalse();
                }
                if (result.isUnderflow() && !eofSeen) {
                    bytes.compact();
                    return wrapped.read(bytes).flatMap(value -> {
                        bytes.flip();
                        if (isEof(value)) {
                            eofSeen = true;
                        }
                        return aTrue();
                    });
                }
                result.throwException();
                return aFailure(new IllegalStateException("Unknown decode result type: " + result));
            }).thenGet(() -> read[0]).listen(outcomeChecker());
        });
    }

    @Override
    public AInput<CharBuffer> export(final Vat vat) {
        return AInputProxyFactory.createProxy(vat, this);
    }
}
