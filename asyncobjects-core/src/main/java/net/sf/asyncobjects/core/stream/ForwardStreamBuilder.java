package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.CoreFunctionUtil;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.AFunction2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.CoreExportUtil.exportIfNeeded;
import static net.sf.asyncobjects.core.CoreFunctionUtil.booleanCallable;
import static net.sf.asyncobjects.core.CoreFunctionUtil.evaluate;

/**
 * The forward builder for the data streams and sinks. It starts with some data source and adds additional downstream
 * processing steps. There are different implementation for forward builder depending on whether is currently building
 * sinks or streams. But this model looks as if the streams are being built.
 *
 * @param <T> the current stream element type
 */
public abstract class ForwardStreamBuilder<T> {
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
    public abstract ForwardStreamBuilder<T> push();

    /**
     * The pool mode is more economical, but it has less potential for parallel processing since items are read
     * one-by-one. But using {@link #window(int)} is still useful, since different steps of processing pipeline
     * will be done in the parallel, while each step will be still executed sequentially.
     *
     * @return the builder that is switched to push mode that uses {@link AStream}.
     */
    public abstract ForwardStreamBuilder<T> pull();

    /**
     * Map the stream per element. Note while item processing could take a different time, the items are available
     * to downstream consumers in the order they arrive at the map stage.
     *
     * @param mapper the element mapper.
     * @param <N>    the next element type
     * @return the next phase builder
     */
    public abstract <N> ForwardStreamBuilder<N> map(AFunction<N, T> mapper);

    /**
     * The flat map for {@link Maybe} type. This operation makes item available for downstream processing only if
     * the resulting {@link Maybe} contains a value.
     *
     * @param mapper the mapper
     * @param <N>    the next type
     * @return the next builder
     */
    public abstract <N> ForwardStreamBuilder<N> flatMapMaybe(AFunction<Maybe<N>, T> mapper);

    /**
     * The flat map for {@link AStream} type. This operation after receiving the result reads the stream
     * and makes it available for downstream processing in the specified order.
     *
     * @param mapper the mapper
     * @param <N>    the next type
     * @return the next builder
     */
    public abstract <N> ForwardStreamBuilder<N> flatMapStream(AFunction<AStream<N>, T> mapper);

    /**
     * The flat map for {@link Iterator} type. This operation after receiving the result reads the stream
     * and makes it available for downstream processing in the specified order. The default implementation
     * uses {@link #flatMapStream(net.sf.asyncobjects.core.AFunction)}
     *
     * @param mapper the mapper
     * @param <N>    the next type
     * @return the next builder
     */
    public <N> ForwardStreamBuilder<N> flatMapIterator(final AFunction<Iterator<N>, T> mapper) {
        return flatMapStream(value -> mapper.apply(value)
                .map(value1 -> aValue(Streams.aForIterator(value1).localStream())));
    }

    /**
     * The flat map for {@link Iterable} type. This operation after receiving the result reads the stream
     * and makes it available for downstream processing in the specified order. The default implementation
     * uses {@link #flatMapIterator(net.sf.asyncobjects.core.AFunction)}
     *
     * @param mapper the mapper
     * @param <N>    the next type
     * @param <C>    the collection type
     * @return the next builder
     */
    public <N, C extends Iterable<N>> ForwardStreamBuilder<N> flatMapIterable(final AFunction<C, T> mapper) {
        return flatMapIterator(value -> evaluate(value, mapper).map(mappedValue -> aValue(mappedValue.iterator())));
    }

    /**
     * Buffer the stream. This operation tries to prefetch the specified amount of elements so downstream processing
     * could be started faster. For {@link #pull()} mode, the elements are being kept in the buffer for
     * the faster access. For the {@link #push()} mode, the elements are sent to downstream consumers.
     *
     * @param size the size of buffer
     * @return the buffered stream
     */
    public abstract ForwardStreamBuilder<T> window(int size);

    /**
     * Filter the stream, the default implementation uses {@link #flatMapMaybe(AFunction)}.
     *
     * @param filter the filter to use
     * @return the filter
     */
    public ForwardStreamBuilder<T> filter(final AFunction<Boolean, T> filter) {
        return flatMapMaybe(value -> filter.apply(value).map(filterResult -> {
            if (filterResult) {
                return aMaybeValue(value);
            } else {
                return aMaybeEmpty();
            }
        }));
    }

    /**
     * @return this item returns only changed elements
     */
    public ForwardStreamBuilder<T> changed() {
        final Cell<T> current = new Cell<>();
        return filter(value -> {
            if (current.isEmpty()) {
                current.setValue(value);
                return aTrue();
            }
            if (current.getValue() == null ? value == null : current.getValue().equals(value)) {
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
    public abstract Promise<Void> consume(AFunction<Boolean, T> loopBody);

    /**
     * Left fold the stream. The default implementation is based on {@link #consume(AFunction)}.
     *
     * @param initial the initial value
     * @param folder  the folder
     * @param <R>     the result type
     * @return the promise for the result.
     */
    public <R> Promise<R> leftFold(final R initial, final AFunction2<R, R, T> folder) {
        final Cell<R> cell = new Cell<>(initial);
        return consume(value -> folder.apply(cell.getValue(), value).map(value1 -> {
            cell.setValue(value1);
            return aTrue();
        })).thenDo(() -> aValue(cell.getValue()));
    }

    /**
     * @return the fold to unit value
     */
    public Promise<Void> toVoid() {
        return consume(CoreFunctionUtil.<Boolean, T>discardArgument(booleanCallable(true)));
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
     * Start building all stream with the specified window size.
     *
     * @param windowSize the window size
     * @return the builder
     */
    public AllStreamBuilder<T> all(final int windowSize) {
        return new AllStreamBuilder<>(Streams.aForStream(
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
