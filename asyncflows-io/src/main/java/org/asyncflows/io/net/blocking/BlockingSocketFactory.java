/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

package org.asyncflows.io.net.blocking;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.net.ADatagramSocket;
import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.io.net.ASocketFactoryProxyFactory;

import java.net.SocketException;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aValue;

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
        return ASocketFactoryProxyFactory.createProxy(vat, this);
    }
}
