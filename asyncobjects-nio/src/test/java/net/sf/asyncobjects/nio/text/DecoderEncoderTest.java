package net.sf.asyncobjects.nio.text;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.data.Tuple3;
import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.CharIOUtil;
import net.sf.asyncobjects.nio.util.BufferedPipe;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
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
        final Tuple3<Void, String, Tuple2<byte[], byte[]>> result = doAsync(new ACallable<Tuple3<Void, String,
                Tuple2<byte[], byte[]>>>() {
            @Override
            public Promise<Tuple3<Void, String, Tuple2<byte[], byte[]>>> call() throws Throwable {
                final Promise<byte[]> inputDigest = new Promise<byte[]>();
                final Promise<byte[]> outputDigest = new Promise<byte[]>();
                final AChannel<ByteBuffer> bytePipe = BufferedPipe.bytePipe(3);
                return aAll(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return aTry(bytePipe.getOutput().map(new AFunction<AOutput<CharBuffer>, AOutput<ByteBuffer>>() {
                            @Override
                            public Promise<AOutput<CharBuffer>> apply(final AOutput<ByteBuffer> value) {
                                final AOutput<ByteBuffer> digested =
                                        digestOutput(value, outputDigest.resolver()).sha256();
                                return aSuccess(EncoderOutput.encode(digested, CharIOUtil.UTF8, 4));
                            }
                        })).run(new AFunction<Void, AOutput<CharBuffer>>() {
                            @Override
                            public Promise<Void> apply(final AOutput<CharBuffer> value2) throws Throwable {
                                return value2.write(CharBuffer.wrap(sample));
                            }
                        });
                    }
                }).and(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return aTry(bytePipe.getInput().map(new AFunction<AInput<CharBuffer>, AInput<ByteBuffer>>() {
                            @Override
                            public Promise<AInput<CharBuffer>> apply(final AInput<ByteBuffer> value) {
                                final AInput<ByteBuffer> digested =
                                        digestInput(value, inputDigest.resolver()).sha256();
                                return aSuccess(DecoderInput.decode(digested, CharIOUtil.UTF8, 5));
                            }
                        })).run(new AFunction<String, AInput<CharBuffer>>() {
                            @Override
                            public Promise<String> apply(final AInput<CharBuffer> value) {
                                return CharIOUtil.getContent(value, CharBuffer.allocate(3));
                            }
                        });
                    }
                }).andLast(new ACallable<Tuple2<byte[], byte[]>>() {
                    @Override
                    public Promise<Tuple2<byte[], byte[]>> call() {
                        return aAll(promiseCallable(inputDigest)).andLast(promiseCallable(outputDigest));
                    }
                });
            }
        });
        assertEquals(sample, result.getValue2());
        assertArrayEquals(result.getValue3().getValue1(), result.getValue3().getValue2());
    }

}
