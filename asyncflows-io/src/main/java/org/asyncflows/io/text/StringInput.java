package org.asyncflows.io.text;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.AInput;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.NIOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableBase;

import java.nio.CharBuffer;

import static org.asyncflows.core.AsyncControl.aValue;

/**
 * The input over character sequence.
 */
public class StringInput extends CloseableBase implements AInput<CharBuffer>, NeedsExport<AInput<CharBuffer>> {
    /**
     * The buffer with data.
     */
    private final CharBuffer data;

    /**
     * The character buffer with data.
     *
     * @param data the buffer with data
     */
    public StringInput(final CharBuffer data) {
        this.data = data;
    }

    /**
     * Wrap character sequence into the input.
     *
     * @param chars the characters to wrap
     * @return the input
     */
    public static AInput<CharBuffer> wrap(final CharSequence chars) {
        return new StringInput(CharBuffer.wrap(chars)).export();
    }

    @Override
    public Promise<Integer> read(final CharBuffer buffer) {
        ensureOpen();
        if (data.hasRemaining()) {
            return aValue(BufferOperations.CHAR.put(buffer, data));
        } else {
            return IOUtil.EOF_PROMISE;
        }
    }

    @Override
    public AInput<CharBuffer> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
