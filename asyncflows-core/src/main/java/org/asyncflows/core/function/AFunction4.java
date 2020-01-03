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

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.core.vats.Vat;

/**
 * The four argument function.
 *
 * @param <A> the first argument type
 * @param <B> the second argument type
 * @param <C> the third argument type
 * @param <D> the forth argument type
 * @param <R> the result type
 */
@Asynchronous
@FunctionalInterface
public interface AFunction4<A, B, C, D, R> extends AsynchronousFunction<AFunction4<A, B, C, D, R>> {

    @Override
    default AFunction4<A, B, C, D, R> forceExport(Vat vat) {
        return AFunction4ProxyFactory.createProxy(vat, this);
    }

    /**
     * Invoke function.
     *
     * @param a the first argument
     * @param b the second argument
     * @param c the third argument
     * @param d the forth argument
     * @return the promise for result.
     * @throws Throwable if any failure
     */
    @SuppressWarnings("squid:S00112")
    Promise<R> apply(A a, B b, C c, D d) throws Throwable;
}
