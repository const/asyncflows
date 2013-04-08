package net.sf.asyncobjects.nio.text;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
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
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
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
        final String result = doAsync(new ACallable<String>() {
            @Override
            public Promise<String> call() throws Throwable {
                final AChannel<ByteBuffer> bytePipe = BufferedPipe.bytePipe(3);
                return aAll(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return aTry(bytePipe.getOutput().map(new AFunction<AOutput<CharBuffer>, AOutput<ByteBuffer>>() {
                            @Override
                            public Promise<AOutput<CharBuffer>> apply(final AOutput<ByteBuffer> value) throws Throwable {
                                return aSuccess(EncoderOutput.encode(value, CharIOUtil.UTF8, 4));
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
                            public Promise<AInput<CharBuffer>> apply(final AInput<ByteBuffer> value) throws Throwable {
                                return aSuccess(DecoderInput.decode(value, CharIOUtil.UTF8, 5));
                            }
                        })).run(new AFunction<String, AInput<CharBuffer>>() {
                            @Override
                            public Promise<String> apply(final AInput<CharBuffer> value) throws Throwable {
                                return CharIOUtil.getContent(value, CharBuffer.allocate(3));
                            }
                        });
                    }
                }).selectValue2();
            }
        });
        assertEquals(sample, result);
    }

}
