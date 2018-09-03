package org.asyncflows.core.function;

import org.asyncflows.core.Promise;

public interface AFunction<A, R> {
    Promise<R> apply(A argument) throws Throwable;
}
