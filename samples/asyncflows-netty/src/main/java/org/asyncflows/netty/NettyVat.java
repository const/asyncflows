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
