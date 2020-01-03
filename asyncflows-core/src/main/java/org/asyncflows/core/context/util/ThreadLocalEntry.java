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

package org.asyncflows.core.context.util;


import org.asyncflows.core.context.Context;
import org.asyncflows.core.context.spi.PrivateContextEntry;
import org.asyncflows.core.data.Subcription;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Thread local entry.
 *
 * @param <T> the value type
 */
public class ThreadLocalEntry<T> implements PrivateContextEntry {
    /**
     * The thread local value.
     */
    @SuppressWarnings("squid:S5164")
    private final ThreadLocal<T> threadLocal;
    /**
     * The value to set.
     */
    private final T value;


    /**
     * The constructor.
     *
     * @param threadLocal the thread local value
     * @param value       the value to set
     */
    private ThreadLocalEntry(ThreadLocal<T> threadLocal, T value) {
        this.threadLocal = checkedThreadLocal(threadLocal);
        this.value = value;
    }

    /**
     * Return thread local value checked for null.
     *
     * @param threadLocal the thread local value
     * @param <V>         the value type
     * @return the thread local value
     */
    private static <V> ThreadLocal<V> checkedThreadLocal(ThreadLocal<V> threadLocal) {
        return Objects.requireNonNull(threadLocal, "threadLocal");
    }

    /**
     * Save current value for thread local and propagate with operations. The method is intended to be used with
     * {@link Context#transform(Function)}.
     *
     * @param threadLocal the thread local value to save
     * @param <V>         the value type
     * @return the transformer for the context
     */
    public static <V> UnaryOperator<Context> withSavedThreadLocal(ThreadLocal<V> threadLocal) {
        return withThreadLocal(threadLocal, checkedThreadLocal(threadLocal).get());
    }

    /**
     * Set current value for thread local and propagate with operations. The method is intended to be used with
     * {@link Context#transform(Function)}.
     *
     * @param threadLocal the thread local value to save
     * @param valueToSet  the value to set to thread local when context activates
     * @param <V>         the value type
     * @return the transformer for the context
     */
    public static <V> UnaryOperator<Context> withThreadLocal(ThreadLocal<V> threadLocal, V valueToSet) {
        return c -> c.withPrivate(new ThreadLocalEntry<>(threadLocal, valueToSet));
    }

    @Override
    public Object identity() {
        return threadLocal;
    }

    @Override
    public Subcription setContextInTheCurrentThread() {
        final T previous = threadLocal.get();
        if (previous == value) {
            return null;
        }
        // value is not cleared in the case of null, because after action is complete, it has to be restored anyway
        threadLocal.set(value);
        return previous == null ? threadLocal::remove : () -> threadLocal.set(previous);
    }
}
