/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

import org.asyncflows.io.AOutput;
import org.asyncflows.core.Promise;

import java.nio.Buffer;
import java.util.function.Consumer;

/**
 * The counting output.
 *
 * @param <B> the buffer type
 */
public class CountingOutput<B extends Buffer> extends CountingStreamBase implements AOutput<B> {
    /**
     * The output.
     */
    private final AOutput<B> output;

    /**
     * The constructor.
     *
     * @param output   the output
     * @param listener the listener (must be exported, or be otherwise safe for use from other threads)
     */
    public CountingOutput(final AOutput<B> output, final Consumer<StreamFinishedEvent> listener) {
        super(listener);
        this.output = output;
    }

    /**
     * Count stream content if needed.
     *
     * @param output   the output
     * @param listener the listener (must be exported, or be otherwise safe for use from other threads)
     * @param <B>      the buffer type
     * @return if listener is null, returns original stream otherwise a stram wrapper is returned
     */
    public static <B extends Buffer> AOutput<B> countIfNeeded(final AOutput<B> output,
                                                              final Consumer<StreamFinishedEvent> listener) {
        if (listener == null) {
            return output;
        } else {
            return new CountingOutput<B>(output, listener);
        }
    }

    @Override
    public Promise<Void> write(final B buffer) {
        final int amount = buffer.remaining();
        return output.write(buffer).listen(resolution -> {
            if (resolution.isSuccess()) {
                transferred(amount - buffer.remaining());
            } else {
                fireFinished(resolution.failure());
            }
        });
    }

    @Override
    public Promise<Void> flush() {
        return output.flush();
    }

    @Override
    public Promise<Void> close() {
        return output.close().listen(this::fireFinished);
    }
}
