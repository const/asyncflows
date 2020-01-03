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

import org.asyncflows.core.context.util.ContextClassLoaderEntry;
import org.asyncflows.core.context.util.MdcContextEntry;
import org.asyncflows.core.context.util.MdcContextKeyEntry;
import org.asyncflows.core.context.util.ThreadLocalEntry;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.asyncflows.core.context.util.ContextClassLoaderEntry.withContextClassloader;
import static org.asyncflows.core.context.util.ContextClassLoaderEntry.withSavedContextClassloader;
import static org.asyncflows.core.context.util.MdcContextEntry.withSavedMdcContext;
import static org.asyncflows.core.context.util.MdcContextKeyEntry.withMdcEntry;
import static org.asyncflows.core.context.util.ThreadLocalEntry.withSavedThreadLocal;
import static org.asyncflows.core.context.util.ThreadLocalEntry.withThreadLocal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ContextTest {
    private static final String MDC_KEY = "mdc.test.key";
    private static final ContextKey<String> TEST_KEY = ContextKey.get(ContextTest.class, "test");
    private static final ContextKey<String> TEST_KEY2 = ContextKey.get(ContextTest.class, "test2");
    private static final ThreadLocal<String> TEST_THREAD_LOCAL = new ThreadLocal<>();

    @Test
    public void testValue() {
        final Context test = Context.empty().with(TEST_KEY, "test");
        assertNull(Context.current().getOrNull(TEST_KEY));
        test.run(() -> {
            assertEquals("test", Context.current().getOrNull(TEST_KEY));
            test.without(TEST_KEY).run(() -> assertNull(Context.current().getOrNull(TEST_KEY)));
            assertEquals("test", Context.current().getOrNull(TEST_KEY));
        });
        assertNull(Context.current().getOrNull(TEST_KEY));
    }

    @Test
    public void testForEach() {
        Context.empty().forEach((k, v) -> fail("It should never be called"));
        final AtomicBoolean called = new AtomicBoolean(false);
        Context.empty().with(TEST_KEY, "test").forEach((k, v) -> {
            assertSame(TEST_KEY, k);
            assertEquals("test", v);
            called.set(true);
        });
        assertTrue(called.compareAndSet(true, false));
        Context.empty().transform(withSavedMdcContext()).forEach((k, v) -> {
            assertNull(k);
            assertTrue(v instanceof MdcContextEntry);
            called.set(true);
        });
        assertTrue(called.compareAndSet(true, false));
    }

    @Test
    public void iteratorTest() {
        assertEquals(0, Context.empty().size());
        assertEquals(0L, Context.empty().stream().count());
        final Context context = Context.empty()
                .with(TEST_KEY, "test")
                .transform(withMdcEntry(MDC_KEY, "test2"))
                .transform(withSavedMdcContext())
                .transform(withSavedContextClassloader())
                .transform(withSavedThreadLocal(TEST_THREAD_LOCAL));
        assertEquals("test", context.getOrDefault(TEST_KEY, "test2"));
        assertNull(context.getOrNull(TEST_KEY2));
        assertEquals("test2", context.getOrDefault(TEST_KEY2, "test2"));
        assertEquals(5, context.size());
        final List<Map.Entry<ContextKey, Object>> entries = context.stream().collect(Collectors.toList());
        assertEquals(TEST_KEY, entries.get(0).getKey());
        assertEquals("test", entries.get(0).getValue());
        assertNull(entries.get(1).getKey());
        assertTrue(entries.get(1).getValue() instanceof MdcContextKeyEntry);
        assertNull(entries.get(2).getKey());
        assertTrue(entries.get(2).getValue() instanceof MdcContextEntry);
        assertEquals(ContextClassLoaderEntry.KEY, entries.get(3).getKey());
        assertTrue(entries.get(3).getValue() instanceof ContextClassLoaderEntry);
        assertNull(entries.get(4).getKey());
        assertTrue(entries.get(4).getValue() instanceof ThreadLocalEntry);
    }


    @Test
    public void testActiveEntry() {
        assertNull(MDC.get(MDC_KEY));
        final Context test = Context.empty().transform(withMdcEntry(MDC_KEY, "test"));
        assertNull(MDC.get(MDC_KEY));
        assertSame(Context.empty(), Context.current());
        test.run(() -> {
            assertSame(test, Context.current());
            assertEquals("test", MDC.get(MDC_KEY));
        });
        assertSame(Context.empty(), Context.current());
        assertNull(MDC.get(MDC_KEY));
    }

    @Test
    public void testMap() {
        assertNull(MDC.get(MDC_KEY));
        assertSame(Context.empty(), Context.current());
        try (MDC.MDCCloseable c = MDC.putCloseable(MDC_KEY, "a")) {
            assertEquals("a", MDC.get(MDC_KEY));
            final Context test = Context.empty().transform(withSavedMdcContext());
            MDC.put(MDC_KEY, "b");
            assertEquals("b", MDC.get(MDC_KEY));
            test.run(() -> {
                assertSame(test, Context.current());
                assertEquals("a", MDC.get(MDC_KEY));
            });
            assertSame(Context.empty(), Context.current());
            assertEquals("b", MDC.get(MDC_KEY));
        }
        assertNull(MDC.get(MDC_KEY));
    }

    @Test
    public void testOverride() {
        final Context test1 = Context.empty().transform(withMdcEntry(MDC_KEY, "test1"));
        final Context test2 = test1.transform(withMdcEntry(MDC_KEY, "test2"));
        test1.run(() -> {
            assertSame(test1, Context.current());
            assertEquals("test1", MDC.get(MDC_KEY));
            test2.run(() -> {
                assertSame(test2, Context.current());
                assertEquals("test2", MDC.get(MDC_KEY));
            });
            assertSame(test1, Context.current());
            assertEquals("test1", MDC.get(MDC_KEY));
        });
        assertSame(Context.empty(), Context.current());
    }

    @Test
    public void testThreadLocal() {
        final ThreadLocal<String> t = new ThreadLocal<>();
        final Context test1 = Context.empty().transform(withThreadLocal(t, "test1"));
        assertNull(t.get());
        test1.run(() -> {
            assertSame(test1, Context.current());
            assertEquals("test1", t.get());
        });
        assertNull(t.get());
        assertSame(Context.empty(), Context.current());
    }

    @Test
    public void testThreadLocalValue() {
        final ThreadLocal<String> t = new ThreadLocal<>();
        t.set("p");
        final Context test1 = Context.empty().transform(withThreadLocal(t, "test1"));
        assertEquals("p", t.get());
        test1.run(() -> {
            assertSame(test1, Context.current());
            assertEquals("test1", t.get());
        });
        assertEquals("p", t.get());
        assertSame(Context.empty(), Context.current());
    }


    @Test
    public void testContextClassLoader() {
        final ClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());
        final Context test1 = Context.empty().transform(withContextClassloader(classLoader));
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        test1.run(() -> {
            assertSame(test1, Context.current());
            assertSame(classLoader, Context.current()
                    .getOrEmpty(ContextClassLoaderEntry.KEY).map(ContextClassLoaderEntry::value).orElse(null));
            assertSame(classLoader, Thread.currentThread().getContextClassLoader());
        });
        assertSame(contextClassLoader, Thread.currentThread().getContextClassLoader());
    }
}
