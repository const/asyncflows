package net.sf.asyncobjects.core;

import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.vats.SingleThreadVat;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The test for promises
 */
public class PromiseTest {
    @Test
    public void test() {
        final Cell<Outcome<String>> cell = new Cell<Outcome<String>>();
        final SingleThreadVat vat = new SingleThreadVat(null);
        vat.execute(vat, () -> {
            try {
                final Promise<String> rc = new Promise<>();
                rc.resolver().resolve(Outcome.success("test"));
                rc.listen(resolution -> {
                    cell.setValue(resolution);
                    vat.stop(null);
                });
            } catch (Throwable t) {
                cell.setValue(Outcome.<String>failure(t));
                vat.stop(null);
            }
        });
        vat.runInCurrentThread();
        assertEquals(Outcome.success("test"), cell.getValue());
    }
}
