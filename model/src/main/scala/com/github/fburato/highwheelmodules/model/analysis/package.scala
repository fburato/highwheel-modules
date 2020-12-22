package com.github.fburato.highwheelmodules.model

package object analysis {

  sealed trait AnalysisModeS

  case object STRICT extends AnalysisModeS

  case object LOOSE extends AnalysisModeS

}
