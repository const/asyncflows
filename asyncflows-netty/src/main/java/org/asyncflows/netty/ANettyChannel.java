package org.asyncflows.netty;

import org.asyncflows.core.Promise;
import org.asyncflows.core.streams.ASink;
import org.asyncflows.core.streams.AStream;

public class ANettyChannel<I, O> {
    Promise<AStream<I>> getInput();
    Promise<ASink<O>> getOutput();
}
