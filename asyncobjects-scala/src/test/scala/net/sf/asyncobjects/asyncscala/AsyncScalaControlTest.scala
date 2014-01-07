package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.asyncscala.AsyncScalaControl._
import org.junit.Assert._
import org.junit.Test

/**
 * The test for AsyncScala
 */
class AsyncScalaControlTest {
  @Test
  def testSimple() {
    val rc = doAsync {
      42
    }
    assertEquals(42, rc)
  }

  @Test
  def testSimpleBoolean() {
    val rc = doAsync {
      true
    }
    assertEquals(java.lang.Boolean.TRUE, rc)
  }


  @Test
  def testSimpleLater() {
    val rc = doAsync {
      aLater {
        42
      }
    }
    assertEquals(42, rc)
  }

  @Test
  def testThrowLater() {
    try {
      doAsync {
        aLater {
          throw new IllegalStateException("Test")
        }
      }
      fail("the doAsync should throw an exception!")
    } catch {
      case ex: IllegalStateException =>
        assertEquals("Test", ex.getMessage)
    }
  }

}
