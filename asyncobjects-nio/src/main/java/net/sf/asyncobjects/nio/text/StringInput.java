package net.sf.asyncobjects.nio.text;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableBase;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.BufferOperations;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.NIOExportUtil;

import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aValue;

/**
 * The input over character sequence.
 */
public class StringInput extends CloseableBase implements AInput<CharBuffer>, ExportsSelf<AInput<CharBuffer>> {
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
    public AInput<CharBuffer> export() {
        return export(Vat.current());
    }

    @Override
    public AInput<CharBuffer> export(final Vat vat) {
        return NIOExportUtil.export(vat, this);
    }
}
