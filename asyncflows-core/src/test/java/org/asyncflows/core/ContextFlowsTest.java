/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

import org.asyncflows.core.context.Context;
import org.asyncflows.core.vats.Vats;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.ContextFlows.inContext;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aNull;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.context.util.MdcContextKeyEntry.withMdcEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The test for context operations.
 */
public class ContextFlowsTest {
    @Test
    public void nowTest() {
        doAsync(() -> {
            assertNull(MDC.get("k"));
            return inContext(withMdcEntry("k", "v"), () -> {
                assertEquals("v", MDC.get("k"));
                return inContext(Context.empty(), () -> {
                    assertNull(MDC.get("k"));
                    return aVoid();
                });
            }).thenGet(() -> {
                assertNull(MDC.get("k"));
                return null;
            });
        });
    }

    @Test
    public void laterTest() {
        doAsync(() -> {
            assertNull(MDC.get("k"));
            return inContext(withMdcEntry("k", "v"), () -> aLater(() -> {
                assertEquals("v", MDC.get("k"));
                return aNull();
            })).thenGet(() -> {
                assertNull(MDC.get("k"));
                return null;
            });
        });
    }

    @Test
    public void otherVatTest() {
        doAsync(() -> {
            assertNull(MDC.get("k"));
            return inContext(withMdcEntry("k", "v"), () -> aLater(Vats.daemonVat(), () -> {
                assertEquals("v", MDC.get("k"));
                return aNull();
            })).thenGet(() -> {
                assertNull(MDC.get("k"));
                return null;
            });
        });
    }

    @Test
    public void asyncLaterTest() {
        doAsync(() -> {
            assertNull(MDC.get("k"));
            return inContext(c -> aValue(c.transform(withMdcEntry("k", "v")))).run(() -> aLater(() -> {
                assertEquals("v", MDC.get("k"));
                return inContext(() -> aValue(Context.empty())).run(() -> {
                    assertNull(MDC.get("k"));
                    return aVoid();
                }).thenGet(() -> {
                    assertEquals("v", MDC.get("k"));
                    return aVoid();
                });
            })).thenGet(() -> {
                assertNull(MDC.get("k"));
                return null;
            });
        });
    }
}
