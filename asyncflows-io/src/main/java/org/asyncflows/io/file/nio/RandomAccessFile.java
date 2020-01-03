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

package org.asyncflows.io.file.nio;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.adapters.nio.AsyncNioFlows;
import org.asyncflows.io.file.ARandomAccessFile;
import org.asyncflows.io.file.ARandomAccessFileProxyFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.ExecutorService;

import static org.asyncflows.core.CoreFlows.aExecutorAction;
import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.io.adapters.nio.AsyncNioFlows.aCompletionHandler;

/**
 * File channel.
 */
class RandomAccessFile extends CloseableBase implements ARandomAccessFile, NeedsExport<ARandomAccessFile> {
    /**
     * The wrapped channel.
     */
    private final AsynchronousFileChannel channel;
    /**
     * The executor service.
     */
    private final ExecutorService executorService;

    /**
     * The constructor.
     *
     * @param channel         the channel
     * @param executorService the executor service
     */
    RandomAccessFile(AsynchronousFileChannel channel, ExecutorService executorService) {
        this.channel = channel;
        this.executorService = executorService;
    }

    @Override
    public Promise<Long> size() {
        return aNow(() -> aValue(getSize()));
    }

    /**
     * The internal method.
     *
     * @return the internal method
     * @throws IOException if failure
     */
    public long getSize() throws IOException {
        return channel.size();
    }

    @Override
    public Promise<Void> truncate(long newSize) {
        return aExecutorAction(executorService, () -> channel.truncate(newSize));
    }

    @Override
    public Promise<Void> force(boolean metadata) {
        return aExecutorAction(executorService, () -> channel.force(metadata));
    }

    @Override
    public Promise<Integer> read(ByteBuffer buffer, long position) {
        return aCompletionHandler((r, h) -> channel.read(buffer, position, r, h));
    }

    @Override
    public Promise<Void> write(ByteBuffer buffer, long position) {
        return AsyncNioFlows.<Integer>aCompletionHandler((r, h) -> channel.write(buffer, position, r, h)).thenFlatGet(
                () -> !buffer.hasRemaining() ? aVoid() : aFailure(
                        new IllegalStateException("Buffer has not been completely written: n = " + buffer.remaining()))
        );
    }

    @Override
    public Promise<FileLock> lock(long position, long size, boolean shared) {
        return aCompletionHandler((r, h) -> channel.lock(position, size, shared, r, h));
    }

    @Override
    public Promise<FileLock> lock() {
        return aCompletionHandler(channel::lock);
    }

    @Override
    public Promise<FileLock> tryLock(long position, long size, boolean shared) {
        return aNow(() -> aValue(channel.tryLock(position, size, shared)));
    }

    @Override
    public Promise<FileLock> tryLock() {
        return aNow(() -> aValue(channel.tryLock()));
    }

    @Override
    protected Promise<Void> closeAction() {
        return aExecutorAction(executorService, channel::close);
    }

    @Override
    public ARandomAccessFile export(Vat vat) {
        return ARandomAccessFileProxyFactory.createProxy(vat, this);
    }
}
