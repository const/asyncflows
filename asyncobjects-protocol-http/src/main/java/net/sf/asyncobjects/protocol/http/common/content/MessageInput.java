package net.sf.asyncobjects.protocol.http.common.content;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.NIOExportUtil;
import net.sf.asyncobjects.nio.util.ByteParserContext;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * The base class for the message body input.
 */
public abstract class MessageInput extends CloseableInvalidatingBase
        implements AInput<ByteBuffer>, ExportsSelf<AInput<ByteBuffer>> {
    /**
     * Read requests.
     */
    protected final RequestQueue reads = new RequestQueue();
    /**
     * the input stream.
     */
    protected final ByteParserContext input;
    /**
     * The tracker ofr the state of input.
     */
    private final AResolver<InputState> stateTracker;
    /**
     * Notify that the state has changed.
     */
    private InputState lastState = InputState.IDLE;


    /**
     * The constructor.
     *
     * @param input        the input tracker
     * @param stateTracker the state tracker
     */
    protected MessageInput(final ByteParserContext input, final AResolver<InputState> stateTracker) {
        this.input = input;
        this.stateTracker = stateTracker;
    }

    /**
     * Notify that the state has been changed.
     *
     * @param state the state change
     */
    protected final void stateChanged(final InputState state) {
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
    protected void onInvalidation(final Throwable throwable) {
        notifyFailure(stateTracker, throwable);
        lastState = InputState.ERROR;
        super.onInvalidation(throwable);
    }

    @Override
    public AInput<ByteBuffer> export() {
        return export(Vat.current());
    }

    @Override
    public AInput<ByteBuffer> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
