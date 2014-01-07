package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.core.{ACallable, AsyncControl}
import net.sf.asyncobjects.core.Promise
import net.sf.asyncobjects.core.vats.Vat

/**
 * The scala control constructs. The methods mostly delegate to AsyncControl.
 * This class is needed just because Scala does not yt support automatic conversion
 * of functions to SAM, and with implicits the functions looks quite terrible in IDEA.
 */
object AsyncScalaControl {
  /**
   * For compatibility with java library, this promise type should be used for boolean promises
   */
  type BooleanPromise = Promise[java.lang.Boolean]

  implicit def aValue[T](value: T) = AsyncControl.aValue(value)

  def aFailure[T](t: Throwable) = AsyncControl.aFailure[T](t)

  def aTrue() = AsyncControl.aTrue()

  def aFalse() = AsyncControl.aFalse()

  implicit def aBoolean(value: Boolean) = AsyncControl.aBoolean(value)

  def aVoid() = AsyncControl.aVoid()

  def aNull() = AsyncControl.aNull()

  def aUnit() = aVoid()

  def aLater[T](b: => Promise[T]) = AsyncControl.aLater(new ACallable[T]() {
    def call(): Promise[T] = b
  })

  def aSpawn[T](vat: Vat)(b: => Promise[T]) = AsyncControl.aLater(vat, new ACallable[T]() {
    def call(): Promise[T] = b
  })

  def aNow[T](b: => Promise[T]) = AsyncControl.aNow(new ACallable[T]() {
    def call(): Promise[T] = b
  })

  def aSend[T](b: => Unit) = AsyncControl.aSend(new Runnable() {
    def run(): Unit = b
  })

  def aSend[T](vat: Vat)(b: => Unit) = AsyncControl.aSend(vat, new Runnable {
    def run(): Unit = b
  })

  def doAsync[T](b: => Promise[T]): T = AsyncControl.doAsyncThrowable(new ACallable[T] {
    def call(): Promise[T] = b
  })
}
