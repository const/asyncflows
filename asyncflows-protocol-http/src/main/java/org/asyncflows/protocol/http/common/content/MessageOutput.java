package org.asyncflows.protocol.http.common.content;

import org.asyncflows.io.AOutput;
import org.asyncflows.io.IOExportUtil;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;

import java.nio.ByteBuffer;

import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The base class for the message output.
 */
public abstract class MessageOutput extends CloseableInvalidatingBase
        implements AOutput<ByteBuffer>, NeedsExport<AOutput<ByteBuffer>> {
    /**
     * Read requests.
     */
    protected final RequestQueue writes = new RequestQueue();
    /**
     * the input stream.
     */
    protected final ByteGeneratorContext output;
    /**
     * The tracker of the state of input.
     */
    private final AResolver<OutputState> stateTracker;
    /**
     * Notify that the state has changed.
     */
    private OutputState lastState = OutputState.NOT_STARTED;

    /**
     * The constructor.
     *
     * @param output       the output
     * @param stateTracker the state tracker
     */
    protected MessageOutput(final ByteGeneratorContext output, final AResolver<OutputState> stateTracker) {
        this.output = output;
        this.stateTracker = stateTracker;
    }

    /**
     * Notify that the state has been changed.
     *
     * @param state the state change
     */
    protected final void stateChanged(final OutputState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        if (isValid() && lastState != state) { // NOPMD
            if (state.ordinal() < lastState.ordinal()) {
                throw new IllegalStateException("Shifting state backward: " + lastState + " -> " + state);
            }
            notifySuccess(stateTracker, state);
        }
    }

    @Override
    public Promise<Void> flush() {
        return writes.run(() -> {
            ensureValidAndOpen();
            return output.send().thenFlatGet(() -> output.getOutput().flush()).listen(outcomeChecker());
        });
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        notifyFailure(stateTracker, throwable);
        lastState = OutputState.ERROR;
        super.onInvalidation(throwable);
    }

    @Override
    public AOutput<ByteBuffer> export(final Vat vat) {
        return IOExportUtil.export(vat, this);
    }
}
