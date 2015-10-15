package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableBase;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.nio.Buffer;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.closeResourceAction;

/**
 * Simple channel over input and output. Closing channel closes both input and output.
 *
 * @param <B> the buffer type
 */
public class SimpleChannel<B extends Buffer> extends CloseableBase implements AChannel<B>, ExportsSelf<AChannel<B>> {
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
    public AChannel<B> export() {
        return export(Vat.current());
    }

    @Override
    public AChannel<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
