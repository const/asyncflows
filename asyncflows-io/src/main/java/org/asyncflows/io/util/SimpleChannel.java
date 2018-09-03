package org.asyncflows.io.util;

import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.NIOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.NeedsExport;

import java.nio.Buffer;

import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.closeResourceAction;

/**
 * Simple channel over input and output. Closing channel closes both input and output.
 *
 * @param <B> the buffer type
 */
public class SimpleChannel<B extends Buffer> extends CloseableBase implements AChannel<B>, NeedsExport<AChannel<B>> {
    /**
     * Input for the channel.
     */
    private final AInput<B> input;
    /**
     * Output for the channel.
     */
    private final AOutput<B> output;

    /**
     * The constructor.
     *
     * @param input  the input
     * @param output the output
     */
    public SimpleChannel(final AInput<B> input, final AOutput<B> output) {
        this.input = input;
        this.output = output;
    }

    @Override
    protected Promise<Void> closeAction() {
        // just force closing both channels
        return aAll(closeResourceAction(input)).andLast(closeResourceAction(output)).toVoid();
    }

    @Override
    public Promise<AInput<B>> getInput() {
        return aValue(input);
    }

    @Override
    public Promise<AOutput<B>> getOutput() {
        return aValue(output);
    }

    @Override
    public AChannel<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
