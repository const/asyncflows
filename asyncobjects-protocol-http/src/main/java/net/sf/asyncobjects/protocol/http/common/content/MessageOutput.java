package net.sf.asyncobjects.protocol.http.common.content;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.NIOExportUtil;
import net.sf.asyncobjects.nio.util.ByteGeneratorContext;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * The base class for the message output.
 */
public abstract class MessageOutput extends CloseableInvalidatingBase
        implements AOutput<ByteBuffer>, ExportsSelf<AOutput<ByteBuffer>> {
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
            return output.send().thenDo(() -> output.getOutput().flush()).observe(outcomeChecker());
        });
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        notifyFailure(stateTracker, throwable);
        lastState = OutputState.ERROR;
        super.onInvalidation(throwable);
    }

    @Override
    public AOutput<ByteBuffer> export() {
        return export(Vat.current());
    }

    @Override
    public AOutput<ByteBuffer> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
