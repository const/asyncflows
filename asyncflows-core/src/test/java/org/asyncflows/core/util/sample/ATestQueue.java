package org.asyncflows.core.util.sample;

import org.asyncflows.core.Promise;

public interface ATestQueue<T> {
    Promise<T> take();
    void put(T element); // bad practice
}