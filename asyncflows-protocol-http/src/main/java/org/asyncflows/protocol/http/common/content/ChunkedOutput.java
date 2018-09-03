package org.asyncflows.protocol.http.common.content;

import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;

import java.nio.ByteBuffer;

import static org.asyncflows.protocol.LineUtil.CR;
import static org.asyncflows.protocol.LineUtil.CRLF;
import static org.asyncflows.protocol.LineUtil.LF;
import static org.asyncflows.protocol.LineUtil.writeASCII;
import static org.asyncflows.core.AsyncControl.aVoid;

/**
 * The chunked output.
 */
public class ChunkedOutput extends MessageOutput {
    /**
     * The HTTP headers.
     */
    private final ASupplier<HttpHeaders> trailersProvider;
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
                            final ASupplier<HttpHeaders> trailersProvider) {
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
            return writeASCII(output, b.toString()).thenFlatGet(() -> {
                if (buffer.remaining() < output.buffer().remaining()) {
                    output.buffer().put(buffer);
                    if (output.buffer().remaining() >= 2) {
                        output.buffer().put((byte) CR).put((byte) LF);
                        needsNewLine = false;
                    }
                    return output.send().toVoid();
                } else {
                    return output.send().thenFlatGet(() -> output.getOutput().write(buffer));
                }
            });
        }).listen(outcomeChecker());
    }

    @Override
    protected Promise<Void> closeAction() {
        if (isValid()) {
            //noinspection Convert2MethodRef
            return writes.run(
                    () -> writeASCII(output, needsNewLine ? "\r\n0\r\n" : "0\r\n")).thenFlatGet(() -> {
                        if (trailersProvider == null) {
                            return writeASCII(output, CRLF);
                        } else {
                            return trailersProvider.get().flatMap(value -> {
                                if (value == null) {
                                    return writeASCII(output, CRLF);
                                } else {
                                    return value.write(output).thenFlatGet((ASupplier<Void>) () -> {
                                        stateChanged(OutputState.TRAILERS_ADDED);
                                        return aVoid();
                                    });
                                }
                            });
                        }
                    }
            ).thenFlatGet(
                    () -> output.send()
            ).listen(outcomeChecker()).flatMapOutcome(value -> {
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
