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

package org.asyncflows.netty;

import io.netty.channel.EventLoop;
import org.asyncflows.core.vats.Vat;

/**
 * This vat assumes single thread event loop and that relationship
 * between event loop and thread is stable. Netty does not tracks
 * relationship between executors and threads using thread local,
 * so some specific logic of associating it is required.
 */
public final class NettyVat extends Vat {
    /**
     * The event loop.
     */
    private final EventLoop eventLoop;

    /**
     * The constructor.
     *
     * @param eventLoop the event loop
     */
    private NettyVat(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    /**
     * Initialize netty vat in the current thread if needed.
     *
     * @param loop the loop
     * @return the initialized or current vat
     */
    public static NettyVat vat(EventLoop loop) {
        Vat vat = Vat.currentOrNull();
        if (vat == null) {
            if (!loop.inEventLoop(Thread.currentThread())) {
                throw new IllegalStateException("Wrong thread for event loop");
            }
            NettyVat newVat = new NettyVat(loop);
            newVat.enter();
            return newVat;
        } else if (vat instanceof NettyVat) {
            return (NettyVat) vat;
        } else {
            throw new IllegalStateException("Current thread already has vat of type " + vat.getClass().getName());
        }
    }

    public static NettyVat currentNettyVat() {
        Vat vat = Vat.currentOrNull();
        if (vat instanceof NettyVat) {
            return (NettyVat) vat;
        } else if (vat == null) {
            throw new IllegalStateException("No current vat context");
        } else {
            throw new IllegalStateException("Current thread already has vat of type " + vat.getClass().getName());
        }
    }

    @Override
    public void execute(Runnable action) {
        eventLoop.execute(action);
    }

    public EventLoop getEventLoop() {
        return eventLoop;
    }
}
