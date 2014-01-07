package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.core.{Promise, ACallable, AFunction, Outcome, AResolver}
import net.sf.asyncobjects.core.util.{AFunction3, AFunction2}

/**
 * A conversion functions.
 */
object CoreFunctionConversions {

  implicit def toACallable[T](f: () => Promise[T]): ACallable[T] = new ACallable[T] {
    def call(): Promise[T] = f()
  }

  implicit def toAFunction[T, A](f: (A) => Promise[T]): AFunction[T, A] = new AFunction[T, A] {
    def apply(arg: A): Promise[T] = f(arg)
  }

  implicit def toAFunction2[T, A1, A2](f: (A1, A2) => Promise[T]): AFunction2[T, A1, A2] = new AFunction2[T, A1, A2] {
    def apply(arg1: A1, arg2: A2): Promise[T] = f(arg1, arg2)
  }

  implicit def toAFunction3[T, A1, A2, A3](f: (A1, A2, A3) => Promise[T]): AFunction3[T, A1, A2, A3] = {
    new AFunction3[T, A1, A2, A3] {
      def apply(arg1: A1, arg2: A2, arg3: A3): Promise[T] = f(arg1, arg2, arg3)
    }
  }

  implicit def toAResolver[T](f: (Outcome[T]) => Unit): AResolver[T] = new AResolver[T] {
    def resolve(resolution: Outcome[T]): Unit = f(resolution)
  }
}
