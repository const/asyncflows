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

package org.asyncflows.core.function;

import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.core.vats.Vat;

/**
 * A one-way action. The difference from runnable is that any exception is allowed,
 * including checked exception and errors.
 */
@Asynchronous
@FunctionalInterface
public interface AOneWayAction extends AsynchronousFunction<AOneWayAction> {

    @Override
    default AOneWayAction forceExport(Vat vat) {
        return AOneWayActionProxyFactory.createProxy(vat, this);
    }

    /**
     * Run action.
     *
     * @throws Throwable if any problem
     */
    @SuppressWarnings("squid:S00112")
    void run() throws Throwable;
}
