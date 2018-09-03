package org.asyncflows.core.function;

import org.asyncflows.core.Promise;

@FunctionalInterface
public interface ASupplier<T> {
    Promise<T> get() throws Throwable;
}
