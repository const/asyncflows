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

package org.asyncflows.io.adapters.blocking;

import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;

import java.nio.Buffer;

import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.util.CoreFlowsResource.closeResource;

/**
 * The export utilities.
 */
public final class BlockingIOExportUtil {
    /**
     * Private constructor for utility class.
     */
    private BlockingIOExportUtil() {
    }

    /**
     * Export blocking input stream. It uses two independent daemon vats for reading and closing. So the stream
     * could be closed while read is in progress.
     *
     * @param input the input
     * @param <B>   the buffer type
     * @return exported stream
     */
    public static <B extends Buffer> AInput<B> exportBlocking(final AInput<B> input) {
        final Vat readVat = Vats.daemonVat();
        final Vat closeVat = Vats.daemonVat();
        return exportInput(input, readVat, closeVat);
    }

    /**
     * Export input stream.
     *
     * @param input    the input to export
     * @param readVat  the read vat
     * @param closeVat the close vat
     * @param <B>      the stream.
     * @return the exported stream
     */
    private static <B extends Buffer> AInput<B> exportInput(final AInput<B> input, final Vat readVat,
                                                            final Vat closeVat) {
        return new AInput<B>() {
            @Override
            public Promise<Integer> read(final B buffer) {
                return aLater(readVat, () -> input.read(buffer));
            }

            @Override
            public Promise<Void> close() {
                return closeResource(closeVat, input);
            }
        };
    }

    /**
     * Export blocking output stream. It uses two independent daemon vats for writing and closing. So the stream
     * could be closed while a write is in progress.
     *
     * @param input the output
     * @param <B>   the buffer type
     * @return exported stream
     */
    public static <B extends Buffer> AOutput<B> exportBlocking(final AOutput<B> input) {
        final Vat writeVat = Vats.daemonVat();
        final Vat closeVat = Vats.daemonVat();
        return exportOutput(input, writeVat, closeVat);
    }

    /**
     * Export output stream.
     *
     * @param output   the output to export
     * @param writeVat the write and flush vat
     * @param closeVat the close vat
     * @param <B>      the stream.
     * @return the exported stream
     */
    private static <B extends Buffer> AOutput<B> exportOutput(final AOutput<B> output, final Vat writeVat,
                                                              final Vat closeVat) {
        return new AOutput<B>() {
            @Override
            public Promise<Void> close() {
                return closeResource(closeVat, output);
            }

            @Override
            public Promise<Void> write(final B buffer) {
                return aLater(writeVat, () -> output.write(buffer));
            }

            @Override
            public Promise<Void> flush() {
                return aLater(writeVat, output::flush);
            }
        };
    }
}
