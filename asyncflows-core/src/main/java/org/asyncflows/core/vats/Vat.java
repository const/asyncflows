/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

package org.asyncflows.core.vats;

import java.util.concurrent.Executor;

/**
 * The execution context for asynchronous objects.
 */
public abstract class Vat implements Executor {
    /**
     * The current vat.
     */
    private static final ThreadLocal<Vat> CURRENT = new ThreadLocal<>();
    /**
     * If true, the vat is active.
     */
    private boolean active;

    /**
     * @return true if the vat is available on the current thread
     */
    public static boolean isVatAvailable() {
        return CURRENT.get() != null;
    }

    /**
     * @return the current vat or null
     */
    public static Vat currentOrNull() {
        return CURRENT.get();
    }

    /**
     * @return the current vat or fail if vat is not available.
     */
    public static Vat current() {
        final Vat vat = currentOrNull();
        if (vat == null) {
            throw new IllegalStateException("No vat is available");
        }
        return vat;
    }

    /**
     * This vat enters the context.
     */
    protected final void enter() {
        if (active) {
            throw new IllegalStateException("The vat is already active: "
                    + this);
        }
        if (CURRENT.get() != null) {
            throw new IllegalStateException("Some vat is already active "
                    + "on this thread: " + CURRENT.get());
        }
        active = true;
        CURRENT.set(this);
    }

    /**
     * This vat leaves the context.
     */
    protected final void leave() {
        if (CURRENT.get() != this) {
            throw new IllegalStateException("Some other vat is active on "
                    + "this thread: " + CURRENT.get());
        }
        CURRENT.remove();
        active = false;
    }

}
