package com.github.fburato.highwheelmodules.utils

import scala.util.Try

object TryUtils {
  def sequence[T](xs: Seq[Try[T]]): Try[Seq[T]] = xs.foldLeft(Try(Seq[T]())) {
    (a, b) => a flatMap (c => b map (d => c :+ d))
  }
}
