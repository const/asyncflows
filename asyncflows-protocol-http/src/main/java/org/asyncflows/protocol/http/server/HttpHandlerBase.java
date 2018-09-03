package org.asyncflows.protocol.http.server;

import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.util.NeedsExport;

import static org.asyncflows.core.AsyncControl.aLater;

/**
 * The base class for HTTP handler.
 */
public abstract class HttpHandlerBase implements AHttpHandler, NeedsExport<AHttpHandler> {

    @Override
    public final AHttpHandler export(final Vat vat) {
        return exchange -> aLater(() -> HttpHandlerBase.this.handle(exchange), vat);
    }
}
