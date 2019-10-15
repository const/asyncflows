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
import org.asyncflows.core.context.ContextKey;
import org.asyncflows.core.context.spi.ActiveContextValue;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Context classloader entry.
 */
public class ContextClassLoaderEntry implements ActiveContextValue<ClassLoader> {
    /**
     * The context class loader entry.
     */
    public static final ContextKey<ContextClassLoaderEntry> KEY = ContextKey.get(ContextClassLoaderEntry.class);
    /**
     * The saved class loader.
     */
    private final ClassLoader classLoader;

    /**
     * The entry constructor.
     *
     * @param classLoader the classloader
     */
    private ContextClassLoaderEntry(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Utility method that setups the class loader. Intended for usage with {@link Context#transform(Function)}.
     *
     * @return a transform for context.
     */
    public static UnaryOperator<Context> withSavedContextClassloader() {
        return withContextClassloader(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Utility method that setups the context classloader. Intended for usage with {@link Context#transform(Function)}.
     *
     * @param newClassLoader the new class loader
     * @return a transform for context.
     */
    public static UnaryOperator<Context> withContextClassloader(ClassLoader newClassLoader) {
        return c -> c.with(KEY, new ContextClassLoaderEntry(newClassLoader));
    }

    @Override
    public ClassLoader value() {
        return classLoader;
    }

    @Override
    public Context.Cleanup setContextInTheCurrentThread() {
        final Thread thread = Thread.currentThread();
        final ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        return () -> Thread.currentThread().setContextClassLoader(previous);
    }
}
