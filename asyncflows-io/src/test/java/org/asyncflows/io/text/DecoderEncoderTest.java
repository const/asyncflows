package org.asyncflows.io.text;

import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.util.BufferedPipe;
import org.asyncflows.io.util.CharIOUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.data.Tuple3;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static org.asyncflows.io.util.DigestingInput.digestInput;
import static org.asyncflows.io.util.DigestingOutput.digestOutput;
import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.function.AsyncFunctionUtil.promiseSupplier;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test for decoders and encoders.
 */
public class DecoderEncoderTest {
    @Test
    public void testSimple() {
        final StringBuilder builder = new StringBuilder(); // NOPMD
        builder.append("Test ");
        builder.appendCodePoint(0x405).appendCodePoint(0x1F648).appendCodePoint(0x1F649).appendCodePoint(0x1F64A);
        final String sample = builder.toString();
        final Tuple3<Void, String, Tuple2<byte[], byte[]>> result = doAsync(() -> {
            final Promise<byte[]> inputDigest = new Promise<byte[]>();
            final Promise<byte[]> outputDigest = new Promise<byte[]>();
            final AChannel<ByteBuffer> bytePipe = BufferedPipe.bytePipe(3);
            return aAll(
                    () -> aTry(bytePipe.getOutput().flatMap(value -> {
                        final AOutput<ByteBuffer> digested =
                                digestOutput(value, outputDigest.resolver()).sha256();
                        return aValue(EncoderOutput.encode(digested, CharIOUtil.UTF8, 4));
                    })).run(
                            value2 -> value2.write(CharBuffer.wrap(sample))
                    )
            ).and(
                    () -> aTry(bytePipe.getInput().flatMap(value -> {
                        final AInput<ByteBuffer> digested =
                                digestInput(value, inputDigest.resolver()).sha256();
                        return aValue(DecoderInput.decode(digested, CharIOUtil.UTF8, 5));
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
