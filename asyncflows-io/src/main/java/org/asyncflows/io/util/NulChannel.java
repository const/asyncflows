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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static org.asyncflows.core.AsyncControl.aValue;
import static org.asyncflows.core.util.AsyncAllControl.aAll;
import static org.asyncflows.core.util.ResourceUtil.closeResourceAction;

/**
 * The null channel.
 *
 * @param <B> the buffer type
 */
public class NulChannel<B extends Buffer> extends CloseableBase implements AChannel<B>, NeedsExport<AChannel<B>> {
    /**
     * The input.
     */
    private final AInput<B> input = new NulInput<B>().export();
    /**
     * The output.
     */
    private final AOutput<B> output = new NulOutput<B>().export();

    /**
     * @return null input for bytes
     */
    public static AChannel<ByteBuffer> bytes() {
        return new NulChannel<ByteBuffer>().export();
    }

    /**
     * @return null input for characters
     */
    public static AChannel<CharBuffer> chars() {
        return new NulChannel<CharBuffer>().export();
    }

    @Override
    public AChannel<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
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
    protected Promise<Void> closeAction() {
        return aAll(closeResourceAction(input)).andLast(closeResourceAction(output)).toVoid();
    }
}
