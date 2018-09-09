package org.asyncflows.core.util.sample;

import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aSend;

public class TestQueueExporter {
    public static <T> ATestQueue<T> exportTestQueue(final ATestQueue<T> service, final Vat vat) {
        return new ATestQueue<T>() {
            @Override
            public Promise<T> take() {
                return aLater(vat, () -> service.take());
            }

            @Override
            public void put(T element) {
                aSend(vat, () -> put(element));
            }
        };
    }
}
