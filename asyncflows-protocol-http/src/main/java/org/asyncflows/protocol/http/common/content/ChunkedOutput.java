/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.protocol.http.common.content;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;

import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.protocol.LineUtil.CR;
import static org.asyncflows.protocol.LineUtil.CRLF;
import static org.asyncflows.protocol.LineUtil.LF;
import static org.asyncflows.protocol.LineUtil.writeASCII;

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
    @SuppressWarnings("squid:S3776")
    protected Promise<Void> closeAction() {
        if (isValid()) {
            return writes.run(
                    () -> writeASCII(output, needsNewLine ? "\r\n0\r\n" : "0\r\n")).thenFlatGet(() -> {
                        if (trailersProvider == null) {
                            return writeASCII(output, CRLF);
                        } else {
                            return trailersProvider.get().flatMap(value -> {
                                if (value == null) {
                                    return writeASCII(output, CRLF);
                                } else {
                                    return value.write(output).thenFlatGet(() -> {
                                        stateChanged(OutputState.TRAILERS_ADDED);
                                        return aVoid();
                                    });
                                }
                            });
                        }
                    }
            ).thenFlatGet(
                    output::send
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
