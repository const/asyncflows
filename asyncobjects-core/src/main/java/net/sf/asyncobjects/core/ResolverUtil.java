package net.sf.asyncobjects.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The resolver utility class.
 */
public final class ResolverUtil {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResolverUtil.class);

    /**
     * The private constructor for utility class.
     */
    private ResolverUtil() {
    }

    /**
     * Notify resolver about outcome.
     *
     * @param resolver a resolver
     * @param outcome  a outcome
     * @param <X>      the type of outcome
     */
    @SuppressWarnings("unchecked")
    public static <X> void notifyResolver(final AResolver<? super X> resolver, final Outcome<X> outcome) {
        try {
            ((AResolver) resolver).resolve((Outcome) outcome);
        } catch (Throwable throwable) {
            try {
                ((AResolver) resolver).resolve(new Failure(throwable));
            } catch (Throwable failed) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to notify resolver", failed);
                }
            }
        }
    }

    /**
     * Notify resolver about a success outcome.
     *
     * @param resolver a resolver
     * @param value    a outcome
     * @param <X>      the type of outcome
     */
    @SuppressWarnings("unchecked")
    public static <X> void notifySuccess(final AResolver<? super X> resolver, final X value) {
        notifyResolver(resolver, new Success<X>(value));
    }

    /**
     * Notify resolver about a failure outcome.
     *
     * @param resolver a resolver
     * @param failure  a outcome
     * @param <X>      the type of outcome
     */
    @SuppressWarnings("unchecked")
    public static <X> void notifyFailure(final AResolver<? super X> resolver, final Throwable failure) {
        try {
            ((AResolver) resolver).resolve(new Failure(failure));
        } catch (Throwable throwable) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to notify resolver", throwable);
            }
        }
    }
}
