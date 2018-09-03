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
     * @return the current vat
     */
    public static Vat currentOrNull() {
        return CURRENT.get();
    }

    public static Vat current() {
        Vat vat = currentOrNull();
        if(vat == null) {
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


    /**
     * Execute action on the vat from unknown context.
     *
     * @param action the action to action
     */
    public abstract void execute(final Runnable action);
}
