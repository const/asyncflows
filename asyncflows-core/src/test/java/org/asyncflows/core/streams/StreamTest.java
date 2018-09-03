package org.asyncflows.core.streams;

import org.asyncflows.core.data.Tuple2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aBoolean;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.function.AsyncFunctionUtil.constantSupplier;
import static org.asyncflows.core.streams.AsyncStreams.aForRange;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test for the asynchronous stream classes.
 */
public class StreamTest {

    @Test
    public void seqForCollectStream() {
        final int rc = doAsync(() ->
                aForRange(0, 11)
                        .filter(i -> aBoolean(i % 2 == 0))
                        .map(i -> aValue(i / 2))
                        .collect(Collectors.summingInt(e -> e))
        );
        assertEquals(15, rc);
    }

    @Test
    public void seqForCollectStreamWindow() {
        final Function<StreamBuilder<Integer>, StreamBuilder<Integer>> delay =
                s -> s.map(a -> aForRange(0, 10).toVoid().thenValue(a));
        List<Integer> result = new ArrayList<>();
        final int rc = doAsync(() ->
                aForRange(0, 10)
                        .filter(i -> aBoolean(i % 2 == 0))
                        .mapSync(a -> {
                            result.add(a);
                            return a;
                        })
                        .window(1)
                        .process(delay)
                        .mapSync(a -> {
                            result.add(-a);
                            return a;
                        })
                        .map(i -> aValue(i / 2))
                        .collect(Collectors.summingInt(e -> e))
        );
        assertEquals(10, rc);
        assertEquals(Arrays.asList(0, 2, -0, 4, -2, 6, -4, 8, -6, -8), result);
    }


    @Test
    public void seqForCollectStreamSync() {
        final int rc = doAsync(() ->
                aForRange(0, 11)
                        .filterSync(i -> i % 2 == 0)
                        .mapSync(i -> i / 2)
                        .collect(Collectors.summingInt(e -> e))
        );
        assertEquals(15, rc);
    }


    @Test
    public void allForCollectStreamSyncAll() {
        final int rc = doAsync(() ->
                aForRange(0, 11)
                        .all(2)
                        .filterSync(i -> i % 2 == 0)
                        .mapSync(i -> i / 2)
                        .collect(Collectors.summingInt(e -> e))
        );
        assertEquals(15, rc);
    }


    @Test
    public void test() {
        final Tuple2<List<Integer>, List<Integer>> result = doAsync(() -> aAll(() ->
                aForRange(0, 6)
                        .map(value -> aLater(constantSupplier(value + 5)))
                        .pull().window(3)
                        .filter(value -> aValue((value & 1) == 0))
                        .flatMapIterable(value -> {
                            final List<Integer> rc = new ArrayList<Integer>();
                            for (int i = 0; i < value; i++) {
                                rc.add(value);
                            }
                            return aValue(rc);
                        }).window(10).toList())
                .andLast(() -> aForRange(0, 6).push().window(10)
                        .map(value -> aLater(constantSupplier(value + 5)))
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
