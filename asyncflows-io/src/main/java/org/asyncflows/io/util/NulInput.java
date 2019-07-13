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

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AInputProxyFactory;
import org.asyncflows.io.IOUtil;
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
        return AInputProxyFactory.createProxy(vat, this);
    }
}
