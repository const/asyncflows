package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.core.stream._
import net.sf.asyncobjects.core.Promise
import net.sf.asyncobjects.core.data.Maybe
import net.sf.asyncobjects.asyncscala.AsyncScalaControl._
import RichPromise._
import scala.collection.mutable.ArrayBuffer
import scala.collection.convert.WrapAsJava._

/**
 * The wrapper over ForwardStreamBuilder.
 */
class ScalaStreamBuilder[T](val builder: ForwardStreamBuilder[T]) {
  /**
   * @return the build stream that returns elements one by one (the stream is exported).
   */
  def stream: AStream[T] = builder.stream()

  /**
   * The optimized version intended to be used with other builders. The returned value is
   * unsafe for use from other vats. But sill could be useful for this vat (particularly,
   * if wrapped into the
   *
   * @return the build stream that returns elements one by one (the stream is not exported)
   */
  def localStream: AStream[T] = builder.stream()

  /**
   * Connect the created data stream to a sink.
   *
   * @param consumer a sink to connect to
   */
  def connect(consumer: ASink[_ >: T]) = builder.connect(consumer)

  /**
   * The push mode is might be more parallel, since the items are processed as soon as they are generated.
   * However, it is required to use [[net.sf.asyncobjects.asyncscala.ScalaStreamBuilder!.w i n d o w ( i n t )]]
   * method to specify amount of parallel processing on the stage.
   *
   * @return the builder that is switched to push mode that uses [[net.sf.asyncobjects.core.stream.ASink]]
   */
  def push: ScalaStreamBuilder[T] = new ScalaStreamBuilder[T](builder.push())

  /**
   * The pool mode is more economical, but it has less potential for parallel processing since items are read
   * one-by-one. But using [[net.sf.asyncobjects.asyncscala.ScalaStreamBuilder]].window(int) is still useful,
   * since different steps of processing pipeline will be done in the parallel, while each step will be still
   * executed sequentially.
   *
   * @return the builder that is switched to push mode that uses [[net.sf.asyncobjects.core.stream.AStream]].
   */
  def pull: ScalaStreamBuilder[T] = new ScalaStreamBuilder[T](builder.pull())

  /**
   * Map the stream per element. Note while item processing could take a different time, the items are available
   * to downstream consumers in the order they arrive at the map stage.
   *
   * @param mapper the element mapper.
   * @tparam N    the next element type
   * @return the next phase builder
   */
  def map[N](mapper: T => Promise[N]): ScalaStreamBuilder[N] =
    new ScalaStreamBuilder[N](builder.map(CoreFunctionConversions.toAFunction(mapper)))

  /**
   * The flat map for [[net.sf.asyncobjects.core.data.Maybe]] type. This operation makes item available for downstream
   * processing only if the resulting [[net.sf.asyncobjects.core.data.Maybe]] contains a value.
   *
   * @param mapper the mapper
   * @tparam N    the next type
   * @return the next builder
   */
  def flatMapMaybe[N](mapper: T => Promise[Maybe[N]]): ScalaStreamBuilder[N] =
    new ScalaStreamBuilder[N](builder.flatMapMaybe(CoreFunctionConversions.toAFunction(mapper)))

  /**
   * The flat map for [[net.sf.asyncobjects.core.stream.AStream]] type. This operation after receiving the result
   * reads the stream and makes it available for downstream processing in the specified order.
   *
   * @param mapper the mapper
   * @tparam N    the next type
   * @return the next builder
   */
  def flatMapStream[N](mapper: T => Promise[AStream[N]]): ScalaStreamBuilder[N] =
    new ScalaStreamBuilder[N](builder.flatMapStream(CoreFunctionConversions.toAFunction(mapper)))

  /**
   * The flat map for [[java.util.Iterator]] type. This operation after receiving the result reads the stream
   * and makes it available for downstream processing in the specified order. The default implementation
   * uses [[net.sf.asyncobjects.asyncscala.ScalaStreamBuilder]].flatMapStream(net.sf.asyncobjects.core.AFunction)
   *
   * @param mapper the mapper
   * @tparam N    the next type
   * @return the next builder
   */
  def flatMapJavaIterator[N](mapper: T => Promise[java.util.Iterator[N]]): ScalaStreamBuilder[N] =
    new ScalaStreamBuilder[N](builder.flatMapIterator(CoreFunctionConversions.toAFunction(mapper)))


