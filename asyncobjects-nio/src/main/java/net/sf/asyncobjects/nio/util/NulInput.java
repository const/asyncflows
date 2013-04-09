package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableBase;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aSuccess;

/**
 * The null input.
 *
 * @param <B> the buffer type
 */
public class NulInput<B extends Buffer> extends CloseableBase implements AInput<B>, ExportsSelf<AInput<B>> {

    @Override
    public Promise<Integer> read(final B buffer) {
        ensureOpen();
        return aSuccess(-1);
    }

    @Override
    public AInput<B> export() {
        return export(Vat.current());
    }

    @Override
    public AInput<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }

    /**
     * @return null input for bytes
     */
    public static AInput<ByteBuffer> bytes() {
        return new NulInput<ByteBuffer>().export();
    }

    /**
     * @return null input for characters
     */
    public static AInput<CharBuffer> chars() {
        return new NulInput<CharBuffer>().export();
    }
}