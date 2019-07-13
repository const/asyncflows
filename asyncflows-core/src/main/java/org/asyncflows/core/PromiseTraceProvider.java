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

package org.asyncflows.core;

import org.asyncflows.core.annotations.Experimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * The trace provider for promises. The feature is experimental. The API could change w/o any notice.
 */
@Experimental
public interface PromiseTraceProvider {
    /**
     * The instance of trace provider.
     */
    PromiseTraceProvider INSTANCE = ((Supplier<PromiseTraceProvider>) () -> {
        final String provider = System.getProperty("org.asyncflows.core.trace.provider");
        if ("EXCEPTION".equals(provider)) {
            return new ExceptionProvider();
        } else if (provider == null || "NOP".equals(provider)) {
            return new NopProvider();
        } else {
            Logger logger = LoggerFactory.getLogger(PromiseTraceProvider.class);
            try {
                ServiceLoader<PromiseTraceProvider> loader = ServiceLoader.load(PromiseTraceProvider.class);
                for (PromiseTraceProvider traceProvider : loader) {
                    if (provider.equals(traceProvider.getClass().getName())) {
                        return traceProvider;
                    }
                }
                if (logger.isErrorEnabled()) {
                    logger.error(String.format("The trace provider: %s not found.", provider));
                }
            } catch (Throwable ex) {
                if (logger.isErrorEnabled()) {
                    logger.error(String.format("The trace provider: %s not found.", provider));
                }
            }
            return new NopProvider();
        }
    }).get();

    /**
     * @return this method is used to get the current trace when promise is created.
     */
    Object recordTrace();

    /**
     * Merge trace into received exception.
     *
     * @param problem the problem received to promise.
     * @param trace   the recorded trace
     */
    void mergeTrace(Throwable problem, Object trace);

    /**
     * The no-op provider that just returns null.
     */
    class NopProvider implements PromiseTraceProvider {

        @Override
        public Object recordTrace() {
            return null;
        }

        @Override
        public void mergeTrace(final Throwable problem, final Object trace) {
            // do nothing
        }
    }


    /**
     * The very expensive provider that records context using exception.
     * Use it only for debug purposes.
     */
    class ExceptionProvider implements PromiseTraceProvider {

        @Override
        public Object recordTrace() {
            return new PromiseTraceException();
        }

        @Override
        public void mergeTrace(final Throwable problem, final Object trace) {
            problem.addSuppressed((PromiseTraceException) trace);
        }
    }

    /**
     * The exception used to record trace.
     */
    final class PromiseTraceException extends Exception {
    }
}
