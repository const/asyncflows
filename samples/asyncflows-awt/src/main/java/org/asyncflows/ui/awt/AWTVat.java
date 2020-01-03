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

package org.asyncflows.ui.awt;

import org.asyncflows.core.vats.Vat;
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
     * The lock.
     */
    private static final Object INIT_LOCK = new Object();
    /**
     * AWT vat.
     */
    private static AWTVat vat;

    /**
     * Get instance of AWT vat.
     *
     * @return the vat instance
     */
    public static AWTVat instance() {
        synchronized (INIT_LOCK) {
            if (vat == null) {
                vat = new AWTVat();
                EventQueue.invokeLater(() -> vat.initInQueue());
            }
            return vat;
        }
    }

    private void initInQueue() {
        enter();
    }

    @Override
    public void execute(final Runnable action) {
        EventQueue.invokeLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                LOG.error("Failed to execute action on the vat", t);
            }
        });
    }
}
