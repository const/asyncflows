package net.sf.asyncobjects.nio.net.selector;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.net.ADatagramSocket;
import net.sf.asyncobjects.nio.net.AServerSocket;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.net.SocketExportUtil;

import java.io.IOException;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aValue;

/**
 * Selector based socket factory.
 */
public class SelectorSocketFactory implements ASocketFactory, ExportsSelf<ASocketFactory> {
    /**
     * The selector vat to use.
     */
    private final SelectorVat selectorVat;

    /**
     * The constructor.
     *
     * @param selectorVat the selector vat
     */
    public SelectorSocketFactory(final SelectorVat selectorVat) {
        this.selectorVat = selectorVat;
    }

    /**
     * The socket factory.
     */
    public SelectorSocketFactory() {
        this(getCurrentSelectorVat());
    }

    /**
     * @return the current selector vat
     */
    private static SelectorVat getCurrentSelectorVat() {
        final Vat current = Vat.current();
        if (!(current instanceof SelectorVat)) {
            throw new IllegalStateException("SelectorVatFactory could be used only on SelectorVat: "
                    + (current == null ? null : current.getClass().getName()));
        }
        return (SelectorVat) current;
    }

    @Override
    public ASocketFactory export() {
        return export(Vat.current());
    }

    @Override
    public ASocketFactory export(final Vat vat) {
        return SocketExportUtil.export(vat, this);
    }

    @Override
    public Promise<ASocket> makeSocket() {
        final SelectorVat vat = getSelectorVat();
        try {
            return aValue(new SelectorSocket(vat.getSelector()).export(vat));
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    /**
     * @return the current selector vat
     */
    private SelectorVat getSelectorVat() {
        return selectorVat;
    }

    @Override
    public Promise<AServerSocket> makeServerSocket() {
        final SelectorVat vat = getSelectorVat();
        try {
            return aValue(new SelectorServerSocket(vat.getSelector()).export(vat));
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<ADatagramSocket> makeDatagramSocket() {
        final SelectorVat vat = getSelectorVat();
        try {
            return aValue(new SelectorDatagramSocket(vat.getSelector()).export(vat));
        } catch (IOException e) {
            return aFailure(e);
        }
    }
}
