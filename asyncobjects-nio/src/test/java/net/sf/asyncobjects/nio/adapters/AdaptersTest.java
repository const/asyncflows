package net.sf.asyncobjects.nio.adapters;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.util.AFunction2;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.IOUtil;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * The test for adapters.
 */
public class AdaptersTest {
    /**
     * The data used for test.
     */
    private static final byte[] DATA5 = new byte[]{1, 2, 3, 4, 5};

    @Test
    public void testCopy() {
        final ByteBuffer buffer = ByteBuffer.allocate(2);
        final Tuple2<Long, byte[]> result = doCopyTest(true, buffer, DATA5);
        assertEquals(5L, result.getValue1().longValue());
        assertArrayEquals(DATA5, result.getValue2());
    }

    @Test
    public void testCopyDirect() {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(2);
        final Tuple2<Long, byte[]> result = doCopyTest(false, buffer, DATA5);
        assertEquals(5L, result.getValue1().longValue());
        assertArrayEquals(DATA5, result.getValue2());
    }

    @Test
    public void testCopyEmpty() {
        final ByteBuffer buffer = ByteBuffer.allocate(2);
        final byte[] empty = new byte[0];
        final Tuple2<Long, byte[]> result = doCopyTest(false, buffer, empty);
        assertEquals(0L, result.getValue1().longValue());
        assertArrayEquals(empty, result.getValue2());
    }

    @Test
    public void testStringCopy() {
        final CharBuffer buffer = CharBuffer.allocate(2);
        final String test = "Test String";
        final Tuple2<Long, String> tuple2 = doAsync(new ACallable<Tuple2<Long, String>>() {
            @Override
            public Promise<Tuple2<Long, String>> call() throws Throwable {
                final Promise<String> second = new Promise<String>();
                return aAll(new ACallable<Long>() {
                    @Override
                    public Promise<Long> call() throws Throwable {
                        return aTry(new ACallable<AInput<CharBuffer>>() {
                            @Override
                            public Promise<AInput<CharBuffer>> call() throws Throwable {
                                return aSuccess(Adapters.getStringInput(test));
                            }
                        }).andOther(new ACallable<AOutput<CharBuffer>>() {
                            @Override
                            public Promise<AOutput<CharBuffer>> call() throws Throwable {
                                return aSuccess(Adapters.getStringOutput(second.resolver()));
                            }
                        }).run(new AFunction2<Long, AInput<CharBuffer>, AOutput<CharBuffer>>() {
                            @Override
                            public Promise<Long> apply(final AInput<CharBuffer> value1,
                                                       final AOutput<CharBuffer> value2) throws Throwable {
                                return IOUtil.CHAR.copy(value1, value2, false, buffer);
                            }
                        });
                    }
                }).andLast(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return second;
                    }
                });
            }
        });
        assertEquals(test.length(), tuple2.getValue1().longValue());
        assertEquals(test, tuple2.getValue2());
    }

    /**
     * Execute copy test.
     *
     * @param autoFlush the value of autoFlush to use for copy
     * @param buffer    the buffer to test
     * @param inputData the input data
     * @return the result
     */
    private Tuple2<Long, byte[]> doCopyTest(final boolean autoFlush, final ByteBuffer buffer, final byte[] inputData) {
        return doAsync(new ACallable<Tuple2<Long, byte[]>>() {
            @Override
            public Promise<Tuple2<Long, byte[]>> call() throws Throwable {
                final Promise<byte[]> array = new Promise<byte[]>();
                return aAll(new ACallable<Long>() {
                    @Override
                    public Promise<Long> call() throws Throwable {
                        return aTry(new ACallable<AInput<ByteBuffer>>() {
                            @Override
                            public Promise<AInput<ByteBuffer>> call() throws Throwable {
                                return aSuccess(Adapters.getByteArrayInput(inputData));
                            }
                        }).andOther(new ACallable<AOutput<ByteBuffer>>() {
                            @Override
                            public Promise<AOutput<ByteBuffer>> call() throws Throwable {
                                return aSuccess(Adapters.getByteArrayOutput(array.resolver()));
                            }
                        }).run(new AFunction2<Long, AInput<ByteBuffer>, AOutput<ByteBuffer>>() {
                            @Override
                            public Promise<Long> apply(final AInput<ByteBuffer> input,
                                                       final AOutput<ByteBuffer> output) throws Throwable {
                                return IOUtil.BYTE.copy(input, output, autoFlush, buffer);
                            }
                        });
                    }
                }).andLast(new ACallable<byte[]>() {
                    @Override
                    public Promise<byte[]> call() throws Throwable {
                        return array;
                    }
                });
            }
        });
    }
}
