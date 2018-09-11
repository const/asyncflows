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

package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Cell;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.AFunction2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static org.asyncflows.core.CoreFlows.aBoolean;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.function.AsyncFunctionUtil.booleanSupplier;
import static org.asyncflows.core.function.AsyncFunctionUtil.evaluate;
import static org.asyncflows.core.function.AsyncFunctionUtil.supplierToFunction;
import static org.asyncflows.core.util.NeedsExport.exportIfNeeded;

/**
 * The forward builder for the data streams and sinks. It starts with some data source and adds additional downstream
 * processing steps. There are different implementation for forward builder depending on whether is currently building
 * sinks or streams. But this model looks as if the streams are being built.
 *
 * @param <T> the current stream element type
 */
public abstract class StreamBuilder<T> {
    /**
     * @return the build stream that returns elements one by one (the stream is exported).
     */
    public AStream<T> stream() {
        return exportIfNeeded(localStream());
    }

    /**
     * The optimized version intended to be used with other builders. The returned value is
     * unsafe for use from other vats. But sill could be useful for this vat (particularly,
     * if wrapped into the
     *
     * @return the build stream that returns elements one by one (the stream is not exported)
     */
    public abstract AStream<T> localStream();

    /**
     * Connect the created data stream to a sink.
     *
     * @param consumer a sink to connect to
     */
    public abstract void connect(ASink<? super T> consumer);

    /**
     * The push mode is might be more parallel, since the items are processed as soon as they are generated.
     * However, it is required to use {@link #window(int)} method to specify amount of parallel processing on
     * the stage.
     *
     * @return the builder that is switched to push mode that uses {@link ASink}.
     */
    public abstract StreamBuilder<T> push();

    /**
     * The pool mode is more economical, but it has less potential for parallel processing since items are read
     * one-by-one. But using {@link #window(int)} is still useful, since different steps of processing pipeline
     * will be done in the parallel, while each step will be still executed sequentially.
     *
     * @return the builder that is switched to push mode that uses {@link AStream}.
     */
    public abstract StreamBuilder<T> pull();

    /**
     * Map the stream per element. Note while item processing could take a different time, the items are available
     * to downstream consumers in the order they arrive at the map stage.
     *
     * @param mapper the element mapper.
     * @param <N>    the next element type
     * @return the next phase builder
     */
    public abstract <N> StreamBuilder<N> map(AFunction<T, N> mapper);

    /**
     * Map using synchronous function.
     *
     * @param mapper the mapper
     * @param <N>    the function
     * @return the next phase builder
     */
    public <N> StreamBuilder<N> mapSync(Function<T, N> mapper) {
        return map(t -> aValue(mapper.apply(t)));
    }

    /**
     * The flat map for {@link Maybe} type. This operation makes item available for downstream processing only if
     * the resulting {@link Maybe} contains a value.
     *
     * @param mapper the mapper
     * @param <N>    the next type
     * @return the next builder
     */
    public abstract <N> StreamBuilder<N> flatMapMaybe(AFunction<T, Maybe<N>> mapper);

    /**
     * The flat map for {@link AStream} type. This operation after receiving the result reads the stream
     * and makes it available for downstream processing in the specified order.
     *
     * @param mapper the mapper
     * @param <N>    the next type
     * @return the next builder
     */
    public abstract <N> StreamBuilder<N> flatMapStream(AFunction<T, AStream<N>> mapper);

    /**
     * The flat map for {@link Iterator} type. This operation after receiving the result reads the stream
     * and makes it available for downstream processing in the specified order. The default implementation
     * uses {@link #flatMapStream(AFunction)}
     *
     * @param mapper the mapper
     * @param <N>    the next type
     * @return the next builder
     */
    public <N> StreamBuilder<N> flatMapIterator(final AFunction<T, Iterator<N>> mapper) {
        return flatMapStream(value -> mapper.apply(value)
                .flatMap(value1 -> aValue(AsyncStreams.aForIterator(value1).localStream())));
    }

    /**
     * The flat map for {@link Iterable} type. This operation after receiving the result reads the stream
     * and makes it available for downstream processing in the specified order. The default implementation
     * uses {@link #flatMapIterator(AFunction)}
     *
     * @param mapper the mapper
     * @param <N>    the next type
     * @param <C>    the collection type
     * @return the next builder
     */
    public <N, C extends Iterable<N>> StreamBuilder<N> flatMapIterable(final AFunction<T, C> mapper) {
        return flatMapIterator(value -> evaluate(mapper, value).flatMap(mappedValue -> aValue(mappedValue.iterator())));
    }

