package net.sf.asyncobjects.core;

/**
 * Resolver for the value.
 *
 * @param <T> the value type
 */
public interface AResolver<T> {
    /**
     * This method is notified when resolution for some asynchronous
     * operation is ready.
     *
     * @param resolution the resolution
     * @throws Throwable in case of any problem
     */
    void resolve(Outcome<T> resolution) throws Throwable;
}
