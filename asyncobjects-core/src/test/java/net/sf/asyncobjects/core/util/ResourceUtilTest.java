package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.vats.Vat;
import org.junit.Test;

import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
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
        final Cell<Boolean> r1 = new Cell<Boolean>(false);
        final Cell<Boolean> r2 = new Cell<Boolean>(false);
        final Cell<Boolean> r3 = new Cell<Boolean>(false);
        doAsync(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return aTry(new ACallable<ACloseable>() {
                    @Override
                    public Promise<ACloseable> call() throws Throwable {
                        return aSuccess(new SampleResource(r1).export());
                    }
                }).andChain(new AFunction<ACloseable, ACloseable>() {
                    @Override
                    public Promise<ACloseable> apply(final ACloseable value) throws Throwable {
                        return aSuccess(new SampleResource(r2).export());
                    }
                }).andChainSecond(new AFunction<ACloseable, ACloseable>() {
                    @Override
                    public Promise<ACloseable> apply(final ACloseable value) throws Throwable {
                        return aSuccess(new SampleResource(r3).export());
                    }
                }).run(new AFunction3<Void, ACloseable, ACloseable, ACloseable>() {
                    @Override
                    public Promise<Void> apply(final ACloseable value1, final ACloseable value2,
                                               final ACloseable value3) throws Throwable {
                        return aVoid();
                    }
                });
            }
        });
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
            return new ACloseable() {
                @Override
                public Promise<Void> close() {
                    return ResourceUtil.closeResource(vat, SampleResource.this);
                }
            };
        }
    }
}
