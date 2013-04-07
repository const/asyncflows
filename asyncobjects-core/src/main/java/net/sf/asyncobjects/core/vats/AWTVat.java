package net.sf.asyncobjects.core.vats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * The AWT vat used to schedule events on AWT event queue.
 */
public final class AWTVat extends Vat {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AWTVat.class);
    /**
     * AWT vat.
     */
    private static AWTVat vat;
    /**
     * The lock.
     */
    private static final Object INIT_LOCK = new Object();

    @Override
    public void execute(final Vat currentVat, final Runnable action) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } catch (Throwable t) {
                    LOG.error("Failed to execute action on the vat", t);
                }
            }
        });
    }

    @Override
    public void execute(final Runnable action) {
        execute(null, action);
    }

    /**
     * Get instance of AWT vat.
     *
     * @return the vat instance
     */
    public static AWTVat instance() {
        synchronized (INIT_LOCK) {
            if (vat == null) {
                vat = new AWTVat();
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        vat.enter();
                    }
                });
            }
            return vat;
        }
    }
}
