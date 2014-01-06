package net.sf.asyncobjects.core.util.control;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.CoreFunctionUtil;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.data.Tuple3;
import net.sf.asyncobjects.core.util.AFunction2;
import net.sf.asyncobjects.core.util.AFunction3;
import net.sf.asyncobjects.core.util.AllControl;
import org.junit.Test;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AllControlTest {
    @Test
    public void testAll2() {
        final Tuple2<String, Integer> rc = doAsync(new ACallable<Tuple2<String, Integer>>() {
            @Override
            public Promise<Tuple2<String, Integer>> call() throws Throwable {
                return aAll(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return aValue("The answer");
                    }
                }).andLast(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return aLater(new ACallable<Integer>() {
                            @Override
                            public Promise<Integer> call() throws Throwable {
                                return aValue(42);
                            }
                        });
                    }
                });
            }
        });
        assertEquals(Tuple2.of("The answer", 42), rc);
    }

    @Test
    public void testAll2Failures() {
        final Outcome<Tuple2<String, Integer>> rc1 = doAsync(new ACallable<Outcome<Tuple2<String, Integer>>>() {
            @Override
            public Promise<Outcome<Tuple2<String, Integer>>> call() throws Throwable {
                return aAll(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return aFailure(new IllegalStateException("1"));
                    }
                }).andLast(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return aFailure(new IllegalStateException("2"));
                    }
                }).toOutcomePromise();
            }
        });
        assertFalse(rc1.isSuccess());
        //noinspection ThrowableResultOfMethodCallIgnored
        assertEquals("1", rc1.failure().getMessage());
        final Outcome<Tuple2<String, Integer>> rc2 = doAsync(new ACallable<Outcome<Tuple2<String, Integer>>>() {
            @Override
            public Promise<Outcome<Tuple2<String, Integer>>> call() throws Throwable {
                return aAll(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return aValue("1");
                    }
                }).andLast(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        throw new IllegalStateException("2");
                    }
                }).toOutcomePromise();
            }
        });
        assertFalse(rc2.isSuccess());
        //noinspection ThrowableResultOfMethodCallIgnored
        assertEquals("2", rc2.failure().getMessage());
    }

    @Test
    public void testAll2SelectZip() {
        final Tuple2<String, Integer> rc = doAsync(new ACallable<Tuple2<String, Integer>>() {
            @Override
            public Promise<Tuple2<String, Integer>> call() throws Throwable {
                return aAll(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return partialAll().selectValue1();
                    }
                }).and(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return partialAll().selectValue2();
                    }
                }).unzip(new AFunction2<Tuple2<String, Integer>, String, Integer>() {
                    @Override
                    public Promise<Tuple2<String, Integer>> apply(final String value1, final Integer value2)
                            throws Throwable {
                        return aValue(Tuple2.of(value1, value2));
                    }
                });
            }

            private AllControl.AllBuilder.AllBuilder2<String, Integer> partialAll() {
                return aAll(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return aValue("The answer");
                    }
                }).and(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return aLater(new ACallable<Integer>() {
                            @Override
                            public Promise<Integer> call() throws Throwable {
                                return aValue(42);
                            }
                        });
                    }
                });
            }
        });
        assertEquals(Tuple2.of("The answer", 42), rc);
    }


    @Test
    public void testAll3SelectZip() {
        final Tuple3<String, Integer, Boolean> rc = doAsync(new ACallable<Tuple3<String, Integer, Boolean>>() {
            @Override
            public Promise<Tuple3<String, Integer, Boolean>> call() throws Throwable {
                return aAll(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return partialAll().selectValue1();
                    }
                }).and(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return partialAll().selectValue2();
                    }
                }).and(new ACallable<Boolean>() {
                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        return partialAll().selectValue3();
                    }
                }).unzip(new AFunction3<Tuple3<String, Integer, Boolean>, String, Integer, Boolean>() {
                    @Override
                    public Promise<Tuple3<String, Integer, Boolean>> apply(
                            final String value1, final Integer value2, final Boolean value3) throws Throwable {
                        return aValue(Tuple3.of(value1, value2, value3));
                    }
                });
            }

            private AllControl.AllBuilder.AllBuilder3<String, Integer, Boolean> partialAll() {
                return aAll(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return aValue("The answer");
                    }
                }).and(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return aLater(new ACallable<Integer>() {
                            @Override
                            public Promise<Integer> call() throws Throwable {
                                return aValue(42);
                            }
                        });
                    }
                }).and(new ACallable<Boolean>() {
                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        return aLater(CoreFunctionUtil.booleanCallable(false));
                    }
                });
            }
        });
        assertEquals(Tuple3.of("The answer", 42, false), rc);
    }


    @Test
    public void testAll3() {
        final Tuple3<String, Integer, Boolean> rc = doAsync(new ACallable<Tuple3<String, Integer, Boolean>>() {
            @Override
            public Promise<Tuple3<String, Integer, Boolean>> call() throws Throwable {
                return aAll(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return aValue("The answer");
                    }
                }).and(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return aLater(new ACallable<Integer>() {
                            @Override
                            public Promise<Integer> call() throws Throwable {
                                return aValue(42);
                            }
                        });
                    }
                }).andLast(new ACallable<Boolean>() {
                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        return aValue(true);
                    }
                });
            }
        });
        assertEquals(Tuple3.of("The answer", 42, true), rc);
    }

    @Test
    public void testAllZip3() {
        final String rc = doAsync(new ACallable<String>() {
            @Override
            public Promise<String> call() throws Throwable {
                return aAll(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return aValue("The answer");
                    }
                }).and(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return aLater(new ACallable<Integer>() {
                            @Override
                            public Promise<Integer> call() throws Throwable {
                                return aValue(42);
                            }
                        });
                    }
                }).and(new ACallable<String>() {
                    @Override
                    public Promise<String> call() throws Throwable {
                        return aValue("is");
                    }
                }).unzip(new AFunction3<String, String, Integer, String>() {
                    @Override
                    public Promise<String> apply(final String noun, final Integer definition, final String verb) {
                        return aValue(noun + " " + verb + " " + definition);
                    }
                });
            }
        });
        assertEquals("The answer is 42", rc);
    }
}
