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
import org.asyncflows.core.context.ContextKey;
import org.asyncflows.core.context.spi.PrivateContextEntry;
import org.asyncflows.core.data.Subcription;
import org.slf4j.MDC;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Differently from {@link MdcContextEntry}, this context only propagates single key in the context map.
 * This is similar to behavior of {@link MDC#putCloseable(String, String)} except the previous value is restored,
 * rather than value is set to null. This is more efficient operation as it does not involve copying the values.
 */
public class MdcContextKeyEntry implements PrivateContextEntry {
    /**
     * The entry key.
     */
    private final ContextKey<MdcContextKeyEntry> entryKey;
    /**
     * The key name.
     */
    private final String key;
    /**
     * The value.
     */
    private final String value;

    /**
     * The private constructor.
     *
     * @param key   the key
     * @param value the value
     */
    private MdcContextKeyEntry(String key, String value) {
        this.key = Objects.requireNonNull(key, "key");
        this.entryKey = ContextKey.get(MdcContextKeyEntry.class, "entry:" + key);
        this.value = value;
    }

    /**
     * Execute with the specified MDC entry.
     *
     * @param key   the key
     * @param value the value
     * @return the transform
     */
    public static UnaryOperator<Context> withMdcEntry(String key, String value) {
        return c -> c.withPrivate(new MdcContextKeyEntry(key, value));
    }

    /**
     * Execute with saved MDC entry. The value used is at the moment when this method is executed.
     *
     * @param key the key
     * @return the transform
     */
    public static UnaryOperator<Context> withSavedMdcEntry(String key) {
        return withMdcEntry(key, MDC.get(key));
    }

    @Override
    public Object identity() {
        return entryKey;
    }

    @Override
    public Subcription setContextInTheCurrentThread() {
        final String previous = MDC.get(key);
        if (Objects.equals(value, previous)) {
            return null;
        }
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
        return previous == null ? () -> MDC.remove(key) : () -> MDC.put(key, previous);
    }
}
