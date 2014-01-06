package net.sf.asyncobjects.groovy.test

import net.sf.asyncobjects.core.data.Tuple2
import org.junit.Test

import static net.sf.asyncobjects.core.AsyncControl.*
import static net.sf.asyncobjects.core.util.AllControl.aAll
import static org.junit.Assert.assertEquals

/**
 * The control test.
 */
class AllControlTest {
    @Test
    void testAll2() {
        def rc = doAsync {
            aAll {
                aValue("The answer")
            } andLast {
                aLater {
                    aValue(42)
                }
            }
        }
        assertEquals(Tuple2.of("The answer", 42), rc)
    }
}
