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

package org.asyncflows.core.context;

import org.asyncflows.core.annotations.Experimental;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Context key. The context key is compared by identity. The context keys are kept in the static
 * {@link ConcurrentHashMap} by name and they are never removed while class loader of {@link ContextKey}
 * is active. They is supposed to be used as constant in the classes, so avoid creating keys dynamically.
 *
 * @param <T> the key value type, this is just an helper to help understanding the expected value type
 */
@Experimental
@SuppressWarnings({"unused", "squid:S2326"})
public final class ContextKey<T> implements Serializable {
    /**
     * Key map.
     */
    private static final ConcurrentHashMap<String, ContextKey<?>> KEYS = new ConcurrentHashMap<>();
    /**
     * The key name.
     */
    private final String name;

    /**
     * The constructor from full name.
     *
     * @param name the full name
     */
    private ContextKey(String name) {
        this.name = name;
    }

    /**
     * Get key.
     *
     * @param context the context of the key, usually the class that keeps the key as constant
     * @param name    the name of the key
     * @param <T>     the key value type
     * @return the created key, the full name consists of the name of the context class and supplied name
     */
    public static <T> ContextKey<T> get(Class<?> context, String name) {
        final String keyName = requireNonNull(context, "context").getName()
                + "#" + requireNonNull(name, "name");
        return getByFullName(keyName);
    }

    /**
     * Get key.
     *
     * @param context the context of the key, usually the class that keeps the key as constant
     * @param <T>     the key value type
     * @return the created key, the full name consists of the name of the context class and supplied name
     */
    public static <T> ContextKey<T> get(Class<?> context) {
        return get(context, "default");
    }

    @SuppressWarnings("unchecked")
    private static <T> ContextKey<T> getByFullName(String keyName) {
        return (ContextKey<T>) KEYS.computeIfAbsent(keyName, ContextKey::new);
    }

    private Object readResolve() {
        return getByFullName(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
