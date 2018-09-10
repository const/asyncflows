package org.asyncflows.protocol.http.common.content;

import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.protocol.ProtocolStreamTruncatedException;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;

import java.nio.ByteBuffer;

import static org.asyncflows.io.IOUtil.isEof;
import static org.asyncflows.core.CoreFlows.aOutcome;
import static org.asyncflows.core.CoreFlows.aValue;

/**
 * The stream that is limited by the specified size, the {@link #close()} does not close underlying stream.
 */
public class ContentLengthInput extends MessageInput {
    /**
     * The limit.
     */
    private final long limit;
    /**
     * The read amount.
     */
    private long readAmount;
    /**
     * True, if eof notified.
     */
    private boolean eofNotified;

    /**
     * The constructor for the stream.
     *
     * @param stateTracker the state tracker
     * @param input        the input stream
     * @param limit        the limit
     */
    protected ContentLengthInput(final AResolver<InputState> stateTracker, final ByteParserContext input,
                                 final long limit) {
        super(input, stateTracker);
        this.limit = limit;
        if (limit == 0) {
            stateChanged(InputState.EOF_NO_TRAILERS);
        }
    }


    @Override
    public Promise<Integer> read(final ByteBuffer buffer) { // NOPMD
        return reads.run(() -> {
            ensureValidAndOpen();
            if (readAmount > limit) {
                throw new IllegalStateException("Stream has read too much!");
            }
            if (readAmount == limit) {
                if (!eofNotified) {
                    stateChanged(InputState.EOF_NO_TRAILERS);
                    eofNotified = true;
                }
                return IOUtil.EOF_PROMISE;
            }
            stateChanged(InputState.DATA);
            final int savedLimit = buffer.limit();
            if (limit - readAmount < buffer.remaining()) {
                buffer.limit(savedLimit - buffer.remaining() + (int) (limit - readAmount));
            }
            if (input.buffer().hasRemaining()) {
                final int rc = BufferOperations.BYTE.put(buffer, input.buffer());
                readAmount += rc;
                buffer.limit(savedLimit);
                return aValue(rc);
            }
            return input.input().read(buffer).flatMapOutcome(resolution -> {
                buffer.limit(savedLimit);
                if (resolution.isSuccess()) {
                    final boolean eof = isEof(resolution.value());
                    if (resolution.isSuccess() && !eof) {
                        readAmount += resolution.value();
                    }
                    if (eof && readAmount != limit) {
                        throw new ProtocolStreamTruncatedException(
                                "EOF before all data is read: " + (limit - readAmount));
                    }
                }
                return aOutcome(resolution);
            });
        }).listen(outcomeChecker());
    }

    @Override
    protected Promise<Void> closeAction() {
        stateChanged(readAmount == limit ? InputState.CLOSED : InputState.CLOSED_BEFORE_EOF);
        return super.closeAction();
    }
}