package net.sf.asyncobjects.core.vats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The batched vat. It it implements internally a queue of action that is optimized for
 * multiple-writers-single-reader usage pattern.
 */
public abstract class BatchedVat extends Vat {
    /**
     * The default size of batch to execute.
     */
    public static final int DEFAULT_BATCH_SIZE = 256;
    /**
     * The amount of optimized adds before.
     */
    private static final int OPTIMIZED_ENTRIES_MAX = 256;
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BatchedVat.class);
    /**
     * The vat lock.
     */
    private final Object lock = new Object();
    /**
     * The batch size to execute.
     */
    private final int batchSize;
    /**
     * The synchronized head, used for access from other vats.
     */
    private Cell head;
    /**
     * The synchronized tail, used for access from other vats.
     */
    private Cell tail;
    /**
     * Execution head, used for access from this vat.
     */
    private Cell execHead;
    /**
     * Execution tail, used for access from this vat.
     */
    private Cell execTail;
    /**
     * The amount of optimized entries to add before, list is added.
     */
    private int optimizedAdds = OPTIMIZED_ENTRIES_MAX;
    /**
     * If true, the vat is scheduled.
     */
    private boolean scheduled;

    /**
     * The constructor with default batch size.
     */
    protected BatchedVat() {
        this(DEFAULT_BATCH_SIZE);
    }

    /**
     * The constructor with the specified batch size.
     *
     * @param maxBatchSize the batch size
     */
    protected BatchedVat(final int maxBatchSize) {
        this.batchSize = maxBatchSize;
    }

    /**
     * Execute the action, this method allows vats to execute actions faster
     * in case when the current vat is known.
     *
     * @param currentVat the current vat if it is known
     * @param action     the action action
     */
    @Override
    public final void execute(final Vat currentVat, final Runnable action) {
        final Cell cell = new Cell(action);
        if (currentVat == this) {
            final Cell previousTail = execTail;
            if (previousTail == null) {
                execHead = cell;
                execTail = cell;
            } else {
                previousTail.next = cell;
                execTail = cell;
            }
            optimizedAdds--;
            if (optimizedAdds <= 0) {
                optimizedAdds = OPTIMIZED_ENTRIES_MAX;
                synchronized (lock) {
                    if (this.head != null) {
                        cell.next = this.head;
                        execTail = this.tail;
                        this.head = null;
                        this.tail = null;
                    }
                }
            }
            // scheduling is not needed here because the vat is being executed.
        } else {
            final boolean scheduleNeeded;
            synchronized (lock) {
                final Cell previousTail = this.tail;
                if (previousTail == null) {
                    this.head = cell;
                    this.tail = cell;
                } else {
                    previousTail.next = cell;
                    this.tail = cell;
                }
                scheduleNeeded = !this.scheduled;
                if (scheduleNeeded) {
                    this.scheduled = true;
                }
            }
            if (scheduleNeeded) {
                schedule();
            }
        }
    }

    @Override
    public final void execute(final Runnable action) {
        execute(Vat.current(), action);
    }

    /**
     * Schedule vat for the further execution.
     */
    protected abstract void schedule();

    /**
     * Run a batch and leave.
     */
    protected final void runBatch() {
        enter();
        try {
            for (int step = batchSize; step > 0; step--) {
                Cell current = execHead;
                if (current == null) {
                    synchronized (lock) {
                        current = head;
                        if (current == null) {
                            return;
                        }
                        if (current.next != null) {
                            execHead = current.next;
                            execTail = tail;
                        }
                        head = null;
                        tail = null;
                    }
                } else {
                    if (current.next != null) {
                        execHead = current.next;
                    } else {
                        execHead = null;
                        execTail = null;
                    }
                }
                try {
                    current.action.run();
                } catch (Throwable t) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Error while executing action: " + current.action, t);
                    }
                }
            }
        } finally {
            leave();
            boolean scheduleNeeded;
            synchronized (lock) {
                scheduleNeeded = !this.scheduled && (execHead != null || head != null);
                if (scheduleNeeded) {
                    this.scheduled = true;
                }
            }
            if (scheduleNeeded) {
                schedule();
            }
        }
    }

    /**
     * The queue cell.
     */
    private static final class Cell {
        /**
         * The action for the cell.
         */
        private final Runnable action;
        /**
         * The next cell.
         */
        private Cell next;

        /**
         * The constructor.
         *
         * @param submittedAction the action
         */
        private Cell(final Runnable submittedAction) {
            this.action = submittedAction;
        }
    }
}
