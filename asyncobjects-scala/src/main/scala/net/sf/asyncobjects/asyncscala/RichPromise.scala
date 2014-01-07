package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.core.{Outcome, ACallable, Promise}

/**
 * The promise utility class that adds some methods to the Promise. The names are different because
 * method added via implicit has to be named differently.
 */
class RichPromise[T](val promise: Promise[T]) {

  def flatMap[R](f: T => Promise[R]): Promise[R] = promise.map(CoreFunctionConversions.toAFunction(f))

  def thenAction[R](f: => Promise[R]): Promise[R] = promise.thenDo(new ACallable[R] {
    def call(): Promise[R] = f
  })

  def toUnit = promise.toVoid

  def listenWith(a: Outcome[T] => Unit) = promise.listen(CoreFunctionConversions.toAResolver(a))

  def observeWith(a: Outcome[T] => Unit) = {
    listenWith(a)
    promise
  }
}


object RichPromise {
  implicit def toRichPromise[T](p: Promise[T]) = new RichPromise[T](p)
}