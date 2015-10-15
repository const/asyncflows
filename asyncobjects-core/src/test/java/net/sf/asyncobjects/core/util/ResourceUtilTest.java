package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.vats.Vat;
import org.junit.Test;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static org.junit.Assert.assertTrue;

/**
 * Resource util test.
 */
public class ResourceUtilTest {

    @Test
    public void tryTest() {
        final Cell<Boolean> r1 = new Cell<>(false);
        final Cell<Boolean> r2 = new Cell<>(false);
        final Cell<Boolean> r3 = new Cell<>(false);
        doAsync(() -> aTry(
                () -> aValue(new SampleResource(r1).export())
        ).andChain(
                value -> aValue(new SampleResource(r2).export())
        ).andChainSecond(
                value -> aValue(new SampleResource(r3).export())
        ).run((value1, value2, value3) -> aVoid()));
        assertTrue(r1.getValue());
        assertTrue(r2.getValue());
        assertTrue(r3.getValue());
    }

    /**
     * The sample resource.
     */
    public static class SampleResource implements ACloseable, ExportsSelf<ACloseable> {
        /**
         * The closed cell.
         */
        private final Cell<Boolean> closed;

        /**
         * The constructor
         *
         * @param closed the cell that will set close parameter.
         */
        public SampleResource(final Cell<Boolean> closed) {
            this.closed = closed;
        }

        @Override
        public Promise<Void> close() {
            closed.setValue(true);
            return aVoid();
        }

        @Override
        public ACloseable export() {
            return export(Vat.current());
        }

        @Override
        public ACloseable export(final Vat vat) {
            return () -> ResourceUtil.closeResource(vat, SampleResource.this);
        }
    }
}
