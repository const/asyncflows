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
import org.asyncflows.core.annotations.ThreadSafe;
import org.asyncflows.core.context.spi.ActiveContextEntry;
import org.asyncflows.core.context.spi.PrivateContextEntry;
import org.asyncflows.core.data.Subcription;
import org.asyncflows.core.data.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * <p>The operation context that is propagated along with asynchronous calls.
 * The object is introduced to support logging, transactions, cancellable, and other thread contexts.
 * It also needed for context propagation for Kotlin coroutines, Project Reactor, and other frameworks.
 * The implementation is inspired by Project Reactor context implementation.</p>
 *
 * <p>The class plays the similar role as ThreadLocal for synchronous programming.</p>
 *
 * <p>{@link Iterable} implementation returns private entries with null key. Entries are iterated
 * from outermost to innermost (order of context creations).</p>
 */
@Experimental
@ThreadSafe
@SuppressWarnings("squid:S1452")
public abstract class Context implements Iterable<Map.Entry<ContextKey<?>, Object>> {
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Context.class);
    /**
     * The current context.
     */
    private static final ThreadLocal<Tuple2<Context, Subcription>> CURRENT = new ThreadLocal<>();
    /**
     * Empty context.
     */
    @SuppressWarnings("squid:S2390")
    private static final Context EMPTY = new EmptyContext();

    /**
     * The empty context.
     *
     * @return the initial context.
     */
    public static Context empty() {
        return EMPTY;
    }

    /**
     * @return the current context.
     */
    public static Context current() {
        final Tuple2<Context, Subcription> current = CURRENT.get();
        return current == null ? EMPTY : current.getValue1();
    }

    /**
     * Replace the current context.
     *
     * @param context a new context or null
     * @return the replaced context or null
     */
    private static Context replaceContext(Context context) {
        final Tuple2<Context, Subcription> previous = CURRENT.get();
        if (previous != null) {
            if (previous.getValue1() == context) {
                return context;
            }
            if (previous.getValue2() != null) {
                try {
                    previous.getValue2().close();
                } catch (Throwable t) {
                    LOGGER.error("The context clean up should not fail!", t);
                }
            }
        } else if (context == null || context == EMPTY) {
            // if previous context is null and new context is also empty or null, just do nothing
            return null;
        }
        if (context == null) {
            CURRENT.remove();
        } else {
            Subcription cleanup = null;
            try {
                cleanup = context.activateContext();
            } catch (Throwable t) {
                LOGGER.error("The context activation up should not fail!", t);
            }
            CURRENT.set(Tuple2.of(context, cleanup));
        }
        return previous == null ? null : previous.getValue1();
    }

    /**
     * @return the stream of entries
     */
    public Stream<Map.Entry<ContextKey<?>, Object>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Iterate context entries.
     *
     * @param action the action
     */
    public abstract void forEach(BiConsumer<ContextKey<?>, Object> action);

    /**
     * @return true if context is empty
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Transform context.
     *
     * @param function the function
     * @param <R>      the result
     * @return transformed
     */
    public <R> R transform(Function<Context, R> function) {
        return Objects.requireNonNull(function, "function").apply(this);
    }

    /**
     * Get the value or return null if the value is missing in the context. Private entries could not be got
     * with this method even if they are using a {@link ContextKey} as {@link PrivateContextEntry#identity()}.
     *
     * @param key the key
     * @param <T> the key type
     * @return the value or null
     */
    public abstract <T> T getOrNull(ContextKey<T> key);

    /**
     * Get the value or return {@code defaultValue} if the value is missing in the context
     *
     * @param key          the key
     * @param defaultValue the default value
     * @param <T>          the value type
     * @return the value
     */
    public <T> T getOrDefault(ContextKey<T> key, T defaultValue) {
        final T t = getOrNull(key);
        return t != null ? t : defaultValue;
    }

    /**
     * Get the value as optional.
     *
     * @param key the key
     * @param <T> the value type
     * @return the optional value
     */
    public <T> Optional<T> getOrEmpty(ContextKey<T> key) {
        return Optional.ofNullable(getOrNull(key));
    }

    /**
     * With additional context value.
     *
     * @param key   the key
     * @param value the value
     * @param <T>   the value type
     * @return a new context
     */
    public <T> Context with(ContextKey<T> key, T value) {
        if (value instanceof PrivateContextEntry) {
            throw new IllegalArgumentException("Private context entries must be added using withPrivate(...): "
                    + value.getClass().getName());
        }
        return withIdentity(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
    }

    /**
     * Remove entry from context. If there is no such entry, the operation is no-op and returns the same context.
     * If the result of operation is an empty context, the operation must return {@link #empty()}.
     *
     * @param key the key
     * @return a new context w/o specified entry.
     */
    public abstract Context without(ContextKey<?> key);

    /**
     * With private context value.
     *
     * @param privateContextEntry the context entry
     * @return a new context
     */
    public Context withPrivate(PrivateContextEntry privateContextEntry) {
        Objects.requireNonNull(privateContextEntry, "privateContextEntry");
        return withIdentity(Objects.requireNonNull(privateContextEntry.identity(), "identity"), privateContextEntry);
    }

    /**
     * Add entry with the specified identity.
     *
     * @param key   the identity
     * @param value the value
     * @return new context instance
     */
    protected abstract Context withIdentity(Object key, Object value);

    /**
     * Set context in the current thread. The returned {@link Subcription} action must be executed on the same thread
     * to restore context back. The operation is supposed to be used in try/finally block like the following:
     * <pre>{@code
     *  try (final Context.Cleanup ignored = context.setContext()) {
     *      actionInContext.run()
     *  }
     * }</pre>
     *
     * @return action that restores previous context, the returned value is never null.
     * @see #run(Runnable)
     */
    public final Subcription setContext() {
        final Context previous = replaceContext(this);
        return () -> replaceContext(previous);
    }

    /**
     * Activate context in the current thread.
     *
     * @return the runnable that undo activation
     */
    protected abstract Subcription activateContext();

    /**
     * Run action within context.
     *
     * @param action the action
     */
    public void run(Runnable action) {
        Objects.requireNonNull(action, "action");
        try (Subcription ignored = setContext()) {
            action.run();
        }
    }

    /**
     * @return the context size
     */
    public abstract int size();

    /**
     * Empty context that does not contain any entries.
     */
    private static final class EmptyContext extends Context {

        @Override
        public <T> T getOrNull(ContextKey<T> key) {
            return null;
        }

        @Override
        public Context without(ContextKey<?> key) {
            return this;
        }

        @Override
        protected Context withIdentity(Object key, Object value) {
            return ArrayContext.of(key, value);
        }

        @Override
        protected Subcription activateContext() {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void forEach(BiConsumer<ContextKey<?>, Object> action) {
            // do nothing
        }

        /**
         * Returns an iterator over elements of type {@code T}.
         *
         * @return an Iterator.
         */
        @Override
        public Iterator<Map.Entry<ContextKey<?>, Object>> iterator() {
            return new Iterator<Map.Entry<ContextKey<?>, Object>>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Map.Entry<ContextKey<?>, Object> next() {
                    throw new NoSuchElementException();
                }
            };
        }
    }

    /**
     * The context backed by array. Possibly more optimized implementations based on map
     * and fields will be introduced later.
     */
    private static final class ArrayContext extends Context {
        /**
         * Data array that contains key at even positions, and values at odd positions.
         */
        private final Object[] data;
        /**
         * The amount of active entries. It is used preallocate array for cleanup operations.
         */
        private final int activeCount;

        /**
         * The constructor from field.
         *
         * @param data        the data array
         * @param activeCount the active element count
         */
        private ArrayContext(Object[] data, int activeCount) {
            this.data = data;
            this.activeCount = activeCount;
        }

        private static ArrayContext of(Object key, Object value) {
            return new ArrayContext(new Object[]{key, value,},
                    activeCount(value));
        }

        /**
         * Count value as active or not.
         *
         * @param value the value to count
         * @return 1 if value is active, 0 otherwise
         */
        private static int activeCount(Object value) {
            return value instanceof ActiveContextEntry ? 1 : 0;
        }

        @SuppressWarnings("squid:S3776")
        @Override
        protected Context withIdentity(Object key, Object value) {
            final int oldLength = data.length;
            int p = indexOf(key);
            if (p != -1) {
                final Object oldValue = data[p + 1];
                if (value == oldValue) {
                    return this;
                } else {
                    if (oldLength == 2) {
                        return of(key, value);
                    } else {
                        Object[] newData = new Object[oldLength];
                        System.arraycopy(data, 0, newData, 0, oldLength);
                        newData[p + 1] = value;
                        int newActiveCount = activeCount + activeCount(value) - activeCount(oldValue);
                        return new ArrayContext(newData, newActiveCount);
                    }
                }
            } else {
                Object[] newData = new Object[oldLength + 2];
                System.arraycopy(data, 0, newData, 0, oldLength);
                newData[oldLength] = key;
                newData[oldLength + 1] = value;
                int newActiveCount = activeCount + activeCount(value);
                return new ArrayContext(newData, newActiveCount);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getOrNull(ContextKey<T> key) {
            final int i = indexOf(key);
            final T t = i == -1 ? null : (T) data[i + 1];
            return t instanceof PrivateContextEntry ? null : t;
        }

        @Override
        public Context without(ContextKey<?> key) {
            final int p = indexOf(key);
            if (p == -1) {
                return this;
            }
            if (data.length == 2) {
                return EMPTY;
            }
            final int newSize = data.length - 2;
            final Object[] newData = new Object[newSize];
            if (p != 0) {
                System.arraycopy(data, 0, newData, 0, p);
            }
            if (p != newSize) {
                System.arraycopy(data, p + 2, newData, p, newSize - p);
            }
            final int newActiveCount = activeCount - activeCount(data[p + 1]);
            return new ArrayContext(newData, newActiveCount);
        }

        @SuppressWarnings("squid:S3776")
        @Override
        protected Subcription activateContext() {
            if (activeCount == 0) {
                return null;
            }
            Subcription[] deactivation = new Subcription[activeCount];
            int count = 0;
            for (int i = 1; i < data.length; i += 2) {
                Object entry = data[i];
                if (entry instanceof ActiveContextEntry) {
                    try {
                        final Subcription rollback = ((ActiveContextEntry) entry).setContextInTheCurrentThread();
                        if (rollback != null) {
                            deactivation[count++] = rollback;
                        }
                    } catch (Throwable ex) {
                        LOGGER.error("Activation of context entry failed: " + entry.getClass().getName(), ex);
                    }
                }
            }
            if (count == 0) {
                return null;
            }
            int last = count - 1;
            return () -> {
                for (int i = last; i >= 0; i--) {
                    try {
                        deactivation[i].close();
                    } catch (Throwable t) {
                        LOGGER.error("Deactivation of context entry failed: " + deactivation[i].getClass().getName(), t);
                    }
                }
            };
        }

        @Override
        public int size() {
            return data.length / 2;
        }

        /**
         * Get index of key in the data array
         *
         * @param key the key
         * @return the index of key in data array or -1
         */
        private int indexOf(Object key) {
            for (int i = 0; i < data.length; i += 2) {
                if (data[i] == key) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public Iterator<Map.Entry<ContextKey<?>, Object>> iterator() {
            return new Iterator<Map.Entry<ContextKey<?>, Object>>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < data.length - 1;
                }

                @Override
                public Map.Entry<ContextKey<?>, Object> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException("position = " + i);
                    }
                    Object value = data[i + 1];
                    ContextKey<?> key = value instanceof PrivateContextEntry ? null : (ContextKey<?>) data[i];
                    i += 2;
                    return new Map.Entry<ContextKey<?>, Object>() {
                        @Override
                        public ContextKey<?> getKey() {
                            return key;
                        }

                        @Override
                        public Object getValue() {
                            return value;
                        }

                        @Override
                        public Object setValue(Object value) {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        }

        @Override
        public void forEach(BiConsumer<ContextKey<?>, Object> action) {
            for (int i = 0; i < data.length - 1; i += 2) {
                Object key = data[i];
                Object value = data[i + 1];
                action.accept((ContextKey<?>) (value instanceof PrivateContextEntry ? null : key), value);
            }
        }
    }
}
