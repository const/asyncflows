package org.asyncflows.core.vats;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The vats utility class.
 */
public final class Vats {
    /**
     * Daemon executor.
     */
    public static final ExecutorService DAEMON_EXECUTOR =
            Executors.newCachedThreadPool(new ThreadFactory() {
                private final AtomicInteger threadCount = new AtomicInteger(0);

                @Override
                public Thread newThread(final Runnable r) {
                    final Thread t = new Thread(r, "AsyncFlows-" + threadCount.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            });

    /**
     * The private constructor for utility class.
     */
    private Vats() {
    }

    /**
     * The daemon vat.
     *
     * @return the vat
     */
    public static ExecutorVat daemonVat() {
        return new ExecutorVat(DAEMON_EXECUTOR, Integer.MAX_VALUE);
    }

    /**
     * @return the default executor
     */
    public static Vat defaultVat() {
        Vat current = Vat.current();
        return current != null ? current : daemonVat();
    }
}
