package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.core.{ACallable, Promise}
import net.sf.asyncobjects.core.stream.{AStream, Streams, StreamBuilder}
import net.sf.asyncobjects.core.data.Maybe
import scala.collection.convert.WrapAsJava
import WrapAsJava._

/**
 * The factory for creation different streams
 */
object ScalaStreams {

  private def wrap[T](value: StreamBuilder[T]) = new ScalaStreamBuilder[T](value)

  def aForProducer[T](b: => Promise[Maybe[T]]) = wrap(Streams.aForProducer(new ACallable[Maybe[T]]() {
    def call(): Promise[Maybe[T]] = b
  }))

  def aForRange(start: Int, end: Int) = wrap(Streams.aForRange(start, end))

  def aForJavaIterable[T](collection: java.lang.Iterable[T]) = wrap(Streams.aForIterable(collection))

  def aForArray[T](array: Array[T]) = wrap(Streams.aForArray(array))

  def aForJavaIterator[T](iterator: java.util.Iterator[T]) = wrap(Streams.aForIterator(iterator))

  def aForIterator[T](iterator: Iterator[T]) = aForJavaIterator(iterator)

  def aForSeq[T](values: T*) = aForIterator(values.toIterator)

  def aForIterable[T](values: Iterable[T]) = aForIterator(values.toIterator)

  def aForStream[T](stream: AStream[T]) = wrap(Streams.aForStream(stream))
}
