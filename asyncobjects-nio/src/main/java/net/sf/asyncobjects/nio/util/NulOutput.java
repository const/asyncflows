package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableBase;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * The nul output that discards everything.
 *
 * @param <B> the buffer type
 */
public class NulOutput<B extends Buffer> extends CloseableBase implements AOutput<B>, ExportsSelf<AOutput<B>> {
    @Override
    public Promise<Void> write(final B buffer) {
        ensureOpen();
        buffer.position(buffer.limit());
        return aVoid();
    }

    @Override
    public Promise<Void> flush() {
        ensureOpen();
        return aVoid();
    }

    @Override
    public AOutput<B> export() {
        return export(Vat.current());
    }

    @Override
    public AOutput<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }

    /**
     * @return null output for bytes
     */
    public static AOutput<ByteBuffer> bytes() {
        return new NulOutput<ByteBuffer>().export();
    }

    /**
     * @return null output for characters
     */
    public static AOutput<CharBuffer> chars() {
        return new NulOutput<CharBuffer>().export();
    }
}
