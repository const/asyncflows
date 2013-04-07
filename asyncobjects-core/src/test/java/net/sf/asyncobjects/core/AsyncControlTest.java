package net.sf.asyncobjects.core;

import org.junit.Test;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static org.junit.Assert.assertEquals;

public class AsyncControlTest {
    @Test
    public void testSimple() {
        final int rc = doAsync(new ACallable<Integer>() {
            @Override
            public Promise<Integer> call() throws Throwable {
                return aSuccess(42);
            }
        });
        assertEquals(42, rc);
    }

    @Test
    public void testSimpleLater() {
        final int rc = doAsync(new ACallable<Integer>() {
            @Override
            public Promise<Integer> call() throws Throwable {
                return aLater(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return aSuccess(42);
                    }
                });
            }
        });
        assertEquals(42, rc);
    }
}
