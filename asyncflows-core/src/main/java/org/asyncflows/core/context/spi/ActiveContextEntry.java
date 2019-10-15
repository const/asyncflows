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

package org.asyncflows.core.context.spi;

import org.asyncflows.core.annotations.Experimental;
import org.asyncflows.core.context.Context;

/**
 * The active context entry helps to establish and remove context from the current thread. The cases include:
 * <ul>
 *     <li>Logging context</li>
 *     <li>Transactions</li>
 * </ul>
 * The context entries are assumed to be independent of each other and might be established in any order.
 * The current implementation tries to keep order of entries, but his could change later. If you need several
 * entries in the specific order like: security context, logging, transactions. Then use single entry
 * in the context for this.
 */
@Experimental
public interface ActiveContextEntry {

    /**
     * Set context of the entry in the current thread. The returned {@link Context.Cleanup}s are executed
     * when operation is finished in reverse order they have been created.
     *
     * @return a cleanup action that is needed to clean up entries, or null if nothing should be done.
     */
    Context.Cleanup setContextInTheCurrentThread();
}
