package org.asyncflows.core.function;

import org.asyncflows.core.Promise;

public interface AFunction3<A, B, C, R> {
    Promise<R> apply(A a, B b, C c) throws Throwable;
}
