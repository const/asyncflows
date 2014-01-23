package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.util.AFunction2;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.adapters.Adapters;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.doAsyncThrowable;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Simple tests for {@link GZipInput} with files actually created by the command-line GZip.
 */
public class GZipInputTest {

    @Test
    public void testBest() throws Throwable {
        final GZipHeader header = checkFile("gzip-data-best.txt.gz");
        assertEquals(GZipHeader.DEFLATE_COMPRESSION, header.getCompressionMethod());
        assertEquals(GZipHeader.MAXIMUM_COMPRESSION, header.getExtraFlags());
        assertEquals("gzip-data-best.txt", header.getName());
    }

    @Test
    public void testFast() throws Throwable {
        final GZipHeader header = checkFile("gzip-data-fast.txt.gz");
        assertEquals(GZipHeader.DEFLATE_COMPRESSION, header.getCompressionMethod());
        assertEquals(GZipHeader.FASTEST_COMPRESSION, header.getExtraFlags());
        assertEquals("gzip-data-fast.txt", header.getName());
    }

    @Test
    public void testNoName() throws Throwable {
        final GZipHeader header = checkFile("gzip-data-no-name.txt.gz");
        assertNull(header.getName());
    }

    /**
     * Check file and return header.
     *
     * @param file the file
     * @return the GZip header
     * @throws Throwable the problem
     */
    private GZipHeader checkFile(final String file) throws Throwable {
        final Tuple2<Long, GZipHeader> rc = doAsyncThrowable(new ACallable<Tuple2<Long, GZipHeader>>() {
            @Override
            public Promise<Tuple2<Long, GZipHeader>> call() throws Throwable {
                final Promise<GZipHeader> header = new Promise<GZipHeader>();
                return aAll(new ACallable<Long>() {
                    @Override
                    public Promise<Long> call() throws Throwable {
                        return aTry(Adapters.getResource(GZipInputTest.class, file)).andChain(
                                new AFunction<AInput<ByteBuffer>, AInput<ByteBuffer>>() {
                                    @Override
                                    public Promise<AInput<ByteBuffer>> apply(final AInput<ByteBuffer> value)
                                            throws Throwable {
                                        return aValue(GZipInput.gunzip(value, header.resolver()));
                                    }
                                }).run(new AFunction2<Long, AInput<ByteBuffer>, AInput<ByteBuffer>>() {
                            @Override
                            public Promise<Long> apply(final AInput<ByteBuffer> value1,
                                                       final AInput<ByteBuffer> value2) throws Throwable {
                                return IOUtil.BYTE.discard(value2, ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE));
                            }
                        });
                    }
                }).andLast(new ACallable<GZipHeader>() {
                    @Override
                    public Promise<GZipHeader> call() throws Throwable {
                        return header;
                    }
                });
            }
        });
        assertEquals(rc.getValue1().longValue(), 227);
        return rc.getValue2();
    }
}
