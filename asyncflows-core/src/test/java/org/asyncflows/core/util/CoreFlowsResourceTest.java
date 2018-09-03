package org.asyncflows.core.util;


import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Cell;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.ACloseable;
import org.junit.jupiter.api.Test;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resource util test.
 */
public class CoreFlowsResourceTest {

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
    public static class SampleResource implements ACloseable, NeedsExport<ACloseable> {
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
        public ACloseable export(final Vat vat) {
            return () -> CoreFlowsResource.closeResource(vat, SampleResource.this);
        }
    }
}
