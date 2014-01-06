package net.sf.asyncobjects.nio.text;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.util.ChainedClosable;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

/**
 * The character decoder input.
 */
public class DecoderInput extends ChainedClosable<AInput<ByteBuffer>>
        implements AInput<CharBuffer>, ExportsSelf<AInput<CharBuffer>> {
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
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.limit(0);
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
    public Promise<Integer> read(final CharBuffer buffer) { // NOPMD
        return requests.run(new ACallable<Integer>() {
            @Override
            public Promise<Integer> call() throws Throwable { // NOPMD
                if (!isValidAndOpen()) {
                    return invalidationPromise();
                }
                if (eofDecoded) {
                    return aSuccess(-1);
                }
                final Cell<Integer> read = new Cell<Integer>();
                return aSeqLoop(new ACallable<Boolean>() {
                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        ensureValidAndOpen();
                        final int position = buffer.position();
                        final CoderResult result = decoder.decode(bytes, buffer, eofSeen); // NOPMD
                        eofDecoded = eofSeen && !bytes.hasRemaining();
                        if (eofDecoded && position == buffer.position()) {
                            read.setValue(-1);
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
                            read.setValue(decodedChars);
                            return aFalse();
                        }
                        if (result.isUnderflow() && !eofSeen) {
                            bytes.compact();
                            return wrapped.read(bytes).map(new AFunction<Boolean, Integer>() {
                                @Override
                                public Promise<Boolean> apply(final Integer value) throws Throwable {
                                    bytes.flip();
                                    if (value < 0) {
                                        eofSeen = true;
                                    }
                                    return aTrue();
                                }
                            });
                        }
                        result.throwException();
                        return aFailure(new IllegalStateException("Unknown decode result type: " + result));
                    }
                }).thenDo(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return aSuccess(read.getValue());
                    }
                }).observe(outcomeChecker());
            }
        });
    }

    @Override
    public AInput<CharBuffer> export() {
        return export(Vat.current());
    }

    @Override
    public AInput<CharBuffer> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
