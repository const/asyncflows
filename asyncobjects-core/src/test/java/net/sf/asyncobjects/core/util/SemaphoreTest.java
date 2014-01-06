package net.sf.asyncobjects.core.util;


import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import org.junit.Test;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.stream.Streams.aForRange;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static org.junit.Assert.assertSame;

/**
 * The semaphore test.
 */
public class SemaphoreTest {
    @Test
    public void test() {
        final Void t = doAsync(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                final ASemaphore semaphore = new Semaphore(0).export();
                return aAll(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return aSeq(new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                return semaphore.acquire();
                            }
                        }).thenDo(new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                return semaphore.acquire(3);
                            }
                        }).thenDoLast(new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                return semaphore.acquire();
                            }
                        });
                    }
                }).andLast(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return aSeq(new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                return aForRange(0, 10).toUnit();
                            }
                        }).thenDo(new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                semaphore.release(2);
                                return aVoid();
                            }
                        }).thenDo(new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                return aForRange(0, 10).toUnit();
                            }
                        }).thenDoLast(new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                semaphore.release();
                                semaphore.release(3);
                                return aVoid();
                            }
                        });
                    }
                }).toVoid();
            }
        });
        assertSame(null, t);
    }
}
