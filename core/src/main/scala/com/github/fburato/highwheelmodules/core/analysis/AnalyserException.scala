package com.github.fburato.highwheelmodules.core.analysis

case class AnalyserException(msg: String, cause: Exception) extends RuntimeException(msg, cause)

object AnalyserException {
  def apply(msg: String): AnalyserException = AnalyserException(msg, null)

  def apply(cause: Exception): AnalyserException = AnalyserException(null, cause)
}
