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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
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
     * @return the new daemon vat
     */
    // FIXME revise all places where it is used to support explicit specification of executor for Java EE context
    // (ManagedExecutorService)
    public static ExecutorVat daemonVat() {
        return new ExecutorVat(DAEMON_EXECUTOR, Integer.MAX_VALUE);
    }

    /**
     * @return a new vat over forkjoin pool.
     */
    public static ExecutorVat forkJoinVat() {
        return new ExecutorVat(ForkJoinPool.commonPool());
    }

    /**
     * @return the default executor
     */
    public static Vat defaultVat() {
        final Vat current = Vat.currentOrNull();
        return current != null ? current : daemonVat();
    }
}
