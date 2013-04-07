package net.sf.asyncobjects.core.vats;

/**
 * The execution context for asynchronous objects.
 */
public abstract class Vat {
    /**
     * The current vat.
     */
    private static final ThreadLocal<Vat> CURRENT = new ThreadLocal<Vat>();
    /**
     * If true, the vat is active.
     */
    private boolean active;

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
     * Execute the action, this method allows vats to execute actions faster
     * in case when the current vat is known.
     *
     * @param currentVat the current vat if it is known
     * @param action     the action action
     */
    public abstract void execute(Vat currentVat, Runnable action);

    /**
     * Execute action on the vat from unknown context.
     *
     * @param action the action to action
     */
    public abstract void execute(final Runnable action);

    /**
     * @return true if the vat is available on the current thread
     */
    public static boolean isVatAvailable() {
        return CURRENT.get() != null;
    }

    /**
     * @return the current vat
     */
    public static Vat current() {
        final Vat vat = CURRENT.get();
        if (vat == null) {
            throw new IllegalStateException("There are no current vat");
        }
        return vat;
    }
}