  /**
   * The flat map for [[java.lang.Iterable]] type. This operation after receiving the result reads the stream
   * and makes it available for downstream processing in the specified order. The default implementation
   * uses [[net.sf.asyncobjects.asyncscala.ScalaStreamBuilder]].flatMapIterator(net.sf.asyncobjects.core.AFunction)
   *
   * @param mapper the mapper
   * @tparam N    the next type
   * @tparam C    the collection type
   * @return the next builder
   */
  def flatMapJavaIterable[N, C[X] <: java.lang.Iterable[X]](mapper: T => Promise[C[N]]) =
    new ScalaStreamBuilder[N](builder.flatMapIterable(CoreFunctionConversions.toAFunction(mapper)))

  def flatMapIterable[N, C[X] <: Iterable[X]](mapper: T => Promise[C[N]]): ScalaStreamBuilder[N] =
    flatMapJavaIterable[N, java.lang.Iterable](v =>
      mapper.apply(v).flatMap {
        c => asJavaIterable(c).asInstanceOf[java.lang.Iterable[N]]
      })

  def flatMapIterator[N](mapper: T => Promise[Iterator[N]]) =
    flatMapJavaIterator[N](v => mapper.apply(v).flatMap(c => asJavaIterator(c)))

  /**
   * Buffer the stream. This operation tries to prefetch the specified amount of elements so downstream processing
   * could be started faster. For pull mode, the elements are being kept in the buffer for
   * the faster access. For the push mode, the elements are sent to downstream consumers.
   *
   * @param size the size of buffer
   * @return the buffered stream
   */
  def window(size: Int): ScalaStreamBuilder[T] = new ScalaStreamBuilder[T](builder.window(size))

  /**
   * Filter the stream, the default implementation uses
   * [[net.sf.asyncobjects.asyncscala.ScalaStreamBuilder]].flatMapMaybe(net.sf.asyncobjects.core.AFunction).
   *
   * @param filter the filter to use
   * @return the filter
   */
  def filter(filter: T => BooleanPromise): ScalaStreamBuilder[T] = new ScalaStreamBuilder[T](
    builder.filter(CoreFunctionConversions.toAFunction(filter)))

  /**
   * @return this item returns only changed elements
   */
  def changed: ScalaStreamBuilder[T] = new ScalaStreamBuilder[T](builder.changed())

  /**
   * Consume elements from the stream in the order they arrive. The stream is closed after all values are consumed.
   * If failure is received from the stream iteration stops with received failure.
   *
   * @param loopBody the loop body that is invoked. If false is returned from the loop, the iteration stops
   *                 (but stream is closed anyway).
   * @return a promise that resolves when loop finished.
   */
  def consume(loopBody: T => BooleanPromise): Promise[Void] = builder.consume(CoreFunctionConversions.toAFunction(loopBody))

  /**
   * Left fold the stream. The default implementation is based on
   * [[net.sf.asyncobjects.asyncscala.ScalaStreamBuilder]].consume(AFunction).
   *
   * @param initial the initial value
   * @param folder  the folder
   * @tparam R     the result type
   * @return the promise for the result.
   */
  def leftFold[R](initial: R)(folder: (R, T) => Promise[R]): Promise[R] =
    builder.leftFold(initial, CoreFunctionConversions.toAFunction2(folder))

  /**
   * @return the list from the stream
   */
  def toList: Promise[List[T]] = leftFold(List.empty[T])((r, a) => a :: r).flatMap((l) => l.reverse)

  /**
   * @return the list from the stream
   */
  def toArrayBuffer: Promise[ArrayBuffer[T]] = leftFold(new ArrayBuffer[T])((r, a) => r += a)


  /**
   * @return the fold to unit value
   */
  def toUnit: Promise[Void] = builder.toUnit

  /**
   * @return the fold to list
   */
  def toJavaList: Promise[java.util.List[T]] = builder.toList


  /**
   * Start building all stream with the specified window size.
   *
   * @param windowSize the window size
   * @return the builder
   */
  def all(windowSize: Int): ScalaStreamBuilder[T] = new ScalaStreamBuilder[T](builder.all(windowSize))

  /**
   * Start building all stream with the window size [[java.lang.Integer]].MAX_VALUE.
   *
   * @return the builder
   */
  def all: ScalaStreamBuilder[T] = all(Integer.MAX_VALUE)
}


