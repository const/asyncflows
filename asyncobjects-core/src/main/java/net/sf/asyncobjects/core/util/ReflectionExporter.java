package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.WeakHashMap;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aSend;

/**
 * Create a reflection based proxy for the class.
 */
public final class ReflectionExporter {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReflectionExporter.class);
    /**
     * The factories.
     */
    private static final WeakHashMap<Class, WeakReference<Factory>> FACTORIES
            = new WeakHashMap<Class, WeakReference<Factory>>();

    /**
     * The constructor for utility class.
     */
    private ReflectionExporter() {
        // do nothing
    }

    /**
     * Export object using reflection. It uses type argument of ExportsSelf interface to derive return type.
     * However the proxy implements all interfaces except for ExportsSelf or those that extend it.
     *
     * @param vat    the vat
     * @param object the object to export using reflection generated proxy
     * @param <T>    the exported object type. Note that proxy implements all interfaces for the object except
     *               for ExportSelf, not only T.
     * @return the exported object
     */
    public static <T> T export(final Vat vat, final ExportsSelf<T> object) {
        Factory factory;
        synchronized (FACTORIES) {
            final WeakReference<Factory> reference = FACTORIES.get(object.getClass());
            factory = reference != null ? reference.get() : null;
            if (factory == null) {
                factory = new Factory(object.getClass());
                FACTORIES.put(object.getClass(), new WeakReference<Factory>(factory));
            }
        }
        return factory.create(vat, object);
    }

    /**
     * Export object using reflection. It uses type argument of ExportsSelf interface to derive return type.
     * However the proxy implements all interfaces except for ExportsSelf or those that extend it.
     *
     * @param object the object to export using reflection generated proxy
     * @param <T>    the exported object type. Note that proxy implements all interfaces for the object except
     *               for ExportSelf, not only T.
     * @return the exported object
     */
    public static <T> T export(final ExportsSelf<T> object) {
        return export(Vat.current(), object);
    }

    /**
     * Factory for the proxies.
     */
    private static final class Factory {
        /**
         * The constructor for the proxy.
         */
        private final Constructor constructor;
        /**
         * The types (used for toString).
         */
        private final String types;

        /**
         * The constructor.
         *
         * @param implementation the implementation class
         */
        private Factory(final Class<?> implementation) {
            final Class<?>[] interfaces = implementation.getInterfaces();
            final ArrayList<Class<?>> filtered = new ArrayList<Class<?>>();
            final StringBuilder nameList = new StringBuilder();
            nameList.append('[');
            for (final Class<?> type : interfaces) {
                if (!ExportsSelf.class.isAssignableFrom(type)) {
                    filtered.add(type);
                    if (nameList.length() != 1) {
                        nameList.append('&');
                    }
                    nameList.append(type.getSimpleName());
                }
            }
            nameList.append(']');
            types = nameList.toString();
            final Class<?> proxyClass = Proxy.getProxyClass(implementation.getClassLoader(),
                    filtered.toArray(new Class<?>[filtered.size()]));
            try {
                constructor = proxyClass.getConstructor(InvocationHandler.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No constructor in proxy class for: " + implementation.getName());
            }

        }

        /**
         * Create a proxy instance.
         *
         * @param vat    the vat
         * @param object the object to wrap
         * @param <T>    the return type
         * @return the created proxy
         */
        @SuppressWarnings("unchecked")
        public <T> T create(final Vat vat, final ExportsSelf<T> object) {
            try {
                return (T) constructor.newInstance(new Handler(this, vat, object));
            } catch (InstantiationException e) {
                throw new IllegalStateException("Instance cannot be created for " + constructor, e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Instance cannot be created for " + constructor, e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Instance cannot be created for " + constructor, e);
            }
        }

        @Override
        public String toString() {
            return types;
        }
    }

    /**
     * The handler class.
     */
    private static final class Handler implements InvocationHandler {
        /**
         * There is no particular sense to keep an object except to prevent garbage collection of it.
         */
        private final Factory creator;
        /**
         * The vat for the object.
         */
        private final Vat vat;
        /**
         * The wrapped object.
         */
        private final Object object;

        /**
         * The constructor.
         *
         * @param creator the creator of the proxy
         * @param vat     the context vat
         * @param object  the object to use
         */
        private Handler(final Factory creator, final Vat vat, final Object object) {
            this.creator = creator;
            this.vat = vat;
            this.object = object;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final Class<?> returnType = method.getReturnType();
            if (void.class == returnType) {
                aSend(vat, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            method.invoke(object, args);
                        } catch (IllegalAccessException e) {
                            LOG.error("Failed one-way method: " + method, e);
                        } catch (InvocationTargetException e) {
                            LOG.error("Failed one-way method: " + method, e.getTargetException());
                        }
                    }
                });
                return null;
            } else if (Promise.class == returnType) {
                return aLater(vat, new ACallable<Object>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public Promise<Object> call() throws Throwable {
                        try {
                            return (Promise<Object>) method.invoke(object, args);
                        } catch (InvocationTargetException e) {
                            throw e.getTargetException();
                        }
                    }
                });
            }
            final String name = method.getName();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0) {
                if ("toString".equals(name)) {
                    return creator.toString() + "@" + System.identityHashCode(object);
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(object);
                }
            }
            if (parameterTypes.length == 1 && parameterTypes[0] == Object.class && "equals".equals(name)) {
                final Object arg = args[0];
                if (arg == null || !Proxy.isProxyClass(arg.getClass())) {
                    return false;
                }
                final InvocationHandler handler = Proxy.getInvocationHandler(arg);
                return handler instanceof Handler && ((Handler) handler).object == object;
            }
            throw new UnsupportedOperationException("The method " + method
                    + " is not supported for asynchronous proxy: " + proxy.toString());
        }
    }
}
