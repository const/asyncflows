package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Random;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.doAsyncThrowable;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static org.junit.Assert.assertArrayEquals;

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
        final Tuple2<byte[], byte[]> rc = doAsyncThrowable(new ACallable<Tuple2<byte[], byte[]>>() {
            @Override
            public Promise<Tuple2<byte[], byte[]>> call() throws Throwable {
                final AChannel<ByteBuffer> bytePipe = BufferedPipe.bytePipe(137);
                return aAll(new ACallable<byte[]>() {
                    @Override
                    public Promise<byte[]> call() throws Throwable {
                        return aTry(bytePipe.getOutput()).andChain(compress).runWithSecond(
                                new AFunction<byte[], AOutput<ByteBuffer>>() {
                                    @Override
                                    public Promise<byte[]> apply(final AOutput<ByteBuffer> deflate) throws Throwable {
                                        final byte[] data = new byte[length];
                                        for (int i = 0; i < data.length; i++) {
                                            data[i] = (byte) (r.nextGaussian() * 20);
                                        }
                                        final Promise<byte[]> digest = new Promise<byte[]>();
                                        final AOutput<ByteBuffer> stream = DigestingOutput.digestOutput(
                                                deflate, digest.resolver()).sha1();
                                        return aTry(stream).run(new AFunction<Void, AOutput<ByteBuffer>>() {
                                            @Override
                                            public Promise<Void> apply(final AOutput<ByteBuffer> value)
                                                    throws Throwable {
                                                return value.write(ByteBuffer.wrap(data));
                                            }
                                        }).thenPromise(digest);
                                    }
                                }
                        );
                    }
                }).andLast(new ACallable<byte[]>() {
                    @Override
                    public Promise<byte[]> call() throws Throwable {
                        return aTry(bytePipe.getInput()).andChain(decompress).runWithSecond(
                                new AFunction<byte[], AInput<ByteBuffer>>() {
                                    @Override
                                    public Promise<byte[]> apply(final AInput<ByteBuffer> inflate) throws Throwable {
                                        return DigestingInput.digestAndDiscardInput(inflate,
                                                AbstractDigestingStream.SHA_1);
                                    }
                                });
                    }
                });
            }
        });
        assertArrayEquals(rc.getValue1(), rc.getValue2());
    }

    @Test
    public void testDeflate() throws Throwable { // NOPMD
        checkCompression(
                new AFunction<AOutput<ByteBuffer>, AOutput<ByteBuffer>>() {
                    @Override
                    public Promise<AOutput<ByteBuffer>> apply(final AOutput<ByteBuffer> value) throws Throwable {
                        return aValue(DeflateOutput.deflated(value, 512));
                    }
                },
                new AFunction<AInput<ByteBuffer>, AInput<ByteBuffer>>() {
                    @Override
                    public Promise<AInput<ByteBuffer>> apply(final AInput<ByteBuffer> value) throws Throwable {
                        return aValue(InflateInput.inflated(value, 477));
                    }
                }
        );
    }

    @Test
    public void tesGZip() throws Throwable { // NOPMD
        checkCompression(
                new AFunction<AOutput<ByteBuffer>, AOutput<ByteBuffer>>() {
                    @Override
                    public Promise<AOutput<ByteBuffer>> apply(final AOutput<ByteBuffer> value) throws Throwable {
                        return aValue(GZipOutput.gzip(value));
                    }
                },
                new AFunction<AInput<ByteBuffer>, AInput<ByteBuffer>>() {
                    @Override
                    public Promise<AInput<ByteBuffer>> apply(final AInput<ByteBuffer> value) throws Throwable {
                        return aValue(GZipInput.gunzip(value));
                    }
                }
        );
    }
}
