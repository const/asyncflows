package org.asyncflows.core.function;

import org.asyncflows.core.Outcome;

public interface AResolver<T> {
    void resolve(Outcome<T> outcome);
}
