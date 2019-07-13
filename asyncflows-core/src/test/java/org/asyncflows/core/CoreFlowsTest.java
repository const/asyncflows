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

import org.junit.jupiter.api.Test;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class CoreFlowsTest {
    @Test
    public void testSimple() {
        final int rc = doAsync(() -> aValue(42));
        assertEquals(42, rc);
    }

    @Test
    public void testSimpleLater() {
        final int rc = doAsync(() -> aLater(() -> aValue(42)));
        assertEquals(42, rc);
    }

    @Test
    public void testThrowLater() {
        try {
            doAsync(() -> aLater(() -> {
                throw new IllegalStateException("Test");
            }));
            fail("the doAsync should throw an exception!");
        } catch (AsyncExecutionException ex) {
            assertEquals(IllegalStateException.class, ex.getCause().getClass());
            assertEquals("Test", ex.getCause().getMessage());
        }
    }

}
