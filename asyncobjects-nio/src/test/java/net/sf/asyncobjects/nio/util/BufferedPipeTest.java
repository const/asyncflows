package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.CharIOUtil;
import org.junit.Test;

import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static org.junit.Assert.assertEquals;

/**
 * Test for buffered pipe.
 */
public class BufferedPipeTest {

    @Test
    public void charPipeTest() {
        final String source = "Test String";
        final String result = doAsync(new ACallable<String>() {
            @Override
            public Promise<String> call() throws Throwable {
                final AChannel<CharBuffer> pipe = BufferedPipe.charPipe(2);
                return aAll(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return aTry(pipe.getOutput()).run(new AFunction<Void, AOutput<CharBuffer>>() {
                            @Override
                            public Promise<Void> apply(final AOutput<CharBuffer> value) throws Throwable {
                                return value.write(CharBuffer.wrap(source));
                            }
                        });
                    }
                }).and(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return aTry(pipe.getInput()).run(new AFunction<String, AInput<CharBuffer>>() {
                            @Override
                            public Promise<String> apply(final AInput<CharBuffer> value) throws Throwable {
                                return CharIOUtil.getContent(value, CharBuffer.allocate(3));
                            }
                        });
                    }
                }).selectValue2();
            }
        });
        assertEquals(source, result);
    }
}
