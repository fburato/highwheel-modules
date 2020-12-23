package com.github.fburato.highwheelmodules.utils

case class Pair[F, S](first: F, second: S) {
  override def toString: String = s"($first,$second)"
}