    /**
     * Buffer the stream. This operation tries to prefetch the specified amount of elements so downstream processing
     * could be started faster. For {@link #pull()} mode, the elements are being kept in the buffer for
     * the faster access. For the {@link #push()} mode, the elements are sent to downstream consumers.
     *
     * @param size the size of buffer
     * @return the buffered stream
     */
    public abstract StreamBuilder<T> window(int size);

    /**
     * Filter the stream, the default implementation uses {@link #flatMapMaybe(AFunction)}.
     *
     * @param filter the filter to use
     * @return the filter
     */
    public StreamBuilder<T> filter(final AFunction<T, Boolean> filter) {
        return flatMapMaybe(value -> filter.apply(value).flatMap(filterResult -> {
            if (filterResult) {
                return aMaybeValue(value);
            } else {
                return aMaybeEmpty();
            }
        }));
    }

    /**
     * Filter the stream, the default implementation uses {@link #filter(AFunction)}.
     *
     * @param filter the filter to use
     * @return the filter
     */
    public StreamBuilder<T> filterSync(final Predicate<T> filter) {
        return filter(e -> aBoolean(filter.test(e)));
    }

    /**
     * Process the stream using action. The action could do anything to the stream and return any result.
     * This method is intended for selection of common fragments of processing pipelines so it could be reused.
     *
     * @param action the action
     * @param <R>    the result type
     * @return the result
     */
    public <R> R process(Function<StreamBuilder<T>, R> action) {
        return action.apply(this);
    }

    /**
     * @return this item returns only changed elements
     */
    public StreamBuilder<T> changed() {
        final Cell<T> current = new Cell<>();
        return filter(value -> {
            if (current.isEmpty()) {
                current.setValue(value);
                return aTrue();
            }
            if (Objects.equals(current.getValue(), value)) {
                return aFalse();
            }
            current.setValue(value);
            return aTrue();
        });
    }

    /**
     * Consume elements from the stream in the order they arrive. The stream is closed after all values are consumed.
     * If failure is received from the stream iteration stops with received failure.
     *
     * @param loopBody the loop body that is invoked. If false is returned from the loop, the iteration stops
     *                 (but stream is closed anyway).
     * @return a promise that resolves when loop finished.
     */
    public abstract Promise<Void> consume(AFunction<T, Boolean> loopBody);

    /**
     * Left fold the stream. The default implementation is based on {@link #consume(AFunction)}.
     *
     * @param initial the initial value
     * @param folder  the folder
     * @param <R>     the result type
     * @return the promise for the result.
     */
    public <R> Promise<R> leftFold(final R initial, final AFunction2<R, T, R> folder) {
        final Cell<R> cell = new Cell<>(initial);
        return consume(value -> folder.apply(cell.getValue(), value).flatMap(value1 -> {
            cell.setValue(value1);
            return aTrue();
        })).thenFlatGet(() -> aValue(cell.getValue()));
    }

    /**
     * @return the fold to unit value
     */
    public Promise<Void> toVoid() {
        return consume(supplierToFunction(booleanSupplier(true)));
    }

    /**
     * @return the fold to list
     */
    public Promise<List<T>> toList() {
        return leftFold(new ArrayList<>(), (list, producedValue) -> {
            list.add(producedValue);
            return aValue(list);
        });
    }

    /**
     * Collect using collector.
     *
     * @param collector the collector.
     * @param <R>       the result type
     * @param <A>       the collector accumulator type
     * @return the promise for collect operation
     */
    public <R, A> Promise<R> collect(Collector<T, A, R> collector) {
        return aNow(() -> {
            /// some more optimized versions are possible depending on collector properties.
            final BiConsumer<A, T> acceptor = collector.accumulator();
            A accumulator = collector.supplier().get();
            return leftFold(accumulator, (a, value) -> {
                acceptor.accept(a, value);
                return aValue(a);
            }).map(a -> collector.finisher().apply(a));
        });
    }

    /**
     * Start building all stream with the specified window size.
     *
     * @param windowSize the window size
     * @return the builder
     */
    public AllStreamBuilder<T> all(final int windowSize) {
        return new AllStreamBuilder<>(AsyncStreams.aForStream(
                AllStreamBuilder.outcomeStream(localStream())).push().window(windowSize));
    }

    /**
     * Start building all stream with the window size {@link Integer#MAX_VALUE}.
     *
     * @return the builder
     */
    public AllStreamBuilder<T> all() {
        return all(Integer.MAX_VALUE);
    }
}
