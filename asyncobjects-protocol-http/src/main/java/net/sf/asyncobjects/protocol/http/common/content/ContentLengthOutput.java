package net.sf.asyncobjects.protocol.http.common.content;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.util.ByteGeneratorContext;
import net.sf.asyncobjects.protocol.http.HttpException;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * The output for the content length.
 */
public class ContentLengthOutput extends MessageOutput {
    /**
     * The content length.
     */
    private final long length;
    /**
     * The generated amount.
     */
    private long generated;

    /**
     * The constructor.
     *
     * @param output       the output
     * @param stateTracker the state tracker
     * @param length       the content length
     */
    protected ContentLengthOutput(final ByteGeneratorContext output, final AResolver<OutputState> stateTracker,
                                  final long length) {
        super(output, stateTracker);
        this.length = length;
    }

    @Override
    public Promise<Void> write(final ByteBuffer buffer) {
        return writes.run(() -> {
            ensureValidAndOpen();
            if (!buffer.hasRemaining()) {
                return aVoid();
            }
            if (generated == 0) {
                stateChanged(OutputState.DATA);
            }
            generated += buffer.remaining();
            if (generated > length) {
                throw new HttpException("Writing too much for Content-Length: " + generated + " > " + length);
            }
            if (output.isSendNeeded()) {
                return output.send().thenDo(
                        () -> output.getOutput().write(buffer).thenDo(() -> {
                            if (generated == length) {
                                stateChanged(OutputState.DATA_CLOSEABLE);
                            }
                            return aVoid();
                        }));
            } else {
                return output.getOutput().write(buffer);
            }
        }).observe(outcomeChecker());
    }

    @Override
    protected Promise<Void> closeAction() {
        stateChanged(generated == length ? OutputState.CLOSED : OutputState.CLOSED_LAST);
        return super.closeAction();
    }
}
