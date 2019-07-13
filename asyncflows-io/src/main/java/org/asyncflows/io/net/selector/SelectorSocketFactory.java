/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.io.net.selector;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.net.ADatagramSocket;
import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.net.ASocketFactoryProxyFactory;

import java.io.IOException;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aValue;

/**
 * Selector based socket factory.
 */
public class SelectorSocketFactory implements ASocketFactory, NeedsExport<ASocketFactory> {
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
        return ASocketFactoryProxyFactory.createProxy(vat, this);
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
