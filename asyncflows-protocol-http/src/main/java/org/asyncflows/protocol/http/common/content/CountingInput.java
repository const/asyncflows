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
import org.asyncflows.io.AInput;
import org.asyncflows.io.IOUtil;

import java.nio.Buffer;
import java.util.function.Consumer;

/**
 * The counting input.
 *
 * @param <B> the buffer type
 */
public class CountingInput<B extends Buffer> extends CountingStreamBase implements AInput<B> {
    /**
     * The input.
     */
    private final AInput<B> input;
    /**
     * The read resolver.
     */
    private final AResolver<Integer> readResolver = resolution -> {
        if (resolution.isFailure()) {
            fireFinished(resolution.failure());
        } else {
            if (resolution.value() == null || IOUtil.isEof(resolution.value())) {
                fireFinished();
            } else {
                transferred(resolution.value());
            }
        }
    };

    /**
     * The constructor.
     *
     * @param input    the input
     * @param listener the listener (must be exported, or be otherwise safe for use from other threads)
     */
    public CountingInput(final AInput<B> input, final Consumer<StreamFinishedEvent> listener) {
        super(listener);
        this.input = input;
    }

    /**
     * Count stream content if needed.
     *
     * @param input    the input
     * @param listener the listener (must be exported, or be otherwise safe for use from other threads)
     * @param <B>      the buffer type
     * @return if listener is null, returns original stream otherwise a stram wrapper is returned
     */
    public static <B extends Buffer> AInput<B> countIfNeeded(final AInput<B> input,
                                                             final Consumer<StreamFinishedEvent> listener) {
        if (listener == null) {
            return input;
        } else {
            return new CountingInput<>(input, listener);
        }
    }

    @Override
    public Promise<Integer> read(final B buffer) {
        return input.read(buffer).listen(readResolver);
    }

    @Override
    public Promise<Void> close() {
        return input.close().listen(this::fireFinished);
    }
}
