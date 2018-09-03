package org.asyncflows.io.net.blocking;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.net.ADatagramSocket;
import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.net.SocketExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;

import java.net.SocketException;

import static org.asyncflows.core.AsyncControl.aFailure;
import static org.asyncflows.core.AsyncControl.aValue;

/**
 * Blocking socket factory.
 */
public class BlockingSocketFactory implements ASocketFactory, NeedsExport<ASocketFactory> {
    @Override
    public Promise<ASocket> makeSocket() {
        return aValue(new BlockingSocket().export());
    }

    @Override
    public Promise<AServerSocket> makeServerSocket() {
        try {
            return aValue(new BlockingServerSocket().export());
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<ADatagramSocket> makeDatagramSocket() {
        try {
            return aValue(new BlockingDatagramSocket().export());
        } catch (SocketException e) {
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
