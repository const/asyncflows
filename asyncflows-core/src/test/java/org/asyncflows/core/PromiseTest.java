package org.asyncflows.core;


import org.asyncflows.core.data.Cell;
import org.asyncflows.core.vats.SingleThreadVat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test for promises
 */
public class PromiseTest {
    @Test
    public void test() {
        final Cell<Outcome<String>> cell = new Cell<Outcome<String>>(); // holder for the value
        final SingleThreadVat vat = new SingleThreadVat(null);
        vat.execute(() -> {
            try {
                final Promise<String> rc = new Promise<>(); // the empty promise
                rc.resolver().resolve(Outcome.success("test")); // get resolver send resolve event to promise
                rc.listenSync(o -> { // listen for promise (Sync listener variant)
                    cell.setValue(o); // save value
                    vat.stop(null); // stop vat
                });
            } catch (Throwable t) {
                cell.setValue(Outcome.<String>failure(t)); // save failure
                vat.stop(null); // stop vat
            }
        });
        vat.runInCurrentThread(); // run vat and wait until finish
        assertEquals(Outcome.success("test"), cell.getValue()); // check outcome
    }
}
