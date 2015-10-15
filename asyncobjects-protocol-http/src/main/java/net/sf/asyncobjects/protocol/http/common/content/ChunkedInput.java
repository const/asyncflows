package net.sf.asyncobjects.protocol.http.common.content;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.nio.BufferOperations;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.util.ByteParserContext;
import net.sf.asyncobjects.protocol.LineUtil;
import net.sf.asyncobjects.protocol.ProtocolLineParser;
import net.sf.asyncobjects.protocol.ProtocolStreamTruncatedException;
import net.sf.asyncobjects.protocol.http.HttpException;
import net.sf.asyncobjects.protocol.http.common.HttpLimits;
import net.sf.asyncobjects.protocol.http.common.HttpRuntimeUtil;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifyResolver;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static net.sf.asyncobjects.nio.IOUtil.isEof;

/**
 * The chunked input.
 */
public class ChunkedInput extends MessageInput {
    /**
     * The limit on trailers size.
     */
    private final int trailersSizeLimit;
    /**
     * This resolver is notified with trailers.
     */
    private final AResolver<HttpHeaders> trailersResolver;
    /**
     * True, if eof notified. This is true only when the last zero-sized chunk is read.
     */
    private boolean eofNotified;
    /**
     * The amount of data remaining in the chunk.
     */
    private long chunkRemaining;
    /**
     * true, ff the first chunk.
     */
    private boolean firstChunk = true;

    /**
     * The constructor.
     *
     * @param input             the input
     * @param stateTracker      the state tracker
     * @param trailersSizeLimit the limit for trailers
     * @param trailersResolver  the resolver for trailers
     */
    public ChunkedInput(final ByteParserContext input, final AResolver<InputState> stateTracker,
                        final int trailersSizeLimit, final AResolver<HttpHeaders> trailersResolver) {
        super(input, stateTracker);
        this.trailersSizeLimit = trailersSizeLimit;
        this.trailersResolver = trailersResolver;
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        notifyFailure(trailersResolver, throwable);
        super.onInvalidation(throwable);
    }

    @Override
    protected Promise<Void> closeAction() {
        if (eofNotified) {
            stateChanged(InputState.CLOSED);
            return aVoid();
        } else if (chunkRemaining == 0 && isValid()) {
            return reads.run(() -> {
                // ending read could actually change conditions
                if (eofNotified) {
                    stateChanged(InputState.CLOSED);
                    return aVoid();
                }
                if (chunkRemaining != 0 || !isValid()) {
                    trailersNotReached();
                    stateChanged(InputState.CLOSED_BEFORE_EOF);
                    return aVoid();
                }
                return nextChunk().mapOutcome(value -> {
                    if (value.isSuccess()) {
                        if (eofNotified) {
                            stateChanged(InputState.CLOSED);
                        } else {
                            trailersNotReached();
                            stateChanged(InputState.CLOSED_BEFORE_EOF);
                        }
                        return aVoid();
                    } else {
                        invalidate(value.failure());
                        return aVoid();
                    }
                });
            });
        } else {
            stateChanged(InputState.CLOSED_BEFORE_EOF);
            trailersNotReached();
            return aVoid();
        }
    }

    /**
     * Notify that the trailers has not been reached.
     */
    private void trailersNotReached() {
        notifyFailure(trailersResolver, new HttpException("Trailers not read, because closed before EOF"));
    }

    @Override
    public Promise<Integer> read(final ByteBuffer buffer) {
        return reads.runSeqMaybeLoop(() -> {
            ensureValidAndOpen();
            if (eofNotified) {
                return IOUtil.EOF_MAYBE_PROMISE;
            }
            if (!buffer.hasRemaining()) {
                return aMaybeValue(0);
            }
            stateChanged(InputState.DATA);
            if (chunkRemaining == 0) {
                return nextChunk();
            }
            final int savedLimit = buffer.limit();
            if (chunkRemaining < buffer.remaining()) {
                buffer.limit(savedLimit - buffer.remaining() + (int) chunkRemaining);
            }
            if (input.buffer().hasRemaining()) {
                final int rc = BufferOperations.BYTE.put(buffer, input.buffer());
                buffer.limit(savedLimit);
                chunkRemaining -= rc;
                return aMaybeValue(rc);
            }
            return input.input().read(buffer).mapOutcome(value -> {
                buffer.limit(savedLimit);
                if (value.isSuccess()) {
                    final Integer rc = value.value();
                    if (isEof(rc)) {
                        throw new ProtocolStreamTruncatedException("EOF before end of chunk");
                    }
                    chunkRemaining -= rc;
                    return aMaybeValue(rc);
                } else {
                    return aFailure(value.failure());
                }
            });
        }).observe(outcomeChecker());
    }

    /**
     * @return advance to the next chunk EOF is returned if chunk is read, an empty if it is not the last chunk.
     */
    private Promise<Maybe<Integer>> nextChunk() {
        return readChunkHeader(!firstChunk).map(value -> {
            firstChunk = false;
            if (value == 0L) {
                return HttpHeaders.readHeaders(input, trailersSizeLimit).mapOutcome(
                        value1 -> {
                            notifyResolver(trailersResolver, value1);
                            eofNotified = true;
                            if (value1.isSuccess()) {
                                stateChanged(InputState.TRAILERS_READ);
                                stateChanged(InputState.EOF);
                                return IOUtil.EOF_MAYBE_PROMISE;
                            } else {
                                return aFailure(value1.failure());
                            }
                        });
            } else {
                chunkRemaining = value;
                return aMaybeEmpty();
            }
        });
    }

    /**
     * Read chunk header.
     *
     * @param endPrevious true if CRLF from the previous chunk needs to be read.
     * @return a chunk size
     */
    private Promise<Long> readChunkHeader(final boolean endPrevious) {
        return aSeq(
                () -> endPrevious ? LineUtil.readLineCRLF(input, 0, true) : aValue("")
        ).map(emptyLine -> {
            if (emptyLine == null) {
                throw new HttpException("EOF instead of a chunk header");
            }
            if (emptyLine.length() != 0) {
                throw new HttpException("CRLF is expected after chunk end");
            }
            return LineUtil.readLineCRLF(input, HttpLimits.MAX_CHUNK_LINE, true);
        }).map(chunkSizeLine -> {
            if (chunkSizeLine == null) {
                throw new HttpException("EOF instead of a chunk header");
            }
            final String number = new ProtocolLineParser(chunkSizeLine).hexNumber();
            final long result = Long.parseLong(number, 16);
            if (result < 0) {
                throw new HttpException("The chunk size is too big: " + chunkSizeLine);
            }
            return aValue(result);
        }).failedLast(HttpRuntimeUtil.<Long>toHttpException("Failed to parse chunk header"));
    }
}
