package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.util.ResourceUtil;
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
                                return ResourceUtil.aTryResource(stream).run(
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
