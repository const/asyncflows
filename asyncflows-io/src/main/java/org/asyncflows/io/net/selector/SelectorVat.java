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

package org.asyncflows.io.net.selector;

import org.asyncflows.core.util.ResourceClosedException;
import org.asyncflows.core.vats.SingleThreadVatWithIdle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The vat that controls NIO execution.
 */
public class SelectorVat extends SingleThreadVatWithIdle {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SelectorVat.class);
    /**
     * The selector timeout.
     */
    private static final long TIMEOUT = 1000;
    /**
     * The direct buffer for the vat, it is used when non-direct buffer is passed as argument to the method.
     */
    private final ByteBuffer direct = ByteBuffer.allocateDirect(16 * 1024);
    /**
     * The selector.
     */
    private Selector selector;
    /**
     * If true, the buffer was allocated but not yet freed.
     */
    private boolean isDirectBufferAllocated;

    /**
     * The constructor.
     *
     * @param maxBatchSize max batch size
     * @param stopKey      the stop key
     * @throws IOException if selector could not be opened
     */
    public SelectorVat(final int maxBatchSize, final Object stopKey) throws IOException {
        super(maxBatchSize, stopKey);
        selector = Selector.open();
    }

    /**
     * The constructor.
     *
     * @param stopKey the stop key
     * @throws IOException if selector could not be opened
     */
    public SelectorVat(final Object stopKey) throws IOException {
        this(DEFAULT_BATCH_SIZE, stopKey);
    }

    /**
     * @return the direct buffer
     */
    public ByteBuffer getDirect() {
        if (isDirectBufferAllocated) {
            throw new IllegalStateException("The buffer was allocated but not freed!");
        }
        isDirectBufferAllocated = true;
        return direct;
    }

    /**
     * Release direct buffer.
     *
     * @param buffer the buffer to release (must be same as allocated by {@link #getDirect()}.
     */
    public void releaseDirect(final ByteBuffer buffer) {
        if (buffer != direct) {
            throw new IllegalArgumentException("The wrong buffer is released!");
        }
        if (!isDirectBufferAllocated) {
            throw new IllegalStateException("The buffer was not allocated yet!");
        }
        isDirectBufferAllocated = false;
    }

    @Override
    protected void idle() {
        try {
            selector.select(TIMEOUT);
            notifyKeys();
        } catch (IOException e) {
            failKeys(e);
            throw new IllegalStateException("The selector fails: ", e);
        }
    }

    /**
     * Notify all selected keys.
     */
    private void notifyKeys() {
        for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext();/**/) {
            final SelectionKey selectionKey = i.next();
            i.remove();
            final Object attachment = selectionKey.attachment();
            if (attachment instanceof ChannelContext) {
                final ChannelContext context = (ChannelContext) attachment;
                context.updateReady();
            } else {
                handleUnexpectedAttachment(attachment);
            }
        }
    }

    /**
     * Handle unexpected attachment.
     *
     * @param attachment the attachment
     */
    private void handleUnexpectedAttachment(final Object attachment) {
        if (LOG.isErrorEnabled()) {
            LOG.error("Unexpected kind of attachment: "
                    + (attachment == null ? "null" : attachment.getClass().getName()));
        }
    }

    @Override
    protected void pollIdle() {
        try {
            selector.selectNow();
            notifyKeys();
        } catch (IOException e) {
            failKeys(e);
        } catch (ClosedSelectorException e) {
            // vat closed, ignore it
        }
    }

    /**
     * Fail all keys on the channel.
     *
     * @param e the exception to fail keys with
     */
    private void failKeys(final Exception e) {
        for (final SelectionKey key : selector.keys()) {
            final Object attachment = key.attachment();
            if (attachment instanceof ChannelContext) {
                try {
                    final ChannelContext context = (ChannelContext) attachment;
                    context.fail(e);
                } catch (Throwable t) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to fail key: " + key.channel(), t);
                    }
                }
            } else {
                handleUnexpectedAttachment(attachment);
            }
        }
    }

    @Override
    protected void closeVat() {
        try {
            try {
                failKeys(new ResourceClosedException("The vat is closed"));
            } finally {
                selector.close();
            }
        } catch (IOException e) {
            throw new ResourceClosedException("The vat cannot be closed: ", e);
        }
    }

    @Override
    protected void wakeUp() {
        selector.wakeup();
    }

    /**
     * @return the selector for the vat
     */
    public Selector getSelector() {
        return selector;
    }

    /**
     * Change selector.
     */
    @SuppressWarnings("squid:S3776")
    public void changeSelector() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Changing selector for the SelectorVat, because of broken selector");
        }
        final ArrayList<SelectionKey> keys = new ArrayList<>(selector.keys());
        try {
            selector.close();
        } catch (Throwable t) {
            if (LOG.isErrorEnabled()) {
                LOG.error("failed to close selector", t);
            }
        }
        try {
            selector = Selector.open();
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to open selector:", t);
        }
        for (final SelectionKey key : keys) {
            final Object attachment = key.attachment();
            if (attachment instanceof ChannelContext) {
                try {
                    final ChannelContext context = (ChannelContext) attachment;
                    context.setSelector(selector);
                } catch (Throwable t) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to change selector: " + key.channel(), t);
                    }
                }
            } else {
                handleUnexpectedAttachment(attachment);
            }
        }
    }
}
