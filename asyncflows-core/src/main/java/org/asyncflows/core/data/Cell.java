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

package org.asyncflows.core.data;

/**
 * The simple mutable cell for values. It is mostly used as mutable variable available
 * in inner class scope as well. The array type does not work well in that case since
 * it could not be easily created for generic values.
 *
 * @param <T> the value type
 */
public final class Cell<T> {
    /**
     * the value.
     */
    private T value;
    /**
     * if true, the value has been set.
     */
    private boolean hasValue;

    /**
     * The constructor for the cell.
     *
     * @param value the value
     */
    public Cell(final T value) {
        this.value = value;
        this.hasValue = true;
    }

    /**
     * The constructor with null initial value.
     */
    public Cell() {
        // do nothing
    }

    /**
     * @return the value (null if there is no value or value is null)
     */
    public T getValue() {
        return value;
    }

    /**
     * Set value.
     *
     * @param value the value to set
     */
    public void setValue(final T value) {
        this.hasValue = true;
        this.value = value;
    }

    /**
     * @return true if there is no value
     */
    public boolean isEmpty() {
        return !hasValue;
    }

    /**
     * Remove value from cell.
     */
    public void clearValue() {
        hasValue = false;
        value = null;
    }
}
