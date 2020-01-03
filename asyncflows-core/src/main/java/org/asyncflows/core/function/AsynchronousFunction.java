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

import org.asyncflows.core.util.AsynchronousService;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.vats.Vats.defaultVat;

/**
 * The helper interface that provides some functionality for exporting lambdas in place.
 * Use this interface only for functional interfaces that are not supposed to be extended.
 *
 * @param <T> the self type
 */
public interface AsynchronousFunction<T> {
    /**
     * @return exported function
     */
    default T export() {
        return export(defaultVat());
    }

    /**
     * Export function to the specific Vat.
     *
     * @param vat the vat
     * @return the exported function
     */
    @SuppressWarnings("unchecked")
    default T export(Vat vat) {
        return this instanceof AsynchronousService ? (T) this : this.forceExport(vat);
    }

    /**
     * Force export on the specific vat, even if is already exported.
     *
     * @param vat the vat
     * @return the exported
     */
    T forceExport(Vat vat);
}
