/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

package org.asyncflows.io.net.async;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.net.ADatagramSocket;
import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.net.ASocketFactoryProxyFactory;

import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aValue;

public class AsyncSocketFactory implements ASocketFactory, ExportableComponent<ASocketFactory> {
    private AsynchronousChannelGroup channelGroup;

    public void setChannelGroup(AsynchronousChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
    }

    @Override
    public ASocketFactory export(Vat vat) {
        return ASocketFactoryProxyFactory.createProxy(vat, this);
    }

    @Override
    public Promise<ASocket> makeSocket() {
        return aNow(() -> aValue(new AsyncSocket(AsynchronousSocketChannel.open(channelGroup)).export()));
    }

    @Override
    public Promise<AServerSocket> makeServerSocket() {
        return aNow(() -> aValue(new AsyncServerSocket(AsynchronousServerSocketChannel.open(channelGroup)).export()));
    }

    @Override
    public Promise<ADatagramSocket> makeDatagramSocket() {
        throw new UnsupportedOperationException("Datagram sockets are not supported for async channels");
    }
}
