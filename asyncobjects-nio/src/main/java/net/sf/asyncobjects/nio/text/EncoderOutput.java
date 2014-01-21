package net.sf.asyncobjects.nio.text;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ChainedClosable;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.BufferOperations;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

/**
 * The character output that encodes bytes to byte output. The output caches characters, and performs actual encode
 * only when buffer is full and it should be sent to downstream. This might cause a error later, when encoder
 * detects errors and next write actually tries to encode invalid data that was sent on the previous write.
 * Also {@link #flush()} will send only complete code points downstream, unless it is called as part of the close
 * operation.
 */
public class EncoderOutput extends ChainedClosable<AOutput<ByteBuffer>>
        implements AOutput<CharBuffer>, ExportsSelf<AOutput<CharBuffer>> {
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
        final CharBuffer chars = CharBuffer.allocate(bufferSize);
        chars.limit(0);
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
        return requests.runSeqLoop(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                BufferOperations.CHAR.append(chars, buffer);
                if (!buffer.hasRemaining()) {
                    return aFalse();
                } else {
                    final CoderResult result = encoder.encode(chars, bytes, false);
                    if (result.isUnderflow()) {
                        return aTrue();
                    } else if (result.isOverflow()) {
                        bytes.flip();
                        return wrapped.write(bytes).thenDo(new ACallable<Boolean>() {
                            @Override
                            public Promise<Boolean> call() throws Throwable {
                                bytes.compact();
                                return aTrue();
                            }
                        });
                    } else {
                        result.throwException();
                        return aFalse();
                    }
                }
            }
        }).observe(outcomeChecker());
    }

    /**
     * Flush the buffers.
     *
     * @param eof if true, this is called at the close operation, so the
     * @return the promise that finishes when flush is done.
     */
    public Promise<Void> internalFlush(final boolean eof) {
        return aSeqLoop(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                if (eof ? !isValid() : !isValidAndOpen()) {
                    return invalidationPromise();
                }
                final CoderResult result = encoder.encode(chars, bytes, eof);
                if (result.isOverflow() || result.isUnderflow() && bytes.position() > 0) {
                    bytes.flip();
                    return wrapped.write(bytes).thenDo(new ACallable<Boolean>() {
                        @Override
                        public Promise<Boolean> call() throws Throwable {
                            bytes.compact();
                            return aTrue();
                        }
                    });
                } else {
                    if (result.isError()) {
                        result.throwException();
                    }
                    return aFalse();
                }
            }
        });
    }

    @Override
    public Promise<Void> flush() {
        return requests.run(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return internalFlush(false).thenDo(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return wrapped.flush();
                    }
                }).observe(outcomeChecker());
            }
        });
    }

    @Override
    protected Promise<Void> beforeClose() {
        return requests.run(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return isValid() ? internalFlush(true) : aVoid();
            }
        });
    }

    @Override
    public AOutput<CharBuffer> export() {
        return export(Vat.current());
    }

    @Override
    public AOutput<CharBuffer> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
