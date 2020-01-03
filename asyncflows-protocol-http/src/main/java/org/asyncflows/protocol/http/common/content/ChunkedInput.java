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

package org.asyncflows.protocol.http.common.content;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.protocol.LineUtil;
import org.asyncflows.protocol.ProtocolLineParser;
import org.asyncflows.protocol.ProtocolStreamTruncatedException;
import org.asyncflows.protocol.http.HttpException;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.asyncflows.protocol.http.common.HttpRuntimeUtil;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;

import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifyResolver;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.io.IOUtil.isEof;

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
    @SuppressWarnings("squid:S3776")
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
                return nextChunk().flatMapOutcome(value -> {
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
            trailersNotReached();
            stateChanged(InputState.CLOSED_BEFORE_EOF);
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
    @SuppressWarnings("squid:S3776")
    public Promise<Integer> read(final ByteBuffer buffer) {
        return reads.runSeqUntilValue(() -> {
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
            return input.input().read(buffer).flatMapOutcome(value -> {
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
        }).listen(outcomeChecker());
    }

    /**
     * @return advance to the next chunk EOF is returned if chunk is read, an empty if it is not the last chunk.
     */
    private Promise<Maybe<Integer>> nextChunk() {
        return readChunkHeader(!firstChunk).flatMap(value -> {
            firstChunk = false;
            if (value == 0L) {
                return HttpHeaders.readHeaders(input, trailersSizeLimit).flatMapOutcome(
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
        }).failedLast(HttpRuntimeUtil.toHttpException("Failed to parse chunk header"));
    }
}
