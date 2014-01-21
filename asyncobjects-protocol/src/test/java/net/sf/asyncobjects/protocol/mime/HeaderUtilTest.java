package net.sf.asyncobjects.protocol.mime;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.adapters.Adapters;
import net.sf.asyncobjects.nio.util.InputContext;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.doAsyncThrowable;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The test for header utilities.
 */
public class HeaderUtilTest {

    @Test
    public void test() throws Throwable {
        final HeaderSet headers = doAsyncThrowable(new ACallable<HeaderSet>() {
            @Override
            public Promise<HeaderSet> call() throws Throwable {
                return aTry(Adapters.getResource(HeaderUtilTest.class, "mime-rfc822-A.3.3.sample")).run(
                        new AFunction<HeaderSet, AInput<ByteBuffer>>() {
                            @Override
                            public Promise<HeaderSet> apply(final AInput<ByteBuffer> value) throws Throwable {
                                return HeaderUtil.readHeaders(new InputContext(value, 23), 2048);
                            }
                        });
            }
        });
        final String text = headers.toText();
        assertEquals(1047, text.length());
        final Header h = headers.iterator().next();
        assertEquals("Date", h.getName());
        assertEquals("27 Aug 76 0932 PDT", h.getValue());
        assertEquals("Date     :  27 Aug 76 0932 PDT\r\n", h.getRaw());
    }

    /**
     * The test checks that everything is done in one turn, if needed data is prefetched.
     *
     * @throws Throwable the problem with test
     */
    @Test
    public void testBuffered() throws Throwable {
        final HeaderSet headers = doAsyncThrowable(new ACallable<HeaderSet>() {
            @Override
            public Promise<HeaderSet> call() throws Throwable {
                return aTry(Adapters.getResource(HeaderUtilTest.class, "mime-rfc822-A.3.3.sample")).run(
                        new AFunction<HeaderSet, AInput<ByteBuffer>>() {
                            @Override
                            public Promise<HeaderSet> apply(final AInput<ByteBuffer> value) throws Throwable {
                                final InputContext context = new InputContext(value, 2048);
                                return context.ensureAvailable(1047).thenDo(new ACallable<HeaderSet>() {
                                    @Override
                                    public Promise<HeaderSet> call() throws Throwable {
                                        final Promise<HeaderSet> promise = HeaderUtil.readHeaders(context, 2048);
                                        assertTrue(promise.isResolved());
                                        return promise;
                                    }
                                });
                            }
                        });
            }
        });
        final String text = headers.toText();
        assertEquals(1047, text.length());
        final Header h = headers.iterator().next();
        assertEquals("Date", h.getName());
        assertEquals("27 Aug 76 0932 PDT", h.getValue());
        assertEquals("Date     :  27 Aug 76 0932 PDT\r\n", h.getRaw());
    }

}
