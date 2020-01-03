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

package org.asyncflows.core.context.util;

import org.asyncflows.core.context.Context;
import org.asyncflows.core.context.spi.PrivateContextEntry;
import org.asyncflows.core.data.Subcription;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The context entry that saves MDC context.
 */
public class MdcContextEntry implements PrivateContextEntry {
    /**
     * The context map to use.
     */
    private final Map<String, String> contextMap;

    /**
     * The MDC context map entries.
     *
     * @param contextMap the context map
     */
    private MdcContextEntry(Map<String, String> contextMap) {
        this.contextMap = contextMap;
    }

    /**
     * Use the specified map as MDC context. Intended for usage with {@link Context#transform(Function)}.
     *
     * @param contextMap the context map
     * @return the context transform
     */
    public static UnaryOperator<Context> withMdcContext(Map<String, String> contextMap) {
        return c -> c.withPrivate(new MdcContextEntry(contextMap));
    }

    /**
     * Use the current MDC as MDC context. Intended for usage with {@link Context#transform(Function)}.
     *
     * @return the context transform
     */
    public static UnaryOperator<Context> withSavedMdcContext() {
        return c -> c.withPrivate(new MdcContextEntry(MDC.getCopyOfContextMap()));
    }

    @Override
    public Object identity() {
        return MdcContextEntry.class;
    }

    @Override
    public Subcription setContextInTheCurrentThread() {
        final Map<String, String> previous = MDC.getCopyOfContextMap();
        if (contextMap == null || contextMap.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(contextMap);
        }
        return previous == null || previous.isEmpty() ? MDC::clear : () -> MDC.setContextMap(previous);
    }
}
