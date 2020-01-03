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

package org.asyncflows.core.trace;

import org.asyncflows.core.annotations.Experimental;
import org.asyncflows.core.annotations.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * This internal class is not supposed to be used directly.
 */
@Experimental
@Internal
public class PromiseTrace {
    /**
     * The instance of trace provider.
     */
    @Internal
    public static final PromiseTraceProvider INSTANCE = getPromiseTraceProvider();

    /**
     * The private constructor for utility class
     */
    private PromiseTrace() {
        // do nothing
    }

    /**
     * @return a configured trace provider
     */
    @SuppressWarnings("squid:S3776")
    private static PromiseTraceProvider getPromiseTraceProvider() {
        final String provider = System.getProperty("org.asyncflows.core.trace.provider");
        if ("EXCEPTION".equals(provider)) {
            return new PromiseTraceExceptionProvider();
        } else if (provider == null || "NOP".equals(provider)) {
            return new PromiseTraceNopProvider();
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
            return new PromiseTraceNopProvider();
        }
    }
}
