/*
 * Copyright (c) 2018 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.io.util;

import org.asyncflows.io.AOutput;
import org.asyncflows.io.IOExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.NeedsExport;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static org.asyncflows.core.CoreFlows.aVoid;

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
        return IOExportUtil.export(vat, this);
    }
}
