package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.core.Outcome


import AsyncScalaControl._
import org.junit.Test
import org.junit.Assert._
import scala.collection.mutable.ListBuffer
import ScalaStreams._
import SeqScalaControl._
import SeqScalaTest._
import net.sf.asyncobjects.core.vats.Vat

/**
 * The tests for sequential scala
 */
class SeqScalaTest {
  @Test
  def testSeq() {
    val list = new ListBuffer[Integer]
    val rc: Int = doAsync {
      aSeq {
        list += 1
        1
      } map { value =>
        list += value + 1
        throw new IllegalStateException
      } thenDo {
        list += -1
        aValue(-1)
      } failed {
        case value: IllegalStateException =>
          list += 3
          42
      } finallyDo {
        list += 4
      }
    }

    assertEquals(42, rc)
    assertEquals(List(1, 2, 3, 4), list)
  }

  @Test
  def testSeqSimple() {
    val test = doAsync {
      aSeq {
        42
      } map { value =>
        aLater {
          "The answer is " + value
        }
      } finallyDo {
        24
      }
    }
    assertEquals("The answer is 42", test)
  }

  @Test
  def testSeqFailed() {
    val test = doAsync {
      (aSeq {
        aFailure[Int](new IllegalStateException("test"))
      } failedLast {
        case ex => 42
      }).toOutcomePromise
    }
    assertEquals(Outcome.success(42), test)
    val test2 = doAsync {
      (aSeq[Integer] {
        aFailure(new IllegalStateException("1"))
      } failedLast {
        case ex => throw new IllegalStateException("2")
      }).toOutcomePromise
    }
    assertFailureMessage("2", test2)
  }


  @Test
  def testSeqFinallyFailed() {
    val test = doAsync {
      (aSeq {
        aFailure[Integer](new IllegalStateException("test"))
      } finallyDo {
        aVoid()
      }).toOutcomePromise
    }
    assertFailureMessage("test", test)
    val test2 = doAsync {
      (aSeq[Integer] {
        throw new IllegalStateException("test")
      } finallyDo {
        aSpawn(Vat.current) {
          throw new IllegalStateException("finally")
        }
      }).toOutcomePromise
    }
    assertFailureMessage("test", test2)
    val test3 = doAsync {
      (aSeq {
        aValue(42)
      } finallyDo {
        throw new IllegalStateException("finally")
      }).toOutcomePromise
    }
    assertFailureMessage("finally", test3)
  }

  @Test
  def testSeqForLoop() {
    val rc: Int = doAsync {
      aForSeq(0, 1, 2, 3, 4).leftFold(0) { (result, item) =>
        result + item
      }
    }
    assertEquals(10, rc)
  }

  @Test
  def testSeqLoopFail() {
    var flag = true
    val rc = doAsync {
      aSeqLoop {
        if (flag) {
          flag = false
          true
        } else {
          aLater {
            throw new IllegalStateException("failed")
          }
        }
      }.toOutcomePromise
    }
    assertFailureMessage("failed", rc)
  }
}

object SeqScalaTest {
  /**
   * Check if failure is an exception and has a required method
   *
   * @param message the message
   * @param outcome the outcome to check
   */
  private def assertFailureMessage(message: String, outcome: Outcome[_]) {
    assertEquals(message, outcome.failure.getMessage)
  }
}