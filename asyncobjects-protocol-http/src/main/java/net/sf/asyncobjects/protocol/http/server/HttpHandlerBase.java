package net.sf.asyncobjects.protocol.http.server;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aLater;

/**
 * The base class for HTTP handler.
 */
public abstract class HttpHandlerBase implements AHttpHandler, ExportsSelf<AHttpHandler> {
    @Override
    public final AHttpHandler export() {
        return export(Vat.current());
    }

    @Override
    public final AHttpHandler export(final Vat vat) {
        return exchange -> aLater(vat, () -> HttpHandlerBase.this.handle(exchange));
    }
}
