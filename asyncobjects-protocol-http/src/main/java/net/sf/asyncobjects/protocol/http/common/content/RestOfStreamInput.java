package net.sf.asyncobjects.protocol.http.common.content;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.BufferOperations;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.util.ByteParserContext;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.nio.IOUtil.isEof;

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
            return input.input().read(buffer).mapOutcome(resolution -> {
                if (resolution.isSuccess() && isEof(resolution.value())) {
                    eofNotified = true;
                    stateChanged(InputState.EOF_NO_TRAILERS);
                }
                return Promise.forOutcome(resolution);
            });
        }).observe(outcomeChecker());
    }


    @Override
    protected Promise<Void> closeAction() {
        stateChanged(eofNotified ? InputState.CLOSED : InputState.CLOSED_BEFORE_EOF);
        return super.closeAction();
    }
}
