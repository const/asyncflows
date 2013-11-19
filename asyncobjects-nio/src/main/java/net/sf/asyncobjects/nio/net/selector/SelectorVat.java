package net.sf.asyncobjects.nio.net.selector;

import net.sf.asyncobjects.core.vats.SingleThreadVatWithIdle;
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
        if (buffer != direct) { // NOPMD
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
        LOG.error("Unexpected kind of attachment: "
                + (attachment == null ? "null" : attachment.getClass().getName()));
    }

    @Override
    protected void pollIdle() {
        try {
            selector.selectNow();
            notifyKeys();
        } catch (IOException e) {
            failKeys(e);
            throw new IllegalStateException("The selector fails: ", e);
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
                    LOG.error("Failed to fail key: " + key.channel(), t);
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
                failKeys(new ClosedSelectorException());
            } finally {
                selector.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("The vat cannot be closed: ", e);
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
    public void changeSelector() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Changing selector for the SelectorVat, because of broken selector");
        }
        final ArrayList<SelectionKey> keys = new ArrayList<SelectionKey>(selector.keys()); // NOPMD
        try {
            selector.close();
        } catch (Throwable t) {
            LOG.error("failed to close selector", t);
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
                    LOG.error("Failed to change selector: " + key.channel(), t);
                }
            } else {
                handleUnexpectedAttachment(attachment);
            }
        }
    }
}
