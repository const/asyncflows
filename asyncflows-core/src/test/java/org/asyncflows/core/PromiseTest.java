/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
