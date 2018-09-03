package org.asyncflows.core.util;


import org.asyncflows.core.vats.Vat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.AsyncControl.aVoid;
import static org.asyncflows.core.streams.AsyncStreams.aForRange;
import static org.asyncflows.core.util.AsyncAllControl.aAll;
import static org.asyncflows.core.util.AsyncSeqControl.aSeq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The semaphore test.
 */
public class SemaphoreTest {
    @Test
    public void test() {
        final ArrayList<Integer> result = new ArrayList<>();
        final Void t = doAsync(() -> {
            final ASemaphore semaphore = new Semaphore(0).export();
            //noinspection Convert2MethodRef
            return aAll(() ->
                            aSeq(
                                    () -> semaphore.acquire().listen(o -> result.add(1))
                            ).thenDo(
                                    () -> semaphore.acquire(3).listen(o -> result.add(2))
                            ).thenDoLast(
                                    () -> semaphore.acquire().listen(o -> result.add(3))
                            )
            ).andLast(() ->
                    aSeq(
                            () -> aForRange(0, 10).toVoid()
                    ).thenDo(() -> {
                        result.add(-1);
                        semaphore.release(2);
                        return aVoid();
                    }).thenDo(
                            () -> aForRange(0, 10).toVoid()
                    ).thenDo(() -> {
                        result.add(-2);
                        semaphore.release();
                        return aVoid();
                    }).thenDo(
                            () -> aForRange(0, 10).toVoid()
                    ).thenDoLast(() -> {
                        result.add(-3);
                        semaphore.release(3);
                        return aVoid();
                    })).toVoid();
        });
        assertSame(null, t);
        assertEquals(Arrays.asList(-1, 1, -2, -3,  2, 3), result);
    }

    @Test
    public void testReflection() {
        final Void t = doAsync(() -> {
            final Semaphore object = new Semaphore(0);
            final ASemaphore semaphore = ObjectExporter.export(Vat.current(), object);
            assertTrue(semaphore.toString().startsWith("[ASemaphore]@"));
            assertEquals(semaphore, ObjectExporter.export(Vat.current(), object));
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
