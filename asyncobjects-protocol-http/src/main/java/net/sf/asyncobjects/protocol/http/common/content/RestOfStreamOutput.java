package net.sf.asyncobjects.protocol.http.common.content;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.util.ByteGeneratorContext;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * The output for the rest of the stream.
 */
public class RestOfStreamOutput extends MessageOutput {
    /**
     * The constructor.
     *
     * @param output       the output
     * @param stateTracker the state tracker
     */
    public RestOfStreamOutput(final ByteGeneratorContext output, final AResolver<OutputState> stateTracker) {
        super(output, stateTracker);
    }

    @Override
    public Promise<Void> write(final ByteBuffer buffer) {
        return writes.run(() -> {
            ensureValidAndOpen();
            stateChanged(OutputState.DATA_CLOSEABLE);
            if (output.isSendNeeded()) {
                return output.send().thenDo(() -> output.getOutput().write(buffer));
            } else {
                return output.getOutput().write(buffer);
            }
        }).observe(outcomeChecker());
    }

    @Override
    protected Promise<Void> closeAction() {
        return output.send().thenDo(() -> {
            stateChanged(OutputState.CLOSED_LAST);
            return aVoid();
        });
    }
}
