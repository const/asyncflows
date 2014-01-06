package net.sf.asyncobjects.groovy.test

import net.sf.asyncobjects.core.data.Tuple2
import org.junit.Test

import static net.sf.asyncobjects.core.AsyncControl.*
import static net.sf.asyncobjects.core.CoreFunctionUtil.constantCallable
import static net.sf.asyncobjects.core.stream.Streams.aForRange
import static net.sf.asyncobjects.core.util.AllControl.aAll
import static org.junit.Assert.assertEquals

/**
 * The test for the streams.
 */
class StreamTest {
    @Test
    void test() {
        final Tuple2<List<Integer>, List<Integer>> result = doAsync {
            aAll {
                aForRange(0, 5).map {
                    aLater(constantCallable(it + 5))
                }.pull().window(3).filter {
                    aValue((it & 1) == 0)
                }.flatMapIterable {
                    def rc = new ArrayList<Integer>();
                    for (int i = 0; i < it; i++) {
                        rc.add(it as Integer)
                    }
                    aValue(rc);
                }.window(10).toList();
            }.andLast {
                aForRange(0, 5).push().window(10).map {
                    aLater(constantCallable(it + 5))
                }.filter {
                    aValue((it & 1) == 0);
                }.flatMapIterable {
                    def rc = new ArrayList<Integer>();
                    for (int i = 0; i < it; i++) {
                        rc.add(it as Integer)
                    }
                    aValue(rc);
                }.window(3).pull().push().toList()
            }
        }
        assertEquals(result.getValue1(), result.getValue2())
    }
}
