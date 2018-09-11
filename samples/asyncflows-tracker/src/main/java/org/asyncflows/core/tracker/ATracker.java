/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

package org.asyncflows.core.tracker;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.streams.ASink;
import org.asyncflows.core.streams.AStream;
import org.asyncflows.core.util.ASubscription;

import java.util.function.Consumer;

/**
 * The tracker for some values.
 *
 * @param <T> the tracker element type
 */
public interface ATracker<T> {
    /**
     * Subscribe to tracker. The subscription ends when {@link ASink#finished()} method finishes.
     * On subscription registration a value is pushed when it is available. If there is no listeners
     * for watchers, the values are not tracked normally.
     *
     * @param sink the sink based
     * @return a promise with subscription object
     */
    Promise<ASubscription> subscribe(Consumer<Outcome<T>> sink);

    /**
     * Open watcher as stream. This method just uses pipe to track values.
     *
     * @return the stream.
     */
    Promise<AStream<Outcome<T>>> open();
}

