package org.asyncflows.netty;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.streams.ASink;
import org.asyncflows.core.streams.AStream;

public interface ANettyChannel<I, O> extends ACloseable  {
    Promise<AStream<I>> getInput();
    Promise<ASink<O>> getOutput();
}
