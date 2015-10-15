package net.sf.asyncobjects.core;

import net.sf.asyncobjects.core.vats.Vat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aSend;

/**
 * Utilities for exporting the objects from core.
 */
public final class CoreExportUtil {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CoreExportUtil.class);

    /**
     * The private constructor for utility class.
     */
    private CoreExportUtil() {
        // do nothing
    }

    /**
     * Export an object if it implements {@link ExportsSelf} interface.
     *
     * @param value the value to to export
     * @param <T>   the value type
     * @return exported value
     */
    @SuppressWarnings("unchecked")
    public static <T> T exportIfNeeded(final T value) {
        if (value instanceof ExportsSelf) {
            // needs export
            return (T) ((ExportsSelf) value).export();
        } else {
            // safe to use
            return value;
        }
    }

    /**
     * Export resolver on the vat.
     *
     * @param resolver the resolver
     * @param <T>      the vat
     * @return the exported resolver
     */
    public static <T> AResolver<T> export(final AResolver<T> resolver) {
        return export(Vat.current(), resolver);
    }

    /**
     * Export resolver on the vat.
     *
     * @param vat      the vat to export to
     * @param resolver the resolver
     * @param <T>      the vat
     * @return the exported resolver
     */
    public static <T> AResolver<T> export(final Vat vat, final AResolver<T> resolver) {
        return resolution -> vat.execute(() -> {
            try {
                resolver.resolve(resolution);
            } catch (Throwable throwable) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Oneway action " + resolver + " thrown an error", throwable);
                }
            }
        });
    }

    /**
     * Export callable on the specified vat.
     *
     * @param vat      the vat
     * @param callable the callable
     * @param <T>      the return type
     * @return the exported callable
     */
    public static <T> ACallable<T> export(final Vat vat, final ACallable<T> callable) {
        return () -> aLater(vat, callable);
    }

    /**
     * Export callable on the current vat.
     *
     * @param callable the callable
     * @param <T>      the return type
     * @return the exported callable
     */
    public static <T> ACallable<T> export(final ACallable<T> callable) {
        return export(Vat.current(), callable);
    }


    /**
     * Export mapper on the specified vat.
     *
     * @param vat    the vat for the mapper
     * @param mapper the mapper
     * @param <I>    the input type
     * @param <O>    the output type
     * @return the mapper
     */
    public static <I, O> AFunction<O, I> export(final Vat vat, final AFunction<O, I> mapper) {
        return value -> aLater(vat, () -> mapper.apply(value));
    }

    /**
     * Export runnable on other vat. Runnable is an interface provided by JDK that happens to follow rules
     * of asynchronous interfaces.
     *
     * @param vat    the vat
     * @param action the action
     * @return exported runnable
     */
    public static Runnable export(final Vat vat, final Runnable action) {
        return () -> aSend(vat, action);
    }

    /**
     * Export runnable on the current vat.
     *
     * @param action the action to export
     * @return the exported action
     */
    public static Runnable export(final Runnable action) {
        return export(Vat.current(), action);
    }
}
