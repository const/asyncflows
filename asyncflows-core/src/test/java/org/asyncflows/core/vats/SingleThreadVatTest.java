package org.asyncflows.core.vats;

import org.asyncflows.core.data.Cell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The test for single thread vat
 */
public class SingleThreadVatTest {
    @Test
    public void testSimple() {
        final Cell<Vat> result = new Cell<>(); // create holder for value
        final SingleThreadVat vat = new SingleThreadVat(null); // create vat
        vat.execute(() -> { // schedule event
            result.setValue(Vat.current()); // save current vat value
            vat.stop(null); // stop vat execution
        });
        assertNull(result.getValue()); // check that it is not executed yet
        vat.runInCurrentThread(); // start vat and execute event
        // vat is stopped
        assertSame(vat, result.getValue()); // get vat value
    }
}
