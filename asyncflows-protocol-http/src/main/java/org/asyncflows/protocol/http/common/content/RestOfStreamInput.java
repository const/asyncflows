package org.asyncflows.protocol.http.common.content;

import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;

import java.nio.ByteBuffer;

import static org.asyncflows.io.IOUtil.isEof;
import static org.asyncflows.core.AsyncControl.aOutcome;
import static org.asyncflows.core.AsyncControl.aValue;

/**
 * The input that tracks state and allows to read until EOF in the socket.
 * This input is used only for reading responses.
 */
public class RestOfStreamInput extends MessageInput {
    /**
     * True, if eof notified.
     */
    private boolean eofNotified;

    /**
     * The constructor.
     *
     * @param input        the input
     * @param stateTracker the stat tracker
     */
    public RestOfStreamInput(final ByteParserContext input, final AResolver<InputState> stateTracker) {
        super(input, stateTracker);
    }

    @Override
    public Promise<Integer> read(final ByteBuffer buffer) {
        return reads.run(() -> {
            ensureValidAndOpen();
            if (eofNotified) {
                return IOUtil.EOF_PROMISE;
            }
            stateChanged(InputState.DATA);
            if (input.buffer().hasRemaining()) {
                final int rc = BufferOperations.BYTE.put(buffer, input.buffer());
                return aValue(rc);
            }
            return input.input().read(buffer).flatMapOutcome(resolution -> {
                if (resolution.isSuccess() && isEof(resolution.value())) {
                    eofNotified = true;
                    stateChanged(InputState.EOF_NO_TRAILERS);
                }
                return aOutcome(resolution);
            });
        }).listen(outcomeChecker());
    }


    @Override
    protected Promise<Void> closeAction() {
        stateChanged(eofNotified ? InputState.CLOSED : InputState.CLOSED_BEFORE_EOF);
        return super.closeAction();
    }
}
