package net.sf.asyncobjects.asyncscala

import AllScalaControl._
import ScalaStreams._
import org.junit.Assert._
import org.junit.Test
import AsyncScalaControl._
import scala.collection.mutable.ArrayBuffer

/**
 * The test for the stream
 */
class ScalaStreamTest {
  @Test
  def test() {
    val result = doAsync {
      aAll {
        aForRange(0, 5).map { v =>
          aLater(v + 5)
        }.pull.window(3).filter {
          v => (v & 1) == 0
        }.flatMapIterator { v =>
          val rc = new ArrayBuffer[Int]
          for (i <- 0 to v) {
            rc += v
          }
          rc.iterator
        }.toList
      }.andLast {
        aForIterable(0 to 5).push.window(10).map {
          v => aLater(v + 5)
        }.filter {
          v => (v & 1) == 0
        }.flatMapIterator { v =>
          val rc = new ArrayBuffer[Int]
          for (i <- 0 to v) {
            rc += v
          }
          rc.iterator
        }.window(3).pull.push.toList
      }
    }
    assertEquals(result._1, result._2)
  }
}
