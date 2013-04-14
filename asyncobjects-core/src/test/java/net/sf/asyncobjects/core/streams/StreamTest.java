package net.sf.asyncobjects.core.streams;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.stream.Streams;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.CoreFunctionUtil.constantCallable;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static org.junit.Assert.assertEquals;

/**
 * The test for the asynchronous stream classes.
 */
public class StreamTest {

    @Test
    public void test() {
        final Tuple2<List<Integer>, List<Integer>> result = doAsync(new ACallable<Tuple2<List<Integer>, List<Integer>>>() {
            @Override
            public Promise<Tuple2<List<Integer>, List<Integer>>> call() throws Throwable {
                return aAll(new ACallable<List<Integer>>() {
                    @Override
                    public Promise<List<Integer>> call() throws Throwable {
                        return Streams.aForRange(0, 5).map(new AFunction<Integer, Integer>() {
                            @Override
                            public Promise<Integer> apply(final Integer value) throws Throwable {
                                return aLater(constantCallable(value + 5));
                            }
                        }).pull().window(3).filter(new AFunction<Boolean, Integer>() {
                            @Override
                            public Promise<Boolean> apply(final Integer value) throws Throwable {
                                return aSuccess((value & 1) == 0);
                            }
                        }).flatMapIterable(new AFunction<List<Integer>, Integer>() {
                            @Override
                            public Promise<List<Integer>> apply(final Integer value) throws Throwable {
                                final List<Integer> rc = new ArrayList<Integer>();
                                for (int i = 0; i < value; i++) {
                                    rc.add(value);
                                }
                                return aSuccess(rc);
                            }
                        }).window(10).toList();
                    }
                }).andLast(new ACallable<List<Integer>>() {
                    @Override
                    public Promise<List<Integer>> call() throws Throwable {
                        return Streams.aForRange(0, 5).push().window(10).map(new AFunction<Integer, Integer>() {
                            @Override
                            public Promise<Integer> apply(final Integer value) throws Throwable {
                                return aLater(constantCallable(value + 5));
                            }
                        }).filter(new AFunction<Boolean, Integer>() {
                            @Override
                            public Promise<Boolean> apply(final Integer value) throws Throwable {
                                return aSuccess((value & 1) == 0);
                            }
                        }).flatMapIterable(new AFunction<List<Integer>, Integer>() {
                            @Override
                            public Promise<List<Integer>> apply(final Integer value) throws Throwable {
                                final List<Integer> rc = new ArrayList<Integer>();
                                for (int i = 0; i < value; i++) {
                                    rc.add(value);
                                }
                                return aSuccess(rc);
                            }
                        }).window(3).pull().push().toList();
                    }
                });
            }
        });
        assertEquals(result.getValue1(), result.getValue2());
    }

}
