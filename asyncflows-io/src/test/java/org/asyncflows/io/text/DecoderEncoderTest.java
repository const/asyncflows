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

package org.asyncflows.io.text;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.data.Tuple3;
import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.util.BufferedPipe;
import org.asyncflows.io.util.CharIOUtil;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.function.AsyncFunctionUtil.promiseSupplier;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.asyncflows.io.util.DigestingInput.digestInput;
import static org.asyncflows.io.util.DigestingOutput.digestOutput;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test for decoders and encoders.
 */
public class DecoderEncoderTest {
    @Test
    public void testSimple() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Test ");
        builder.appendCodePoint(0x405).appendCodePoint(0x1F648).appendCodePoint(0x1F649).appendCodePoint(0x1F64A);
        final String sample = builder.toString();
        final Tuple3<Void, String, Tuple2<byte[], byte[]>> result = doAsync(() -> {
            final Promise<byte[]> inputDigest = new Promise<>();
            final Promise<byte[]> outputDigest = new Promise<>();
            final AChannel<ByteBuffer> bytePipe = BufferedPipe.bytePipe(3);
            return aAll(
                    () -> aTry(bytePipe.getOutput().flatMap(value -> {
                        final AOutput<ByteBuffer> digested =
                                digestOutput(value, outputDigest.resolver()).sha256();
                        return aValue(EncoderOutput.encode(digested, StandardCharsets.UTF_8, 4));
                    })).run(
                            value2 -> value2.write(CharBuffer.wrap(sample))
                    )
            ).and(
                    () -> aTry(bytePipe.getInput().flatMap(value -> {
                        final AInput<ByteBuffer> digested =
                                digestInput(value, inputDigest.resolver()).sha256();
                        return aValue(DecoderInput.decode(digested, StandardCharsets.UTF_8, 5));
                    })).run(
                            value -> CharIOUtil.getContent(value, CharBuffer.allocate(3))
                    )
            ).andLast(
                    () -> aAll(promiseSupplier(inputDigest)).andLast(promiseSupplier(outputDigest))
            );
        });
        assertEquals(sample, result.getValue2());
        assertArrayEquals(result.getValue3().getValue1(), result.getValue3().getValue2());
    }

}
