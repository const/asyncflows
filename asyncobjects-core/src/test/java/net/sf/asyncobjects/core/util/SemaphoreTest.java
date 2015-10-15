package net.sf.asyncobjects.core.util;


import net.sf.asyncobjects.core.vats.Vat;
import org.junit.Test;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.stream.Streams.aForRange;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The semaphore test.
 */
public class SemaphoreTest {
    @Test
    public void test() {
        final Void t = doAsync(() -> {
            final ASemaphore semaphore = new Semaphore(0).export();
            //noinspection Convert2MethodRef
            return aAll(() ->
                            aSeq(
                                    () -> semaphore.acquire()
                            ).thenDo(
                                    () -> semaphore.acquire(3)
                            ).thenDoLast(
                                    () -> semaphore.acquire()
                            )
            ).andLast(() ->
                    aSeq(
                            () -> aForRange(0, 10).toVoid()
                    ).thenDo(() -> {
                        semaphore.release(2);
                        return aVoid();
                    }).thenDo(
                            () -> aForRange(0, 10).toVoid()
                    ).thenDoLast(() -> {
                        semaphore.release();
                        semaphore.release(3);
                        return aVoid();
                    })).toVoid();
        });
        assertSame(null, t);
    }

    @Test
    public void testReflection() {
        final Void t = doAsync(() -> {
            final Semaphore object = new Semaphore(0);
            final ASemaphore semaphore = ReflectionExporter.export(Vat.current(), object);
            assertTrue(semaphore.toString().startsWith("[ASemaphore]@"));
            assertEquals(semaphore, ReflectionExporter.export(Vat.current(), object));
            assertEquals(System.identityHashCode(object), semaphore.hashCode());
            return aAll(
                    () -> aSeq(
                            semaphore::acquire
                    ).thenDo(
                            () -> semaphore.acquire(3)
                    ).thenDoLast(semaphore::acquire)
            ).andLast(
                    () -> aSeq(
                            () -> aForRange(0, 10).toVoid()
                    ).thenDo(() -> {
                        semaphore.release(2);
                        return aVoid();
                    }).thenDo(
                            () -> aForRange(0, 10).toVoid()
                    ).thenDoLast(() -> {
                        semaphore.release();
                        semaphore.release(3);
                        return aVoid();
                    })
            ).toVoid();
        });
        assertSame(null, t);
    }

}
