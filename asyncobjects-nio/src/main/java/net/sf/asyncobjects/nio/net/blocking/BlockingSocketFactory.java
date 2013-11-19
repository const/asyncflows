package net.sf.asyncobjects.nio.net.blocking;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.core.vats.Vats;
import net.sf.asyncobjects.nio.net.AServerSocket;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.net.SocketExportUtil;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aSuccess;

/**
 * Blocking socket factory.
 */
public class BlockingSocketFactory implements ASocketFactory, ExportsSelf<ASocketFactory> {
    @Override
    public Promise<ASocket> makeSocket() {
        return aSuccess(new BlockingSocket().export());
    }

    @Override
    public Promise<AServerSocket> makeServerSocket() {
        try {
            return aSuccess(new BlockingServerSocket().export());
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public ASocketFactory export() {
        return export(Vats.daemonVat());
    }

    @Override
    public ASocketFactory export(final Vat vat) {
        return SocketExportUtil.export(vat, this);
    }
}
