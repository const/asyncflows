package org.asyncflows.core.util;

import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;

import static org.asyncflows.core.AsyncControl.aLater;

/**
 * Special variant of closeable that represents usage of some resource w/o direct access to it.
 * Do not extend this interface. If you need to add some methods, just directly extend {@link ACloseable}.
 * This object should be returned when adding listeners to some objects.
 */
public interface ASubscription extends ACloseable {

    static ASubscription exportSubscription(Vat vat, ASubscription subscription) {
        return () -> aLater(ResourceUtil.closeResourceAction(subscription));
    }

    static ASubscription exportSubscription(ASubscription subscription) {
        return exportSubscription(Vats.defaultVat(), subscription);
    }
}
