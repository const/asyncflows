package org.asyncflows.protocol.http.common.content;

import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;

import java.nio.ByteBuffer;

import static org.asyncflows.core.AsyncControl.aVoid;

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
                return output.send().thenFlatGet(() -> output.getOutput().write(buffer));
            } else {
                return output.getOutput().write(buffer);
            }
        }).listen(outcomeChecker());
    }

    @Override
    protected Promise<Void> closeAction() {
        return output.send().thenFlatGet(() -> {
            stateChanged(OutputState.CLOSED_LAST);
            return aVoid();
        });
    }
}
