package net.sf.asyncobjects.nio.codec;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.stream.AStream;
import net.sf.asyncobjects.core.stream.ChainedStreamBase;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.BufferOperations;
import net.sf.asyncobjects.nio.IOUtil;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqMaybeLoop;

/**
 * Object decoder stream that produce uniform data stream form the input.
 *
 * @param <B> the buffer type
 * @param <O> the object type
 */
public class ObjectDecoderStream<B extends Buffer, O> extends ChainedStreamBase<O, AInput<B>> {
    /**
     * The decoder.
     */
    private final ObjectDecoder<B, O> decoder;
    /**
     * The buffer to use.
     */
    private final B buffer;
    /**
     * The request queue.
     */
    private final RequestQueue requests = new RequestQueue();
    /**
     * The buffer operations.
     */
    private final BufferOperations<B, ?> bufferOperations;
    /**
     * true means that eof has been seen from underlying buffer.
     */
    private boolean eofSeen;

    /**
     * The constructor.
     *
     * @param wrapped          the underlying input
     * @param decoder          the decoder
     * @param buffer           the buffer to use (ready for get)
     * @param bufferOperations the buffer operations
     */
    public ObjectDecoderStream(final AInput<B> wrapped, final ObjectDecoder<B, O> decoder, final B buffer,
                               final BufferOperations<B, ?> bufferOperations) {
        super(wrapped);
        this.decoder = decoder;
        this.buffer = buffer;
        this.bufferOperations = bufferOperations;
    }

    /**
     * Decoded input into stream.
     *
     * @param input   the input to decode
     * @param decoder the decoder
     * @param buffer  the buffer
     * @param <O>     the stream element type
     * @return the stream
     */
    public static <O> AStream<O> decodeBytes(final AInput<ByteBuffer> input, final ObjectDecoder<ByteBuffer, O> decoder,
                                             final ByteBuffer buffer) {
        return new ObjectDecoderStream<ByteBuffer, O>(input, decoder, buffer, BufferOperations.BYTE).export();
    }

    /**
     * Decoded input into stream.
     *
     * @param input   the input to decode
     * @param decoder the decoder
     * @param buffer  the buffer
     * @param <O>     the stream element type
     * @return the stream
     */
    public static <O> AStream<O> decodeChars(final AInput<CharBuffer> input, final ObjectDecoder<CharBuffer, O> decoder,
                                             final CharBuffer buffer) {
        return new ObjectDecoderStream<CharBuffer, O>(input, decoder, buffer, BufferOperations.CHAR).export();
    }

    /**
     * Decoded input into stream.
     *
     * @param input   the input to decode
     * @param decoder the decoder
     * @param <O>     the stream element type
     * @return the stream
     */
    public static <O> AStream<O> decodeBytes(final AInput<ByteBuffer> input,
                                             final ObjectDecoder<ByteBuffer, O> decoder) {
        final ByteBuffer buffer = ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE);
        buffer.limit(0);
        return decodeBytes(input, decoder, buffer);
    }

    /**
     * Decoded input into stream.
     *
     * @param input   the input to decode
     * @param decoder the decoder
     * @param <O>     the stream element type
     * @return the stream
     */
    public static <O> AStream<O> decodeChars(final AInput<CharBuffer> input,
                                             final ObjectDecoder<CharBuffer, O> decoder) {
        final CharBuffer buffer = CharBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE);
        buffer.limit(0);
        return decodeChars(input, decoder, buffer);
    }

    @Override
    protected Promise<Maybe<O>> produce() {
        return requests.run(new ACallable<Maybe<O>>() {
            @Override
            public Promise<Maybe<O>> call() throws Throwable {
                return aSeqMaybeLoop(new ACallable<Maybe<Maybe<O>>>() {
                    @Override
                    public Promise<Maybe<Maybe<O>>> call() throws Throwable {
                        ensureValidAndOpen();
                        return decoder.decode(buffer, eofSeen).map(new AFunction<Maybe<Maybe<O>>,
                                ObjectDecoder.DecodeResult>() {
                            @Override
                            public Promise<Maybe<Maybe<O>>> apply(
                                    final ObjectDecoder.DecodeResult decodeResult) {
                                if (decodeResult == ObjectDecoder.DecodeResult.EOF) {
                                    return aMaybeValue(Maybe.<O>empty());
                                }
                                if (decodeResult == ObjectDecoder.DecodeResult.OBJECT_READY) {
                                    return aMaybeValue(Maybe.<O>value(decoder.getObject()));
                                }
                                if (decodeResult == ObjectDecoder.DecodeResult.FAILURE) {
                                    return aFailure(decoder.getFailure());
                                }
                                if (decodeResult != ObjectDecoder.DecodeResult.BUFFER_UNDERFLOW) {
                                    return aFailure(new IllegalStateException("Unknown result from decoder: "
                                            + decodeResult));
                                }
                                bufferOperations.compact(buffer);
                                return wrapped.read(buffer).map(new AFunction<Maybe<Maybe<O>>,
                                        Integer>() {
                                    @Override
                                    public Promise<Maybe<Maybe<O>>> apply(final Integer value) {
                                        buffer.flip();
                                        if (value < 0) {
                                            eofSeen = true;
                                        } else if (value == 0) {
                                            return aFailure(new IOException("Nothing has been read"));
                                        }
                                        return aMaybeEmpty();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

}
