package net.sf.asyncobjects.protocol.http.common.content;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.util.ByteGeneratorContext;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.protocol.LineUtil.CR;
import static net.sf.asyncobjects.protocol.LineUtil.CRLF;
import static net.sf.asyncobjects.protocol.LineUtil.LF;
import static net.sf.asyncobjects.protocol.LineUtil.writeASCII;

/**
 * The chunked output.
 */
public class ChunkedOutput extends MessageOutput {
    /**
     * The HTTP headers.
     */
    private final ACallable<HttpHeaders> trailersProvider;
    /**
     * True if needs new line before the next chunk.
     */
    private boolean needsNewLine;

    /**
     * The constructor.
     *
     * @param output           the output
     * @param stateTracker     the state tracker
     * @param trailersProvider the trailers provider
     */
    protected ChunkedOutput(final ByteGeneratorContext output, final AResolver<OutputState> stateTracker,
                            final ACallable<HttpHeaders> trailersProvider) {
        super(output, stateTracker);
        this.trailersProvider = trailersProvider;
    }

    @Override
    public Promise<Void> write(final ByteBuffer buffer) {
        return writes.run(() -> {
            ensureValidAndOpen();
            stateChanged(OutputState.DATA_CLOSEABLE);
            if (!buffer.hasRemaining()) {
                return aVoid();
            }
            final StringBuilder b = new StringBuilder();
            if (needsNewLine) {
                b.append(CRLF);
            } else {
                needsNewLine = true;
            }
            b.append(Integer.toHexString(buffer.remaining())).append(CRLF);
            return writeASCII(output, b.toString()).thenDo(() -> {
                if (buffer.remaining() < output.buffer().remaining()) {
                    output.buffer().put(buffer);
                    if (output.buffer().remaining() >= 2) {
                        output.buffer().put((byte) CR).put((byte) LF);
                        needsNewLine = false;
                    }
                    return output.send().toVoid();
                } else {
                    return output.send().thenDo(() -> output.getOutput().write(buffer));
                }
            });
        }).observe(outcomeChecker());
    }

    @Override
    protected Promise<Void> closeAction() {
        if (isValid()) {
            //noinspection Convert2MethodRef
            return writes.run(
                    () -> writeASCII(output, needsNewLine ? "\r\n0\r\n" : "0\r\n")).thenDo(() -> {
                        if (trailersProvider == null) {
                            return writeASCII(output, CRLF);
                        } else {
                            return trailersProvider.call().map(value -> {
                                if (value == null) {
                                    return writeASCII(output, CRLF);
                                } else {
                                    return value.write(output).thenDo((ACallable<Void>) () -> {
                                        stateChanged(OutputState.TRAILERS_ADDED);
                                        return aVoid();
                                    });
                                }
                            });
                        }
                    }
            ).thenDo(
                    () -> output.send()
            ).observe(outcomeChecker()).mapOutcome(value -> {
                if (value.isFailure()) {
                    invalidate(value.failure());
                } else if (isValid()) {
                    stateChanged(OutputState.CLOSED);
                }
                return aVoid();
            });
        } else {
            stateChanged(OutputState.CLOSED_LAST);
            return super.closeAction();
        }
    }
}
