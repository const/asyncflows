package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.core.util.RequestQueue
import net.sf.asyncobjects.core.{ACallable, Promise}
import net.sf.asyncobjects.core.data.Maybe
import RichPromise._
import AsyncScalaControl._


/**
 * The wrapped version of request queue.
 */
class RequestQueueScala {
  private val requests = new RequestQueue

  def resume() = requests.resume()

  def suspend() = requests.suspend()

  def suspendThenTrue() = requests.suspendThenTrue()

  def suspendThenEmpty() = requests.suspendThenEmpty()

  def suspendThenNone[T](): Promise[Option[T]] = requests.suspend().thenValue(None)

  def run[T](b: => Promise[T]) = requests.run(new ACallable[T] {
    def call(): Promise[T] = b
  })

  def runSeqLoopFair(b: => Promise[java.lang.Boolean]) = requests.runSeqLoopFair(new ACallable[java.lang.Boolean] {
    def call(): Promise[java.lang.Boolean] = b
  })

  def runSeqMaybeLoopFair[T](b: => Promise[Maybe[T]]) = requests.runSeqMaybeLoopFair(new ACallable[Maybe[T]] {
    def call(): Promise[Maybe[T]] = b
  })

  def runSeqOptionLoopFair[T](b: => Promise[Option[T]]) = requests.runSeqMaybeLoopFair(new ACallable[Maybe[T]] {
    def call(): Promise[Maybe[T]] = b.flatMap { v => DataConversions.fromOption(v)}
  })

  def runSeqLoop(b: => Promise[java.lang.Boolean]) = requests.runSeqLoop(new ACallable[java.lang.Boolean] {
    def call(): Promise[java.lang.Boolean] = b
  })

  def runSeqMaybeLoop[T](b: => Promise[Maybe[T]]) = requests.runSeqMaybeLoop(new ACallable[Maybe[T]] {
    def call(): Promise[Maybe[T]] = b
  })

  def runSeqOptionLoop[T](b: => Promise[Option[T]]) = requests.runSeqMaybeLoop(new ACallable[Maybe[T]] {
    def call(): Promise[Maybe[T]] = b.flatMap { v => DataConversions.fromOption(v)}
  })
}
