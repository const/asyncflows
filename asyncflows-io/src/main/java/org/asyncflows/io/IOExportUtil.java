package org.asyncflows.io;

import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;

import java.nio.Buffer;

import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.util.CoreFlowsResource.closeResource;

/**
 * The export utilities.
 */
public final class IOExportUtil {
    /**
     * Private constructor for utility class.
     */
    private IOExportUtil() {
    }

    /**
     * Export channel.
     *
     * @param vat     the target vat
     * @param channel the channel to export
     * @param <B>     the buffer type
     * @return the exported channel
     */
    public static <B extends Buffer> AChannel<B> export(final Vat vat, final AChannel<B> channel) {
        return new AChannel<B>() {
            @Override
            public Promise<AInput<B>> getInput() {
                return aLater(vat, channel::getInput);
            }

            @Override
            public Promise<AOutput<B>> getOutput() {
                return aLater(vat, channel::getOutput);
            }

            @Override
            public Promise<Void> close() {
                return closeResource(vat, channel);
            }
        };
    }

    /**
     * Export input stream.
     *
     * @param vat   the target vat
     * @param input the input
     * @param <B>   the buffer type
     * @return exported stream
     */
    public static <B extends Buffer> AInput<B> export(final Vat vat, final AInput<B> input) {
        return exportInput(input, vat, vat);
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
     * Export output stream.
     *
     * @param vat   the target vat
     * @param input the input
     * @param <B>   the buffer type
     * @return exported stream
     */
    public static <B extends Buffer> AOutput<B> export(final Vat vat, final AOutput<B> input) {
        return exportOutput(input, vat, vat);
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