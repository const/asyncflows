package net.sf.asyncobjects.asyncscala

import net.sf.asyncobjects.core.data.Maybe

/**
 * The maybe utility class
 */
object DataConversions {
  implicit def toOption[T](m: Maybe[T]): Option[T] = if (m.hasValue) Some(m.value()) else None

  def fromOption[T](m: Option[T]): Maybe[T] = if (m.isEmpty) Maybe.empty() else Maybe.value(m.get)
}
