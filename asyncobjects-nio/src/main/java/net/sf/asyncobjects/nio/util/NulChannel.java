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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.closeResourceAction;

/**
 * The null channel.
 *
 * @param <B> the buffer type
 */
public class NulChannel<B extends Buffer> extends CloseableBase implements AChannel<B>, ExportsSelf<AChannel<B>> {
    /**
     * The input.
     */
    private final AInput<B> input = new NulInput<B>().export();
    /**
     * The output.
     */
    private final AOutput<B> output = new NulOutput<B>().export();

    @Override
    public AChannel<B> export() {
        return export(Vat.current());
    }

    @Override
    public AChannel<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }

    @Override
    public Promise<AInput<B>> getInput() {
        return aSuccess(input);
    }

    @Override
    public Promise<AOutput<B>> getOutput() {
        return aSuccess(output);
    }

    @Override
    protected Promise<Void> closeAction() {
        return aAll(closeResourceAction(input)).andLast(closeResourceAction(output)).toUnit();
    }

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
}
