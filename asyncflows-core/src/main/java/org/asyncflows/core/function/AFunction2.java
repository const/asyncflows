package org.asyncflows.core.function;

import org.asyncflows.core.Promise;

public interface AFunction2<A, B, R> {
    Promise<R> apply(A a, B b) throws Throwable;
}
