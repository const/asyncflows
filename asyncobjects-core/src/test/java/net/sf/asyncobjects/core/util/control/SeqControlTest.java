package net.sf.asyncobjects.core.util.control;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.util.AFunction2;
import net.sf.asyncobjects.core.vats.Vat;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.stream.Streams.aForArray;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoopFair;
import static org.junit.Assert.assertEquals;

/**
 * Tests for sequential control constructs.
 */
public class SeqControlTest {

    @Test
    public void testSeq() {
        final ArrayList<Integer> list = new ArrayList<Integer>();
        final int rc = doAsync(new ACallable<Integer>() {
            @Override
            public Promise<Integer> call() throws Throwable {
                return aSeq(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        list.add(1);
                        return aValue(1);
                    }
                }).map(new AFunction<Object, Integer>() {
                    @Override
                    public Promise<Object> apply(final Integer value) throws Throwable {
                        list.add(value + 1);
                        throw new IllegalStateException();
                    }
                }).thenDo(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        // never called
                        list.add(-1);
                        return aValue(-1);
                    }
                }).failed(new AFunction<Integer, Throwable>() {
                    @Override
                    public Promise<Integer> apply(final Throwable value) throws Throwable {
                        assertEquals(IllegalStateException.class, value.getClass());
                        list.add(3);
                        return aValue(42);
                    }
                }).finallyDo(new ACallable<Object>() {
                    @Override
                    public Promise<Object> call() throws Throwable {
                        list.add(4);
                        return aValue(null);
                    }
                });
            }
        });
        assertEquals(42, rc);
        assertEquals(Arrays.asList(1, 2, 3, 4), list);
    }

    @Test
    public void testSeqSimple() {
        final String test = doAsync(new ACallable<String>() {
            @Override
            public Promise<String> call() throws Exception {
                return aSeq(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Exception {
                        return aValue(42);
                    }
                }).map(new AFunction<String, Integer>() {
                    @Override
                    public Promise<String> apply(final Integer value) throws Throwable {
                        return aLater(new ACallable<String>() {
                            @Override
                            public Promise<String> call() throws Exception {
                                return aValue("The answer is " + value);
                            }
                        });
                    }
                }).finallyDo(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Exception {
                        return aValue(24);
                    }
                });
            }
        });
        assertEquals("The answer is 42", test);
    }

    @Test
    public void testSeqFailed() {
        final Outcome<Integer> test = doAsync(new ACallable<Outcome<Integer>>() {
            @Override
            public Promise<Outcome<Integer>> call() throws Exception {
                return aSeq(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Exception {
                        return aFailure(new IllegalStateException("test"));
                    }
                }).failedLast(new AFunction<Integer, Throwable>() {
                    @Override
                    public Promise<Integer> apply(final Throwable value) throws Throwable {
                        return aValue(42);  //To change body of implemented methods use File | Settings | File Templates.
                    }
                }).toOutcomePromise();
            }
        });
        assertEquals(Outcome.success(42), test);
        final Outcome<Integer> test2 = doAsync(new ACallable<Outcome<Integer>>() {
            @Override
            public Promise<Outcome<Integer>> call() throws Exception {
                return aSeq(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Exception {
                        return aFailure(new IllegalStateException("1"));
                    }
                }).failedLast(new AFunction<Integer, Throwable>() {
                    @Override
                    public Promise<Integer> apply(final Throwable value) throws Throwable {
                        throw new IllegalStateException("2");
                    }
                }).toOutcomePromise();
            }
        });
        assertFailureMessage("2", test2);
    }

    /**
     * Check if failure is an exception and has a required method
     *
     * @param message the message
     * @param outcome the outcome to check
     */
    private static void assertFailureMessage(final String message, final Outcome<?> outcome) {
        //noinspection ThrowableResultOfMethodCallIgnored
        assertEquals(message, outcome.failure().getMessage());
    }


    @Test
    public void testSeqFinallyFailed() {
        final Outcome<Integer> test = doAsync(new ACallable<Outcome<Integer>>() {
            @Override
            public Promise<Outcome<Integer>> call() throws Exception {
                return aSeq(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Exception {
                        return aFailure(new IllegalStateException("test"));
                    }
                }).finallyDo(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return aVoid();
                    }
                }).toOutcomePromise();
            }
        });
        assertFailureMessage("test", test);
        final Outcome<Integer> test2 = doAsync(new ACallable<Outcome<Integer>>() {
            @Override
            public Promise<Outcome<Integer>> call() throws Exception {
                return aSeq(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Exception {
                        throw new IllegalStateException("test");
                    }
                }).finallyDo(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return aLater(Vat.current(), new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                throw new IllegalStateException("finally");
                            }
                        });
                    }
                }).toOutcomePromise();
            }
        });
        assertFailureMessage("test", test2);
        final Outcome<Integer> test3 = doAsync(new ACallable<Outcome<Integer>>() {
            @Override
            public Promise<Outcome<Integer>> call() throws Exception {
                return aSeq(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Exception {
                        return aValue(42);
                    }
                }).finallyDo(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        throw new IllegalStateException("finally");
                    }
                }).toOutcomePromise();
            }
        });
        assertFailureMessage("finally", test3);
    }


    @Test
    public void testSeqForLoop() {
        final int rc = doAsync(new ACallable<Integer>() {
            @Override
            public Promise<Integer> call() throws Throwable {
                return aForArray(0, 1, 2, 3, 4).leftFold(0,
                        new AFunction2<Integer, Integer, Integer>() {
                            @Override
                            public Promise<Integer> apply(final Integer result, final Integer item) throws Throwable {
                                return aValue(result + item);
                            }
                        });
            }
        });
        assertEquals(10, rc);
    }

    @Test
    public void testSeqLoopFail() {
        final Cell<Boolean> flag = new Cell<Boolean>(true);
        final Outcome<Void> rc = doAsync(new ACallable<Outcome<Void>>() {
            @Override
            public Promise<Outcome<Void>> call() throws Throwable {
                return aSeqLoopFair(new ACallable<Boolean>() {
                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        if (flag.getValue()) {
                            flag.setValue(false);
                            return aTrue();
                        } else {
                            return aLater(new ACallable<Boolean>() {
                                @Override
                                public Promise<Boolean> call() throws Throwable {
                                    throw new IllegalStateException("failed");
                                }
                            });
                        }
                    }
                }).toOutcomePromise();
            }
        });
        assertFailureMessage("failed", rc);
    }

}
