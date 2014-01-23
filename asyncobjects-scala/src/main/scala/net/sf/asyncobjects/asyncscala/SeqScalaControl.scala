package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.core.util.SeqControl.SeqBuilder
import net.sf.asyncobjects.core.{AFunction, ACallable}
import net.sf.asyncobjects.asyncscala.AsyncScalaControl._
import net.sf.asyncobjects.core.util.SeqControl
import net.sf.asyncobjects.core.Promise
import RichPromise._
import net.sf.asyncobjects.core.data.Maybe
import scala.collection.convert.WrapAsJava._

/**
 * Utilities for sequential control for Scala (mirrors SeqControl)
 */
object SeqScalaControl {

  def aSeq[T](b: => Promise[T]) = new SeqScalaBuilder[T](SeqControl.aSeq(new ACallable[T] {
    def call(): Promise[T] = b
  }))

  def aSeqLoopFair(b: => Promise[java.lang.Boolean]) = SeqControl.aSeqLoopFair(new ACallable[java.lang.Boolean] {
    def call(): Promise[java.lang.Boolean] = b
  })

  def aSeqMaybeLoopFair[T](b: => Promise[Maybe[T]]) = SeqControl.aSeqMaybeLoopFair(new ACallable[Maybe[T]] {
    def call(): Promise[Maybe[T]] = b
  })

  def aSeqOptionLoopFair[T](b: => Promise[Option[T]]) = SeqControl.aSeqMaybeLoopFair(new ACallable[Maybe[T]] {
    def call(): Promise[Maybe[T]] = b.flatMap { v => DataConversions.fromOption(v)}
  })

  def aSeqLoop(b: => Promise[java.lang.Boolean]) = SeqControl.aSeqLoop(new ACallable[java.lang.Boolean] {
    def call(): Promise[java.lang.Boolean] = b
  })

  def aSeqMaybeLoop[T](b: => Promise[Maybe[T]]) = SeqControl.aSeqMaybeLoop(new ACallable[Maybe[T]] {
    def call(): Promise[Maybe[T]] = b
  })

  def aSeqOptionLoop[T](b: => Promise[Option[T]]) = SeqControl.aSeqMaybeLoop(new ACallable[Maybe[T]] {
    def call(): Promise[Maybe[T]] = b.flatMap { v => DataConversions.fromOption(v)}
  })

  def aSeqForUnitJava[T](c: java.lang.Iterable[T])(body: T => BooleanPromise) = SeqControl.aSeqForUnit(c,
    CoreFunctionConversions.toAFunction(body))

  def aSeqForUnitFairJava[T](c: java.lang.Iterable[T])(body: T => BooleanPromise) = SeqControl.aSeqForUnitFair(c,
    CoreFunctionConversions.toAFunction(body))

  def aSeqForUnit[T](c: Iterable[T])(body: T => BooleanPromise) = aSeqForUnitJava(asJavaIterable(c))(body)

  def aSeqForUnitFair[T](c: Iterable[T])(body: T => BooleanPromise) = aSeqForUnitFairJava(asJavaIterable(c))(body)


  class SeqScalaBuilder[T](val builder: SeqBuilder[T]) {

    def map[X](b: T => Promise[X]) = new SeqScalaBuilder[X](builder.map(CoreFunctionConversions.toAFunction(b)))

    def mapLast[X](b: T => Promise[X]) = builder.mapLast(CoreFunctionConversions.toAFunction(b))

    def finish() = builder.finish()

    def failed(body: PartialFunction[Throwable, Promise[T]]) = {
      new SeqScalaBuilder[T](builder.failed(new AFunction[T, Throwable] {
        def apply(value: Throwable): Promise[T] = {
          body.applyOrElse(value, (t: Throwable) => aFailure(t))
        }
      }))
    }

    def failedLast(body: PartialFunction[Throwable, Promise[T]]) = failed(body).finish()

    def thenDo[X](b: => Promise[X]) = new SeqScalaBuilder[X](builder.thenDo(new ACallable[X] {
      def call(): Promise[X] = b
    }))

    def thenDoLast[X](b: => Promise[X]) = builder.thenDoLast(new ACallable[X] {
      def call(): Promise[X] = b
    })

    def finallyDo[X](b: => Promise[X]) = builder.finallyDo(new ACallable[X] {
      def call(): Promise[X] = b
    })
  }

}
