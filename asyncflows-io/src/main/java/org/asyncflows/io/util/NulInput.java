package org.asyncflows.io.util;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.AInput;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.IOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableBase;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * The null input.
 *
 * @param <B> the buffer type
 */
public class NulInput<B extends Buffer> extends CloseableBase implements AInput<B>, NeedsExport<AInput<B>> {

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

    @Override
    public Promise<Integer> read(final B buffer) {
        ensureOpen();
        return IOUtil.EOF_PROMISE;
    }

    @Override
    public AInput<B> export(final Vat vat) {
        return IOExportUtil.export(vat, this);
    }
}
