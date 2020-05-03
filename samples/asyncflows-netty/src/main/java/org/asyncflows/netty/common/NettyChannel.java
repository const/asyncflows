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

package org.asyncflows.netty.common;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.netty.ANettyChannel;
import org.asyncflows.netty.ANettyChannelProxyFactory;
import org.asyncflows.netty.NettyVat;

import java.util.LinkedList;

import static org.asyncflows.core.CoreFlows.aMaybeValue;

public class NettyChannel<I, O> extends CloseableInvalidatingBase implements ANettyChannel<I, O>, ExportableComponent<ANettyChannel<I, O>> {
    private final NettyVat vat;
    private final ChannelHandlerContext context;
    private final LinkedList<I> input = new LinkedList<>();
    private final RequestQueue reads = new RequestQueue();
    private final RequestQueue writes = new RequestQueue();
    private boolean eofReached;

    public NettyChannel(NettyVat vat, ChannelHandlerContext context) {
        this.vat = vat;
        this.context = context;
    }

    public ChannelInboundHandler inboundHandler() {
        return new ChannelInboundHandlerAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                input.addLast((I) msg);
                super.channelRead(ctx, msg);
            }

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                eofReached = true;
                super.channelReadComplete(ctx);
            }

            @Override
            public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
                // TODO
                super.channelWritabilityChanged(ctx);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                invalidate(cause);
                reads.resume();
                writes.resume();
                super.exceptionCaught(ctx, cause);
            }
        };
    }

    @Override
    public ANettyChannel<I, O> export() {
        return ANettyChannelProxyFactory.createProxy(vat, this);
    }

    @Override
    public ANettyChannel<I, O> export(Vat vat) {
        return export();
    }

    @Override
    public Promise<Maybe<I>> read() {
        return reads.runSeqUntilValue(() -> {
            ensureValidAndOpen();
            if (input.isEmpty()) {
                if (eofReached) {
                    return aMaybeValue(Maybe.empty());
                } else {
                    context.read();
                    return reads.suspendThenEmpty();
                }
            } else {
                return aMaybeValue(Maybe.value(input.removeFirst()));
            }
        });
    }

    @Override
    public Promise<Void> write(O data) {
        return null;
    }


}
