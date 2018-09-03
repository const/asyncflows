package org.asyncflows.core.function;

import org.asyncflows.core.Promise;

public interface AFunction4<A, B, C, D, R> {
    Promise<R> apply(A a, B b, C c, D d) throws Throwable;
}
