package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.core.util.AllControl.AllBuilder
import net.sf.asyncobjects.core.util.AllControl.AllBuilder.{AllBuilder2, AllBuilder3}
import net.sf.asyncobjects.core.{ACallable, Promise}
import net.sf.asyncobjects.core.util.AllControl
import AsyncScalaControl._

/**
 * The Scala counterparts for AllControl. It uses Scala tuples as a return type.
 */
object AllScalaControl {

  def aAll[T](b: => Promise[T]) = new AllScalaBuilder[T](AllControl.aAll[T](new ACallable[T] {
    def call() = b
  }))

  class AllScalaBuilder[T1](val builder: AllBuilder[T1]) {
    def and[T2](b: => Promise[T2]) = new AllScalaBuilder2[T1, T2](builder.and(new ACallable[T2] {
      def call(): Promise[T2] = b
    }))

    def andLast[T2](b: => Promise[T2]) = and(b).finish()
  }

  class AllScalaBuilder2[T1, T2](val builder: AllBuilder2[T1, T2]) {
    def and[T3](b: => Promise[T3]) = new AllScalaBuilder3[T1, T2, T3](builder.and(new ACallable[T3] {
      def call(): Promise[T3] = b
    }))

    def andLast[T3](b: => Promise[T3]) = and(b).finish()

    def finish() = unzip((a, b) => (a, b))

    def selectValue1() = builder.selectValue1()

    def selectValue2() = builder.selectValue2()

    def unzip[R](f: (T1, T2) => Promise[R]): Promise[R] = builder.unzip(CoreFunctionConversions.toAFunction2(f))
  }

  class AllScalaBuilder3[T1, T2, T3](val builder: AllBuilder3[T1, T2, T3]) {
    def finish() = unzip((a, b, c) => (a, b, c))

    def selectValue1() = builder.selectValue1()

    def selectValue2() = builder.selectValue2()

    def selectValue3() = builder.selectValue3()

    def unzip[R](f: (T1, T2, T3) => Promise[R]): Promise[R] = builder.unzip(CoreFunctionConversions.toAFunction3(f))
  }

}
