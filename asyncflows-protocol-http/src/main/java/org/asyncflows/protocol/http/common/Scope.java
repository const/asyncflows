package org.asyncflows.protocol.http.common;

import org.asyncflows.core.annotations.Experimental;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * The scope object. It represent a map with keys and values. Note that scope is a thread-safe object,
 * so the generator should be used with care, since it blocks the entire scope.
 */
@Experimental
public final class Scope {
    /**
     * The actual attributes for the objects.
     */
    private final Map<Key<?>, Object> objects = new HashMap<>(); // NOPMD

    /**
     * Set the value.
     *
     * @param key   the key
     * @param value the value
     * @param <T>   the value type
     * @return the previous value
     */
    @SuppressWarnings("unchecked")
    public <T> T set(final Key<T> key, final T value) {
        synchronized (objects) {
            return (T) objects.put(key, value);
        }
    }

    /**
     * Get the value, if key has generator or default value, and value is missing, the value is set to the scope.
     *
     * @param key the key
     * @param <T> the value type
     * @return the value (or null)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final Key<T> key) {
        if (key.generator == null) {
            synchronized (objects) {
                return (T) objects.get(key);
            }
        } else {
            return get(key, key.generator);
        }
    }

    /**
     * Get the value, if key has generator or default value, and value is missing, the value is set to the scope.
     *
     * @param key the key
     * @param <T> the value type
     * @return the value (or null)
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCreate(final Key<T> key) {
        if (key.generator == null) {
            synchronized (objects) {
                return (T) objects.get(key);
            }
        } else {
            return getOrCreate(key, key.generator);
        }
    }

    /**
     * Get the value of generate it (the value is not set).
     *
     * @param key       the key
     * @param generator the generator
     * @param <T>       the value type
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final Key<T> key, final Callable<? extends T> generator) {
        synchronized (objects) {
            final T value = (T) objects.get(key);
            if (value != null || objects.containsKey(key)) {
                return value;
            } else {
                try {
                    return generator.call();
                } catch (Exception e) { // NOPMD
                    throw new IllegalArgumentException("The generator has thrown an exception", e);
                }
            }
        }
    }


    /**
     * Get the value of generate it (the value is set).
     *
     * @param key       the key
     * @param generator the generator
     * @param <T>       the value type
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCreate(final Key<T> key, final Callable<? extends T> generator) {
        synchronized (objects) {
            final T value = (T) objects.get(key);
            if (value != null || objects.containsKey(key)) {
                return value;
            } else {
                try {
                    final T t = generator.call();
                    set(key, t);
                    return t;
                } catch (Exception e) { // NOPMD
                    throw new IllegalArgumentException("The generator has thrown an exception", e);
                }
            }
        }
    }


    /**
     * Get key with the specified default value.
     *
     * @param key          the key
     * @param defaultValue the default value
     * @param <T>          the result
     * @return the existing or created value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final Key<T> key, final T defaultValue) {
        synchronized (objects) {
            final T value = (T) objects.get(key);
            if (value != null || objects.containsKey(key)) {
                return value;
            } else {
                return defaultValue;
            }
        }
    }

    /**
     * Get key with the specified default value.
     *
     * @param key          the key
     * @param defaultValue the default value
     * @param <T>          the result
     * @return the existing or created value
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCreate(final Key<T> key, final T defaultValue) {
        synchronized (objects) {
            final T value = (T) objects.get(key);
            if (value != null || objects.containsKey(key)) {
                return value;
            } else {
                set(key, defaultValue);
                return defaultValue;
            }
        }
    }

    /**
     * Get value if present and remove it.
     *
     * @param key the key
     * @param <T> the type
     * @return the removed value
     */
    @SuppressWarnings("unchecked")
    public <T> T remove(final Key<?> key) {
        synchronized (objects) {
            return (T) objects.remove(key);
        }
    }

    /**
     * The key type.
     *
     * @param <T> the type of the key value in the scope
     */
    public static final class Key<T> {
        /**
         * The name of the key.
         */
        private final String name;
        /**
         * The hash code. It is a separate field because the key is used in hash maps.
         */
        private final int hashCode;
        /**
         * Generator (might be null).
         */
        private final Callable<T> generator;

        /**
         * The key constructor.
         *
         * @param context the context name
         * @param name    the name of the key
         */
        public Key(final Class<?> context, final String name) {
            this(context, name, (Callable<T>) null);
        }

        /**
         * The key constructor.
         *
         * @param context   the context name
         * @param name      the name of the key
         * @param generator the generator for values
         */
        public Key(final Class<?> context, final String name, final Callable<T> generator) {
            this.generator = generator;
            if (name == null) {
                throw new IllegalStateException("The name must not be null");
            }
            this.name = context.getName() + "#" + name;
            this.hashCode = name.hashCode();
        }

        /**
         * The key constructor.
         *
         * @param context      the context name
         * @param name         the name of the key
         * @param defaultValue the default value
         */
        public Key(final Class<?> context, final String name, final T defaultValue) {
            this(context, name, () -> defaultValue);
        }


        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Key key = (Key) o;

            if (hashCode != key.hashCode) {
                return false;
            }
            //noinspection RedundantIfStatement
            if (!name.equals(key.name)) { // NOPMD
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * @return the key name
         */
        public String name() {
            return name;
        }
    }
}
