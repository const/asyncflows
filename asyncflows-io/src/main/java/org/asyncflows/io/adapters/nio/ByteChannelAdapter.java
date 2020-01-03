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

package org.asyncflows.io.adapters.nio;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AInputProxyFactory;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.AOutputProxyFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.CompletionHandler;

import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.io.adapters.nio.AsyncNioFlows.aCompletionHandler;

/**
 * The adapter for asynchronous channel. By default it supports only duplex close, so calling close anywhere
 * (channel, input, or output) closes the entire channel. Subclasses might define half-duplex close for cases
 * like network sockets.
 */
public class ByteChannelAdapter<C extends AsynchronousByteChannel> extends CloseableBase implements AChannel<ByteBuffer> {
    /**
     * The wrapped channel.
     */
    protected final C channel;
    /**
     * The output.
     */
    private final AOutput<ByteBuffer> output;
    /**
     * The input.
     */
    private final AInput<ByteBuffer> input;

    /**
     * The constructor from channel.
     *
     * @param channel the channel
     */
    public ByteChannelAdapter(C channel) {
        this.channel = channel;
        this.input = createInput();
        this.output = createOutput();
    }

    @Override
    public Promise<AInput<ByteBuffer>> getInput() {
        return aValue(input);
    }

    @Override
    public Promise<AOutput<ByteBuffer>> getOutput() {
        return aValue(output);
    }

    /**
     * This method should e overridden if the channel needs to support half-close.
     *
     * @return when complete
     */
    protected Promise<Void> closeInput() {
        return close();
    }

    /**
     * This method should e overridden if the channel needs to support half-close.
     *
     * @return when complete
     */
    protected Promise<Void> closeOutput() {
        return close();
    }

    /**
     * This method should e overridden if the channel needs to support flush.
     *
     * @return when complete
     */
    protected Promise<Void> flushOutput() {
        return aVoid();
    }

    @Override
    protected Promise<Void> closeAction() {
        return AsyncNioFlows.closeChannel(channel);
    }

    /**
     * Create output.
     *
     * @return the output stream
     */
    protected AOutput<ByteBuffer> createOutput() {
        return new Output().export();
    }

    /**
     * Create input.
     *
     * @return the input stream
     */
    protected AInput<ByteBuffer> createInput() {
        return new ByteChannelInputAdapter().export();
    }

    /**
     * Read from channel.
     *
     * @param buffer buffer
     * @param r      resolver
     * @param h      completion handler
     */
    protected void readFromChannel(ByteBuffer buffer, AResolver<Integer> r, CompletionHandler<Integer, AResolver<Integer>> h) {
        channel.read(buffer, r, h);
    }

    /**
     * Write to channel.
     *
     * @param buffer buffer
     * @param r      resolver
     * @param h      completion handler
     */
    protected void writeToChannel(ByteBuffer buffer, AResolver<Integer> r, CompletionHandler<Integer, AResolver<Integer>> h) {
        channel.write(buffer, r, h);
    }

    /**
     * The input.
     */
    protected class ByteChannelInputAdapter extends CloseableBase implements AInput<ByteBuffer>, NeedsExport<AInput<ByteBuffer>> {
        /**
         * Request queue.
         */
        private final RequestQueue requestQueue = new RequestQueue();

        @Override
        public Promise<Integer> read(ByteBuffer buffer) {
            return requestQueue.run(() -> aCompletionHandler((r, h) -> readFromChannel(buffer, r, h)));
        }

        @Override
        protected Promise<Void> closeAction() {
            return closeInput();
        }

        @Override
        public AInput<ByteBuffer> export(Vat vat) {
            return AInputProxyFactory.createProxy(vat, this);
        }
    }

    /**
     * The output.
     */
    protected class Output extends CloseableBase implements AOutput<ByteBuffer>, NeedsExport<AOutput<ByteBuffer>> {
        /**
         * The request queue.
         */
        private final RequestQueue requestQueue = new RequestQueue();

        @Override
        public Promise<Void> write(ByteBuffer buffer) {
            return requestQueue.run(
                    () -> AsyncNioFlows.<Integer>aCompletionHandler((r, h) -> writeToChannel(buffer, r, h))).toVoid();
        }

        @Override
        public Promise<Void> flush() {
            return flushOutput();
        }

        @Override
        protected Promise<Void> closeAction() {
            return closeOutput();
        }

        @Override
        public AOutput<ByteBuffer> export(Vat vat) {
            return AOutputProxyFactory.createProxy(vat, this);
        }
    }
}
