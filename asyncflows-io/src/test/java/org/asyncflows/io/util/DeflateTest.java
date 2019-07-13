/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.util.CoreFlowsResource;
import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.asyncflows.core.AsyncContext.doAsyncThrowable;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * The test for deflate.
 */
public class DeflateTest {

    /**
     * Run compression test.
     *
     * @param compress   the compress stream transformer
     * @param decompress the decompress stream transformer
     * @throws Throwable
     */
    private static void checkCompression(final AFunction<AOutput<ByteBuffer>, AOutput<ByteBuffer>> compress,
                                         final AFunction<AInput<ByteBuffer>, AInput<ByteBuffer>> decompress)
            throws Throwable {
        final Random r = new Random();
        final int length = 1024 + r.nextInt(10240);
        final Tuple2<byte[], byte[]> rc = doAsyncThrowable(() -> {
            final AChannel<ByteBuffer> bytePipe = BufferedPipe.bytePipe(137);
            return aAll(
                    () -> aTry(bytePipe.getOutput()).andChain(compress).runWithSecond(
                            deflate -> {
                                final byte[] data = new byte[length];
                                for (int i = 0; i < data.length; i++) {
                                    data[i] = (byte) (r.nextGaussian() * 20);
                                }
                                final Promise<byte[]> digest = new Promise<byte[]>();
                                final AOutput<ByteBuffer> stream = DigestingOutput.digestOutput(
                                        deflate, digest.resolver()).sha1();
                                return CoreFlowsResource.aTryResource(stream).run(
                                        value -> value.write(ByteBuffer.wrap(data))
                                ).thenPromise(digest);
                            })
            ).andLast(
                    () -> aTry(bytePipe.getInput()).andChain(decompress).runWithSecond(
                            inflate -> DigestingInput.digestAndDiscardInput(inflate,
                                    AbstractDigestingStream.SHA_1)
                    )
            );
        });
        assertArrayEquals(rc.getValue1(), rc.getValue2());
    }

    @Test
    public void testDeflate() throws Throwable { // NOPMD
        checkCompression(
                value -> aValue(DeflateOutput.deflated(value, 512)),
                value -> aValue(InflateInput.inflated(value, 477))
        );
    }

    @Test
    public void tesGZip() throws Throwable { // NOPMD
        checkCompression(
                value -> aValue(GZipOutput.gzip(value)),
                value -> aValue(GZipInput.gunzip(value))
        );
    }
}
