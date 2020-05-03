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

package org.asyncflows.core.util;

import org.asyncflows.core.vats.Vat;

/**
 * The marker interface that indicate that service needs to be exported to be safely used.
 *
 * @param <T> the service type.
 */
public interface ExportableComponent<T> {
    /**
     * Export service if needed.
     *
     * @param object the object
     * @param <R>    the service type
     * @return the exported object if it needs to be exported or passed value.
     */
    @SuppressWarnings("unchecked")
    static <R> R exportIfNeeded(final R object) {
        if (object instanceof ExportableComponent) {
            return (R) ((ExportableComponent<Object>) object).export();
        }
        return object;
    }

    /**
     * @return the service exported to a default vat.
     */
    default T export() {
        return export(Vat.current());
    }

    /**
     * Export service to the specific vat.
     *
     * @param vat the vat
     * @return the exported service
     */
    T export(final Vat vat);
}
