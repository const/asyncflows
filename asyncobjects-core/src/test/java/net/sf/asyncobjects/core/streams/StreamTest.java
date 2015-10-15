package net.sf.asyncobjects.core.streams;

import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.stream.Streams;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
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
        final Tuple2<List<Integer>, List<Integer>> result = doAsync(() -> aAll(() ->
                Streams.aForRange(0, 5)
                        .map(value -> aLater(constantCallable(value + 5)))
                        .pull().window(3)
                        .filter(value -> aValue((value & 1) == 0))
                        .flatMapIterable(value -> {
                            final List<Integer> rc = new ArrayList<Integer>();
                            for (int i = 0; i < value; i++) {
                                rc.add(value);
                            }
                            return aValue(rc);
                        }).window(10).toList())
                .andLast(() -> Streams.aForRange(0, 5).push().window(10)
                        .map(value -> aLater(constantCallable(value + 5)))
                        .filter(value -> aValue((value & 1) == 0))
                        .flatMapIterable(value -> {
                            final List<Integer> rc = new ArrayList<Integer>();
                            for (int i = 0; i < value; i++) {
                                rc.add(value);
                            }
                            return aValue(rc);
                        }).window(3).pull().push().toList()));
        assertEquals(result.getValue1(), result.getValue2());
    }

}
