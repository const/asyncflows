package net.sf.asyncobjects.core.vats;

import net.sf.asyncobjects.core.data.Cell;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * The test for single thread vat
 */
public class SingleThreadVatTest {
    @Test
    public void testSimple() {
        final Cell<Vat> result = new Cell<Vat>();
        final SingleThreadVat vat = new SingleThreadVat(null);
        vat.execute(vat, new Runnable() {
            @Override
            public void run() {
                result.setValue(Vat.current());
                vat.stop(null);
            }
        });
        vat.runInCurrentThread();
        assertSame(vat, result.getValue());
    }
}
