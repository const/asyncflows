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

package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.util.CoreFlowsResource;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.CoreFlows.aLater;

/**
 * The export utility class for the streams.
 */
public final class StreamExportUtil {
    /**
     * The private constructor for utility class.
     */
    private StreamExportUtil() {
    }

    /**
     * Export a stream.
     *
     * @param vat    the vat to use
     * @param stream the stream
     * @param <T>    the stream element type
     * @return the stream
     */
    public static <T> AStream<T> export(final Vat vat, final AStream<T> stream) {
        return new AStream<T>() {
            private final ASupplier<Maybe<T>> nextElement = StreamUtil.producerFromStream(stream);

            @Override
            public Promise<Maybe<T>> next() {
                return aLater(vat, nextElement);
            }

            @Override
            public Promise<Void> close() {
                return CoreFlowsResource.closeResource(vat, stream);
            }
        };
    }

    /**
     * Export sink.
     *
     * @param vat  the vat for the sink
     * @param sink the sink
     * @param <T>  the element type
     * @return the exported sink
     */
    public static <T> ASink<T> export(final Vat vat, final ASink<T> sink) {
        return new ASink<T>() {
            @Override
            public Promise<Void> put(final T value) {
                return aLater(vat, () -> sink.put(value));
            }

            @Override
            public Promise<Void> fail(final Throwable error) {
                return aLater(vat, () -> sink.fail(error));
            }

            @Override
            public Promise<Void> finished() {
                return aLater(vat, sink::finished);
            }

            @Override
            public Promise<Void> close() {
                return CoreFlowsResource.closeResource(vat, sink);
            }
        };
    }
}
