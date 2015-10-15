package net.sf.asyncobjects.nio.text;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.data.Tuple3;
import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.util.BufferedPipe;
import net.sf.asyncobjects.nio.util.CharIOUtil;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.CoreFunctionUtil.promiseCallable;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static net.sf.asyncobjects.nio.util.DigestingInput.digestInput;
import static net.sf.asyncobjects.nio.util.DigestingOutput.digestOutput;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
                    () -> aTry(bytePipe.getOutput().map(value -> {
                        final AOutput<ByteBuffer> digested =
                                digestOutput(value, outputDigest.resolver()).sha256();
                        return aValue(EncoderOutput.encode(digested, CharIOUtil.UTF8, 4));
                    })).run(
                            value2 -> value2.write(CharBuffer.wrap(sample))
                    )
            ).and(
                    () -> aTry(bytePipe.getInput().map(value -> {
                        final AInput<ByteBuffer> digested =
                                digestInput(value, inputDigest.resolver()).sha256();
                        return aValue(DecoderInput.decode(digested, CharIOUtil.UTF8, 5));
                    })).run(
                            value -> CharIOUtil.getContent(value, CharBuffer.allocate(3))
                    )
            ).andLast(
                    () -> aAll(promiseCallable(inputDigest)).andLast(promiseCallable(outputDigest))
            );
        });
        assertEquals(sample, result.getValue2());
        assertArrayEquals(result.getValue3().getValue1(), result.getValue3().getValue2());
    }

}
