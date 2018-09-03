package org.asyncflows.io.util;

import org.asyncflows.io.AOutput;
import org.asyncflows.io.NIOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.NeedsExport;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static org.asyncflows.core.AsyncControl.aVoid;

/**
 * The nul output that discards everything.
 *
 * @param <B> the buffer type
 */
public class NulOutput<B extends Buffer> extends CloseableBase implements AOutput<B>, NeedsExport<AOutput<B>> {
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
    public AOutput<B> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
